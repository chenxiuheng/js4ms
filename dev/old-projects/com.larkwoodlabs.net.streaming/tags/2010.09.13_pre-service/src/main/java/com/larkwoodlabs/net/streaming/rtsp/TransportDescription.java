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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Logging;

/**
 * Describes the preferred or actual transport characteristics of a media stream.
 * 
 * @author Gregory Bumgardner
 */
public class TransportDescription {
    
    /*-- Inner Classes -------------------------------------------------------*/

    /**
     * Enumeration of streaming protocols.
     */
    enum Protocol {
        /** Use Real-time Transport Protocol (RTP). */
        RTP
    };

    /**
     * Enumeration of streaming protocol profiles.
     */
    enum Profile {
        /** Use RTP profile for audio and video conferences with minimal session control. */
        AVP
    };
    
    /**
     * Enumeration of streaming transports.
     */
    enum Transport {
        /** Use User Datagram Protocol. */
        UDP,
        /** Use Transmission Control Protocol. */
        TCP
    };

    /**
     * Enumeration of streaming distribution types.
     */
    enum Distribution {
        /** Use unicast destination addresses */
        unicast,
        /** Use multicast destination addresses */
        multicast
    };

    /**
     * Enumeration of streaming modes.
     */
    enum Mode {
        /** Request play mode. */
        PLAY,
        /** Request record mode. */
        RECORD 
    };

    /*-- Member Variables ----------------------------------------------------*/

    final String ObjectId = Logging.identify(this);

    boolean isProtocolSpecified = false;
    Protocol protocol = Protocol.RTP;

    boolean isProfileSpecified = false;
    Profile profile = Profile.AVP;
    
    boolean isTransportSpecified = false;
    Transport transport = Transport.UDP;

    boolean isDistributionSpecified = false;
    Distribution distribution = Distribution.multicast;
    
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

    boolean isClientPortSpecified = false;
    int firstClientStreamPort;
    int clientStreamCount = 0;

    boolean isMulticastPortSpecified = false;
    int firstMulticastStreamPort;
    int multicastStreamCount = 0;

    boolean isServerPortSpecified = false;
    int firstServerStreamPort;
    int serverStreamCount = 0;

