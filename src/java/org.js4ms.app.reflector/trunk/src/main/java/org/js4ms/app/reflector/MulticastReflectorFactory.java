package org.js4ms.app.reflector;

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
import org.js4ms.io.streams.FixedLengthInputStream;
import org.js4ms.service.protocol.rest.RequestException;
import org.js4ms.service.protocol.rest.message.Method;
import org.js4ms.service.protocol.rest.message.Request;
import org.js4ms.service.protocol.rest.message.Status;
import org.js4ms.service.protocol.rtsp.RtspMethods;
import org.js4ms.service.protocol.rtsp.RtspStatusCodes;
import org.js4ms.service.protocol.rtsp.presentation.Presentation;
import org.js4ms.service.protocol.rtsp.presentation.PresentationResolver;
import org.js4ms.service.protocol.rtsp.server.RtspService;




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
        String presentationUri = requestUri.toString();

        // The RTSP URL must carry a query string consisting of an SDP URL.
        String sdpUrl = requestUri.getQuery();
 
        if (sdpUrl == null || sdpUrl.length() == 0) {
            throw RequestException.create(request.getProtocolVersion(),
                                          RtspStatusCodes.NotFound,
                                          "the RTSP URL for a reflected presentation must include a query string containing an SDP URL",
                                          log.getPrefix(),
                                          logger);
        }

        Method method = request.getRequestLine().getMethod();

        if (method.equals(RtspMethods.SETUP)) {
            // The request URI is a stream control URI.
            // The control URI is the presentation URL (Content-Base) concatenated
            // to the stream control attribute as specified in the SDP.
            // Strip stream control attribute from end of control URL (e.g. '/trackID=0')
            // TODO what if there isn't a stream ID because there is only one stream?
            sdpUrl = sdpUrl.substring(0, sdpUrl.lastIndexOf("/"));
            presentationUri = presentationUri.substring(0, presentationUri.lastIndexOf("/"));
        }

        URI sdpUri = null;

        try {
            sdpUri = new URI(sdpUrl.replaceAll(" ","+"));
        }
        catch(URISyntaxException e) {
            throw RequestException.create(request.getProtocolVersion(),
                                          RtspStatusCodes.BadRequest,
                                          "invalid URI specified in request",
                                          e,
                                          log.getPrefix(),
                                          logger);
        }

        logger.finer(log.msg("looking for presentation URI="+presentationUri));

        Presentation presentation = this.lastPresentation.get();

        // Have we cached a presentation instance for this URI?
        if (presentation != null) {
            logger.finer(log.msg("current cached presentation is URI="+presentation.getUri().toString()));
            if (presentation.getUri().toString().equals(presentationUri)) {
                logger.finer(log.msg("using cached presentation"));
                if (method.equals(RtspMethods.SETUP)) {
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

        SessionDescription inputSessionDescription = retrieveSessionDescription(sdpUri);
        SessionDescription outputSessionDescription;
        try {
            outputSessionDescription = constructUnicastSessionDescription(inputSessionDescription);
        }
        catch (SdpException e) {
            throw RequestException.create(request.getProtocolVersion(),
                                          RtspStatusCodes.InvalidMedia,
                                          "invalid SDP description",
                                          e,
                                          log.getPrefix(),
                                          logger);
        }

        try {
            presentation = new MulticastReflector(new URI(presentationUri), inputSessionDescription, outputSessionDescription);
        }
        catch (URISyntaxException e) {
            throw RequestException.create(request.getProtocolVersion(),
                                          RtspStatusCodes.BadRequest,
                                          "invalid URI specified in request",
                                          e,
                                          log.getPrefix(),
                                          logger);
        }
        catch (SdpException e) {
            throw RequestException.create(request.getProtocolVersion(),
                                          RtspStatusCodes.InvalidMedia,
                                          "invalid SDP description",
                                          e,
                                          log.getPrefix(),
                                          logger);
        }

        if (!method.equals(RtspMethods.SETUP)) {
            // Cache the presentation if this is a OPTIONS or DESCRIBE request.
            this.lastPresentation.set(presentation);
        }

        return presentation;

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
                                                          RtspStatusCodes.BadRequest,
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
                                                  RtspStatusCodes.Forbidden,
                                                  "cannot fetch presentation description - HTTP connection refused",
                                                  e,
                                                  log.getPrefix(),
                                                  logger);
                }
                catch (IOException e) { 
                    throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                  RtspStatusCodes.InternalServerError,
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
                                                  RtspStatusCodes.NotFound,
                                                  "cannot read presentation description - file not found",
                                                  e,
                                                  log.getPrefix(),
                                                  logger);
                }
                catch (IOException e) {
                    throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                                  RtspStatusCodes.InternalServerError,
                                                  "cannot read presentation description - read failed with IO exception",
                                                  e,
                                                  log.getPrefix(),
                                                  logger);
                }
            }

        }

        logger.fine(log.msg("the URI specified in the request cannot be used to fetch an SDP description"));
        throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                      RtspStatusCodes.NotFound,
                                      "cannot read presentation description - file not found",
                                      log.getPrefix(),
                                      logger);
    }

    public SessionDescription retrieveSessionDescription(final URI sdpUri, InputStream inputStream) throws RequestException, IOException {

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
            logger.fine(log.msg("\n"+description));
            logger.fine(log.msg("<---- Server-side SDP"));
        }

        try {
            SessionDescription sessionDescription = SdpFactory.getInstance().createSessionDescription(description);
            return annotateSessionDescription(sdpUri, sessionDescription);
        }
        catch (SdpException e) {
            throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                          RtspStatusCodes.BadRequest,
                                          "cannot parse session description",
                                          e,
                                          log.getPrefix(),
                                          logger);
        }
        
    }

    @SuppressWarnings("unchecked")
    private SessionDescription annotateSessionDescription(final URI sdpUri, SessionDescription sessionDescription) throws RequestException, SdpException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("annotateSessionDescription", sdpUri.toString()));
        }
        
        // Check request URL for query string parameters supply or override source and relay address attribute values
        String query = sdpUri.getQuery();
        InetAddress sourceAddress = null;
        InetAddress relayAddress = null;
        if (query != null && query.length() > 0) {
            String[] parameters = query.split("&");
            for (String parameter : parameters) {
                String[] pair = parameter.split("=");
                if (pair.length == 2) {
                    String name = pair[0].toLowerCase();
                    String value = pair[1];
                    if (name.equals(SOURCE_ADDRESS_QUERY_PARAMETER)) {
                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer(log.msg("resolving source address parameter: "+value));
                        }
                        try {
                            sourceAddress = InetAddress.getByName(value);
                            // Resolve to check validity of address
                            sourceAddress.getHostAddress();
                            if (logger.isLoggable(Level.FINER)) {
                                logger.finer(log.msg("successfully resolved source address parameter: "+value));
                            }
                        }
                        catch (Exception e) {
                            throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                    RtspStatusCodes.BadRequest,
                                    "source address query parameter '" + value + "' is invalid",
                                    e,
                                    log.getPrefix(),
                                    logger);
                        }
                    }
                    else if (name.equals(RELAY_ADDRESS_QUERY_PARAMETER)) {
                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer(log.msg("resolving relay address parameter: "+value));
                        }
                        try {
                            relayAddress = InetAddress.getByName(value);
                            // Resolve to check validity of address
                            relayAddress.getHostAddress();
                            if (logger.isLoggable(Level.FINER)) {
                                logger.finer(log.msg("successfully resolved relay address parameter: "+value));
                            }
                        }
                        catch (Exception e) {
                            throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                    RtspStatusCodes.BadRequest,
                                    "relay address query parameter '" + value + "' is invalid",
                                    e,
                                    log.getPrefix(),
                                    logger);
                        }
                    }
                }
            }
        }


        // Determine whether AMT relay discovery address attribute record must be added. 
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
                        RtspStatusCodes.InternalServerError,
                        e,
                        log.getPrefix(),
                        logger);
            }
        }
        else if (relayAddress != null) {

            String relayHostName = relayAddress.getHostAddress();

            if (logger.isLoggable(Level.FINER)) {
                logger.finer(log.msg("add relay discovery address record for relay "+relayHostName));
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
                        RtspStatusCodes.InternalServerError,
                        e,
                        log.getPrefix(),
                        logger);
            }
        }
        else {
            throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                    RtspStatusCodes.BadRequest,
                    "relay_address query parameter is required for this SDP",
                    log.getPrefix(),
                    logger);
        }

        // Determine whether source filter attribute record must be added. 
        if (sessionDescription.getAttribute(SOURCE_FILTER_SDP_ATTRIBUTE) == null) {

            if (logger.isLoggable(Level.FINER)) {
                logger.finer(log.msg("construct source filter attribute record record"));
            }

            // Verify that a session or media-level connection record specifies a multicast destination address
            Connection connection = sessionDescription.getConnection();
            if (connection == null) {
                Vector<?> descriptions = sessionDescription.getMediaDescriptions(false);
                if (descriptions != null) {
                    for (int i = 0; i < descriptions.size(); i++)
                    {
                        MediaDescription mediaDescription = (MediaDescription) descriptions.get(i);
                        connection = mediaDescription.getConnection();
                        if (connection != null) {
                            break;
                        }
                    }
                }
            }

            if (connection == null) {
                throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                        RtspStatusCodes.BadRequest,
                        "SDP does not specify a connection address",
                        log.getPrefix(),
                        logger);
            }

            ConnectionField connectionField = (ConnectionField) connection;
            ConnectionAddress connectionAddress = connectionField.getConnectionAddress();

            Host host = connectionAddress.getAddress();
            InetAddress groupAddress;

            
            try {
                groupAddress = host.getInetAddress();

                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("group address is "+groupAddress));
                }

            }
            catch (UnknownHostException e) {
                throw new SdpException("cannot resolve connection address in media description");
            }

            if (!groupAddress.isMulticastAddress()) {
                throw new SdpException("input connection address must be a multicast address");
            }

            if (sourceAddress == null) {

                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("no source address specified - attempting to use origin host as source"));
                }

                Origin origin = sessionDescription.getOrigin();

                if (origin == null) {
                    throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                            RtspStatusCodes.BadRequest,
                            "source_address query parameter is required for this SDP",
                            log.getPrefix(),
                            logger);
                }

                OriginField originField = (OriginField) origin;
                String originHost = originField.getAddress();

                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("origin host: "+originHost));
                }

                try {
                    sourceAddress = InetAddress.getByName(originHost);
                }
                catch (UnknownHostException e) {
                    try {
                        // Try it with .local, though that is of questionable use as it will/should
                        // resolve to an address on the local net.
                        sourceAddress = InetAddress.getByName(originHost + ".local");
                    }
                    catch (UnknownHostException e1) {
                        throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                                RtspStatusCodes.BadRequest,
                                "source_address query parameter is required because the SDP origin record does not specify valid source",
                                e1,
                                log.getPrefix(),
                                logger);
                    }
                }
            }

            // Add some information attribute records to show what what we've done
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(log.msg("add x-origin-source-address attribute"));
            }

            AttributeField originSource = new AttributeField();
            originSource.setName("x-origin-source-address");
            originSource.setValue(sourceAddress.getHostName()+" "+sourceAddress.getHostAddress());

            if (sourceAddress.isLoopbackAddress() || sourceAddress.isLinkLocalAddress()) {
                try {
                    sourceAddress = getLocalHostIPAddress(sourceAddress instanceof Inet6Address);
                }
                catch (UnknownHostException e) {
                    throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                            RtspStatusCodes.InternalServerError,
                            e,
                            log.getPrefix(),
                            logger);
                }
                catch (SocketException e) {
                    throw RequestException.create(RtspService.RTSP_PROTOCOL_VERSION,
                            RtspStatusCodes.InternalServerError,
                            e,
                            log.getPrefix(),
                            logger);
                }
            }

            // Add some information attribute records to show what what we've done
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(log.msg("add x-filter-source-address"));
            }

            AttributeField filterSource = new AttributeField();
            filterSource.setName("x-filter-source-address");
            filterSource.setValue(sourceAddress.getHostName()+" "+sourceAddress.getHostAddress());
                

            if (!groupAddress.getClass().equals(sourceAddress.getClass())) {
                throw new SdpException("the IP address types of the source and group address in the SDP do not match");
            }

            // Add some information attribute records to show what what we've done

            // Add source filter attribute that applies to all groups
            String filterDesc = "incl IN " + ((sourceAddress instanceof Inet4Address) ? "IP4" : "IP6") + " * " + sourceAddress.getHostAddress();

            if (logger.isLoggable(Level.FINER)) {
                logger.finer(log.msg("add source filter record: "+filterDesc));
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
