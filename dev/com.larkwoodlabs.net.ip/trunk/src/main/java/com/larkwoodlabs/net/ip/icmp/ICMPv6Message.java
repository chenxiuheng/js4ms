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

package com.larkwoodlabs.net.ip.icmp;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.util.buffer.BufferBackedObject;
import com.larkwoodlabs.util.buffer.fields.ByteArrayField;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.buffer.fields.IntegerField;
import com.larkwoodlabs.util.buffer.fields.SelectorField;
import com.larkwoodlabs.util.buffer.fields.ShortField;
import com.larkwoodlabs.util.buffer.parser.BufferParserSelector;
import com.larkwoodlabs.util.buffer.parser.KeyedBufferParser;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

/**
 * <pre>
 *  0               1               2               3
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      Type     |      Code     |          Checksum             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                                                               |
 * .                                                               .
 * .                         Message Body                          .
 * .                                                               .
 * |                                                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <h2>Type: 8-bit</h2>
 * The type field indicates the type of the message. Its
 * value determines the format of the remaining data.
 * 
 * <h2>Code: 8-bit</h2>
 * The code field depends on the message type. It is used
 * to create an additional level of message granularity.
 * 
 * <h2>Checksum: 16-bit</h2>
 * The checksum field is used to detect data
 * corruption in the ICMPv6 message and parts of the IPv6 header.
 * 
 * ICMPv6 messages are grouped into two classes: error messages and
 * informational messages. Error messages are identified as such by a zero in
 * the high-order bit of their message Type field values. Thus, error messages
 * have message types from 0 to 127; informational messages have message types
 * from 128 to 255.
 * 
 * @author Gregory Bumgardner
 */
public abstract class ICMPv6Message extends BufferBackedObject implements IPMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    public static interface ParserType extends KeyedBufferParser<ICMPv6Message> {
        
