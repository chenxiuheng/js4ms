package org.js4ms.reflector;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MulticastReflectorFactory.java [org.js4ms.jsdk:reflector]
 * %%
 * Copyright (C) 2009 - 2014 Cisco Systems, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import gov.nist.core.Host;
import gov.nist.javax.sdp.fields.AttributeField;
import gov.nist.javax.sdp.fields.ConnectionAddress;
import gov.nist.javax.sdp.fields.ConnectionField;
import gov.nist.javax.sdp.fields.OriginField;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sdp.Attribute;
import javax.sdp.Connection;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.Origin;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;

import org.js4ms.common.util.logging.Log;
import org.js4ms.io.stream.FixedLengthInputStream;
import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.message.Method;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Status;
import org.js4ms.rtsp.message.RtspMethod;
import org.js4ms.rtsp.message.RtspStatusCode;
import org.js4ms.rtsp.presentation.Presentation;
import org.js4ms.rtsp.presentation.PresentationResolver;
import org.js4ms.rtsp.server.RtspService;




/**
 * 
 * 
 *
 * @author gbumgard
 */
public class MulticastReflectorFactory implements PresentationResolver {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(MulticastReflectorFactory.class.getName());


    public static final String SOURCE_FILTER_SDP_ATTRIBUTE = "source-filter";
    public static final String AMT_RELAY_DISCOVERY_ADDRESS_SDP_ATTRIBUTE = "x-amt-relay-discovery-address";
    public static final String SDP_URL_QUERY_PARAMETER = "sdp_url";
    public static final String SOURCE_ADDRESS_QUERY_PARAMETER = "source_address";
    public static final String RELAY_ADDRESS_QUERY_PARAMETER = "relay_address";

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    public final Log log = new Log(this);

    /**
     * The last presentation constructed by the current thread.
     * Provides for simple caching of presentations.
     * Use to avoid repeated construction of a Presentation for the same URI
     * over a sequence of OPTIONS, DESCRIBE and SETUP requests.
     * Since presentations are stateful, the presentation must be removed from the
     * cache once it is passed to a session for processing.
     */
    protected ThreadLocal<Presentation> lastPresentation = new ThreadLocal<Presentation>();

    /**
     * 
     * @param mode
     */
    public MulticastReflectorFactory() {
    }

