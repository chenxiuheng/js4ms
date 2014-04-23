package org.js4ms.rtsp.presentation;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * TransportDescription.java [org.js4ms.jsdk:rtsp]
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


import gov.nist.javax.sdp.fields.ConnectionAddress;
import gov.nist.javax.sdp.fields.ConnectionField;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import javax.sdp.Connection;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;

import org.js4ms.common.util.logging.Logging;


/**
 * Describes the preferred or actual transport characteristics of a media stream.
 * 
 * @author Gregory Bumgardner
 */
public class TransportDescription {
    
    /*-- Inner Classes -------------------------------------------------------*/

    /**
     * Enumeration of streaming distribution types.
     */
    public enum Distribution {
        /** Use unicast destination addresses */
        UNICAST,
        /** Use multicast destination addresses */
        MULTICAST
    };

    /**
     * Enumeration of streaming modes.
     */
    public enum Mode {
        /** Request play mode. */
        PLAY,
        /** Request record mode. */
        RECORD,
        /** Request append mode */
        APPEND
    };

    /*-- Member Variables ----------------------------------------------------*/

    final String ObjectId = Logging.identify(this);

    boolean isProtocolSpecified = false;
    String protocol = "";

    boolean isProfileSpecified = false;
    String profile = "";
    
    boolean isTransportSpecified = false;
    String transport = "UDP";

    boolean isDistributionSpecified = false;
    Distribution distribution = Distribution.MULTICAST;
    
    boolean isDestinationSpecified = false;
    InetAddress destination;

    boolean isSourceSpecified = false;
    InetAddress source;

    boolean isLayersSpecified = false;
    int layers = 1;

    boolean isModeSpecified = false;
    Mode mode = Mode.PLAY;

    boolean isAppendSpecified = false;
    boolean append = false;

    boolean isTTLSpecified = false;
    int ttl = 127;

    boolean isClientPortRangeSpecified = false;
    int firstClientPort;
    int lastClientPort;

    boolean isMulticastPortRangeSpecified = false;
    int firstMulticastPort;
    int lastMulticastPort;

    boolean isServerPortRangeSpecified = false;
    int firstServerPort;
    int lastServerPort;

    boolean isInterleavedChannelRangeSpecified = false;
    int firstInterleavedChannel;
    int lastInterleavedChannel;

