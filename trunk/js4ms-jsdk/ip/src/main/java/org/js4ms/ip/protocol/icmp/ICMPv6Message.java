package org.js4ms.ip.protocol.icmp;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ICMPv6Message.java [org.js4ms.jsdk:ip]
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


import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.BufferBackedObject;
import org.js4ms.common.util.buffer.field.ByteArrayField;
import org.js4ms.common.util.buffer.field.ByteField;
import org.js4ms.common.util.buffer.field.IntegerField;
import org.js4ms.common.util.buffer.field.SelectorField;
import org.js4ms.common.util.buffer.field.ShortField;
import org.js4ms.common.util.buffer.parser.BufferParserSelector;
import org.js4ms.common.util.buffer.parser.KeyedBufferParser;
import org.js4ms.common.util.buffer.parser.MissingParserException;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.ip.IPMessage;
import org.js4ms.ip.IPPacket;




/**
 * Represents an ICMPv6 message as described in [<a
 * href="http://tools.ietf.org/html/rfc2463">RFC-2463</a>].
 * <p>
 * <h3>Message Format</h3> <blockquote>
 * 
 * <pre>
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |      Type     |      Code     |          Checksum             |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  .                                                               .
 *  .                         Message Body                          .
 *  .                                                               .
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <dl>
 * <dt><u>Type</u></dt>
 * <p>
 * <dd>The type field indicates the type of the message. Its value determines the format
 * of the remaining data.</dd>
 * <p>
 * <dt><u>Code</u></dt>
 * <p>
 * <dd>The code field depends on the message type. It is used to create an additional
 * level of message granularity.</dd>
 * <p>
 * <dt><u>Checksum</u></dt>
 * <p>
 * <dd>The checksum field is used to detect data corruption in the ICMPv6 message and
 * parts of the IPv6 header. ICMPv6 messages are grouped into two classes: error messages
 * and informational messages. Error messages are identified as such by a zero in the
 * high-order bit of their message Type field values. Thus, error messages have message
 * types from 0 to 127; informational messages have message types from 128 to 255.</dd>
 * </dl>
 * </blockquote>
 * 
 * @author Gregory Bumgardner
 */
