package com.larkwoodlabs.servlet;
import gov.nist.core.Host;
import gov.nist.javax.sdp.fields.AttributeField;
import gov.nist.javax.sdp.fields.ConnectionAddress;
import gov.nist.javax.sdp.fields.ConnectionField;
import gov.nist.javax.sdp.fields.OriginField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Vector;

import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.Origin;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AmtSdpGeneratorServlet extends HttpServlet {

    static final String SOURCE_FILTER_ATTRIBUTE_NAME = "source-filter";
    static final String RELAY_DISCOVERY_ADDRESS_ATTRIBUTE_NAME = "x-amt-relay-discovery-address";
    static final String SDP_QUERY_PARAMETER = "src_sdp";
    static final String RELAY_QUERY_PARAMETER = "src_relay";

    /**
     * 
     */
    private static final long serialVersionUID = 2387057975465931229L;

    @Override
    public void init(ServletConfig config) throws ServletException
    {
      super.init(config);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String sdpUrl=null;
        String query = request.getQueryString();
        if (query != null && query.length() > 0) {
            String[] parameters = query.split("&");
            for (String parameter : parameters) {
                String[] pair = parameter.split("=");
                if (pair.length == 2) {
                    if (pair[0].toLowerCase().equals(SDP_QUERY_PARAMETER)) {
                        sdpUrl = pair[1];
                        break;
                    }
                }
            }
        }

        if (sdpUrl == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Request must include the query string parameter '"+SDP_QUERY_PARAMETER+"'");
            return;
        }

        try {
            URL url = new URL(sdpUrl);
            HttpURLConnection urlConnection = ((HttpURLConnection)url.openConnection());
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                // TODO: String lastModified = urlConnection.getHeaderField(Header.Last_Modified);

                int contentLength = urlConnection.getContentLength();

                if (contentLength == 0) {
                    response.sendError(HttpServletResponse.SC_NO_CONTENT, "The specified SDP file is empty");
                }
                else {

                    InputStream inputStream = urlConnection.getInputStream();

                    try {

                        StringBuilder sb = new StringBuilder();
                        String line;

                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\r\n");
                        }

                        try {
                            SessionDescription sessionDescription = SdpFactory.getInstance().createSessionDescription(sb.toString());
                            generateSDP(sessionDescription, request, response);
                        }
                        catch (SdpParseException e) {
                            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Cannot parse SDP file -" + e.getMessage());
                        }


                    }
                    finally {
                        inputStream.close();
                    }
                }
            }
            else {
                response.sendError(urlConnection.getResponseCode(), urlConnection.getResponseMessage());
            }
        }
        catch (ConnectException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
        catch (IOException e) { 
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    void generateSDP(SessionDescription sessionDescription, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String relayAddress=null;
        String query = request.getQueryString();
        if (query != null && query.length() > 0) {
            String[] parameters = query.split("&");
            for (String parameter : parameters) {
                String[] pair = parameter.split("=");
                if (pair.length == 2) {
                    if (pair[0].toLowerCase().equals(RELAY_QUERY_PARAMETER)) {
                        relayAddress = pair[1];
                    }
                }
            }
        }

        if (relayAddress == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Request must include the query string parameter '"+RELAY_QUERY_PARAMETER+"'");
            return;
        }

        try {
            InetAddress address = InetAddress.getByName(relayAddress);
            address.getHostAddress();
            relayAddress = address.getHostName();
        }
        catch(Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,"The relay address '"+relayAddress+"' is invalid - "+e.getClass().getSimpleName()+": "+e.getMessage());
            return;
        }
        
        try {
            // Add or update the relay discovery attribute record
            if (sessionDescription.getAttribute(RELAY_DISCOVERY_ADDRESS_ATTRIBUTE_NAME) != null) {
                // Change attribute value
                sessionDescription.setAttribute(RELAY_DISCOVERY_ADDRESS_ATTRIBUTE_NAME, relayAddress);
            }
            else {
                // Add attribute
                AttributeField xAmtRelayDiscoveryAddress = new AttributeField();
                xAmtRelayDiscoveryAddress.setName(RELAY_DISCOVERY_ADDRESS_ATTRIBUTE_NAME);
                xAmtRelayDiscoveryAddress.setValue(relayAddress);
                Vector attributes = sessionDescription.getAttributes(false);
                attributes.add(xAmtRelayDiscoveryAddress);
                sessionDescription.setAttributes(attributes);
            }

            if (sessionDescription.getAttribute(SOURCE_FILTER_ATTRIBUTE_NAME) == null) {

                // Verify that a session or media-level connection record specifies a multicast destination address
                Connection connection = sessionDescription.getConnection();
                if (connection == null) {
                    Vector<?> descriptions = sessionDescription.getMediaDescriptions(false);
                    if (descriptions != null) {
                        for (int i=0; i<descriptions.size(); i++)
                        {
                            MediaDescription mediaDescription = (MediaDescription)descriptions.get(i);

                            connection = mediaDescription.getConnection();
                            if (connection != null) {
                                break;
                            }
                        }
                    }
                }

                if (connection == null) {
                    response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "SDP file does not specify a connection address");
                    return;
                }

                ConnectionField connectionField = (ConnectionField)connection;
                ConnectionAddress connectionAddress = connectionField.getConnectionAddress();

                Host host = connectionAddress.getAddress();
                InetAddress groupAddress;
                try {
                    groupAddress = host.getInetAddress();
                }
                catch (UnknownHostException e) {
                    response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "SDP file does not specify a resolvable connection address");
                    return;
                }

                if (!groupAddress.isMulticastAddress()) {
                    response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "SDP file does not specify a multicast connection address");
                    return;
                }

                // Add or update the relay discovery attribute record
                Origin origin = sessionDescription.getOrigin();
                if (origin == null) {
                    response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "SDP file does not specify an origin");
                    return;
                }

                OriginField originField = (OriginField)origin;
                InetAddress sourceAddress = InetAddress.getByName(originField.getAddress());

                if (!groupAddress.getClass().equals(sourceAddress.getClass())) {
                    response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "the IP address types of the source and group address do not match");
                    return;
                }

                // Add source filter attribute that applies to all groups
                String filterDesc = "incl IN " + ((sourceAddress instanceof Inet4Address) ? "IP4" : "IP6") + " * " + sourceAddress.getHostAddress();
                AttributeField sourceFilter = new AttributeField();
                sourceFilter.setName(SOURCE_FILTER_ATTRIBUTE_NAME);
                sourceFilter.setValue(filterDesc);
                Vector attributes = sessionDescription.getAttributes(false);
                attributes.add(sourceFilter);
                sessionDescription.setAttributes(attributes);
            }
        } catch (SdpException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        //new SdpEncoderImpl().output(sessionDescription, response.getOutputStream());
        response.setContentType("application/sdp");
        response.getWriter().write(sessionDescription.toString());
        return;
    }
}