    boolean isSSRCSpecified = false;
    int ssrc = 0;
    

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a TransportDescription instance from a string containing a
     * transport specification extracted from an RTSP Transport header. The
     * transport specification must conform to the following format:
     * 
     * <pre>
     *   transport-spec      =    transport-protocol/profile[/lower-transport][;parameter]*
     *   transport-protocol  =    "RTP"
     *   profile             =    "AVP"
     *   lower-transport     =    "TCP" | "UDP"
     *   parameter           =    ( "unicast" | "multicast" )
     *                       |    ";" "destination" [ "=" address ]
     *                       |    ";" "source" [ "=" address ]
     *                       |    ";" "interleaved" "=" channel [ "-" channel ]
     *                       |    ";" "append"
     *                       |    ";" "ttl" "=" ttl
     *                       |    ";" "layers" "=" 1*DIGIT
     *                       |    ";" "port" "=" port [ "-" port ]
     *                       |    ";" "client_port" "=" port [ "-" port ]
     *                       |    ";" "server_port" "=" port [ "-" port ]
     *                       |    ";" "ssrc" "=" ssrc
     *                       |    ";" "mode" = &lt;"&gt; 1\#mode &lt;"&gt;
     *   ttl                 =    1*3(DIGIT)
     *   port                =    1*5(DIGIT)
     *   ssrc                =    8*8(HEX)
     *   channel             =    1*3(DIGIT)
     *   address             =    host
     *   mode                =    &lt;"&gt; *Method &lt;"&gt; | Method
     * </pre>
     * A Transport request header field may contain a list of transport options
     * acceptable to the client. In that case, the server MUST return a single
     * option which was actually chosen.
     * <p/>
     * 
     * The syntax for the transport specifier is:
     * <p/>
     * 
     * <blockquote><b>transport/profile/lower-transport</b></blockquote>
     * <p/>
     * 
     * The default value for the "lower-transport" parameters is specific to the
     * profile. For RTP/AVP, the default is UDP.
     * <p/>
     * 
     * Below are the configuration parameters associated with transport:
     * 
     * <h3>General parameters</h3>
     * <h4>unicast | multicast (request/response)</h4>
     * mutually exclusive indication of whether unicast or multicast delivery
     * will be attempted. Default value is multicast. Clients that are capable
     * of handling both unicast and multicast distribution MUST indicate such
     * capability by including two full transport-specs with separate parameters
     * for each.
     * 
     * <h4>destination (request/response)</h4>
     * The address to which a stream will be sent. The client may specify the
     * multicast address with the destination parameter. To avoid becoming the
     * unwitting perpetrator of a remote- controlled denial-of-service attack, a
     * server SHOULD authenticate the client and SHOULD log such attempts before
     * allowing the client to direct a media stream to an address not chosen by
     * the server. This is particularly important if RTSP commands are issued
     * via UDP, but implementations cannot rely on TCP as reliable means of
     * client identification by itself. A server SHOULD not allow a client to
     * direct media streams to an address that differs from the address commands
     * are coming from.
     * 
     * <h4>source (response)</h4>
     * If the source address for the stream is different than can be derived
     * from the RTSP endpoint address (the server in playback or the client in
     * recording), the source MAY be specified.
     * 
     * This information may also be available through SDP. However, since this
     * is more a feature of transport than media initialization, the
     * authoritative source for this information should be in the SETUP
     * response.
     * 
     * <h4>layers (request/response)</h4>
     * The number of multicast layers to be used for this media stream. The
     * layers are sent to consecutive addresses starting at the destination
     * address.
     * 
     * <h4>mode (response)</h4>
     * The mode parameter indicates the methods to be supported for this
     * session. Valid values are PLAY and RECORD. If not provided, the default
     * is PLAY.
     * 
     * <h4>append (request)</h4>
     * If the mode parameter includes RECORD, the append parameter indicates
     * that the media data should append to the existing resource rather than
     * overwrite it. If appending is requested and the server does not support
     * this, it MUST refuse the request rather than overwrite the resource
     * identified by the URI. The append parameter is ignored if the mode
     * parameter does not contain RECORD.
     * 
     * <h4>interleaved (request/response)</h4>
     * The interleaved parameter implies mixing the media stream with the
     * control stream in whatever protocol is being used by the control stream,
     * using the mechanism defined in Section 10.12. The argument provides the
     * channel number to be used in the $ statement. This parameter may be
     * specified as a range, e.g., interleaved=4-5 in cases where the transport
     * choice for the media stream requires it.
     * 
     * This allows RTP/RTCP to be handled similarly to the way that it is done
     * with UDP, i.e., one channel for RTP and the other for RTCP.
     * 
     * <h3>Multicast specific:</h3>
     * 
     * <h4>ttl (request/response)</h4>
     * The multicast time-to-live value.
     * 
     * <h3>RTP Specific:</h3>
     * 
     * <h4>port (request/response)</h4>
     * This parameter provides the RTP/RTCP port pair for a multicast session.
     * It is specified as a range, e.g., port=3456-3457.
     * 
     * <h4>client_port (request/response)</h4>
     * This parameter provides the unicast RTP/RTCP port pair on which the
     * client has chosen to receive media data and control information. It is
     * specified as a range, e.g., client_port=3456-3457.
     * 
     * <h4>server_port (response)</h4>
     * This parameter provides the unicast RTP/RTCP port pair on which the
     * server has chosen to receive media data and control information. It is
     * specified as a range, e.g., server_port=3456-3457.
     * 
     * <h4>ssrc (request/response)</h4>
     * The ssrc parameter indicates the RTP SSRC [24, Sec. 3] value that should
     * be (request) or will be (response) used by the media server. This
     * parameter is only valid for unicast distribution. It identifies the
     * synchronization source to be associated with the media stream.
     * @throws RtspException 
     * @throws RtspException 
     * @throws UnknownHostException 
     */
    public TransportDescription(final String header) throws UnknownHostException {
        parseTransportHeader(header);
    }

