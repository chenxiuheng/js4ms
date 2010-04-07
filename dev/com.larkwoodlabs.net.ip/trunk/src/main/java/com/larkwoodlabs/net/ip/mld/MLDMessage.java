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

package com.larkwoodlabs.net.ip.mld;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.icmp.ICMPv6Message;
import com.larkwoodlabs.net.ip.ipv6.IPv6HopByHopOptionsHeader;
import com.larkwoodlabs.net.ip.ipv6.IPv6Packet;
import com.larkwoodlabs.net.ip.ipv6.IPv6RouterAlertOption;
import com.larkwoodlabs.util.buffer.fields.ShortField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * <pre>
 * <h1>3.0 MLD Message Format</h1>
 * 
 *    MLD is a sub-protocol of ICMPv6, that is, MLD message types are a
 *    subset of the set of ICMPv6 messages, and MLD messages are identified
 *    in IPv6 packets by a preceding Next Header value of 58.  All MLD
 *    messages described in this document are sent with a link-local IPv6
 *    Source Address, an IPv6 Hop Limit of 1, and an IPv6 Router Alert
 *    option [RTR-ALERT] in a Hop-by-Hop Options header.  (The Router Alert
 *    option is necessary to cause routers to examine MLD messages sent to
 *    multicast addresses in which the routers themselves have no
 *    interest.)
 * 
 *    MLD messages have the following format:
 * 
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |     Type      |     Code      |          Checksum             |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |     Maximum Response Delay    |          Reserved             |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                                                               |
 *    +                                                               +
 *    |                                                               |
 *    +                       Multicast Address                       +
 *    |                                                               |
 *    +                                                               +
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 * <h2>3.1. Type (8-bits)</h2>
 *    There are four types of MLD messages:
 *    - Multicast Listener Query (Type = decimal 130)
 *    - Multicast Listener Report (Type = decimal 131)
 *    - Multicast Listener Done (Type = decimal 132)
 *    - Multicast V2 Listener Report (Type = decimal 143)
 * 
 *   These two subtypes of listener queries. They are differentiated
 *   by the contents of the Multicast Address field, as described in
 *   section 3.6:
 * 
 *   - A General Query, used to learn which multicast addresses have
 *     listeners on an attached link.
 *   - A Multicast-Address-Specific Query, used to learn if a
 *     particular multicast address has any listeners on an attached
 *     link.
 * 
 *    In the rest of this document, the above messages types are referred
 *    to simply as &quot;Query&quot;, &quot;Report&quot;, and &quot;Done&quot;.
 * 
 * <h2>3.2. Code (8-bits)</h2>
 *    Initialized to zero by the sender; ignored by receivers.
 * 
 * <h2>3.3. Checksum (16-bits)</h2>
 *    The standard ICMPv6 checksum, covering the entire MLD message plus a
 *    &quot;pseudo-header&quot; of IPv6 header fields [ICMPv6,IPv6].
 * 
 * <h2>3.4. Maximum Response Delay (16-bits)</h2>
 *    The Maximum Response Delay field is meaningful only in Query
 *    messages, and specifies the maximum allowed delay before sending a
 *    responding Report, in units of milliseconds.  In all other messages,
 *    it is set to zero by the sender and ignored by receivers.
 * 
 *    Varying this value allows the routers to tune the &quot;leave latency&quot;
 *    (the time between the moment the last node on a link ceases listening
 *    to a particular multicast address and moment the routing protocol is
 *    notified that there are no longer any listeners for that address), as
 *    discussed in section 7.8.  It also allows tuning of the burstiness of
 *    MLD traffic on a link, as discussed in section 7.3.
 * 
 * <h2>3.5. Reserved (16-bits)</h2>
 *    Initialized to zero by the sender; ignored by receivers.
 * 
 * <h2>3.6. Multicast Address (16-bytes)</h2>
 *    In a Query message, the Multicast Address field is set to zero when
 *    sending a General Query, and set to a specific IPv6 multicast address
 *    when sending a Multicast-Address-Specific Query.
 * 
 *    In a Report or Done message, the Multicast Address field holds a
 *    specific IPv6 multicast address to which the message sender is
 *    listening or is ceasing to listen, respectively.
 * 
 * <h2>3.7. Other fields</h2>
 *    The length of a received MLD message is computed by taking the IPv6
 *    Payload Length value and subtracting the length of any IPv6 extension
 *    headers present between the IPv6 header and the MLD message.  If that
 *    length is greater than 24 octets, that indicates that there are other
 *    fields present beyond the fields described above, perhaps belonging
 *    to a future backwards-compatible version of MLD.  An implementation
 *    of the version of MLD specified in this document MUST NOT send an MLD
 *    message longer than 24 octets and MUST ignore anything past the first
 *    24 octets of a received MLD message.  In all cases, the MLD checksum
 *    MUST be computed over the entire MLD message, not just the first 24
 *    octets.
 * </pre>
 * 
 * @author Gregory Bumgardner
 * 
 */
public abstract class MLDMessage extends ICMPv6Message {

    /*-- Inner Classes ------------------------------------------------------*/
    
    public static interface ParserType extends ICMPv6Message.ParserType {
        
    }

    public static final class Parser extends ICMPv6Message.Parser {

    }

    /*-- Static Variables ---------------------------------------------------*/
    
    public static final Logger logger = Logger.getLogger(MLDMessage.class.getName());