    /**
     * 
     */
    @Override
    public Presentation getPresentation(Request request) throws RequestException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("getPresentation", request));
        }

        URI requestUri = request.getRequestLine().getUri();
        String requestUriString = requestUri.toString();

        Method method = request.getRequestLine().getMethod();

        if (method.equals(RtspMethod.SETUP)) {
            // The request URI is a stream control URI.
            // The control URI is the presentation URL (Content-Base) concatenated
            // to the stream control attribute as specified in the SDP.
            // Strip stream control attribute from end of control URL (e.g. '/trackID=0')
            // TODO what if there isn't a stream ID because there is only one stream?
            requestUriString = requestUriString.substring(0, requestUriString.lastIndexOf("/"));
        }

        URI presentationUri = URI.create(requestUriString);

        logger.finer(log.msg("looking for presentation URI="+presentationUri));

        Presentation presentation = this.lastPresentation.get();

        // Have we cached a presentation instance for this URI?
        if (presentation != null) {
            logger.finer(log.msg("current cached presentation is URI="+presentation.getUri().toString()));
            if (presentation.getUri().equals(presentationUri)) {
                logger.finer(log.msg("using cached presentation"));
                if (method.equals(RtspMethod.SETUP)) {
                    // The presentation cannot be reused once referenced in a SETUP request.
                    // Remove the presentation from the cache
                    logger.finer(log.msg("removing presentation from cache"));
                    this.lastPresentation.set(null);
                }
                return presentation;
            }
        }

        // There is no matching presentation in the cache so we must construct a new presentation
        logger.finer(log.msg("constructing presentation for URI="+presentationUri.toString()));

        String queryString = presentationUri.getQuery();

        if (queryString == null || queryString.length() == 0) {
            throw RequestException.create(request.getProtocolVersion(),
                                          RtspStatusCode.NotFound,
                                          "the RTSP URL for a reflected presentation must include a query string describing the stream source",
                                          log.getPrefix(),
                                          logger);
        }

        HashMap<String,String> parameterMap = new HashMap<String,String>();

        String[] parameters = queryString.split("&");
        for (String parameter : parameters) {
            String[] pair = parameter.split("=");
            if (pair.length == 2) {
                String name = pair[0].toLowerCase();
                String value = pair[1];
                parameterMap.put(name,value);
            }
        }
 
        if (!parameterMap.containsKey(SDP_URL_QUERY_PARAMETER)) {
            throw RequestException.create(request.getProtocolVersion(),
                                          RtspStatusCode.NotFound,
                                          "the RTSP URL must include an 'sdp_url' query parameter that can be resolved to fetch an SDP description.",
                                          log.getPrefix(),
                                          logger);
        }

        URI sdpUri = null;

        try {
            sdpUri = new URI(java.net.URLDecoder.decode(parameterMap.get(SDP_URL_QUERY_PARAMETER).replaceAll(" ","+"), "UTF-8"));
        }
        catch(URISyntaxException e) {
            throw RequestException.create(request.getProtocolVersion(),
                                          RtspStatusCode.BadRequest,
                                          "invalid SDP URL specified in request",
                                          e,
                                          log.getPrefix(),
                                          logger);
        }
        catch (UnsupportedEncodingException e) {
            throw RequestException.create(request.getProtocolVersion(),
                    RtspStatusCode.InternalServerError,
                    "unsupported encoding specified for URL decoder",
                    e,
                    log.getPrefix(),
                    logger);
        }

        if (sdpUri.getScheme() == null) {
            throw RequestException.create(request.getProtocolVersion(),
                    RtspStatusCode.BadRequest,
                    "invalid SDP URL in request - no scheme specified",
                    log.getPrefix(),
                    logger);
        }

        SessionDescription inputSessionDescription = retrieveSessionDescription(sdpUri,parameterMap);
        SessionDescription outputSessionDescription;
        try {
            outputSessionDescription = constructUnicastSessionDescription(inputSessionDescription);
        }
        catch (SdpException e) {
            throw RequestException.create(request.getProtocolVersion(),
                                          RtspStatusCode.InvalidMedia,
                                          "invalid SDP description",
                                          e,
                                          log.getPrefix(),
                                          logger);
        }

        try {
            presentation = new MulticastReflector(presentationUri, inputSessionDescription, outputSessionDescription);
        }
        catch (SdpException e) {
            throw RequestException.create(request.getProtocolVersion(),
                                          RtspStatusCode.InvalidMedia,
                                          "invalid SDP description",
                                          e,
                                          log.getPrefix(),
                                          logger);
        }

        if (!method.equals(RtspMethod.SETUP)) {
            // Cache the presentation if this is a OPTIONS or DESCRIBE request.
            this.lastPresentation.set(presentation);
        }

        return presentation;

    }

    protected SessionDescription retrieveSessionDescription(final URI sdpUri, final HashMap<String,String> parameters) throws RequestException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("retrieveSessionDescription", sdpUri.toString()));
        }
        SessionDescription sessionDescription = retrieveSessionDescription(sdpUri);
        return annotateSessionDescription(sessionDescription, parameters);
    }

    protected SessionDescription retrieveSessionDescription(final URI sdpUri) throws RequestException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("retrieveSessionDescription", sdpUri.toString()));
        }

        // TODO: Add capability to fetch SDP from an RTSP server. Requires RTSP client implementation.

        // Fetch file from web server
        String path = sdpUri.toString();
        if (path != null) {
                                
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(log.msg("fetching SDP from " + path));
            }

            if (sdpUri.getScheme().equals("http")) {
                try {
                    HttpURLConnection urlConnection = ((HttpURLConnection)sdpUri.toURL().openConnection());
                    if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                        // TODO: String lastModified = urlConnection.getHeaderField(Header.Last_Modified);

                        int contentLength = urlConnection.getContentLength();

                        if (contentLength == -1) {
                            // TODO;
                            throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                          RtspStatusCode.BadRequest,
                                                          "fetch from URI specified in request returned an invalid SDP description",
                                                          log.getPrefix(),
                                                          logger);

                        }
                        else {

                            InputStream inputStream = new FixedLengthInputStream(urlConnection.getInputStream(), contentLength);
    
                            try {
                                return retrieveSessionDescription(sdpUri, inputStream);
                            }
                            finally {
                                inputStream.close();
                            }
                        }
                    }
                    else {
                        // GET failed
                        throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                      new Status(urlConnection.getResponseCode(),urlConnection.getResponseMessage()),
                                                      "cannot fetch presentation description - HTTP GET failed",
                                                      log.getPrefix(),
                                                      logger);
                    }
                }
                catch (ConnectException e) {
                    throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                  RtspStatusCode.Forbidden,
                                                  "cannot fetch presentation description - HTTP connection refused",
                                                  e,
                                                  log.getPrefix(),
                                                  logger);
                }
                catch (IOException e) { 
                    throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                  RtspStatusCode.InternalServerError,
                                                  "cannot fetch presentation description - HTTP GET failed with IO exception",
                                                  e,
                                                  log.getPrefix(),
                                                  logger);
                }
            }
            else if (sdpUri.getScheme().equals("file")) {
                try {
                    InputStream inputStream = new FileInputStream(URLDecoder.decode(sdpUri.getSchemeSpecificPart(),"UTF8"));
                    return retrieveSessionDescription(sdpUri, inputStream);
                }
                catch (FileNotFoundException e) {
                    throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                  RtspStatusCode.NotFound,
                                                  "cannot read presentation description - file not found",
                                                  e,
                                                  log.getPrefix(),
                                                  logger);
                }
                catch (IOException e) {
                    throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                  RtspStatusCode.InternalServerError,
                                                  "cannot read presentation description - read failed with IO exception",
                                                  e,
                                                  log.getPrefix(),
                                                  logger);
                }
            }

        }

        logger.fine(log.msg("the URI specified in the request cannot be used to fetch an SDP description"));
        throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                      RtspStatusCode.NotFound,
                                      "cannot read presentation description - file not found",
                                      log.getPrefix(),
                                      logger);
    }

    public SessionDescription retrieveSessionDescription(final URI sdpUri, InputStream inputStream) throws RequestException,
                                                                                                   IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("retrieveSessionDescription", sdpUri.toString(), inputStream));
        }

        StringBuilder sb = new StringBuilder();
        String line;

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\r\n");
        }

        String description = sb.toString();

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(log.msg("----> Server-side SDP"));
            logger.fine(log.msg("\n" + description));
            logger.fine(log.msg("<---- Server-side SDP"));
        }

        try {
            return SdpFactory.getInstance().createSessionDescription(description);
        }
        catch (SdpException e) {
            throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                          RtspStatusCode.BadRequest,
                                          "cannot parse session description",
                                          e,
                                          log.getPrefix(),
                                          logger);
        }
    }

    @SuppressWarnings("unchecked")
    private SessionDescription annotateSessionDescription(SessionDescription sessionDescription,
                                                          final HashMap<String,String> parameters) throws RequestException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("annotateSessionDescription", sessionDescription, parameters));
        }

        // Check request URL for query string parameters supply or override
        // source and relay address attribute values

        InetAddress sourceAddress = null;
        if (parameters.containsKey(SOURCE_ADDRESS_QUERY_PARAMETER)) {
            String value = parameters.get(SOURCE_ADDRESS_QUERY_PARAMETER);
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(log.msg("resolving source address parameter: " + value));
            }
            try {
                sourceAddress = InetAddress.getByName(value);
                // Resolve to check validity of address
                sourceAddress.getHostAddress();
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("successfully resolved source address parameter: " + value));
                }
            }
            catch (Exception e) {
                throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                              RtspStatusCode.BadRequest,
                                              "source address query parameter '" + value + "' is invalid",
                                              e,
                                              log.getPrefix(),
                                              logger);
            }
        }

        InetAddress relayAddress = null;
        if (parameters.containsKey(RELAY_ADDRESS_QUERY_PARAMETER)) {
            String value = parameters.get(RELAY_ADDRESS_QUERY_PARAMETER);
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(log.msg("resolving relay address parameter: " + value));
            }
            try {
                relayAddress = InetAddress.getByName(value);
                // Resolve to check validity of address
                relayAddress.getHostAddress();
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("successfully resolved relay address parameter: " + value));
                }
            }
            catch (Exception e) {
                throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                              RtspStatusCode.BadRequest,
                                              "relay address query parameter '" + value + "' is invalid",
                                              e,
                                              log.getPrefix(),
                                              logger);
            }
        }

        try {
            // Determine whether AMT relay discovery address attribute record
            // must be added.
            if (sessionDescription.getAttribute(AMT_RELAY_DISCOVERY_ADDRESS_SDP_ATTRIBUTE) != null) {

                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("update existing relay discovery address record"));
                }

                // Change attribute
                try {
                    sessionDescription.setAttribute(AMT_RELAY_DISCOVERY_ADDRESS_SDP_ATTRIBUTE, relayAddress.getHostName());
                }
                catch (SdpException e) {
                    throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                  RtspStatusCode.InternalServerError,
                                                  e,
                                                  log.getPrefix(),
                                                  logger);
                }
            }
            else if (relayAddress != null) {

                String relayHostName = relayAddress.getHostAddress();

                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("add relay discovery address record for relay " + relayHostName));
                }

                // Add attribute
                AttributeField xAmtRelayDiscoveryAddress = new AttributeField();
                try {
                    xAmtRelayDiscoveryAddress.setName(AMT_RELAY_DISCOVERY_ADDRESS_SDP_ATTRIBUTE);
                    xAmtRelayDiscoveryAddress.setValue(relayHostName);
                    @SuppressWarnings("rawtypes")
                    Vector attributes = sessionDescription.getAttributes(false);
                    attributes.add(xAmtRelayDiscoveryAddress);
                    sessionDescription.setAttributes(attributes);
                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(log.msg("relay discovery address record added"));
                    }
                }
                catch (SdpException e) {
                    throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                  RtspStatusCode.InternalServerError,
                                                  e,
                                                  log.getPrefix(),
                                                  logger);
                }
            }
            else {
                throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                              RtspStatusCode.BadRequest,
                                              "relay_address query parameter is required for this SDP",
                                              log.getPrefix(),
                                              logger);
            }

            // Determine whether source filter attribute record must be added.
            if (sessionDescription.getAttribute(SOURCE_FILTER_SDP_ATTRIBUTE) == null) {

                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("construct source filter attribute record record"));
                }

                // Verify that a session or media-level connection record
                // specifies a multicast destination address
                Connection connection = sessionDescription.getConnection();
                if (connection == null) {
                    Vector<?> descriptions = sessionDescription.getMediaDescriptions(false);
                    if (descriptions != null) {
                        for (int i = 0; i < descriptions.size(); i++) {
                            MediaDescription mediaDescription = (MediaDescription)descriptions.get(i);
                            connection = mediaDescription.getConnection();
                            if (connection != null) {
                                break;
                            }
                        }
                    }
                }

                if (connection == null) {
                    throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                  RtspStatusCode.BadRequest,
                                                  "SDP does not specify a connection address",
                                                  log.getPrefix(),
                                                  logger);
                }

                ConnectionField connectionField = (ConnectionField)connection;
                ConnectionAddress connectionAddress = connectionField.getConnectionAddress();

                Host host = connectionAddress.getAddress();
                InetAddress groupAddress;

                try {
                    groupAddress = host.getInetAddress();

                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(log.msg("group address is " + groupAddress));
                    }

                }
                catch (UnknownHostException e) {
                    throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                  RtspStatusCode.BadRequest,
                                                  "cannot resolve connection address in media description",
                                                  e,
                                                  log.getPrefix(),
                                                  logger);
                }

                if (!groupAddress.isMulticastAddress()) {
                    throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                  RtspStatusCode.BadRequest,
                                                  "input connection address must be a multicast address",
                                                  log.getPrefix(),
                                                  logger);
                }

                if (sourceAddress == null) {

                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(log.msg("no source address specified - attempting to use origin host as source"));
                    }

                    Origin origin = sessionDescription.getOrigin();

                    if (origin == null) {
                        throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                      RtspStatusCode.BadRequest,
                                                      "source_address query parameter is required for this SDP",
                                                      log.getPrefix(),
                                                      logger);
                    }

                    OriginField originField = (OriginField)origin;
                    String originHost = originField.getAddress();

                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(log.msg("origin host: " + originHost));
                    }

                    try {
                        sourceAddress = InetAddress.getByName(originHost);
                    }
                    catch (UnknownHostException e) {
                        try {
                            // Try it with .local, though that is of
                            // questionable use as it will/should
                            // resolve to an address on the local net.
                            sourceAddress = InetAddress.getByName(originHost + ".local");
                        }
                        catch (UnknownHostException e1) {
                            throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                          RtspStatusCode.BadRequest,
                                                          "source_address query parameter is required because the SDP origin record does not specify valid source",
                                                          e1,
                                                          log.getPrefix(),
                                                          logger);
                        }
                    }
                }

                // Add some information attribute records to show what what
                // we've done
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("add x-origin-source-address attribute"));
                }

                AttributeField originSource = new AttributeField();
                originSource.setName("x-origin-source-address");
                originSource.setValue(sourceAddress.getHostName() + " " + sourceAddress.getHostAddress());

                if (sourceAddress.isLoopbackAddress() || sourceAddress.isLinkLocalAddress()) {
                    try {
                        sourceAddress = getLocalHostIPAddress(sourceAddress instanceof Inet6Address);
                    }
                    catch (UnknownHostException e) {
                        throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                      RtspStatusCode.InternalServerError,
                                                      e,
                                                      log.getPrefix(),
                                                      logger);
                    }
                    catch (SocketException e) {
                        throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                      RtspStatusCode.InternalServerError,
                                                      e,
                                                      log.getPrefix(),
                                                      logger);
                    }
                }

                // Add some information attribute records to show what what
                // we've done
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("add x-filter-source-address"));
                }

                AttributeField filterSource = new AttributeField();
                filterSource.setName("x-filter-source-address");
                filterSource.setValue(sourceAddress.getHostName() + " " + sourceAddress.getHostAddress());

                if (!groupAddress.getClass().equals(sourceAddress.getClass())) {
                    throw new SdpException("the IP address types of the source and group address in the SDP do not match");
                }

                // Add some information attribute records to show what what
                // we've done

                // Add source filter attribute that applies to all groups
                String filterDesc = "incl IN " + ((sourceAddress instanceof Inet4Address) ? "IP4" : "IP6") + " * "
                                    + sourceAddress.getHostAddress();

                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("add source filter record: " + filterDesc));
                }

                AttributeField sourceFilter = new AttributeField();
                sourceFilter.setName(SOURCE_FILTER_SDP_ATTRIBUTE);
                sourceFilter.setValue(filterDesc);
                @SuppressWarnings("rawtypes")
                Vector attributes = sessionDescription.getAttributes(false);
                attributes.add(originSource);
                attributes.add(filterSource);
                attributes.add(sourceFilter);
                sessionDescription.setAttributes(attributes);
            }
        }
        catch (SdpException e) {
            throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                          RtspStatusCode.InternalServerError,
                                          e,
                                          log.getPrefix(),
                                          logger);
        }

        return sessionDescription;
    }

    InetAddress getLocalHostIPAddress(boolean getIpv6) throws UnknownHostException, SocketException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("getLocalHostIPAddress"));
        }

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            NetworkInterface intf = interfaces.nextElement();
            System.out.println("checking interface " + intf.getDisplayName());
            Enumeration<InetAddress> addresses = intf.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("checking interface address" + address.getHostAddress() +
                            " loopback=" + address.isLoopbackAddress() +
                            " link-local=" + address.isLinkLocalAddress() +
                            " site-local=" + address.isSiteLocalAddress()));
                }
                ;
                if ((getIpv6 ? address instanceof Inet6Address : address instanceof Inet4Address) &&
                    !address.isLoopbackAddress() &&
                    !address.isLinkLocalAddress()) {
                    return address;
                }
            }
        }
        return InetAddress.getLocalHost();
    }

    /**
     * Returns a reflector output session description for the specified reflector input session description.
     * @throws SdpException If a unicast representation cannot be generated from the specified multicast description.
     */
    private SessionDescription constructUnicastSessionDescription(final SessionDescription multicastSessionDescription) throws SdpException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("constructUnicastSessionDescription"));
        }


        SessionDescription sessionDescription = null;

        try {
            sessionDescription = (SessionDescription)multicastSessionDescription.clone();
        }
        catch (CloneNotSupportedException e) {
        }

        /*
         * Make session level changes
         */

        // Replace the connection address with a local host unicast address
        Connection connection = sessionDescription.getConnection();
        int addressCount = 1;
        if (connection != null) {
            ConnectionField connectionField = (ConnectionField)connection;
            ConnectionAddress connectionAddress = connectionField.getConnectionAddress();
            Host host = connectionAddress.getAddress();
            InetAddress address;
            try {
                address = host.getInetAddress();
            }
            catch (UnknownHostException e) {
                throw new SdpException("cannot resolve connection address in media description");
            }
            if (!address.isMulticastAddress()) {
                throw new SdpException("input connection address must be a multicast address");
            }

            // Get the connection address count - named "port" in NIST javax.sdp implementation
            addressCount = connectionAddress.getPort();
            if (addressCount == 0) addressCount = 1;

            // Set connection address to wildcard address and set count to 0 (one address).
            connection.setAddress("0.0.0.0");
            connectionAddress.setTtl(0);
            connectionAddress.setPort(0);
        }

        // Remove any source-filter or x-amt-relay-discovery-address attribute records that might exist
        Vector<?> attributes = sessionDescription.getAttributes(false);
        if (attributes != null) {
            Iterator<?> iter = attributes.iterator();
            while (iter.hasNext()) {
                Attribute attribute = (Attribute)iter.next();
                if (attribute.getName().equals(SOURCE_FILTER_SDP_ATTRIBUTE) ||
                    attribute.getName().equals(AMT_RELAY_DISCOVERY_ADDRESS_SDP_ATTRIBUTE)) {
                    iter.remove();
                }
            }
        }

        /*
         * Make media level changes.
         */

        Vector<?> descriptions = sessionDescription.getMediaDescriptions(false);
        if (descriptions != null) {
            for (int i=0; i<descriptions.size(); i++)
            {
                MediaDescription mediaDescription = (MediaDescription)descriptions.get(i);

                // Replace the connection address with a local host unicast address
                connection = mediaDescription.getConnection();
                if (connection != null) {
                    ConnectionField connectionField = (ConnectionField)connection;
                    ConnectionAddress connectionAddress = connectionField.getConnectionAddress();

                    Host host = connectionAddress.getAddress();
                    InetAddress address;
                    try {
                        address = host.getInetAddress();
                    }
                    catch (UnknownHostException e) {
                        throw new SdpException("cannot resolve connection address in media description");
                    }

                    if (!address.isMulticastAddress()) {
                        throw new SdpException("input connection address must be a multicast address");
                    }

                    // Get the connection address count - named "port" in NIST javax.sdp implementation
                    addressCount = connectionAddress.getPort();
                    if (addressCount == 0) addressCount = 1;

                    // Set connection address to wildcard address and set count to 0 (one address).
                    if (address instanceof Inet4Address) {
                        connection.setAddress("0.0.0.0");
                    }
                    else {
                        connection.setAddress("::");
                    }
                    connectionAddress.setTtl(0);
                    connectionAddress.setPort(0);
                }

                Media media = mediaDescription.getMedia();
                media.setMediaPort(0);

                // If there are multiple addresses then the reflector must reflect to multiple ports
                if (addressCount > 1) {
                    if (media.getPortCount() > 1) {
                        throw new SdpException("SDP description of media stream cannot specify multiple addresses AND multiple ports");
                    }
                    media.setPortCount(addressCount);
                }

                // Remove any source-filter or x-amt-relay-anycast attribute records that might exist
                attributes = sessionDescription.getAttributes(false);
                if (attributes != null) {
                    Iterator<?> iter = attributes.iterator();
                    while (iter.hasNext()) {
                        Attribute attribute = (Attribute)iter.next();
                        if (attribute.getName().equals(SOURCE_FILTER_SDP_ATTRIBUTE) ||
                            attribute.getName().equals(AMT_RELAY_DISCOVERY_ADDRESS_SDP_ATTRIBUTE)) {
                            iter.remove();
                        }
                    }
                }
            }

        }

        return sessionDescription;
    }

}
