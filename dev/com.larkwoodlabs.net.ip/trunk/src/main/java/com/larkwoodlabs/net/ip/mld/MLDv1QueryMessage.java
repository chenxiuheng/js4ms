/*
 * Copyright � 2009-2010 Larkwood Labs Software.
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

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.ipv6.IPv6Packet;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

/**
 * A Multicast Listener Query Message as described in  
 * [<a href="http://tools.ietf.org/html/rfc2710">RFC-2710</a>].
 * 
 * <h2>3.0 MLD Message Format</h2>
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
 * <pre>
 *     0               1               2               3
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |  Type = 130   |     Code      |          Checksum             |
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
 * </pre> 
 * <h2>3.1. Type (8-bits)</h2>
 *  A Multicast Listener Query has a type value of decimal 130.
 *  These are two subtypes of listener queries. They are differentiated
 *  by the contents of the Multicast Address field:
 *  <ul>
 *    <li>A General Query, used to learn which multicast addresses have
 *        listeners on an attached link.
 *    <li>A Multicast-Address-Specific Query, used to learn if a
 *        particular multicast address has any listeners on an attached
 *        link.
 *  </ul>
 *  See {@link #getType()}.
 * 
 * <h2>3.2. Code (8-bits)</h2>
 *    Initialized to zero by the sender; ignored by receivers.
 * 
 * <h2>3.3. Checksum (16-bits)</h2>
 *    The standard ICMPv6 checksum, covering the entire MLD message plus a
 *    &quot;pseudo-header&quot; of IPv6 header fields [ICMPv6,IPv6].<p>
 *    See {@link #getChecksum()}, {@link #setChecksum(short)},
 *    {@link #calculateChecksum(ByteBuffer, int, byte[], byte[])}
 *    and {@link #verifyChecksum(byte[], byte[], int)}.
 *    
 * 
 * <h2>3.4. Maximum Response Delay (16-bits)</h2>
 *    The Maximum Response Delay field is meaningful only in Query
 *    messages, and specifies the maximum allowed delay before sending a
 *    responding Report, in units of milliseconds.  In all other messages,
 *    it is set to zero by the sender and ignored by receivers.<p>
 * 
 *    Varying this value allows the routers to tune the &quot;leave latency&quot;
 *    (the time between the moment the last node on a link ceases listening
 *    to a particular multicast address and moment the routing protocol is
 *    notified that there are no longer any listeners for that address), as
 *    discussed in section 7.8.  It also allows tuning of the burstiness of
 *    MLD traffic on a link, as discussed in section 7.3.<p>
 *    See {@link #getMaximumResponseDelay()} and {@link #setMaximumResponseDelay(short)}.
 * 
 * <h2>3.5. Reserved (16-bits)</h2>
 *    Initialized to zero by the sender; ignored by receivers.
 * 
 * <h2>3.6. Multicast Address (16-bytes)</h2>
 *    In a Query message, the Multicast Address field is set to zero when
 *    sending a General Query, and set to a specific IPv6 multicast address
 *    when sending a Multicast-Address-Specific Query.<p>
 *    See {@link #getGroupAddress()}, {@link #setGroupAddress(byte[])}
 *    and {@link #setGroupAddress(java.net.InetAddress)}.
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
 *    octets.<p>
 *
 * @author Gregory Bumgardner
 */
public final class MLDv1QueryMessage extends MLDQueryMessage {

    public static class Parser implements MLDMessage.ParserType {

        @Override
        public MLDMessage parse(ByteBuffer buffer) throws ParseException {
            return new MLDv1QueryMessage(buffer);
        }

        @Override
        public boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) throws MissingParserException, ParseException {
            return MLDv1QueryMessage.verifyChecksum(buffer, sourceAddress, destinationAddress);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/
    
    public static final int BASE_MESSAGE_LENGTH = 24;
    public static final short QUERY_RESPONSE_INTERVAL = 10*1000; // 10 secs as ms


    /*-- Static Functions ---------------------------------------------------*/
    
    public static MLDMessage.Parser getMLDMessageParser() {
        return getMLDMessageParser(new MLDv1QueryMessage.Parser());
    }

    public static IPMessage.Parser getIPMessageParser() {
        return getIPMessageParser(new MLDv1QueryMessage.Parser());
    }

    public static IPv6Packet.Parser getIPv6PacketParser() {
        return getIPv6PacketParser(new MLDv1QueryMessage.Parser());
    }

    public static IPPacket.Parser getIPPacketParser() {
        return getIPPacketParser(new MLDv1QueryMessage.Parser());
    }

    public static IPv6Packet constructGeneralQueryPacket(byte[] sourceAddress) {
        return constructGroupQueryPacket(sourceAddress,IPv6GeneralQueryGroupAddress);
    }

    public static IPv6Packet constructGroupQueryPacket(byte[] sourceAddress, byte[] groupAddress) {
        MLDv1QueryMessage message = new MLDv1QueryMessage(groupAddress);
        message.setMaximumResponseDelay(QUERY_RESPONSE_INTERVAL);
        return constructIPv6Packet(sourceAddress, groupAddress, message);
    }

    /**
     * Verifies the MLD message checksum. Called by the parser prior to constructing the packet.
     * @param segment - the buffer segment containing the MLD message.
     * @param sourceAddress An IPv6 (16-byte) address..
     * @param destinationAddress An IPv6 (16-byte) address.
     */
    public static boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        return Checksum.get(buffer) == MLDMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH, sourceAddress, destinationAddress);
    }

    /**
     * Writes the MLD message checksum into a buffer containing an MLD message.
     * @param buffer - a byte array.
     * @param offset - the offset within the array at which to write the message.
     * @param sourceAddress An IPv6 (16-byte) address..
     * @param destinationAddress An IPv6 (16-byte) address.
     */
    public static void setChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        Checksum.set(buffer, MLDMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH, sourceAddress, destinationAddress));
    }

    
    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param groupAddress
     */
    public MLDv1QueryMessage(byte[] groupAddress) {
        super(BASE_MESSAGE_LENGTH,groupAddress);
        
        if (logger.isLoggable(Level.FINER)){
            logger.finer(Logging.entering(ObjectId, "MLDv1QueryMessage.MLDv1QueryMessage", Logging.address(groupAddress)));
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public MLDv1QueryMessage(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDv1QueryMessage.MLDv1QueryMessage", buffer));
        }
    }

    @Override
    public void writeChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "MLDv1QueryMessage.writeChecksum",
                                          buffer,
                                          Logging.address(sourceAddress),
                                          Logging.address(destinationAddress)));
        }
        
        MLDv1QueryMessage.setChecksum(buffer, sourceAddress, destinationAddress);
    }
    
    @Override
    public int getMessageLength() {
        return BASE_MESSAGE_LENGTH;
    }

}