public class ICMPv6Message
                extends BufferBackedObject
                implements IPMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * 
     */
    public static interface ParserType
                    extends KeyedBufferParser<ICMPv6Message> {

        /**
         * @param buffer
         * @param sourceAddress
         * @param destinationAddress
         * @return
         * @throws MissingParserException
         * @throws ParseException
         */
        public boolean verifyChecksum(ByteBuffer buffer,
                                      byte[] sourceAddress,
                                      byte[] destinationAddress) throws MissingParserException, ParseException;

    }

    /**
     * 
     */
    public static class DefaultParser
                    implements ICMPv6Message.ParserType {

        @Override
        public ICMPv6Message parse(final ByteBuffer buffer) throws ParseException {
            return new ICMPv6Message(buffer);
        }

        @Override
        public boolean verifyChecksum(final ByteBuffer buffer,
                                      final byte[] sourceAddress,
                                      final byte[] destinationAddress) throws MissingParserException, ParseException {
            return ICMPv6Message.verifyChecksum(buffer, sourceAddress, destinationAddress);
        }

        @Override
        public Object getKey() {
            return null;
        }

    }

    /**
     * 
     */
    public static class Parser
                    extends BufferParserSelector<ICMPv6Message>
                    implements IPMessage.ParserType {

        /**
         *
         */
        public Parser() {
            super(new SelectorField<Byte>(ICMPv6Message.MessageType));
        }

        @Override
        public Object getKey() {
            return IP_PROTOCOL_NUMBER;
        }

        @Override
        public boolean verifyChecksum(final ByteBuffer buffer,
                                      final byte[] sourceAddress,
                                      final byte[] destinationAddress) throws MissingParserException, ParseException {
            ParserType parser = (ParserType) get(getKeyField(buffer));
            if (parser == null) {
                // Check for default parser (null key)
                parser = (ParserType) get(null);
                if (parser == null) {
                    throw new MissingParserException();
                }
            }
            return parser.verifyChecksum(buffer, sourceAddress, destinationAddress);
        }
    }

    /**
     * Verifies the ICMPv6 message checksum. Called by the parser prior to constructing
     * the packet.
     * 
     * @param buffer
     *            A buffer containing an ICMPv6 message.
     * @param sourceAddress
     *            An IPv6 (16-byte) address..
     * @param destinationAddress
     *            An IPv6 (16-byte) address.
     * @return
     */
    public static boolean verifyChecksum(final ByteBuffer buffer,
                                         final byte[] sourceAddress,
                                         final byte[] destinationAddress) {
        return Checksum.get(buffer) == ICMPv6Message.calculateChecksum(buffer, calculateMessageSize(buffer), sourceAddress,
                                                                       destinationAddress);
    }

    /**
     * Writes the ICMPv6 message checksum into a buffer containing an ICMPv6 message.
     * 
     * @param buffer
     *            A buffer containing an ICMPv6 message.
     * @param sourceAddress
     *            An IPv6 (16-byte) address..
     * @param destinationAddress
     *            An IPv6 (16-byte) address.
     */
    public static void setChecksum(final ByteBuffer buffer,
                                   final byte[] sourceAddress,
                                   final byte[] destinationAddress) {
        Checksum.set(buffer,
                     ICMPv6Message.calculateChecksum(buffer, calculateMessageSize(buffer), sourceAddress, destinationAddress));
    }

    /**
     * @param buffer
     * @return
     */
    public static short calculateMessageSize(ByteBuffer buffer) {
        return (short) buffer.remaining();
    }

    /**
     * Calculates the ICMPv6 message checksum for an ICMPv6 message contained in a buffer.
     * 
     * @param buffer
     *            A buffer containing an ICMPv6 message.
     * @param messageLength
     *            The length of the ICMPv6 message.
     * @param sourceAddress
     *            An IPv6 (16-byte) address..
     * @param destinationAddress
     *            An IPv6 (16-byte) address.
     * @return
     */
    public static short calculateChecksum(final ByteBuffer buffer,
                                          final int messageLength,
                                          final byte[] sourceAddress,
                                          final byte[] destinationAddress) {
        return IPPacket.calculateChecksum(buffer, Checksum, sourceAddress, destinationAddress, IP_PROTOCOL_NUMBER, messageLength);
    }

    /*-- Static Variables ---------------------------------------------------*/

    /** Logger used to generate ICMPv6 log entries. */
    public static final Logger logger = Logger.getLogger(ICMPv6Message.class.getName());

    /** */
    protected static final int HEADER_LENGTH = 4;

    /** */
    public static final int BASE_MESSAGE_LENGTH = 4;

    /** Protocol number for ICMPv6 headers. */
    public static final byte IP_PROTOCOL_NUMBER = 58;

    /** */
    public static final ByteField MessageType = new ByteField(0);

    /** */
    public static final ByteField Code = new ByteField(1);

    /** */
    public static final ShortField Checksum = new ShortField(2);

    // TODO: Get rid of this - use IPPacket functions?
    // Pseudo Header fields for checksum calculation
    /** */
    public static final ByteArrayField SourceAddress = new ByteArrayField(0, 16);

    /** */
    public static final ByteArrayField DestinationAddress = new ByteArrayField(16, 16);

    /** */
    public static final IntegerField PacketLength = new IntegerField(32);

    /** */
    public static final ByteArrayField Zeroes = new ByteArrayField(36, 3);

    /** */
    public static final ByteField NextHeader = new ByteField(39);

    /** */
    protected static final int PSEUDO_HEADER_LENGTH = 40;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param size
     */
    protected ICMPv6Message(int size) {
        super(size);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("ICMPv6Message.ICMPv6Message", size));
            logState(logger, Level.FINER);
        }
    }

    /**
     * @param buffer
     */
    public ICMPv6Message(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, HEADER_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("ICMPv6Message.ICMPv6Message", buffer));
            logState(logger, Level.FINER);
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void log(final Logger logger, final Level level) {
        super.log(logger, level);
        logState(logger, level);
    }

    /**
     * Logs value of member variables declared or maintained by this class.
     * 
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": protocol=" + getProtocolNumber()));
        logger.log(level,this.log.msg(": protocol-number=" + getProtocolNumber()));
        logger.log(level,this.log.msg(": header-length=" + getHeaderLength()));
        logger.log(level,this.log.msg(": next-header=" + getNextProtocolNumber()));
    }

    /**
     * NOTE: You must call {@link #updateChecksum(byte[],byte[],int)} to
     * write the checksum prior to calling this method!
     * 
     * @param buffer
     */
    @Override
    public void writeTo(final ByteBuffer buffer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("ICMPv6Message.writeTo", buffer));
        }

        super.writeTo(buffer);
    }

    @Override
    public byte getProtocolNumber() {
        return IP_PROTOCOL_NUMBER;
    }

    @Override
    public void setProtocolNumber(final byte protocolNumber) {
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
    public final void setNextMessage(final IPMessage header) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void removeNextMessage() {
        // Do nothing in this class
    }

    /**
     * Gets the total length of this header in bytes.
     * Some ICMPv6 messages override this method to return a fixed value.
     * 
     * @return
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
     * @return
     */
    public int getMessageLength() {
        return getBufferInternal().limit();
    }

    /**
     * @return
     */
    public byte getType() {
        return MessageType.get(getBufferInternal());
    }

    /**
     * @param type
     */
    protected final void setType(final byte type) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("ICMPv6Message.setType", type));
        }

        MessageType.set(getBufferInternal(), type);
    }

    /**
     * @return
     */
    public final byte getCode() {
        return Code.get(getBufferInternal());
    }

    /**
     * @param code
     */
    protected final void setCode(final byte code) {

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.log.entry("ICMPv6Message.setCode", code));
        }

        Code.set(getBufferInternal(), code);
    }

    /**
     * @return
     */
    public final short getChecksum() {
        return Checksum.get(getBufferInternal());
    }

    /**
     * @param checksum
     */
    public final void setChecksum(final short checksum) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("ICMPv6Message.setChecksum", checksum));
        }

        Checksum.set(getBufferInternal(), checksum);
    }

    /**
     * Verifies the ICMP message checksum. Must be called after receiving an ICMPv6
     * packet.
     * 
     * @param sourceAddress
     *            The IP source address from IPv6 header.
     * @param destinationAddress
     *            The IP destination address from IPv6 header.
     * @param packetLength
     *            The total packet length from IPv6 header.
     */
    public final void verifyChecksum(final byte[] sourceAddress,
                                     final byte[] destinationAddress,
                                     final int packetLength) throws ParseException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry(
                                        "ICMPv6Message.verifyChecksum",
                                        Logging.address(sourceAddress),
                                        Logging.address(destinationAddress),
                                        packetLength));
        }

        // Save and clear checksum prior to computation
        short checksum = getChecksum();
        setChecksum((short) 0);

        short computed = calculateChecksum(sourceAddress, destinationAddress, packetLength);

        if (computed != checksum) {
            logger.fine(this.log.msg("ICMP message has invalid checksum - received " + checksum + ", computed " + computed));
            throw new ParseException("ICMP message has invalid checksum - received " + checksum + ", computed " + computed);
        }

        setChecksum(checksum);
    }

    /**
     * Updates the ICMPv6 message checksum. Must be called when constructing an ICMPv6
     * packet.
     * 
     * @param sourceAddress
     *            The IP source address from IPv6 header.
     * @param destinationAddress
     *            The IP destination address from IPv6 header.
     * @param packetLength
     *            The total packet length from IPv6 header.
     */
    public final void updateChecksum(final byte[] sourceAddress,
                                     final byte[] destinationAddress,
                                     final int packetLength) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry(
                                        "ICMPv6Message.updateChecksum",
                                        Logging.address(sourceAddress),
                                        Logging.address(destinationAddress),
                                        packetLength));
        }

        // Clear checksum prior to computation
        setChecksum((short) 0);
        short checksum = calculateChecksum(sourceAddress, destinationAddress, packetLength);
        setChecksum(checksum);
    }

    @Override
    public void writeChecksum(final ByteBuffer buffer,
                              final byte[] sourceAddress,
                              final byte[] destinationAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry(
                                        "ICMPv6Message.writeChecksum",
                                        buffer,
                                        Logging.address(sourceAddress),
                                        Logging.address(destinationAddress)));
        }

        ICMPv6Message.setChecksum(buffer, sourceAddress, destinationAddress);
    }

    /**
     * Calculates the ICMPv6 message checksum.
     * The Checksum is the 16-bit one's complement of the one's complement
     * sum of the whole ICMPv6 message and a pseudo header consisting of values
     * from the IPv6 header. For computing the checksum, the Checksum field
     * is set to zero. When receiving packets, the checksum MUST be verified
     * before processing a packet. [RFC-1071]
     * 
     * @param sourceAddress
     * @param destinationAddress
     * @param packetLength
     * @return
     */
    public final short calculateChecksum(final byte[] sourceAddress,
                                         final byte[] destinationAddress,
                                         final int packetLength) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry(
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

        return (short) total;
    }

}