        public boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) throws MissingParserException, ParseException;

    }

    public static class Parser extends BufferParserSelector<ICMPv6Message> implements IPMessage.ParserType {

        public Parser() {
            super(new SelectorField<Byte>(ICMPv6Message.MessageType));
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
            return parser.verifyChecksum(buffer, sourceAddress, destinationAddress);
        }
    }


    /*-- Static Variables ---------------------------------------------------*/
    
    public static final Logger logger = Logger.getLogger(ICMPv6Message.class.getName());

    protected static final int HEADER_LENGTH = 4;

    public static final byte IP_PROTOCOL_NUMBER = 58;

    public static final ByteField   MessageType = new ByteField(0); 
    public static final ByteField   Code = new ByteField(1); 
    public static final ShortField  Checksum = new ShortField(2);

    // TODO: Get rid of this - use IPPacket functions?
    // Pseudo Header fields for checksum calculation
    public static final ByteArrayField  SourceAddress = new ByteArrayField(0,16);
    public static final ByteArrayField  DestinationAddress = new ByteArrayField(16,16);
    public static final IntegerField    PacketLength = new IntegerField(32);
    public static final ByteArrayField  Zeroes = new ByteArrayField(36,3);
    public static final ByteField       NextHeader = new ByteField(39);
    protected static final int PSEUDO_HEADER_LENGTH = 40;
    

    /*-- Member Variables ---------------------------------------------------*/

    byte protocolNumber;
    

    /*-- Member Functions ---------------------------------------------------*/
    
    /**
     * 
     * @param size
     */
    protected ICMPv6Message(int size) {
        super(size);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ICMPv6Message.ICMPv6Message", size));
            logState(logger);
        }
    }

    /**
     * Constructs an extension header object.
     * This constructor can be used to create a header object that provides
     * an opaque representation of header content. 
     * @param bytes
     * @param offset
     */
    public ICMPv6Message(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, HEADER_LENGTH));
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ICMPv6Message.ICMPv6Message", buffer));
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
        logger.info(ObjectId + " : protocol="+getProtocolNumber());
        logger.fine(ObjectId + " : protocol-number="+getProtocolNumber());
        logger.fine(ObjectId + " : header-length="+getHeaderLength());
        logger.fine(ObjectId + " : next-header="+getNextProtocolNumber());
    }

    /**
     * NOTE: You must call {@link #updateChecksum(byte[],byte[],int)} to
     * write the checksum prior to calling this method!
     */
    public void writeTo(ByteBuffer buffer) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ICMPv6Message.writeTo", buffer));
        }
        
        super.writeTo(buffer);
    }

    @Override
    public byte getProtocolNumber() {
        return IP_PROTOCOL_NUMBER;
    }

    @Override
    public void setProtocolNumber(byte protocolNumber) {
        // Do nothing - protocol number set in constructors
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
    public final void setNextMessage(IPMessage header) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public final void removeNextMessage() {
        // Do nothing in this class
    }

    /**
     * Gets the total length of this header in bytes.
     * Some ICMPv6 messages override this method to return a fixed value.
     */
    @Override
    public final int getHeaderLength() {
        return getMessageLength();
    }
    
    @Override
    public final int getTotalLength() {
        return getHeaderLength();
    }

    /**
     * 
     * @return
     */
    public int getMessageLength() {
        return getBufferInternal().limit();
    }

    /**
     * 
     * @return
     */
    public byte getType() {
        return MessageType.get(getBufferInternal());
    }
    
    /**
     * 
     * @param type
     */
    protected final void setType(byte type) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ICMPv6Message.setType", type));
        }
        
        MessageType.set(getBufferInternal(),type);
    }
    
    /**
     * 
     * @return
     */
    public final byte getCode() {
        return Code.get(getBufferInternal());
    }
    
    /**
     * 
     * @param code
     */
    protected final void setCode(byte code) {
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(Logging.entering(ObjectId, "ICMPv6Message.setCode", code));
        }
        
        Code.set(getBufferInternal(),code);
    }

    /**
     * 
     * @return
     */
    public final short getChecksum() {
        return Checksum.get(getBufferInternal());
    }
    
    /**
     * 
     * @param checksum
     */
    public final void setChecksum(short checksum) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ICMPv6Message.setChecksum", checksum));
        }
        
        Checksum.set(getBufferInternal(), checksum);
    }

    /**
     * Verifies the ICMP message checksum. Must be called after receiving an ICMPv6 packet.
     * @param sourceAddress - IP source address from IPv6 header.
     * @param destinationAddress - IP destination address from IPv6 header.
     * @param packetLength - Total packet length from IPv6 header.
     */
    public final void verifyChecksum(byte[] sourceAddress, byte[] destinationAddress, int packetLength) throws ParseException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "ICMPv6Message.verifyChecksu",
                                          Logging.address(sourceAddress),
                                          Logging.address(destinationAddress),
                                          packetLength));
        }

        // Save and clear checksum prior to computation
        short checksum = getChecksum();
        setChecksum((short)0);
        
        short computed = calculateChecksum(sourceAddress, destinationAddress, packetLength);

        if (computed != checksum) {
            logger.fine(ObjectId+" ICMP message has invalid checksum - received " + checksum + ", computed " + computed);
            throw new ParseException("ICMP message has invalid checksum - received " + checksum + ", computed " + computed);
        }
        
        setChecksum(checksum);
    }
    
    /**
     * Updates the ICMPv6 message checksum. Must be called when constructing an ICMPv6 packet.
     * @param sourceAddress - IP source address from IPv6 header.
     * @param destinationAddress - IP destination address from IPv6 header.
     * @param packetLength - Total packet length from IPv6 header.
     */
    public final void updateChecksum(byte[] sourceAddress, byte[] destinationAddress, int packetLength) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "ICMPv6Message.updateChecksum",
                                          Logging.address(sourceAddress),
                                          Logging.address(destinationAddress),
                                          packetLength));
        }
        
        // Clear checksum prior to computation
        setChecksum((short)0);
        short checksum = calculateChecksum(sourceAddress, destinationAddress, packetLength);
        setChecksum(checksum);
    }

    /**
     * The ICMPv6 message checksum.
     *
     * The Checksum is the 16-bit one's complement of the one's complement
     * sum of the whole ICMPv6 message and a pseudo header consisting of values
     * from the IPv6 header.  For computing the checksum, the Checksum field
     * is set to zero.  When receiving packets, the checksum MUST be verified
     *  before processing a packet. [RFC-1071]
     */
    public final short calculateChecksum(byte[] sourceAddress, byte[] destinationAddress, int packetLength) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                        "ICMPv6Message.calculateChecksum",
                                        Logging.address(sourceAddress),
                                        Logging.address(destinationAddress),
                                        packetLength));
        }

        // Construct pseudoHeader
        ByteBuffer pseudoHeader = ByteBuffer.allocate(PSEUDO_HEADER_LENGTH);
        SourceAddress.set(pseudoHeader, sourceAddress);
        DestinationAddress.set(pseudoHeader, destinationAddress);
        PacketLength.set(pseudoHeader, packetLength);
        Zeroes.set(pseudoHeader, new byte[3]);
        NextHeader.set(pseudoHeader, IP_PROTOCOL_NUMBER);

        int total = 0;
        byte[] buffer = pseudoHeader.array();
        int offset = pseudoHeader.arrayOffset();
        int length = PSEUDO_HEADER_LENGTH;
        int end = offset + length;

        while (offset < end) {
            total += (((buffer[offset++] << 8) & 0xFF00) | (buffer[offset++] & 0xFF));
        }
        
        buffer = getBufferInternal().array();
        offset = getBufferInternal().arrayOffset();
        length = getMessageLength();
        end = offset + length;

        while (offset < end) {
            total += (((buffer[offset++] << 8) & 0xFF00) | (buffer[offset++] & 0xFF));
        }

        // Fold to 16 bits
        while ((total & 0xFFFF0000) != 0) {
            total = (total & 0xFFFF) + (total >> 16);
        }

        total = (~total & 0xFFFF);

        return (short)total;
    }

}