    boolean isInterleavedChannelSpecified = false;
    int firstInterleavedStreamChannel;
    int interleavedStreamCount = 0;

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
     * 
     * 
     */
    public TransportDescription(String header) throws RtspException {
        parseTransportHeader(header);
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
    public void setProtocol(Protocol protocol) {
        this.isProtocolSpecified = true;
        this.protocol = protocol;
    }

    /**
     * Returns <code>true</code> if a protocol was specified in the transport header
     * or was explicitly set with a call to {@link #setProtocol(Protocol)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isProtocolSpecified() {
        return this.isProtocolSpecified;
    }
    
    /**
     * Gets the current protocol attribute value.
     */
    public Protocol getProtocol() {
        return this.protocol;
    }

    /**
     * Sets the profile attribute value.
     * @param profile - The new profile value.
     */
    public void setProfile(Profile profile) {
        this.isProfileSpecified = true;
        this.profile = profile;
    }

    /**
     * Returns <code>true</code> if a profile was specified in the transport header
     * or was explicitly set with a call to {@link #setProfile(Profile)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isProfileSpecified() {
        return this.isProfileSpecified;
    }

    /**
     * Gets the current profile attribute value.
     */
    public Profile getProfile() {
        return this.profile;
    }

    /**
     * Sets the transport attribute value.
     * @param transport - The new transport value.
     */
    public void setTransport(Transport transport) {
        this.isTransportSpecified = true;
        this.transport = transport;
    }

    /**
     * Returns <code>true</code> if a transport was specified in the transport header
     * or was explicitly set with a call to {@link #setTransport(Transport)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isTransportSpecified() {
        return this.isTransportSpecified;
    }

    /**
     * Gets the current transport attribute value.
     */
    public Transport getTransport() {
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
     * Returns the number of ports required for each stream (typically two for RTP/RTCP).
     */
    public int getPortsPerStream() {
        int portsPerStream = 1;
        if (this.protocol == Protocol.RTP) {
            portsPerStream = 2;
        }
        return portsPerStream;
    }

    /**
     * Sets the client port range.
     * @param firstStreamPort - The first port (e.g. even port for RTP packet stream).
     * @param streamCount - Number of separate RTP/RTCP streams that form the media stream (typically one).
     */
    public void setClientPortRange(int firstStreamPort, int streamCount) {
        this.isClientPortSpecified = true;
        this.firstClientStreamPort = firstStreamPort;
        this.clientStreamCount = streamCount;
    }

    /**
     * Returns <code>true</code> if a client port range was specified in the transport header
     * or was explicitly set with a call to {@link #setClientPortRange(int, int)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isClientPortSpecified() {
        return this.isClientPortSpecified;
    }

    /**
     * Gets the current first client stream port number.
     */
    public int getFirstClientStreamPort() {
        return this.firstClientStreamPort;
    }

    /**
     * Gets the current client stream count.
     */
    public int getClientStreamCount() {
        return this.clientStreamCount;
    }

    /**
     * Sets the multicast port range.
     * @param firstStreamPort - The first port (e.g. even port for RTP packet stream).
     * @param streamCount - Number of separate RTP/RTCP streams that form the media stream (typically one).
     */
    public void setMulticastPortRange(int firstStreamPort, int streamCount) {
        this.isMulticastPortSpecified = true;
        this.firstMulticastStreamPort = firstStreamPort;
        this.multicastStreamCount = streamCount;
    }

    /**
     * Returns <code>true</code> if a multicast port range was specified in the transport header
     * or was explicitly set with a call to {@link #setMulticastPortRange(int, int)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isMulticastPortSpecified() {
        return this.isMulticastPortSpecified;
    }

    /**
     * Gets the current first multicast stream port number.
     */
    public int getFirstMulticastStreamPort() {
        return this.firstMulticastStreamPort;
    }

    /**
     * Gets the current multicast stream count.
     */
    public int getMulticastStreamCount() {
        return this.multicastStreamCount;
    }

    /**
     * Sets the server port range.
     * @param firstStreamPort - The first port (e.g. even port for RTP packet stream).
     * @param streamCount - Number of separate RTP/RTCP streams that form the media stream (typically one).
     */
    public void setServerPortRange(int firstStreamPort, int streamCount) {
        this.isServerPortSpecified = true;
        this.firstServerStreamPort = firstStreamPort;
        this.serverStreamCount = streamCount;
    }

    /**
     * Returns <code>true</code> if a server port range was specified in the transport header
     * or was explicitly set with a call to {@link #setServerPortRange(int, int)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isServerPortSpecified() {
        return this.isServerPortSpecified;
    }

    /**
     * Gets the current first server stream port number.
     */
    public int getFirstServerStreamPort() {
        return this.firstServerStreamPort;
    }

    /**
     * Gets the current server stream count.
     */
    public int getServerStreamCount() {
        return this.serverStreamCount;
    }

    /**
     * Sets the interleaved channel range.
     * @param firstStreamChannel - The first channel number (e.g. even number for RTP packet stream).
     * @param streamCount - Number of separate RTP/RTCP streams that form the media stream (typically one).
     */
    public void setInterleavedChannelRange(int firstStreamChannel, int streamCount) {
        this.isInterleavedChannelSpecified = true;
        this.firstInterleavedStreamChannel = firstStreamChannel;
        this.interleavedStreamCount = streamCount;
    }

    /**
     * Returns <code>true</code> if an interleaved channel range was specified in the transport header
     * or was explicitly set with a call to {@link #setInterleavedChannelRange(int, int)} and <code>false</code>
     * if not specified or set.
     */
    public boolean isInterleavedChannelSpecified() {
        return this.isInterleavedChannelSpecified;
    }

    /**
     * Gets the current first interleaved channel number.
     */
    public int getFirstInterleavedStreamChannel() {
        return this.firstInterleavedStreamChannel;
    }

    /**
     * Gets the current interleaved channel count.
     */
    public int getInterleavedStreamCount() {
        return this.interleavedStreamCount;
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

        int portsPerStream = getPortsPerStream();

        String header;
        header = this.protocol.name() + "/" + this.profile.name();
        if (this.isTransportSpecified) header += "/" + this.transport.name();
        if (this.isDistributionSpecified) header += ";" + this.distribution.name();
        if (this.isDestinationSpecified) header += ";destination=" + this.destination.getHostAddress();
        if (this.isSourceSpecified) header += ";source=" + this.source.getHostAddress();
        if (this.isLayersSpecified) header += ";layers=" + this.layers;
        if (this.isModeSpecified) header += ";mode=" + this.mode.name();
        if (this.isAppendSpecified && this.append) header += ";append";
        if (this.isTTLSpecified) header += ";ttl=" + ttl;
        if (this.isClientPortSpecified) header += ";client_port=" + this.firstClientStreamPort;
        int clientPortCount = this.clientStreamCount * portsPerStream;
        if (clientPortCount > 1) header += "-" + (this.firstClientStreamPort + clientPortCount - 1);
        if (this.isMulticastPortSpecified) header += ";port=" + this.firstMulticastStreamPort;
        int multicastPortCount = this.multicastStreamCount * portsPerStream;
        if (multicastPortCount > 1) header += "-" + (this.firstMulticastStreamPort + multicastPortCount - 1);
        if (this.isServerPortSpecified) header += ";server_port=" + this.firstServerStreamPort;
        int serverPortCount = this.serverStreamCount * portsPerStream;
        if (serverPortCount > 1) header += "-" + (this.firstServerStreamPort + serverPortCount - 1);
        if (this.isInterleavedChannelSpecified) header += ";interleaved=" + this.firstInterleavedStreamChannel;
        int interleavedChannelCount = this.interleavedStreamCount * portsPerStream;
        if (interleavedChannelCount > 1) header += "-" + (this.firstInterleavedStreamChannel + interleavedChannelCount - 1);
        if (this.isSSRCSpecified) header += ";ssrc=" + ssrc;
        return header;
    }

    private void parseTransportHeader(String header) throws RtspException {
        String[] fields = header.split(";");
        if (fields.length > 0) {
            parseTransport(fields[0]);
        }
        for (int i=1; i < fields.length; i++) {
            String field = fields[i];
            if (field.equals("unicast")) {
                setDistribution(Distribution.unicast);
            }
            else if (field.equals("multicast")) {
                setDistribution(Distribution.multicast);
            }
            else if (field.startsWith("client_port=")) {
                String fieldValue = field.substring(field.indexOf('=')+1);
                if (fieldValue.indexOf('-') != -1) {
                    String[] ports = fieldValue.split("-");
                    int firstPort = Integer.parseInt(ports[0]);
                    int lastPort = Integer.parseInt(ports[1]);
                    setClientPortRange(firstPort, ((lastPort - firstPort) + 1) / 2);
                }
                else {
                    setClientPortRange(Integer.parseInt(fieldValue), 1);
                }
            }
            else if (field.startsWith("port=")) {
                String fieldValue = field.substring(field.indexOf('=')+1);
                if (fieldValue.indexOf('-') != -1) {
                    String[] ports = fieldValue.split("-");
                    int firstPort = Integer.parseInt(ports[0]);
                    int lastPort = Integer.parseInt(ports[1]);
                    setMulticastPortRange(firstPort, ((lastPort - firstPort) + 1) / 2);
                }
                else {
                    setMulticastPortRange(Integer.parseInt(fieldValue), 1);
                }
            }
            else if (field.startsWith("server_port=")) {
                String fieldValue = field.substring(field.indexOf('=')+1);
                if (fieldValue.indexOf('-') != -1) {
                    String[] ports = fieldValue.split("-");
                    int firstPort = Integer.parseInt(ports[0]);
                    int lastPort = Integer.parseInt(ports[1]);
                    setServerPortRange(firstPort, ((lastPort - firstPort) + 1) / 2);
                }
                else {
                    setServerPortRange(Integer.parseInt(fieldValue),1);
                }
            }
            else if (field.startsWith("interleaved=")) {
                String fieldValue = field.substring(field.indexOf('=')+1);
                if (fieldValue.indexOf('-') != -1) {
                    String[] channels = fieldValue.split("-");
                    int firstChannel = Integer.parseInt(channels[0]);
                    int lastChannel = Integer.parseInt(channels[1]);
                    setInterleavedChannelRange(firstChannel, ((lastChannel - firstChannel) + 1) / 2);
                }
                else {
                    setInterleavedChannelRange(Integer.parseInt(fieldValue), 1);
                }
            }
            else if (field.startsWith("mode=")) {
                setMode(Mode.valueOf(field.substring(field.indexOf('=')+1)));
            }
            else if (field.startsWith("ttl=")) {
                setTTL(Integer.parseInt(field.substring(field.indexOf('=')+1)));
            }
            else if (field.startsWith("ssrc=")) {
                setSSRC(Integer.parseInt(field.substring(field.indexOf('=')+1)));
            }
            else if (field.startsWith("destination=")) {
                try {
                    setDestination(InetAddress.getByName(field.substring(field.indexOf('=')+1)));
                }
                catch (UnknownHostException e) {
                    throw new RtspException(StatusCode.DestinationUnreachable,e.getMessage());
                }
            }
            else if (field.startsWith("source=")) {
                try {
                    setSource(InetAddress.getByName(field.substring(field.indexOf('=')+1)));
                }
                catch (UnknownHostException e) {
                    throw new RtspException(StatusCode.DestinationUnreachable,e.getMessage());
                }
            }
            else if (field.startsWith("layers=")) {
                setLayers(Integer.parseInt(field.substring(field.indexOf('=')+1)));
            }
            else if (field.equals("append")) {
                setAppend(true);
            }
            else {
                throw new RtspException(StatusCode.InvalidParameter,
                                        "'"+field+"' is not a recognized Transport header parameter");
            }
        }
    }


    /**
     * Parses a transport specification and sets internal attributes accordingly.
     * @param transport - The transport specification (e.g. RTP/AVP/TCP).
     * @throws RtspException If the transport specification contains an unrecognized protocol, profile or transport.
     */
    private void parseTransport(String transport) throws RtspException {
        String[] fields = transport.split("/");
        if (fields.length > 0) {
           try {
               this.protocol = Protocol.valueOf(fields[0]); 
           }
           catch (IllegalArgumentException e) {
               throw new RtspException(StatusCode.UnsupportedTransport, "unrecognized protocol: " + fields[0]);
           }
           if (fields.length > 1) {
               try {
                   this.profile = Profile.valueOf(fields[1]);
               }
               catch (IllegalArgumentException e) {
                   throw new RtspException(StatusCode.UnsupportedTransport, "unrecognized profile: " + fields[1]);
               }
               if (fields.length > 2) {
                   try {
                       setTransport(Transport.valueOf(fields[2]));
                   }
                   catch (IllegalArgumentException e) {
                       throw new RtspException(StatusCode.UnsupportedTransport, "unrecognized transport: " + fields[2]);
                   }
               }
           }
        }
    }
    
    
}