    /**
     * Constructs a transport description from an SDP description of a media stream.
     * The protocol field in the media element is used to set the protocol, profile and transport fields.
     * The connection address is used to set the destination address field (unicast or multicast).
     * The layers field is set to either the number of addresses or number of ports (even in unicast case).
     * The server port range is set using media port and port count
     * 
     * @param sessionDescription - The session description that contains the media description. Used to retrieve connection information.
     * @param mediaDescription - The media description containing values that will be used to initialize the transport description.
     * @throws SdpException The SDP description is malformed or invalid.
     */
    public TransportDescription(final SessionDescription sessionDescription,
                                final MediaDescription mediaDescription) throws SdpException {

        Connection connection = mediaDescription.getConnection();
        if (connection == null) {
            connection = sessionDescription.getConnection();
            if (connection == null) {
                throw new SdpException("connection record missing in SDP description");
            }
        }

        ConnectionAddress connectionAddress = ((ConnectionField)connection).getConnectionAddress();

        InetAddress destination;
        
        try {
            destination = connectionAddress.getAddress().getInetAddress();
        }
        catch (UnknownHostException e) {
            throw new SdpException("cannot resolve connection address in SDP description");
        }

        setDestination(destination);

        Distribution distribution = destination.isMulticastAddress() ? Distribution.MULTICAST : Distribution.UNICAST;

        setDistribution(distribution);

        if (distribution == Distribution.MULTICAST) {
            int ttl = connectionAddress.getTtl();
            if (ttl == 0) {
                ttl = 64;
            }
            setTTL(ttl);
        }

        // Get address count from connection record (called "port" for some reason)
        // The address count is the layer count
        int addressCount = connectionAddress.getPort();
        if (addressCount == 0) addressCount = 1;

        if (addressCount > 1 && distribution == Distribution.UNICAST) {
            throw new SdpException("SDP cannot specify an address count for a unicast stream or session");
        }

        Media media = mediaDescription.getMedia();

        parseSdpTransportSpec(media.getProtocol());

        if (!isTransportSpecified()) {
            setTransport("UDP");
        }

        int streamCount = media.getPortCount();
        if (streamCount == 0) streamCount = 1;

        if (addressCount > 1 && streamCount >1) {
            throw new SdpException("SDP media description cannot specify multiple addresses AND multiple ports");
        }

        // Count of layers is count of addresses or count of streams (e.g. RTP/RTCP pairs).
        setLayers(addressCount > 1 ? addressCount : streamCount);

        int baseChannelPort = media.getMediaPort();

        this.setClientPortRange(baseChannelPort, baseChannelPort + getPortsPerLayer()*streamCount - 1);

        this.setMode(Mode.PLAY);
    }

    /**
     * Constructs a default instance.
     */
    public TransportDescription() {
        
    }

    public void log(Logger logger) {
        logger.info(ObjectId + " : " + getHeaderValue());
    }

    /**
     * Sets the protocol attribute value.
     * @param protocol - The new protocol value.
     */
    public void setProtocol(final String protocol) {
        this.isProtocolSpecified = true;
        this.protocol = protocol.toUpperCase();
    }

    /**
     * Returns <code>true</code> if a protocol was specified in the transport header
     * or was explicitly set with a call to {@link #setProtocol(String)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isProtocolSpecified() {
        return this.isProtocolSpecified;
    }
    
    /**
     * Gets the current protocol attribute value.
     */
    public String getProtocol() {
        return this.protocol;
    }

    /**
     * Sets the profile attribute value.
     * @param profile - The new profile value.
     */
    public void setProfile(final String profile) {
        this.isProfileSpecified = true;
        this.profile = profile.toUpperCase();
    }

    /**
     * Returns <code>true</code> if a profile was specified in the transport header
     * or was explicitly set with a call to {@link #setProfile(String)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isProfileSpecified() {
        return this.isProfileSpecified;
    }

    /**
     * Gets the current profile attribute value.
     */
    public String getProfile() {
        return this.profile;
    }

    /**
     * Sets the transport attribute value.
     * @param string - The new transport value.
     */
    public void setTransport(final String transport) {
        this.isTransportSpecified = true;
        this.transport = transport.toUpperCase();
    }

    /**
     * Returns <code>true</code> if a transport was specified in the transport header
     * or was explicitly set with a call to {@link #setTransport(String)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isTransportSpecified() {
        return this.isTransportSpecified;
    }

    /**
     * Gets the current transport attribute value.
     */
    public String getTransport() {
        return this.transport;
    }
    
    /**
     * Sets the distribution attribute value.
     * @param distribution - The new distribution value.
     */
    public void setDistribution(Distribution distribution) {
        this.isDistributionSpecified = true;
        this.distribution = distribution;
    }

