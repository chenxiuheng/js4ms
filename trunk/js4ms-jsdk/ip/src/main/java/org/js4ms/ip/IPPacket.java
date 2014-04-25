package org.js4ms.ip;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPPacket.java [org.js4ms.jsdk:ip]
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


import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.BufferBackedObject;
import org.js4ms.common.util.buffer.field.ByteBitField;
import org.js4ms.common.util.buffer.field.Field;
import org.js4ms.common.util.buffer.field.SelectorField;
import org.js4ms.common.util.buffer.parser.BufferParserSelector;
import org.js4ms.common.util.buffer.parser.KeyedBufferParser;
import org.js4ms.common.util.buffer.parser.KeyedStreamParser;
import org.js4ms.common.util.buffer.parser.MissingParserException;
import org.js4ms.common.util.buffer.parser.StreamParserSelector;
import org.js4ms.common.util.logging.Logging;




/**
 * Base class for the {@link org.js4ms.ip.ipv4.IPv4Packet IPv4Packet} and
 * {@link org.js4ms.ip.ipv6.IPv6Packet IPv6Packet} classes.
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public abstract class IPPacket
                extends BufferBackedObject {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * 
     */
    public static interface ParserType
                    extends KeyedBufferParser<IPPacket>, KeyedStreamParser<IPPacket> {

        public boolean verifyChecksum(ByteBuffer buffer) throws MissingParserException, ParseException;

    }

    /**
     * 
     */
    public static class BufferParser
                    extends BufferParserSelector<IPPacket> {

        public BufferParser() {
            super(new SelectorField<Byte>(IPPacket.Version));
        }

        public boolean verifyChecksum(final ByteBuffer buffer) throws MissingParserException, ParseException {
            ParserType parser = (ParserType) get(getKeyField(buffer));
            if (parser == null) {
                // Check for default parser (null key)
                parser = (ParserType) get(null);
                if (parser == null) {
                    throw new MissingParserException();
                }
            }
            return parser.verifyChecksum(buffer);
        }
    }

    /**
     * 
     */
    public static class StreamParser
                    extends StreamParserSelector<IPPacket> {

        public StreamParser() {
            super(new SelectorField<Byte>(IPPacket.Version));
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    /** Logger used to generate IPPacket log entries. */
    public static final Logger logger = Logger.getLogger(IPPacket.class.getName());

    /** */
    public static final ByteBitField Version = new ByteBitField(0, 4, 4);

    /*-- Member Variables ---------------------------------------------------*/

    /** */
    private IPMessage firstProtocolHeader = null;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param size
     */
    protected IPPacket(final int size) {
        super(size);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entry(this, size, firstProtocolHeader));
            logState(logger, Level.FINER);
        }
    }

    /**
     * @param buffer
     */
    protected IPPacket(final ByteBuffer buffer) {
        super(buffer);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entry(this, buffer));
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
        logger.log(level,this.log.msg(": version=" + getVersion()));
    }

    /**
     * Updates the checksum of a IP packet contained in the byte buffer.
     * Only applies to IPv4 but required for consistency with other message types.
     * 
     * @param buffer
     *            - a ByteBuffer containing a packet image.
     */
    public abstract void writeChecksum(final ByteBuffer buffer);

    /**
     * Gets the current header version field value.
     * 
     * <pre>
     *   0               1               2               3 
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |Version|       |               |                               |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return
     */
    public final byte getVersion() {
        return Version.get(getBufferInternal());
    }

    /**
     * @param version
     */
    protected final void setVersion(final byte version) {
        Version.set(getBufferInternal(), version);
    }

    /**
     * @return
     */
    public abstract byte[] getSourceAddress();

    /**
     * @return
     */
    public abstract InetAddress getSourceInetAddress();

    /**
     * @return
     */
    public abstract byte[] getDestinationAddress();

    /**
     * @return
     */
    public abstract InetAddress getDestinationInetAddress();

    /**
     * Indicates that the packet carries a datagram fragment and not a complete datagram.
     * 
     * @return
     */
    public final boolean isFragmented() {
        return getFragmentOffset() != 0 || isMoreFragments();
    }

    /**
     * Indicates whether this packet carries the first, or left-most fragment in a
     * datagram.
     * Only applies to packets that carry datagram fragments, but will return
     * <code>true</code> if the packet carries a complete datagram.
     * 
     * @return
     * @see {@link #isFragmented()}
     */
    public final boolean isFirstFragment() {
        return getFragmentOffset() == 0 && isMoreFragments();
    }

    /**
     * Indicates whether this packet carries the last, or right-most fragment in a
     * datagram.
     * Only applies to packets that carry datagram fragments, but will return
     * <code>false</code> if the packet carries a complete datagram.
     * 
     * @return
     * @see {@link #isFragmented()}
     */
    public final boolean isLastFragment() {
        return getFragmentOffset() != 0 && !isMoreFragments();
    }

    /**
     * Indicates whether additional packets are required to reconstruct a complete
     * datagram.
     * Returns <code>false</code> if this packet carries the last, or right-most fragment
     * of a datagram
     * or if the packet carries a complete datagram.
     * 
     * @return
     * @see {@link #isFragmented()}
     */
    public abstract boolean isMoreFragments();

    /**
     * Returns the temporally unique identifier assigned to all fragments that comprise a
     * single datagram.
     * 
     * @return
     */
    public abstract int getFragmentIdentifier();

    /**
     * Returns the fragment location within the original datagram.
     * The fragment offset is expressed in 8-byte units.
     * 
     * @return
     */
    public abstract int getFragmentOffset();

    /**
     * Returns the datagram fragment carried by this packet.
     * If the packet carries a complete datagram, this method will return
     * the unparsed portion of the packet payload.
     * 
     * @return
     */
    public abstract ByteBuffer getFragment();

    /**
     * @return
     */
    public abstract ByteBuffer getUnparsedPayload();

    /**
     * @param reassembledPayload
     * @throws ParseException
     */
    public abstract void setReassembledPayload(ByteBuffer reassembledPayload) throws ParseException;

    /**
     * @return
     */
    public abstract int getHeaderLength();

    /**
     * @return
     */
    public abstract int getPayloadLength();

    /**
     * @return
     */
    public int getTotalLength() {
        return getHeaderLength() + getPayloadLength();
    }

    /**
     * @param length
     */
    protected abstract void setPayloadLength(int length);

    /**
     * @return
     */
    public abstract byte getNextProtocolNumber();

    /**
     * @param protocolNumber
     */
    protected abstract void setNextProtocolNumber(byte protocolNumber);

    /**
     * @return
     */
    public final byte getLastProtocolNumber() {
        byte lastProtocolNumber = getNextProtocolNumber();
        IPMessage nextMessage = getFirstProtocolMessage();
        while (nextMessage != null) {
            lastProtocolNumber = nextMessage.getNextProtocolNumber();
            nextMessage = nextMessage.getNextMessage();
        }
        return lastProtocolNumber;
    }

    /**
     * @return
     */
    public final IPMessage getFirstProtocolMessage() {
        return this.firstProtocolHeader;
    }

    /**
     * @param message
     */
    protected final void setFirstProtocolMessage(final IPMessage message) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPPacket.setFirstProtocolMessage", message));
        }

        this.firstProtocolHeader = message;
        if (this.firstProtocolHeader != null) {
            setPayloadLength(firstProtocolHeader.getTotalLength());
            setNextProtocolNumber(firstProtocolHeader.getProtocolNumber());
        }
        else {
            setPayloadLength(0);
            setNextProtocolNumber(IPMessage.NO_NEXT_HEADER);
        }
    }

    /**
     * @param message
     */
    public final void addProtocolMessage(final IPMessage message) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPPacket.addProtocolMessage", message));
        }

        IPMessage nextMessage = getFirstProtocolMessage();
        if (nextMessage == null) {
            setFirstProtocolMessage(message);
        }
        else {
            int length = message.getTotalLength();
            IPMessage lastMessage = nextMessage;
            while (nextMessage != null) {
                length += nextMessage.getTotalLength();
                lastMessage = nextMessage;
                nextMessage = nextMessage.getNextMessage();
            }
            lastMessage.setNextMessage(message);
            setPayloadLength(length);
        }
    }

    /**
     * @param protocolNumber
     * @return
     */
    public final IPMessage getProtocolMessage(final byte protocolNumber) {
        IPMessage nextMessage = getFirstProtocolMessage();
        if (nextMessage == null) {
            return null;
        }
        else {
            while (nextMessage != null && nextMessage.getProtocolNumber() != protocolNumber) {
                nextMessage = nextMessage.getNextMessage();
            }
            return nextMessage;
        }
    }

    /**
     * Calculates upper-layer message checksum for protocols that include
     * an IP pseudo header in the checksum calculation.
     * This function is typically called from concrete implementations of the
     * {@link IPMessage#calculateChecksum(byte[], int, byte[], byte[])
     * IPMessage.calculateChecksum()} method.
     * <p>
     * The Checksum is the 16-bit one's complement of the one's complement sum of the
     * whole upper layer packet and a pseudo header consisting of values from the IP
     * header. While computing the checksum, the Checksum field will be set to zero. When
     * receiving packets, the checksum MUST be verified before processing a packet.
     * <p>
     * The pseudo header conceptually prefixed to the message header contains the source
     * address, the destination address, the protocol number, and the total message length
     * including header.
     * <p>
     * The IPv4 pseudo header has the following format:
     * 
     * <pre>
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |                        Source Address                         |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |                     Destination Address                       |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |      Zero     |    Protocol   |   Upper-Layer Packet Length   |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * The IPv6 pseudo header has a similar format:
     * 
     * <pre>
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |                                                               |
     *  +                                                               +
     *  |                                                               |
     *  +                         Source Address                        +
     *  |                                                               |
     *  +                                                               +
     *  |                                                               |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |                                                               |
     *  +                                                               +
     *  |                                                               |
     *  +                      Destination Address                      +
     *  |                                                               |
     *  +                                                               +
     *  |                                                               |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |                   Upper-Layer Packet Length                   |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |                      zero                     |  Next Header  |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * If the IPv6 packet contains a Routing header, the Destination Address used in the
     * pseudo-header is that of the final destination. At the originating node, that
     * address will be in the last element of the Routing header; at the recipient(s),
     * that address will be in the Destination Address field of the IPv6 header.
     * <p>
     * The Next Header value in the pseudo-header identifies the upper-layer protocol
     * (e.g., 6 for TCP, or 17 for UDP). It will differ from the Next Header value in the
     * IPv6 header if there are extension headers between the IPv6 header and the upper-
     * layer header.
     * <p>
     * The Upper-Layer Packet Length in the pseudo-header is the length of the upper-layer
     * header and data (e.g., UDP header plus UDP data). Some upper-layer protocols carry
     * their own length information (e.g., the Length field in the UDP header); for such
     * protocols, that is the length used in the pseudo- header. Other protocols (such as
     * TCP) do not carry their own length information, in which case the length used in
     * the pseudo-header is the Payload Length from the IPv6 header, minus the length of
     * any extension headers present between the IPv6 header and the upper-layer header.
     * <p>
     * If the computed checksum is zero, it is transmitted as all ones (0xFFFF).
     * <p>
     * In IPv4, the checksum is optional. A zero transmitted checksum value means that the
     * transmitter generated no checksum (for debugging or for higher level protocols that
     * don't care).
     * <p>
     * In IPv6, the UDP checksum is not optional. An IPv6 node transmitting UDP packets
     * must compute a UDP checksum over the packet and the pseudo-header, and, if that
     * computation yields a result of zero, it must be changed to all ones (0xFFFF). IPv6
     * receivers must discard UDP packets containing a zero checksum, and should log the
     * error.
     * <p>
     * See <a href="http://www.ietf.org/rfc/rfc768.txt">[RFC-768]</a> and <a
     * href="http://www.ietf.org/rfc/rfc2460.txt">[RFC-2460]</a>
     * <p>
     * 
     * @see IPMessage#calculateChecksum(byte[], int, byte[], byte[])
     * @param buffer
     *            - the ByteBuffer containing the upper-layer message.
     * @param checksumField
     *            - A Field object that is used to extract and clear the checksum value.
     * @param sourceAddress
     *            An IPv4 (4-byte) or IPv6 (16-byte) address. Size must match that of the
     *            destination address.
     * @param destinationAddress
     *            An IPv4 (4-byte) or IPv6 (16-byte) address. Size must match that of the
     *            destination address.
     * @param protocolNumber
     *            - the IP protocol number of the upper-layer protocol.
     * @param packetLength
     *            - the total length of the upper-layer message.
     * @return
     */
    public final static short calculateChecksum(final ByteBuffer buffer,
                                                final Field<Short> checksumField,
                                                final byte[] sourceAddress,
                                                final byte[] destinationAddress,
                                                final byte protocolNumber,
                                                final int packetLength) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(STATIC,
                                          "IPPacket.calculateChecksum",
                                          buffer,
                                          checksumField,
                                          Logging.address(sourceAddress),
                                          Logging.address(destinationAddress),
                                          protocolNumber,
                                          packetLength));
        }

        // Clear checksum field
        short originalChecksum = checksumField.get(buffer);
        checksumField.set(buffer, (short) 0);

        byte[] bytes = buffer.array();
        int offset = buffer.arrayOffset();

        int total = 0;

        for (int i = 0; i < sourceAddress.length;) {
            total += (((sourceAddress[i++] & 0xFF) << 8) | (sourceAddress[i++] & 0xFF));
        }

        for (int i = 0; i < destinationAddress.length;) {
            total += (((destinationAddress[i++] & 0xFF) << 8) | (destinationAddress[i++] & 0xFF));
        }

        total += protocolNumber;

        total += (((packetLength >> 16) & 0xFFFF) | (packetLength & 0xFFFF));

        if ((packetLength & 0x1) == 0) {
            // The packet length is even (ends on a 16-bit boundary)
            int end = offset + packetLength;
            while (offset < end) {
                total += (((bytes[offset++] & 0xFF) << 8) | (bytes[offset++] & 0xFF));
            }
        }
        else {
            // The packet length is odd (ends before a 16-bit boundary)
            // Total the bytes up to the last 16 bit boundary.
            int end = offset + packetLength - 1;
            while (offset < end) {
                total += (((bytes[offset++] & 0xFF) << 8) | (bytes[offset++] & 0xFF));
            }
            // Add the last byte (effectively adding a zero pad byte)
            total += ((bytes[offset] & 0xFF) << 8);
        }

        // Fold to 16 bits
        while ((total & 0xFFFF0000) != 0) {
            total = (total & 0xFFFF) + (total >> 16);
        }

        // Calculate the one's complement value
        total = (~total & 0xFFFF);

        // Restore original checksum
        checksumField.set(buffer, originalChecksum);

        return (short) total;
    }

    /**
     * Calculates upper-layer message checksum.
     * This function is typically called from concrete implementations of the
     * {@link IPMessage#calculateChecksum(byte[], int, byte[], byte[])
     * IPMessage.calculateChecksum()} method.
     * <p>
     * The Checksum is the 16-bit one's complement of the one's complement sum of the
     * whole upper layer packet. While computing the checksum, the Checksum field will be
     * set to zero. When receiving packets, the checksum MUST be verified before
     * processing a packet.
     * 
     * @param buffer
     *            - the ByteBuffer containing the upper-layer message.
     * @param checksumField
     *            - A Field object that is used to extract and clear the checksum value.
     * @param sourceAddress
     *            An IPv4 (4-byte) or IPv6 (16-byte) address. Size must match that of the
     *            destination address.
     * @param destinationAddress
     *            An IPv4 (4-byte) or IPv6 (16-byte) address. Size must match that of the
     *            destination address.
     * @param packetLength
     *            - the total length of the upper-layer message.
     * @return
     */
    public final static short calculateChecksum(final ByteBuffer buffer,
                                                final Field<Short> checksumField,
                                                final int packetLength) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(STATIC,
                                          "IPPacket.calculateChecksum",
                                          buffer,
                                          checksumField,
                                          packetLength));
        }

        // Clear checksum field
        short originalChecksum = checksumField.get(buffer);
        checksumField.set(buffer, (short) 0);

        byte[] bytes = buffer.array();
        int offset = buffer.arrayOffset();

        int total = 0;

        if ((packetLength & 0x1) == 0) {
            // The packet length is even (ends on a 16-bit boundary)
            int end = offset + packetLength;
            while (offset < end) {
                total += (((bytes[offset++] & 0xFF) << 8) | (bytes[offset++] & 0xFF));
            }
        }
        else {
            // The packet length is odd (ends before a 16-bit boundary)
            // Total the bytes up to the last 16 bit boundary.
            int end = offset + packetLength - 1;
            while (offset < end) {
                total += (((bytes[offset++] & 0xFF) << 8) | (bytes[offset++] & 0xFF));
            }
            // Add the last byte (effectively adding a zero pad byte)
            total += (short) ((bytes[offset] & 0xFF) << 8);
        }

        // Fold to 16 bits
        while ((total & 0xFFFF0000) != 0) {
            total = (total & 0xFFFF) + (total >> 16);
        }

        // Calculate the one's complement value
        total = (~total & 0xFFFF);

        // Restore original checksum
        checksumField.set(buffer, originalChecksum);

        return (short) total;
    }

}
