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

package com.larkwoodlabs.net.ip.igmp;

import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.ipv4.IPv4Packet;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An IGMPv2 Leave Group Message. [See <a
 * href="http://www.ietf.org/rfc/rfc2236.txt">RFC-2236</a>]
 * 
 * <pre>
 *      0                   1                   2                   3
 *      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |  Type = 0x17  | Max Resp Code |           Checksum            |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                         Group Address                         |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 * Type
 * 
 *    0x17 = Leave Group
 * 
 * Max Response Time
 * 
 *    The Max Response Time field is meaningful only in Membership Query
 *    messages.
 *
 * Checksum
 * 
 *    The checksum is the 16-bit one's complement of the one's complement
 *    sum of the whole IGMP message (the entire IP payload).  For computing
 *    the checksum, the checksum field is set to zero.  When transmitting
 *    packets, the checksum MUST be computed and inserted into this field.
 *    When receiving packets, the checksum MUST be verified before
 *    processing a packet.
 * 
 * Group Address
 * 
 *    In a Leave Group message, the group address field holds the IP multicast
 *    group address of the group being left.
 * </pre>
 * 
 * @author Gregory Bumgardner
 * 
 */
public final class IGMPv2LeaveMessage extends IGMPGroupMessage {

    /*-- Inner Classes ------------------------------------------------------*/
    
    public static final class Parser implements IGMPMessage.ParserType {

        @Override
        public IGMPMessage parse(ByteBuffer buffer) throws ParseException {
            return new IGMPv2LeaveMessage(buffer);
        }

        @Override
        public boolean verifyChecksum(ByteBuffer buffer) throws MissingParserException, ParseException {
            return IGMPv2LeaveMessage.verifyChecksum(buffer);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }


    /*-- Static Variables ---------------------------------------------------*/
    
    public static final byte MESSAGE_TYPE = 0x17;

    public static final int BASE_MESSAGE_LENGTH = 8;


    /*-- Static Functions ---------------------------------------------------*/

    public static IGMPMessage.Parser getIGMPMessageParser() {
        return getIGMPMessageParser(new IGMPv2LeaveMessage.Parser());
    }

    public static IPMessage.Parser getIPMessageParser() {
        return getIPMessageParser(new IGMPv2LeaveMessage.Parser());
    }

    public static IPv4Packet.Parser getIPv4PacketParser() {
        return getIPv4PacketParser(new IGMPv2LeaveMessage.Parser());
    }

    public static IPPacket.Parser getIPPacketParser() {
        return getIPPacketParser(new IGMPv2LeaveMessage.Parser());
    }

    /**
     * Verifies the IGMP message checksum. Called by the parser prior to constructing the packet.
     * @param segment - the buffer segment containing the IGMP message.
     */
    public static boolean verifyChecksum(ByteBuffer buffer) {
        return Checksum.get(buffer) == IGMPMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH);
    }

    /**
     * Writes the IGMP message checksum into a buffer containing an IGMP message.
     */
    public static void setChecksum(ByteBuffer buffer) {
        Checksum.set(buffer, IGMPMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH));
    }

    
    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param groupAddress
     */
    public IGMPv2LeaveMessage(byte[] groupAddress) {
        super(BASE_MESSAGE_LENGTH,MESSAGE_TYPE,(byte)0,groupAddress);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv2LeaveMessage.IGMPv2LeaveMessage", Logging.address(groupAddress)));
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public IGMPv2LeaveMessage(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv2LeaveMessage.IGMPv2LeaveMessage", buffer));
        }
    }

    @Override
    public void writeChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv2LeaveMessage.writeChecksum", buffer, Logging.address(sourceAddress), Logging.address(destinationAddress)));
        }
        
        IGMPv2LeaveMessage.setChecksum(buffer);
    }

    @Override
    public int getMessageLength() {
        return BASE_MESSAGE_LENGTH;
    }

    @Override
    public byte getType() {
        return MESSAGE_TYPE;
    }

}