    /**
     * Returns <code>true</code> if a distribution parameter appeared in the transport header
     * or was explicitly set with a call to {@link #setDistribution(Distribution)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isDistributionSpecified() {
        return this.isDistributionSpecified;
    }

    /**
     * Gets the current distribution attribute value.
     */
    public Distribution getDistribution() {
        return this.distribution;
    }
    
    /**
     * Sets the destination address attribute value.
     * @param destination - The new destination address.
     */
    public void setDestination(InetAddress destination) {
        this.isDestinationSpecified = true;
        this.destination = destination;
    }

    /**
     * Returns <code>true</code> if a destination address was specified in the transport header
     * or was explicitly set with a call to {@link #setDestination(InetAddress)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isDestinationSpecified() {
        return this.isDestinationSpecified;
    }

    /**
     * Gets the current destination address attribute value.
     */
    public InetAddress getDestination() {
        return this.destination;
    }

    /**
     * Sets the source address attribute value.
     * @param source - The new source address.
     */
    public void setSource(InetAddress source) {
        this.isSourceSpecified = true;
        this.source = source;
    }

    /**
     * Returns <code>true</code> if a source address was specified in the transport header
     * or was explicitly set with a call to {@link #setSource(InetAddress)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isSourceSpecified() {
        return this.isSourceSpecified;
    }

    /**
     * Gets the current source address attribute value.
     */
    public InetAddress getSource() {
        return this.source;
    }

    /**
     * Sets the multicast layer count attribute value.
     * @param layers - The new layer count value.
     */
    public void setLayers(int layers) {
        this.isLayersSpecified = true;
        this.layers = layers;
    }

    /**
     * Returns <code>true</code> if a layer count was specified in the transport header
     * or was explicitly set with a call to {@link #setLayers(int)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isLayersSpecified() {
        return this.isLayersSpecified;
    }

    /**
     * Gets the current multicast layer count attribute value.
     */
    public int getLayers() {
        return this.layers;
    }

    /**
     * Sets the mode attribute value.
     * @param mode - The new streaming mode.
     */
    public void setMode(Mode mode) {
        this.isModeSpecified = true;
        this.mode = mode;
    }

    /**
     * Returns <code>true</code> if a mode was specified in the transport header
     * or was explicitly set with a call to {@link #setMode(Mode)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isModeSpecified() {
        return this.isModeSpecified;
    }

    /**
     * Gets the current mode attribute value.
     */
    public Mode getMode() {
        return this.mode;
    }

    /**
     * Sets the record append attribute value.
     * @param append - The new record append value.
     */
    public void setAppend(boolean append) {
        this.isAppendSpecified = true;
        this.append = append;
    }

    /**
     * Returns <code>true</code> if the append parameter appeared in the transport header
     * or was explicitly set with a call to {@link #setAppend(boolean)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isAppendSpecified() {
        return this.isAppendSpecified;
    }
    
    /**
     * Gets the record append attribute value.
     */
    public boolean getAppend() {
        return this.append;
    }

    /**
     * Sets the TTL attribute value.
     * @param ttl - The new TTL value.
     */
    public void setTTL(int ttl) {
        this.isLayersSpecified = true;
        this.ttl = ttl;
    }

    /**
     * Returns <code>true</code> if the TTL parameter was specified in the transport header
     * or was explicitly set with a call to {@link #setTTL(int)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isTTLSpecified() {
        return this.isTTLSpecified;
    }

    /**
     * Gets the TTL attribute value.
     */
    public int getTTL() {
        return this.ttl;
    }
    
    /**
     * Returns the number of ports (or channels) required for each layer (typically two for RTP/RTCP).
     */
    public int getPortsPerLayer() {
        int portsPerLayer = 1;
        if (this.protocol.contains("RTP")) {
            portsPerLayer = 2;
        }
        return portsPerLayer;
    }

    /**
     * Sets the client port range.
     * For normal RTP/RTCP the first port is typically an even number and the last port is the first port plus one.
     * For multiplexed RTP/RTCP, the first and last ports will have the same value.
     * For a layered encoding, the port range indicates the number of layers that are present or requested.
     * For example, if an RTP/RTCP stream has three layers available starting at port 9000, then
     * the port range will be 9000,9005 for non-multiplexed RTP/RTCP or 9000,9002 for multiplexed RTP/RTCP.
     * @param firstPort - The first port in the range.
     * @param lastPort - The last port in the range.
     */
    public void setClientPortRange(int firstPort, int lastPort) {
        this.isClientPortRangeSpecified = true;
        this.firstClientPort = firstPort;
        this.lastClientPort = lastPort;
    }