    public static final short MLD_ROUTER_ALERT_VALUE = 0;
    
    public static final ShortField MaximumResponseDelay = new ShortField(4);
    public static final ShortField Reserved = new ShortField(6);

    // 0:0:0:0:0:0:0:0 - General Query Multicast address
    public static final byte[] IPv6GeneralQueryGroupAddress = {
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
    };

    // FF02:0:0:0:0:0:0:1 - All nodes address
    public static final byte[] IPv6QueryDestinationAddress = {
        (byte)0xFF, (byte)0x02, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01
    };

    // FF02:0:0:0:0:0:0:16 - All MLDv2 routers address
    public static final byte[] IPv6ReportDestinationAddress = {
        (byte)0xFF, (byte)0x02, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x16
    };

    
    /*-- Static Functions ---------------------------------------------------*/
   
    public static MLDMessage.Parser getMLDMessageParser() {
        MLDMessage.Parser parser = new MLDMessage.Parser();
        parser.add(new MLDv1QueryMessage.Parser());
        parser.add(new MLDv1ReportMessage.Parser());
        parser.add(new MLDv2ReportMessage.Parser());
        parser.add(new MLDv1DoneMessage.Parser());
        return parser;
    }

    public static IPMessage.Parser getIPMessageParser() {
        IPMessage.Parser parser = new IPMessage.Parser();
        parser.add(getMLDMessageParser());
        return parser;
    }

    public static IPv6Packet.Parser getIPv6PacketParser() {
        IPv6Packet.Parser parser = new IPv6Packet.Parser();
        parser.setProtocolParser(getIPMessageParser());
        return parser;
    }

    public static IPPacket.Parser getIPPacketParser() {
        IPPacket.Parser parser = new IPPacket.Parser();
        parser.add(getIPv6PacketParser());
        return parser;
    }
   
    public static MLDMessage.Parser getMLDMessageParser(MLDMessage.ParserType messageParser) {
        MLDMessage.Parser parser = new MLDMessage.Parser();
        parser.add(messageParser);
        return parser;
    }
    
    public static IPMessage.Parser getIPMessageParser(MLDMessage.ParserType messageParser) {
        IPMessage.Parser parser = new IPMessage.Parser();
        parser.add(getMLDMessageParser(messageParser));
        return parser;
    }

    public static IPv6Packet.Parser getIPv6PacketParser(MLDMessage.ParserType messageParser) {
        IPv6Packet.Parser parser = new IPv6Packet.Parser();
        parser.setProtocolParser(getIPMessageParser(messageParser));
        return parser;
    }

    public static IPPacket.Parser getIPPacketParser(MLDMessage.ParserType messageParser) {
        IPPacket.Parser parser = new IPPacket.Parser();
        parser.add(getIPv6PacketParser(messageParser));
        return parser;
    }

    /**
     * Calculates the MLD message checksum for an MLD packet contained in a buffer.
     * @param segment - the buffer segment containing the MLD message.
     * @param messageLength - the length of the IGMP message.
     * @param sourceAddress An IPv6 (16-byte) address..
     * @param destinationAddress An IPv6 (16-byte) address.
     */
    public static short calculateChecksum(ByteBuffer buffer, int messageLength, byte[] sourceAddress, byte[] destinationAddress) {
        return IPPacket.calculateChecksum(buffer, Checksum, sourceAddress, destinationAddress, IP_PROTOCOL_NUMBER, messageLength);
    }

    public static IPv6Packet constructIPv6Packet(byte[] sourceAddress, byte[] destinationAddress, MLDMessage message) {
        IPv6HopByHopOptionsHeader optionsHeader = new IPv6HopByHopOptionsHeader();
        optionsHeader.addOption(new IPv6RouterAlertOption(MLD_ROUTER_ALERT_VALUE));
        IPv6Packet header =  new IPv6Packet((byte)0,            // Priority
                                            0,                  // Flow Label (no flow)
                                            (byte)1,            // Hop Limit (stops at router)
                                            sourceAddress,      // Host address
                                            destinationAddress, // Query or Report Address
                                            optionsHeader);     // First IP Protocol header - MLD message added later
        header.addProtocolMessage(message);
        return header;
    }
    

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param size
     * @param type
     */
    protected MLDMessage(int size, byte type) {
        super(size);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDMessage.MLDMessage", size, type));
        }
        
        setType(type);
        setCode((byte)0);
        setChecksum((short)0);
        MLDMessage.Reserved.set(getBufferInternal(), (short)0);
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param segment
     * @throws ParseException
     */
    protected MLDMessage(ByteBuffer segment) throws ParseException {
        super(segment);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDMessage.MLDMessage", segment));
            logState(logger);
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void log(Logger logger) {
        super.log(logger);
        logState(logger);
    }
    
    /**
     * Logs value of member variables declared or maintained by this class.
     * @param logger
     */
    private void logState(Logger logger) {
        logger.info(ObjectId + " : message-length="+getTotalLength());
    }
    
    /**
     * 
     * @return
     */
    public short getMaximumResponseDelay() {
        return MLDMessage.MaximumResponseDelay.get(getBufferInternal());
    }

    /**
     * 
     * @param milliseconds
     */
    public void setMaximumResponseDelay(short milliseconds) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDMessage.setMaximumResponseDelay", milliseconds));
        }
        
        MLDMessage.MaximumResponseDelay.set(getBufferInternal(), milliseconds);
    }
}
