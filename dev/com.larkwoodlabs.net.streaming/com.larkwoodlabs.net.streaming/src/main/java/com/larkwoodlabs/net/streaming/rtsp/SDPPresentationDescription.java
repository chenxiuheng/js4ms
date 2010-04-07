/*
 * Copyright © 2009-2010 Larkwood Labs Software.
 *
 * Licensed under the Larkwood Labs Software Source Code License, Version 1.0.
 * You may not use this file except in compliance with this License.
 *
 * You may view the Source Code License at
 * http://www.larkwoodlabs.com/source-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the license.
 */

package com.larkwoodlabs.net.streaming.rtsp;

import gov.nist.javax.sdp.fields.AttributeField;
import gov.nist.javax.sdp.fields.ConnectionAddress;
import gov.nist.javax.sdp.fields.ConnectionField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;

import javax.sdp.Connection;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;

import com.larkwoodlabs.io.FixedLengthInputStream;
import com.larkwoodlabs.net.Precondition;
import com.larkwoodlabs.net.amt.SourceFilter;
import com.larkwoodlabs.util.logging.Logging;

final class SDPPresentationDescription extends PresentationDescription {

    /*-- Inner Classes -------------------------------------------------------*/
    
    public static class Factory extends PresentationDescription.Factory.AbstractFactory {

        @Override
        public boolean constructs(URI uri) throws RtspException {
            return uri.getScheme().equals("http") && uri.getPath().endsWith(".sdp");
        }
        
        @Override
        public PresentationDescription construct(URI uri) throws RtspException {

            final String ObjectId = "[ static ]";
            if (uri.getScheme().equals("http")) {
                // Fetch file from web server
                String path = uri.getPath();
                if (path != null && path.endsWith(".sdp")) {
                                        
                    try {
                        String resourceUrl = uri.toString();
                        HttpURLConnection urlConnection = ((HttpURLConnection)new URL(resourceUrl).openConnection());
                        if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
    
                            //String lastModified = urlConnection.getHeaderField(Header.Last_Modified);
    
                            int contentLength = urlConnection.getContentLength();
    
                            if (contentLength == -1) {
                                // TODO;
                                throw RtspException.create(StatusCode.BadRequest,
                                                           "fetch from URI specified in request returned an invalid SDP description",
                                                           ObjectId, logger);

                            }
                            else {

                                InputStream inputStream = new FixedLengthInputStream(urlConnection.getInputStream(), contentLength);
        
                                StringBuilder sb = new StringBuilder();
                                String line;
                                try {
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                                    if (logger.isLoggable(Level.FINE)) {
                                        logger.fine(" ----> Server-side SDP");
                                        while ((line = reader.readLine()) != null) {
                                            logger.fine(" : " + line);
                                            sb.append(line).append("\r\n");
                                        }
                                        logger.fine(" <---- Server-side SDP");
                                    }
                                    else {
                                        while ((line = reader.readLine()) != null) {
                                            sb.append(line).append("\r\n");
                                        }
                                    }
                                    SessionDescription sessionDescription;
    
                                    try {
                                        sessionDescription = SdpFactory.getInstance().createSessionDescription(sb.toString());
                                        return new SDPPresentationDescription(uri, /*lastModified,*/ sessionDescription);
                                    }
                                    catch (SdpParseException e) {
                                        throw RtspException.create(StatusCode.BadRequest,
                                                                   "cannot parse session description - " + e.getMessage(),
                                                                   ObjectId, logger);
                                    }
                                }
                                finally {
                                    inputStream.close();
                                }
                            }
                        }
                        else {
                            // GET failed
                            // TODO create RtspException with same status code - but not all HTTP codes are present in StatusCode!
                            StatusCode.getByCode(urlConnection.getResponseCode());
                        }
                    }
                    catch (ConnectException e) {
                        throw RtspException.create(StatusCode.NotFound, "cannot fetch session description - " + e.getMessage() + ":" + e.getMessage(), ObjectId, logger);
                    }
                    catch (IOException e) { 
                        // GET failed
                        // throw RtspException with something
                    }

                }
            }