    /**
     * Returns <code>true</code> if a client port range was specified in the transport header
     * or was explicitly set with a call to {@link #setClientPortRange(int, int)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isClientPortRangeSpecified() {
        return this.isClientPortRangeSpecified;
    }

    /**
     * Gets the current first client port number.
     */
    public int getFirstClientPort() {
        return this.firstClientPort;
    }

    /**
     * Gets the current last client port number.
     */
    public int getLastClientPort() {
        return this.lastClientPort;
    }

    /**
     * Sets the multicast port range.
     * For normal RTP/RTCP the first port is typically an even number and the last port is the first port plus one.
     * For multiplexed RTP/RTCP, the first and last ports will have the same value.
     * @param firstPort - The first port in the range.
     * @param lastPort - The last port in the range.
     */
    public void setMulticastPortRange(int firstPort, int lastPort) {
        this.isMulticastPortRangeSpecified = true;
        this.firstMulticastPort = firstPort;
        this.lastMulticastPort = lastPort;
    }

    /**
     * Returns <code>true</code> if a multicast port range was specified in the transport header
     * or was explicitly set with a call to {@link #setMulticastPortRange(int, int)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isMulticastPortRangeSpecified() {
        return this.isMulticastPortRangeSpecified;
    }

    /**
     * Gets the current first multicast port number.
     */
    public int getFirstMulticastPort() {
        return this.firstMulticastPort;
    }

    /**
     * Gets the current last multicast port number.
     */
    public int getLastMulticastPort() {
        return this.lastMulticastPort;
    }

    /**
     * Sets the server port range.
     * For normal RTP/RTCP the first port is typically an even number and the last port is the first port plus one.
     * For multiplexed RTP/RTCP, the first and last ports will have the same value.
     * For a layered encoding, the port range indicates the number of layers that are present or requested.
     * For example, if an RTP/RTCP stream has three layers available starting at port 9000, then
     * the port range will be 9000,9005 for non-multiplexed RTP/RTCP or 9000,9002 for multiplexed RTP/RTCP.
     * @param firstPort - The first port in the range.
     * @param lastPort - The last port in the range.
     */
    public void setServerPortRange(int firstPort, int lastPort) {
        this.isServerPortRangeSpecified = true;
        this.firstServerPort = firstPort;
        this.lastServerPort = lastPort;
    }

    /**
     * Returns <code>true</code> if a server port range was specified in the transport header
     * or was explicitly set with a call to {@link #setServerPortRange(int, int)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isServerPortRangeSpecified() {
        return this.isServerPortRangeSpecified;
    }

    /**
     * Gets the current first server port number.
     */
    public int getFirstServerPort() {
        return this.firstServerPort;
    }

    /**
     * Gets the current last server port number.
     */
    public int getLastServerPort() {
        return this.lastServerPort;
    }

    /**
     * Sets the interleaved channel range.
     * For normal RTP/RTCP the first channel is typically an even number and the last channel is the first channel plus one.
     * For multiplexed RTP/RTCP, the first and last channel value will be equal.
     * For a layered encoding, the channel range indicates the number of layers that are present or requested.
     * For example, if an RTP/RTCP stream has three layers available starting at channel 0, then
     * the channel range will be 0,5 for non-multiplexed RTP/RTCP or 0,2 for multiplexed RTP/RTCP.
     * @param firstChannel - The first channel in the range.
     * @param lastChannel - The last channel in the range.
     */
    public void setInterleavedChannelRange(int firstChannel, int lastChannel) {
        this.isInterleavedChannelRangeSpecified = true;
        this.firstInterleavedChannel = firstChannel;
        this.lastInterleavedChannel = lastChannel;
    }

    /**
     * Returns <code>true</code> if an interleaved channel range was specified in the transport header
     * or was explicitly set with a call to {@link #setInterleavedChannelRange(int, int)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isInterleavedChannelRangeSpecified() {
        return this.isInterleavedChannelRangeSpecified;
    }

    /**
     * Gets the current first interleaved channel number.
     */
    public int getFirstInterleavedChannel() {
        return this.firstInterleavedChannel;
    }

    /**
     * Gets the current last interleaved channel number.
     */
    public int getLastInterleavedChannel() {
        return this.lastInterleavedChannel;
    }

