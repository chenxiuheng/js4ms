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

package com.larkwoodlabs.net.ip.igmp;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.ipv4.IPv4Packet;
import com.larkwoodlabs.net.ip.ipv4.IPv4RouterAlertOption;
import com.larkwoodlabs.util.buffer.BufferBackedObject;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.buffer.fields.SelectorField;
import com.larkwoodlabs.util.buffer.fields.ShortField;
import com.larkwoodlabs.util.buffer.parser.BufferParserSelector;
import com.larkwoodlabs.util.buffer.parser.KeyedBufferParser;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Base class for IGMP Messages as described in
 * [<a href="http://www.ietf.org/rfc/rfc1112.txt">RFC-1112</a>],
 * [<a href="http://www.ietf.org/rfc/rfc2236.txt">RFC-2236</a>] and
 * [<a href="http://www.ietf.org/rfc/rfc3376.txt">RFC-3376</a>].
 * 
 * <pre>
 *      0                   1                   2                   3
 *      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |       Type    | Max Resp Time |          Checksum             |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                             ...                               |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * <h3>Type</h3>
 *  <ul>
 *    <li>0x11 V2/V3 Membership Query       [RFC-2236] and [RFC-3376]
 *    <li>0x12 Version 1 Membership Report  [RFC-1112]
 *    <li>0x16 Version 2 Membership Report  [RFC-2236]
 *    <li>0x17 Version 2 Leave Group        [RFC-2236]
 *    <li>0x22 Version 3 Membership Report  [RFC-3376]
 *  </ul>
 *  See {@link #MessageType}, {@link #getType()} and {@link #setType(byte)}.
 * 
 * <h3>Max Response Time (or Reserved)</h3>
 * 
 *    The Max Resp Time field is meaningful only in Membership Query messages.<p>
 *    See {@link #MaxRespCode}, {@link #getMaxRespCode()}, and {@link #setMaxRespCode()}.
 * 
 * <h3>Checksum</h3>
 * 
 *    The checksum is the 16-bit one's complement of the one's complement
 *    sum of the whole IGMP message (the entire IP payload).  For computing
 *    the checksum, the checksum field is set to zero.  When transmitting
 *    packets, the checksum MUST be computed and inserted into this field.
 *    When receiving packets, the checksum MUST be verified before
 *    processing a packet.<p>
 *    See {@link #Checksum}, {@link #getChecksum()}, {@link #setChecksum()}, and {@link #verifyChecksum()}.
 * 
 * @author Gregory Bumgardner
 */
public abstract class IGMPMessage extends BufferBackedObject implements IPMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    public static interface ParserType extends KeyedBufferParser<IGMPMessage> {

        public boolean verifyChecksum(ByteBuffer buffer) throws MissingParserException, ParseException;

    }

    public static final class Parser extends BufferParserSelector<IGMPMessage> implements IPMessage.ParserType {

        public Parser() {
            super(new SelectorField<Byte>(IGMPMessage.MessageType));
        }

        @Override
        public Object getKey() {
            return IP_PROTOCOL_NUMBER;
        }

        @Override
        public boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) throws MissingParserException, ParseException {
            ParserType parser = (ParserType)get(getKeyField(buffer));
            if (parser == null) {
                // Check for default parser (null key)
                parser = (ParserType)get(null);
                if (parser == null) {
                    throw new MissingParserException();
                }
            }
            return parser.verifyChecksum(buffer);
        }

    }

    /*-- Static Variables ---------------------------------------------------*/
    
    public static final Logger logger = Logger.getLogger(IGMPMessage.class.getName());

    /**
     * Protocol {@value}
     */
    public static final byte IP_PROTOCOL_NUMBER = 2;

    public static final int IGMPv2_MESSAGE_LENGTH = 8;

    public static final byte IGMP_PRECEDENCE = IPv4Packet.PRECEDENCE_INTERNETWORK_CONTROL;
    
    public static final byte IGMP_TTL = 1;

    public static final short IGMP_ROUTER_ALERT_VALUE = 0;
    
    /**
     * All multicast hosts address (224.0.0.1).
     */
    public static final byte[] IPv4GeneralQueryGroupAddress = {
        (byte)0, (byte)0, (byte)0, (byte)0
    };

    /**
     * All multicast hosts address (224.0.0.1).
     */
    public static final byte[] IPv4QueryDestinationAddress = {
        (byte)224, (byte)0, (byte)0, (byte)1
    };

    /**
     * All IGMP routers address (224.0.0.22).
     */
    public static final byte[] IPv4ReportDestinationAddress = {
        (byte)224, (byte)0, (byte)0, (byte)22
    };

    /**
     * The field that identifies the IGMP message type.
     */
    public static final ByteField MessageType = new ByteField(0);
    
    /**
     * Field that specifies the maximum response time (or Reserved).
     */
    public static final ByteField Reserved = new ByteField(1);
    
    /**
     * Field that contains the received or computed checksum for the IGMP message.
     */
    public static final ShortField Checksum = new ShortField(2);

    /*-- Static Functions ---------------------------------------------------*/
    
    public static IGMPMessage.Parser getIGMPMessageParser() {
        IGMPMessage.Parser parser = new IGMPMessage.Parser();
        parser.add(new IGMPQueryMessage.Parser());
        parser.add(new IGMPv3ReportMessage.Parser());
        parser.add(new IGMPv2LeaveMessage.Parser());
        return parser;
    }

    public static IPMessage.Parser getIPMessageParser() {
        IPMessage.Parser parser = new IPMessage.Parser();
        parser.add(getIGMPMessageParser());
        return parser;
    }

    public static IPv4Packet.Parser getIPv4PacketParser() {
        IPv4Packet.Parser parser = new IPv4Packet.Parser();
        parser.setProtocolParser(getIPMessageParser());
        return parser;
    }

    public static IPPacket.Parser getIPPacketParser() {
        IPPacket.Parser parser = new IPPacket.Parser();
        parser.add(getIPv4PacketParser());
        return parser;
    }
    
    public static IGMPMessage.Parser getIGMPMessageParser(IGMPMessage.ParserType messageParser) {
        IGMPMessage.Parser parser = new IGMPMessage.Parser();
        parser.add(messageParser);
        return parser;
    }
    public static IPMessage.Parser getIPMessageParser(IGMPMessage.ParserType messageParser) {
        IPMessage.Parser parser = new IPMessage.Parser();
        parser.add(getIGMPMessageParser(messageParser));
        return parser;
    }

    public static IPv4Packet.Parser getIPv4PacketParser(IGMPMessage.ParserType messageParser) {
        IPv4Packet.Parser parser = new IPv4Packet.Parser();
        parser.setProtocolParser(getIPMessageParser(messageParser));
        return parser;
    }

    public static IPPacket.Parser getIPPacketParser(IGMPMessage.ParserType messageParser) {
        IPPacket.Parser parser = new IPPacket.Parser();
        parser.add(getIPv4PacketParser(messageParser));
        return parser;
    }

    /**
     * Calculates the IGMP message checksum for an IGMP packet contained in a buffer.
     * @param segment - the buffer segment containing the IGMP message.
     * @param messageLength - the length of the IGMP message.
     */
    public final static short calculateChecksum(ByteBuffer buffer, int messageLength) {
        short checksum = IPPacket.calculateChecksum(buffer, Checksum, messageLength);
        return checksum;
    }

    public final static IPv4Packet constructIPv4Packet(byte[] sourceAddress, byte[] destinationAddress, IGMPMessage message) {
        IPv4Packet header =  new IPv4Packet(IGMP_PRECEDENCE,
                                            false, // normal delay
                                            false, // normal throughput
                                            false, // normal reliability
                                            false, // normal monetary cost
                                            (short)0, // Identification
                                            false, // may fragment
                                            false, // no more fragments
                                            (short)0, // Fragment Offset
                                            IGMP_TTL,
                                            sourceAddress,
                                            destinationAddress,
                                            message);
        header.addOption(new IPv4RouterAlertOption(IGMP_ROUTER_ALERT_VALUE));
        return header;
    }
    

    /*-- Member Functions ---------------------------------------------------*/
    
    protected IGMPMessage(int size, byte type, short maximumResponseTime) {
        super(size);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPMessage.IGMPMessage", size, type, maximumResponseTime));
        }
        
        Reserved.set(getBufferInternal(), (byte)0);
        setType(type);
        setChecksum((short)0);
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }
    
    protected IGMPMessage(ByteBuffer buffer) {
        super(buffer);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPMessage.IGMPMessage", buffer));
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
        logger.info(ObjectId + " : type="+getType());
        logger.info(ObjectId + " : checksum="+getChecksum());
    }

    @Override
    public final byte getProtocolNumber() {
        return IP_PROTOCOL_NUMBER;
    }

    @Override
    public final void setProtocolNumber(byte protocolNumber) {
        // Do nothing - protocol number is defined by derived classes
    }

    @Override
    public final byte getNextProtocolNumber() {
        return NO_NEXT_HEADER;
    }

    @Override
    public final IPMessage getNextMessage() {
        return null;
    }
    
    @Override
    public final void setNextMessage(IPMessage nextHeader) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPMessage.setNextMessage", nextHeader));
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public final void removeNextMessage() {
        // Do nothing 
    }

    public abstract int getMessageLength();
    
    @Override
    public final int getHeaderLength() {
        return getMessageLength();
    }

    @Override
    public final int getTotalLength() {
        return getMessageLength();
    }

    @Override
    public void writeTo(ByteBuffer buffer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPMessage.writeTo", buffer));
        }

        super.writeTo(buffer);
    }

    /**
     * Returns the value of the message {@link #MessageType Type} field.
     */
    public byte getType() {
        return MessageType.get(getBufferInternal());
    }
    
    /**
     * Sets the value of the message {@link #MessageType Type} field.
     */
    protected final void setType(byte type) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPMessage.setType", type));
        }
        
        MessageType.set(getBufferInternal(),type);
    }
    
    /**
     * Returns the value of the message {@link #Checksum} field.
     */
    public final short getChecksum() {
        return Checksum.get(getBufferInternal());
    }
    
    public final void setChecksum(short checksum) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPMessage.setChecksum", checksum));
        }
        
        Checksum.set(getBufferInternal(),checksum);
    }

    /**
     * Converts a time value in units of 1/10 second into
     * an 8-bit integer or floating-point value.
     * @param value - A time value expressed in units of 1/10 second.
     * @return An 8-bit value suitable for use in the {@linkplain #MaxRespCode Max Resp Code} field.
     */
    public final static byte convertTimeVal(short value) {
        if (value < 128) {
            return (byte)(value & 0xFF);
        }
        else {
            // convert to floating point
            short fp = (short)(value << 8); // fixed point 8.8
            int exponent = 3;
            while(fp > 0x01FF) {
                fp = (short)((fp >> 1) & 0x7FFF);
                exponent++;
            }
            byte mantissa = (byte)((fp >> 4) & 0x0F);
            return (byte)(0x80 | (exponent << 4) | mantissa);
        }
    }
}