            logger.fine(Logging.identify(Presentation.class) + " the URI specified in the request cannot be used to fetch an SDP description");
            throw RtspException.create(StatusCode.BadRequest,
                                       "the URI specified in the request cannot be used to fetch an SDP description",
                                       ObjectId, logger);
        }

    }
    
    /*-- Static Constants ----------------------------------------------------*/

    public static final String STREAM_CONTROL_IDENTIFIER = "trackID=";

    /*-- Static Variables ----------------------------------------------------*/

    /*-- Static Functions ----------------------------------------------------*/


    /*-- Member Variables ----------------------------------------------------*/

    //private Date lastModified;
    protected SessionDescription sessionDescription;
    protected SessionDescription clientSideDescription = null;

    /*-- Member Functions ----------------------------------------------------*/

    protected SDPPresentationDescription(URI uri, /*Date lastModified,*/ SessionDescription sessionDescription) throws RtspException {
        super(uri);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "SDPPresentationDescription.SDPPresentationDescription",
                                          uri.toString(),
                                          /*lastModified.toString(),*/
                                          sessionDescription));
        }

        this.uri = uri;
        //this.lastModified = lastModified;
        this.sessionDescription = sessionDescription;
        
        initStreamDescriptions(sessionDescription);
    }
    
    public SessionDescription getSessionDescription() {
        return this.sessionDescription;
    }

    /*
    @Override
    public final Date getDateLastModified() {
        return this.lastModified;
    }
    */

    @Override
    public final boolean isSupported(String mimeType) {
        return mimeType.equals("application/sdp");
    }
    
    @Override
    public final String getMimeType() {
        return "application/sdp";
    }

    @Override
    public final boolean isPauseAllowed() {
        return false;
    }

    @Override
    public final String getStreamControlIdentifier() {
        return STREAM_CONTROL_IDENTIFIER;
    }

    @Override
    public final String describe(String mimeType) throws RtspException {
        if (isSupported(mimeType)) {
            return getClientSideDescription().toString();
        }
        else {
            throw RtspException.create(StatusCode.NotAcceptable,
                                       "cannot accept request for mime-type " +
                                       mimeType + " - server only supports mime-type: application/sdp",
                                       ObjectId, logger);
        }
    }

    public SessionDescription getClientSideDescription() throws RtspException {
        if (this.clientSideDescription == null) {
            this.clientSideDescription = constructClientSideDescription();
        }
        return this.clientSideDescription;
    }
    
    @SuppressWarnings("unchecked")
    private SessionDescription constructClientSideDescription() throws RtspException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "SDPPresentationDescription.constructClientSideDescription"));
        }

        SessionDescription sessionDescription = null;
        
        try {
            sessionDescription = (SessionDescription)this.sessionDescription.clone();
        }
        catch (CloneNotSupportedException e) {
            e.printStackTrace();
            throw RtspException.create(StatusCode.InternalServerError, e.getMessage(), ObjectId, logger);
        }

        /*
        String hostAddress = null;
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
            throw RtspException.create(RtspStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage(), ObjectId, logger);
        }
        */
        
        try {
            // Replace the connection address with a local host unicast address
            Connection connection = sessionDescription.getConnection();
            int addressCount = 1;
            if (connection != null) {
                ConnectionField connectionField = (ConnectionField)connection;
                ConnectionAddress connectionAddress = connectionField.getConnectionAddress();
                addressCount = connectionAddress.getPort();
                if (addressCount == 0) addressCount = 1;
                //connection.setAddress(hostAddress);
                connection.setAddress("0.0.0.0");
                connectionAddress.setTtl(0);
                connectionAddress.setPort(0);
            }
            
            // Rewrite or add a presentation session control attribute 
            // The SessionDescription.setAttribute() method is currently broken and will not add new Name/Value pairs
            if (sessionDescription.getAttribute("control") == null) {
                Vector attributes = sessionDescription.getAttributes(true);
                AttributeField attribute = new AttributeField();
                attribute.setName("control");
                //attribute.setValue(this.uri.toString());
                attribute.setValue("*");
                attributes.add(attribute);
            }
            else {
                sessionDescription.setAttribute("control", this.uri.toString());
            }
    
            // Remove the a=source-filter attribute records used by server
            sessionDescription.removeAttribute("source-filter");

            // Remove the a=x-amt-relay-anycast attribute records used by server
            sessionDescription.removeAttribute("x-amt-relay-anycast");

            // media level changes
            int nextPort = 1024;
            Vector descriptions = sessionDescription.getMediaDescriptions(false);
            for (int i=0; i<descriptions.size(); i++)
            {
                MediaDescription mediaDescription = (MediaDescription)descriptions.get(i);
    
                // Replace the connection address with a local host unicast address
                connection = mediaDescription.getConnection();
                if (connection != null) {
                    ConnectionField connectionField = (ConnectionField)connection;
                    ConnectionAddress connectionAddress = connectionField.getConnectionAddress();
                    addressCount = connectionAddress.getPort();
                    if (addressCount == 0) addressCount = 1;
                    //connection.setAddress(hostAddress);
                    connection.setAddress("0.0.0.0");
                    connectionAddress.setTtl(0);
                    connectionAddress.setPort(0);
                }

                if (addressCount > 1) {
                    // Convert multiple multicast addresses into multiple ports
                    Media media = mediaDescription.getMedia();
                    if (media.getPortCount() > 1) {
                        throw RtspException.create(StatusCode.BadRequest,
                                                   "SDP description of media stream cannot specify multiple addresses AND multiple ports",
                                                   ObjectId, logger);
                    }
                    if (i == 0) {
                        nextPort = media.getMediaPort();
                    }
                    //media.setMediaPort(nextPort);
                    media.setMediaPort(0);
                    media.setPortCount(addressCount);
                    nextPort += 2 * addressCount;
                }
                else {
                    Media media = mediaDescription.getMedia();
                    media.setMediaPort(0);
                }
                
                // Rewrite or add a media session control attribute
                int streamIndex = i;
                mediaDescription.setAttribute("control", STREAM_CONTROL_IDENTIFIER + streamIndex);
                /*
                connection = mediaDescription.getConnection();
                if (connection != null) {
                    connection.setAddress(hostAddress);
                }
                */
                
                //mediaDescription.setAttribute("3GPP-Adaptation-Support","1");

                // Remove the a=source-filter attribute records used by the server
                mediaDescription.removeAttribute("source-filter");

                // Remove the a=x-amt-relay-anycast attribute records used by server
                mediaDescription.removeAttribute("x-amt-relay-anycast");
            }

            
        }
        catch (SdpException e) {
            e.printStackTrace();
            throw RtspException.create(StatusCode.BadRequest,
                                       "cannot process session description - " + e.getMessage(),
                                       ObjectId, logger);
        }

        return sessionDescription;
    }

    /**
     */
    private final void initStreamDescriptions(SessionDescription sessionDescription) throws RtspException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "SDPPresentationDescription.initStreamDescriptions", sessionDescription));
        }

        try {

            // Transfer session-wide connection record to media stream description if required
            Connection sessionConnection = sessionDescription.getConnection();
            Vector<?> mediaDescriptions = sessionDescription.getMediaDescriptions(false);
            int streamIndex = 0;
            for (Object object : mediaDescriptions) {
                if (object instanceof MediaDescription) {
                    MediaDescription mediaDescription = (MediaDescription)object;
                    Connection mediaConnection = mediaDescription.getConnection();
                    if (mediaConnection == null) {
                        mediaConnection = sessionConnection;
                    }
                    // Look for source-filter attributes that apply to the connection address(es)
                    initStreamDescription(sessionDescription, mediaDescription, streamIndex++, mediaConnection);
                }
            }
        }
        catch (SdpException e) {
            throw RtspException.create(StatusCode.BadRequest,
                                       "the SDP description is invalid - " + e.getMessage(),
                                       ObjectId, logger);
        }
        
    }

    /**
     * Parses a connection record to determine the address range for a media stream,
     * computes a source filter for each stream address, and then constructs an
     * {@link SDPMediaStreamDescription} instance.
     * @param sessionDescription - the top-level session description.
     * @param mediaDescription - the SDP media description associated with the stream.
     * @param streamIndex - the stream index of the media description (within the session description).
     * @param connection - the connection record that applies to the media description.
     * @throws RtspException
     */
    private final void initStreamDescription(SessionDescription sessionDescription,
                                             MediaDescription mediaDescription,
                                             int streamIndex,
                                             Connection connection) throws RtspException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "SDPPresentationDescription.initStreamDescription",
                                          sessionDescription,
                                          mediaDescription,
                                          streamIndex,
                                          connection)); 
        }

        Media media = mediaDescription.getMedia();
        
        ConnectionField connectionField = (ConnectionField)connection;
        ConnectionAddress addressSpec = connectionField.getConnectionAddress();
        int addressCount = addressSpec.getPort(); // Don't know why they call this "port".
        if (addressCount == 0) addressCount = 1;

        InetAddress inetAddress;
            
        // Get the connection address (the first address if the address count > 1).
        try {
            inetAddress = addressSpec.getAddress().getInetAddress();
        }
        catch (UnknownHostException e) {
            throw RtspException.create(StatusCode.BadRequest,
                                      "SDP connection record contains an invalid address - " +
                                      addressSpec.getAddress().getAddress() + " is not recognized as a valid address",
                                      ObjectId, logger);
        }

        // TODO - this will change is RTSP proxy can act as client to another RTSP server
        if (!inetAddress.isMulticastAddress()) {
            throw RtspException.create(StatusCode.UnsupportedMediaType,
                                       "SDP connection record must specify a multicast address - unicast streaming is not supported by this server",
                                       ObjectId, logger);
        }
        
        int ttl = addressSpec.getTtl();

        if (ttl > 0 && !inetAddress.isMulticastAddress()) {
            throw RtspException.create(StatusCode.BadRequest,
                                       "SDP connection record cannot specify a TTL value for a unicast address",
                                       ObjectId, logger);
        }

        if (addressCount < 0 || addressCount > 255) {
            throw RtspException.create(StatusCode.BadRequest,
                                       "SDP connection record contains an invalid address count - " +
                                       addressCount + " is not a valid count value",
                                       ObjectId, logger);
        }
        
        try {
            if (addressCount > 1 && media.getPortCount() > 1) {
                throw RtspException.create(StatusCode.BadRequest,
                                           "SDP description cannot specify multiple addresses and multiple ports for a single stream",
                                           ObjectId, logger);
            }
        }
        catch (SdpParseException e) {
            throw RtspException.create(StatusCode.BadRequest,
                                       "SDP media record is malformed - "+e.getMessage(),
                                       ObjectId, logger);
        }

        // Get address (should not be FQDN if an address count is specified)
        byte[] address = inetAddress.getAddress();

        Vector<SourceFilter> filters = new Vector<SourceFilter>();

        for (int i=0; i < addressCount; i++) {

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " constructing source filter for " + Logging.address(address));
            }

            SourceFilter filter;
            try {
                filter = new SourceFilter(InetAddress.getByAddress(address));
            }
            catch (UnknownHostException e) {
                throw RtspException.create(StatusCode.BadRequest,
                                          "SDP connection record contains an invalid address - " +
                                          Logging.address(address) + " is not a valid address",
                                          ObjectId, logger);
            }
            
            // Increment LSB for next iteration.
            address[address.length - 1]++;

            // Iterate over any media level source-filter attribute records
            Vector<?> attributes = mediaDescription.getAttributes(false);
            if (attributes != null) {
                // Iterate over all of the session level attributes looking for source-filter records
                for (int j=0; j<attributes.size(); j++) {
                    Object o = attributes.elementAt(j);
                    if (o instanceof AttributeField) {
                        AttributeField field = (AttributeField)o;
                        String name;
                        try {
                            name = field.getName();
                        }
                        catch (SdpParseException e) {
                            throw RtspException.create(StatusCode.BadRequest,
                                                       "SDP contains an invalid attribute record - " + e.getMessage(),
                                                       ObjectId, logger);
                        }
                        if (name != null) {
                            if (name.equals("source-filter")) {
                                String value;
                                try {
                                    value = field.getValue();
                                }
                                catch (SdpParseException e) {
                                    throw RtspException.create(StatusCode.BadRequest,
                                                               "SDP contains an invalid source-filter record - " + e.getMessage(),
                                                               ObjectId, logger);
                                }
                                if (value != null) {
                                    applySourceFilterAttribute(filter, value, false);
                                }
                            }
                        }
                    }
                }
            }
            
            attributes = sessionDescription.getAttributes(false);
            if (attributes != null) {
                // Iterate over all of the session level attributes looking for source-filter records
                for (int j=0; j<attributes.size(); j++) {
                    Object o = attributes.elementAt(j);
                    if (o instanceof AttributeField) {
                        AttributeField field = (AttributeField)o;
                        String name;
                        try {
                            name = field.getName();
                        }
                        catch (SdpParseException e) {
                            throw RtspException.create(StatusCode.BadRequest,
                                                       "SDP contains an invalid attribute record - " + e.getMessage(),
                                                       ObjectId, logger);
                        }
                        if (name != null) {
                            if (name.equals("source-filter")) {
                                String value;
                                try {
                                    value = field.getValue();
                                }
                                catch (SdpParseException e) {
                                    throw RtspException.create(StatusCode.BadRequest,
                                                               "SDP contains an invalid source-filter record - " + e.getMessage(),
                                                               ObjectId, logger);
                                }
                                if (value != null) {
                                    applySourceFilterAttribute(filter, value, true);
                                }
                            }
                        }
                    }
                }
            }

            if (logger.isLoggable(Level.FINER)) {
                logger.finer(ObjectId + " ----> Source Filter");
                filter.log(logger);
                logger.finer(ObjectId + " <---- Source Filter");
            }

            if (Precondition.isSSMMulticastAddress(filter.getGroupAddress()) && filter.isEmpty()) {
                throw RtspException.create(StatusCode.BadRequest,
                                           "SDP must contain a source-filter attribute record for any media originating from a source-specific multicast address",
                                           ObjectId, logger);
                
            }

            // Save the source filter for this address
            filters.add(filter);
        }

        // Look for relay discovery address attribute record
        InetAddress relayDiscoveryAddress = null;
        try {
            String anycastAddress = mediaDescription.getAttribute("x-amt-relay-anycast");
            if (anycastAddress == null) {
                anycastAddress = sessionDescription.getAttribute("x-amt-relay-anycast");
            }
            if (anycastAddress != null) {
                try {
                    relayDiscoveryAddress = InetAddress.getByName(anycastAddress);
                }
                catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        catch (SdpParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        MediaStreamDescription streamDescription = new SDPMediaStreamDescription(sessionDescription, mediaDescription, filters, ttl, relayDiscoveryAddress);
        this.streamDescriptions.add(streamIndex, streamDescription);
        
    }
    
    /*-- Static Functions ----------------------------------------------------*/

    /**
     * Updates the source filter for a single media stream address.
     * <pre>
     * The source-filter attribute has the following syntax:
     * 
     *        a=source-filter: <filter-mode> <filter-spec>
     * 
     *    The <filter-mode> is either "incl" or "excl" (for inclusion or
     *    exclusion, respectively).  The <filter-spec> has four sub-components:
     * 
     *        <nettype> <address-types> <dest-address> <src-list>
     * 
     *    A <filter-mode> of "incl" means that an incoming packet is accepted
     *    only if its source address is in the set specified by <src-list>.  A
     *    <filter-mode> of "excl" means that an incoming packet is rejected if
     *    its source address is in the set specified by <src-list>.
     * 
     *    The first sub-field, <nettype>, indicates the network type, since SDP
     *    is protocol independent.  This document is most relevant to the value
     *    "IN", which designates the Internet Protocol.
     * 
     *    The second sub-field, <address-types>, identifies the address family,
     *    and for the purpose of this document may be either <addrtype> value
     *    "IP4" or "IP6".  Alternately, when <dest-address> is an FQDN, the
     *    value MAY be "*" to apply to both address types, since either address
     *    type can be returned from a DNS lookup.
     * 
     *    The third sub-field, <dest-address>, is the destination address,
     *    which MUST correspond to one or more of the session's "connection-
     *    address" field values.  It may be either a unicast or multicast
     *    address, an FQDN, or the "*" wildcard to match any/all of the
     *    session's "connection-address" values.
     * 
     *    The fourth sub-field, <src-list>, is the list of source
     *    hosts/interfaces in the source-filter, and consists of one or more
     *    unicast addresses or FQDNs, separated by space characters.
     * 
     * </pre>
     * 
     * @throws RtspException 
     */
   private static void applySourceFilterAttribute(SourceFilter filter,
                                                  String filterAttribute,
                                                  boolean isSessionLevel) throws RtspException {
        
       final String ObjectId = "[ static ]";

       if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "SDPPresentationDescription.applySourceFilterAttribute"));
        }

        StringTokenizer tokenizer = new StringTokenizer(filterAttribute);
        if (tokenizer.hasMoreElements()) {
            String filterMode = tokenizer.nextToken();
            if (filterMode.equals("incl") || filterMode.equals("excl")) {
                if (tokenizer.hasMoreElements()) {
                    String netType = tokenizer.nextToken();
                    if (netType.equals("IN")) {
                        if (tokenizer.hasMoreElements()) {
                            String addressType = tokenizer.nextToken();
                            if (addressType.equals("IP4") || addressType.equals("IP6") || addressType.equals("*")) {
                                if (tokenizer.hasMoreElements()) {
                                    String destinationAddress = tokenizer.nextToken();
                                    InetAddress groupAddress;
                                    if (destinationAddress.equals("*")) {
                                        if (!filter.isEmpty()) {
                                            // Ignore wild-card if a filter has already been applied.
                                            return;
                                        }
                                        groupAddress = filter.getGroupAddress();
                                    }
                                    else
                                    {
                                        if (!filter.isEmpty()) {
                                            // Ignore wild-card if a filter has already been applied.
                                            return;
                                        }
                                        try {
                                            groupAddress = InetAddress.getByName(destinationAddress);
                                        }
                                        catch (UnknownHostException e) {
                                            throw RtspException.create(StatusCode.BadRequest,
                                                                       "SDP source-filter attribute must specify a valid destination address - " +
                                                                       destinationAddress + " is not recognized as a valid address",
                                                                       ObjectId, logger);
                                        }
                                    }
                                    if (!groupAddress.equals(filter.getGroupAddress())) {
                                        // This source-filter record does not apply to this filter
                                        return;
                                    }
                                    else {
                                        // The source-filter list applies to this filter
                                        if (!filter.isEmpty()) {
                                            if (isSessionLevel) {
                                                // A media level source-filter already applied
                                                // Could also be duplicate at session-level, but will ignore that for now.
                                                return;
                                            }
                                            else {
                                                throw RtspException.create(StatusCode.BadRequest,
                                                                           "SDP must not contain multiple source-filter attributes that apply to the same destination address",
                                                                           ObjectId, logger);
                                            }
                                        }
                                        if (tokenizer.hasMoreElements()) {
                                            if (filterMode.equals("incl")) {
                                                while (tokenizer.hasMoreElements()) {
                                                    String sourceAddress = tokenizer.nextToken();
                                                    try {
                                                        filter.include(InetAddress.getByName(sourceAddress));
                                                    }
                                                    catch (UnknownHostException e) {
                                                        throw RtspException.create(StatusCode.BadRequest,
                                                                                   "SDP source-filter attribute must specify a valid source address - " +
                                                                                   sourceAddress + " is not recognized as a valid address",
                                                                                   ObjectId, logger);
                                                    }
                                                }
                                            }
                                            else {
                                                while (tokenizer.hasMoreElements()) {
                                                    String sourceAddress = tokenizer.nextToken();
                                                    try {
                                                        filter.exclude(InetAddress.getByName(sourceAddress));
                                                    }
                                                    catch (UnknownHostException e) {
                                                        throw RtspException.create(StatusCode.BadRequest,
                                                                                   "SDP source-filter attribute must specify a valid source address - " +
                                                                                   sourceAddress + " is not recognized as a valid address",
                                                                                   ObjectId, logger);
                                                    }
                                                }
                                            }
                                        }
                                        else {
                                            throw RtspException.create(StatusCode.BadRequest,
                                                                       "SDP source-filter attribute must specify at least one source address",
                                                                       ObjectId, logger);
                                        }
                                    }
                                }
                                else {
                                    throw RtspException.create(StatusCode.BadRequest,
                                                               "SDP source-filter attribute must specify at least one source address",
                                                               ObjectId, logger);
                                }
                            }
                            else {
                                throw RtspException.create(StatusCode.BadRequest,
                                                           "SDP source-filter attribute must specify a valid address type - " +
                                                           addressType + " is not a valid type (must be IP4, IP6 or *)",
                                                           ObjectId, logger);
                            }
                        }
                        else {
                            throw RtspException.create(StatusCode.BadRequest,
                                                       "SDP source-filter attribute must specify an address type",
                                                       ObjectId, logger);
                        }
                    }
                    else {
                        throw RtspException.create(StatusCode.BadRequest,
                                                   "SDP source-filter attribute must specify a valid network protocol - " +
                                                   netType + " is not recognized (must be IP)",
                                                   ObjectId, logger);
                    }
                }
                else {
                    throw RtspException.create(StatusCode.BadRequest,
                                               "SDP source-filter attribute must specify an network protocol",
                                               ObjectId, logger);
                }   
            }
            else {
                throw RtspException.create(StatusCode.BadRequest,
                                           "SDP source-filter attribute must specify a valid filter mode - " +
                                           filterMode + " is not recognized (must be incl or excl)",
                                           ObjectId, logger);
            }
        }
        else {
            throw RtspException.create(StatusCode.BadRequest,
                                       "SDP source-filter attribute must specify a filter mode",
                                       ObjectId, logger);
        }
    }

    @Override
    public Date getDateLastModified() {
        // TODO Auto-generated method stub
        return null;
    }


}