    /**
     * Sets the synchronization source identifier (SSRC) attribute value.
     * @param ssrc - The new SSRC value.
     */
    public void setSSRC(int ssrc) {
        this.isSSRCSpecified = true;
        this.ssrc = ssrc;
    }

    /**
     * Returns <code>true</code> if an SSRC was specified in the transport header
     * or was explicitly set with a call to {@link #setSSRC(int)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isSSRCSpecified() {
        return this.isSSRCSpecified;
    }

    /**
     * Gets the current synchronization source identifier (SSRC) attribute value.
     */
    public int getSSRC() {
        return this.ssrc;
    }

    @Override
    public String toString() {
        return getHeaderValue();
    }

    /**
     * Constructs a transport specification for use in an RTSP Transport header.
     * See {@link #TransportDescription(String)} for parameter descriptions.
     * <pre>
     *   transport-spec      =    transport-protocol/profile[/lower-transport][;parameter]*
     *   transport-protocol  =    "RTP"
     *   profile             =    "AVP"
     *   lower-transport     =    "TCP" | "UDP"
     *   parameter           =    ( "unicast" | "multicast" )
     *                       |    ";" "destination" [ "=" address ]
     *                       |    ";" "interleaved" "=" channel [ "-" channel ]
     *                       |    ";" "append"
     *                       |    ";" "ttl" "=" ttl
     *                       |    ";" "layers" "=" 1*DIGIT
     *                       |    ";" "port" "=" port [ "-" port ]
     *                       |    ";" "client_port" "=" port [ "-" port ]
     *                       |    ";" "server_port" "=" port [ "-" port ]
     *                       |    ";" "ssrc" "=" ssrc
     *                       |    ";" "mode" = &lt;"&gt; 1\#mode &lt;"&gt;
     *   ttl                 =    1*3(DIGIT)
     *   port                =    1*5(DIGIT)
     *   ssrc                =    8*8(HEX)
     *   channel             =    1*3(DIGIT)
     *   address             =    host
     *   mode                =    &lt;"&gt; *Method &lt;"&gt; | Method
     * </pre>
     */
    String getHeaderValue() {

        String header;
        header = this.protocol + "/" + this.profile;
        if (this.isTransportSpecified) header += "/" + this.transport;
        if (this.isDistributionSpecified) header += ";" + this.distribution.name().toLowerCase();
        if (this.isDestinationSpecified) header += ";destination=" + this.destination.getHostAddress();
        if (this.isSourceSpecified) header += ";source=" + this.source.getHostAddress();
        if (this.isLayersSpecified) header += ";layers=" + this.layers;
        if (this.isModeSpecified) header += ";mode=" + this.mode.name();
        if (this.isAppendSpecified && this.append) header += ";append";
        if (this.isTTLSpecified) header += ";ttl=" + ttl;
        if (this.isClientPortRangeSpecified) {
            header += ";client_port=" + this.firstClientPort;
            if (this.lastClientPort != this.firstClientPort) {
                header += "-" + this.lastClientPort;
            }
        }
        if (this.isMulticastPortRangeSpecified) {
            header += ";port=" + this.firstMulticastPort;
            if (this.lastMulticastPort != this.firstMulticastPort) {
                header += "-" + this.lastMulticastPort;
            }
        }
        if (this.isServerPortRangeSpecified) {
            header += ";server_port=" + this.firstServerPort;
            if (this.lastServerPort != this.firstServerPort) {
                header += "-" + this.lastServerPort;
            }
        }
        if (this.isInterleavedChannelRangeSpecified) {
            header += ";interleaved=" + this.firstInterleavedChannel;
            if (this.lastInterleavedChannel != this.firstInterleavedChannel) {
                header += "-" + this.lastInterleavedChannel;
            }
        }
        if (this.isSSRCSpecified) header += ";ssrc=" + ssrc;
        return header;
    }

    private void parseTransportHeader(String header) throws UnknownHostException {
        String[] fields = header.split(";");
        if (fields.length > 0) {
            parseRtspTransportSpec(fields[0]);
        }
        for (int i=1; i < fields.length; i++) {
            String field = fields[i];
            if (field.equals("unicast")) {
                setDistribution(Distribution.UNICAST);
            }
            else if (field.equals("multicast")) {
                setDistribution(Distribution.MULTICAST);
            }
            else if (field.startsWith("client_port=")) {
                String fieldValue = field.substring(field.indexOf('=')+1);
                if (fieldValue.indexOf('-') != -1) {
                    String[] ports = fieldValue.split("-");
                    int firstPort = Integer.parseInt(ports[0]);
                    int lastPort = Integer.parseInt(ports[1]);
                    setClientPortRange(firstPort, lastPort);
                }
                else {
                    int port = Integer.parseInt(fieldValue);
                    setClientPortRange(port, port);
                }
            }
            else if (field.startsWith("port=")) {
                String fieldValue = field.substring(field.indexOf('=')+1);
                if (fieldValue.indexOf('-') != -1) {
                    String[] ports = fieldValue.split("-");
                    int firstPort = Integer.parseInt(ports[0]);
                    int lastPort = Integer.parseInt(ports[1]);
                    setMulticastPortRange(firstPort, lastPort);
                }
                else {
                    int port = Integer.parseInt(fieldValue);
                    setMulticastPortRange(port, port);
                }
            }
            else if (field.startsWith("server_port=")) {
                String fieldValue = field.substring(field.indexOf('=')+1);
                if (fieldValue.indexOf('-') != -1) {
                    String[] ports = fieldValue.split("-");
                    int firstPort = Integer.parseInt(ports[0]);
                    int lastPort = Integer.parseInt(ports[1]);
                    setServerPortRange(firstPort, lastPort);
                }
                else {
                    int port = Integer.parseInt(fieldValue);
                    setServerPortRange(port, port);
                }
            }
            else if (field.startsWith("interleaved=")) {
                String fieldValue = field.substring(field.indexOf('=')+1);
                if (fieldValue.indexOf('-') != -1) {
                    String[] channels = fieldValue.split("-");
                    int firstChannel = Integer.parseInt(channels[0]);
                    int lastChannel = Integer.parseInt(channels[1]);
                    setInterleavedChannelRange(firstChannel, lastChannel);
                }
                else {
                    int channel = Integer.parseInt(fieldValue);
                    setInterleavedChannelRange(channel,channel);
                }
            }
            else if (field.startsWith("mode=")) {
                String mode = field.substring(field.indexOf('=')+1);
                int firstIndexOf = mode.indexOf('"');
                if (firstIndexOf != -1) {
                    int lastIndexOf = mode.lastIndexOf('"');
                    if (lastIndexOf > firstIndexOf) {
                        // Mode is quoted
                        mode = mode.substring(firstIndexOf+1,lastIndexOf);
                    }
                }
                setMode(Mode.valueOf(mode.toUpperCase()));
            }
            else if (field.startsWith("ttl=")) {
                setTTL(Integer.parseInt(field.substring(field.indexOf('=')+1)));
            }
            else if (field.startsWith("ssrc=")) {
                setSSRC(Integer.parseInt(field.substring(field.indexOf('=')+1)));
            }
            else if (field.startsWith("destination=")) {
                setDestination(InetAddress.getByName(field.substring(field.indexOf('=')+1)));
            }
            else if (field.startsWith("source=")) {
                    setSource(InetAddress.getByName(field.substring(field.indexOf('=')+1)));
            }
            else if (field.startsWith("layers=")) {
                setLayers(Integer.parseInt(field.substring(field.indexOf('=')+1)));
            }
            else if (field.equals("append")) {
                setAppend(true);
            }
        }
    }


    /**
     * Parses a transport specification and sets internal attributes accordingly.
     * @param transportSpec - The transport specification (e.g. TCP/RTP/AVP).
     */
    private void parseSdpTransportSpec(String transportSpec) {
        String[] fields = transportSpec.split("/");
        if (fields.length == 3) {
            setTransport(fields[0]);
            setProtocol(fields[1]); 
            setProfile(fields[2]);
        }
        else if (fields.length == 2) {
            setProtocol(fields[0]); 
            setProfile(fields[1]);
        }
    }

    /**
     * Parses a transport specification from a Transport header and sets internal attributes accordingly.
     * @param transportSpec - The transport specification (e.g. RTP/AVP/TCP).
     */
    private void parseRtspTransportSpec(String transportSpec) {
        String[] fields = transportSpec.split("/");
        if (fields.length > 0) {
            setProtocol(fields[0]); 
           if (fields.length > 1) {
               setProfile(fields[1]);
               if (fields.length > 2) {
                   setTransport(fields[2]);
               }
           }
        }
    }

}
