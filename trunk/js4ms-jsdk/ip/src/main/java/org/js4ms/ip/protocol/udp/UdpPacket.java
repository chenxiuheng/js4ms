package org.js4ms.ip.protocol.udp;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * UdpPacket.java [org.js4ms.jsdk:ip]
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
import org.js4ms.common.util.buffer.field.ShortField;
import org.js4ms.common.util.buffer.parser.MissingParserException;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.ip.IPMessage;
import org.js4ms.ip.IPPacket;
import org.js4ms.ip.ipv4.IPv4Packet;
import org.js4ms.ip.ipv6.IPv6Packet;




/**
 * Represents a User Datagram Protocol (UDP) packet.
 * <p>
 * See <a href="http://www.ietf.org/rfc/rfc768.txt">[RFC-768]</a> and <a
 * href="http://www.ietf.org/rfc/rfc2460.txt">[RFC-2460]</a>.
 * <h3>Format</h3> <blockquote>
 * 
 * <pre>
 *   0                   1                   2                   3   
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |          Source Port          |       Destination Port        | 
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |            Length             |           Checksum            | 
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  ~                          Data Octets ...                      ~
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <dl>
 * <dt><u>Source Port</u></dt>
 * <p>
 * <dd>The Source Port is an optional field, when meaningful, it indicates the port of the
 * sending process, and may be assumed to be the port to which a reply should be addressed
 * in the absence of any other information. If not used, a value of zero is inserted.
 * <p>
 * <dt><u>Destination Port</u></dt>
 * <p>
 * <dd>The Destination Port has a meaning within the context of a particular internet
 * destination address.
 * <p>
 * <dt><u>Length</u></dt>
 * <p>
 * <dd>Length is the length in octets of this user datagram including this header and the
 * data. (This means the minimum value of the length is eight.)
 * <p>
 * <dt><u>Checksum</u></dt>
 * <p>
 * <dd>Checksum is the 16-bit one's complement of the one's complement sum of a pseudo
 * header of information from the IP header, the UDP header, and the data, padded with
 * zero octets at the end (if necessary) to make a multiple of two octets. See
 * <p>
 * The pseudo header conceptually prefixed to the UDP header contains the source address,
 * the destination address, the protocol, and the UDP length. This information gives
 * protection against mis-routed datagrams. This checksum procedure is the same as is used
 * in TCP.
 * <p>
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
 * pseudo-header is that of the final destination. At the originating node, that address
 * will be in the last element of the Routing header; at the recipient(s), that address
 * will be in the Destination Address field of the IPv6 header.
 * <p>
 * The Next Header value in the pseudo-header identifies the upper-layer protocol (e.g., 6
 * for TCP, or 17 for UDP). It will differ from the Next Header value in the IPv6 header
 * if there are extension headers between the IPv6 header and the upper- layer header.
 * <p>
 * The Upper-Layer Packet Length in the pseudo-header is the length of the upper-layer
 * header and data (e.g., UDP header plus UDP data). Some upper-layer protocols carry
 * their own length information (e.g., the Length field in the UDP header); for such
 * protocols, that is the length used in the pseudo- header. Other protocols (such as TCP)
 * do not carry their own length information, in which case the length used in the
 * pseudo-header is the Payload Length from the IPv6 header, minus the length of any
 * extension headers present between the IPv6 header and the upper-layer header.
 * <p>
 * If the computed checksum is zero, it is transmitted as all ones (0xFFFF).
 * <p>
 * In IPv4, the checksum is optional. A zero transmitted checksum value means that the
 * transmitter generated no checksum (for debugging or for higher level protocols that
 * don't care).
 * <p>
 * In IPv6, the UDP checksum is not optional. An IPv6 node transmitting UDP packets must
 * compute a UDP checksum over the packet and the pseudo-header, and, if that computation
 * yields a result of zero, it must be changed to all ones (0xFFFF). IPv6 receivers must
 * discard UDP packets containing a zero checksum, and should log the error.
 * <p></dd>
 * </dl>
 * </blockquote>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class UdpPacket
                extends BufferBackedObject
                implements IPMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * A parser that constructs a {@link UdpPacket} from a {@link BufferSegment} and
     * optionally parses an {@link KeyedApplicationMessage} contained within that packet.
     */
    public final static class Parser
                    implements IPMessage.ParserType {

        @Override
        public final IPMessage parse(final ByteBuffer buffer) throws ParseException, MissingParserException {
            UdpPacket packet = new UdpPacket(buffer);
            return packet;
        }

        @Override
        public final Object getKey() {
            return IP_PROTOCOL_NUMBER;
        }

        @Override
        public final boolean verifyChecksum(final ByteBuffer segment, final byte[] sourceAddress, final byte[] destinationAddress) {
            return UdpPacket.verifyChecksum(segment, sourceAddress, destinationAddress);
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    /** Logger used to generate IPHeaderOption log entries. */
    public static final Logger logger = Logger.getLogger(UdpPacket.class.getName());

    /** Protocol number for UDP headers. */
    public final static byte IP_PROTOCOL_NUMBER = 17;

    /** */
    protected final static int BASE_HEADER_LENGTH = 8;

    /** */
    protected final static byte UDP_PRECEDENCE = IPv4Packet.PRECEDENCE_ROUTINE;

    /** */
    protected final static byte UDP_TTL = 64;

    /** */
    protected final static int CHECKSUM_OFFSET = 6;

    /** */
    public static final ShortField SourcePort = new ShortField(0);

    /** */
    public static final ShortField DestinationPort = new ShortField(2);

    /** */
    public static final ShortField Length = new ShortField(4);

    /** */
    public static final ShortField Checksum = new ShortField(6);

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return
     */
    public final static UdpPacket.Parser constructUdpPacketParser() {
        return new UdpPacket.Parser();
    }

    /**
     * @return
     */
    public final static IPMessage.Parser constructIPMessageParser() {
        IPMessage.Parser parser = new IPMessage.Parser();
        parser.add(constructUdpPacketParser());
        return parser;
    }

    /**
     * @return
     */
    public final static IPv4Packet.Parser constructIPv4PacketParser() {
        IPv4Packet.Parser parser = new IPv4Packet.Parser();
        parser.setProtocolParser(constructIPMessageParser());
        return parser;
    }

    /**
     * @return
     */
    public final static IPv6Packet.Parser constructIPv6PacketParser() {
        IPv6Packet.Parser parser = new IPv6Packet.Parser();
        parser.setProtocolParser(constructIPMessageParser());
        return parser;
    }

    /**
     * @return
     */
    public final static IPPacket.BufferParser constructIPPacketParser() {
        IPPacket.BufferParser parser = new IPPacket.BufferParser();
        parser.add(constructIPv4PacketParser());
        parser.add(constructIPv6PacketParser());
        return parser;
    }

    /**
     * Verifies the UDP message checksum. Called by the parser prior to constructing the
     * packet.
     * 
     * @param buffer
     *            - the buffer containing the UDP message.
     * @param sourceAddress
     *            - IP source address from IPv4 or IPv6 header.
     * @param destinationAddress
     *            - IP destination address from IPv4 or IPv6 header.
     */
    public final static boolean verifyChecksum(final ByteBuffer buffer,
                                               final byte[] sourceAddress,
                                               final byte[] destinationAddress) {

        short checksum = Checksum.get(buffer);

        // IPv4 UDP packets can have zero checksum
        if (sourceAddress.length == 4 && checksum == 0) {
            return true;
        }

        short computedChecksum = calculateChecksum(buffer, sourceAddress, destinationAddress);
        if (checksum != computedChecksum) {
            logger.warning("received UDP packet with invalid checksum: received=" + checksum + " computed=" + computedChecksum);
            return false;
        }
        return true;
    }

    /**
     * Calculates the UDP message checksum for a UDP packet contained in a buffer.
     * 
     * @param buffer
     *            - the buffer containing the UDP message.
     * @param sourceAddress
     *            - IP source address from IPv4 or IPv6 header.
     * @param destinationAddress
     *            - IP destination address from IPv4 or IPv6 header.
     */
    public final static short calculateChecksum(final ByteBuffer buffer,
                                                final byte[] sourceAddress,
                                                final byte[] destinationAddress) {
        return IPPacket.calculateChecksum(buffer, Checksum, sourceAddress, destinationAddress, IP_PROTOCOL_NUMBER,
                                          Length.get(buffer));
    }

    /*-- Member Variables ---------------------------------------------------*/

    /** */
    private ByteBuffer payload;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param sourcePort
     * @param destinationPort
     */
    public UdpPacket(final int sourcePort, final int destinationPort) {
        super(BASE_HEADER_LENGTH);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpPacket.UdpPacket", sourcePort, destinationPort));
        }

        setSourcePort(sourcePort);
        setDestinationPort(destinationPort);
        setChecksum((short) 0);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    /**
     * @param sourcePort
     * @param destinationPort
     * @param payload
     */
    public UdpPacket(final int sourcePort,
                     final int destinationPort,
                     final ByteBuffer payload) {
        this(sourcePort, destinationPort);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpPacket.UdpPacket", sourcePort, destinationPort, payload));
        }

        setPayload(payload);
    }

    /**
     * @param buffer
     * @throws ParseException
     */
    public UdpPacket(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_HEADER_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpPacket.UdpPacket", buffer));
        }

        this.payload = consume(buffer, Length.get(getBufferInternal()) - BASE_HEADER_LENGTH);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    @Override
    public final Logger getLogger() {
        return logger;
    }

    @Override
    public final void log(final Logger logger, final Level level) {
        super.log(logger, level);
        logState(logger, level);
    }

    /**
     * Logs value of member variables declared or maintained by this class.
     * 
     * @param logger
     */
    private final void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": source-port=" + getSourcePort()));
        logger.log(level,this.log.msg(": destination-port=" + getDestinationPort()));
        logger.log(level,this.log.msg(": total-length=" + getTotalLength()));
        logger.log(level,this.log.msg(": payload-length=" + getPayloadLength()));
        logger.log(level,this.log.msg(": checksum=" + getChecksum()));
        if (this.payload != null) {
            logger.log(level,this.log.msg("----> payload"));
            logger.log(level,this.log.msg(": buffer array-offset=" + this.payload.arrayOffset() +
                                     ", position=" + this.payload.position() +
                                     ", remaining=" + this.payload.remaining() +
                                     ", limit=" + this.payload.limit() +
                                     ", capacity=" + this.payload.capacity()));
            logger.log(level,this.log.msg("<---- payload"));
        }
    }

    @Override
    public final void writeTo(final ByteBuffer buffer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpPacket.writeTo", buffer));
        }

        super.writeTo(buffer);
        this.payload.rewind();
        buffer.put(this.payload);
        this.payload.rewind();
    }

    @Override
    public final void writeChecksum(final ByteBuffer buffer,
                                    final byte[] sourceAddress,
                                    final byte[] destinationAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpPacket.writeChecksum",
                                        buffer,
                                        Logging.address(sourceAddress),
                                        Logging.address(destinationAddress)));
        }

        Checksum.set(buffer, calculateChecksum(buffer, sourceAddress, destinationAddress));
    }

    @Override
    public final byte getProtocolNumber() {
        return IP_PROTOCOL_NUMBER;
    }

    @Override
    public final void setProtocolNumber(final byte protocolNumber) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpPacket.setProtocolNumber", protocolNumber));
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public final byte getNextProtocolNumber() {
        return IPMessage.NO_NEXT_HEADER;
    }

    @Override
    public final IPMessage getNextMessage() {
        return null;
    }

    @Override
    public final void setNextMessage(final IPMessage protocolHeader) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpPacket.setNextMessage", protocolHeader));
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public final void removeNextMessage() {
        // Do nothing in this class
    }

    @Override
    public final int getHeaderLength() {
        return BASE_HEADER_LENGTH;
    }

    /**
     * @return
     */
    public final int getLength() {
        return Length.get(getBufferInternal());
    }

    /**
     * @param length
     */
    protected final void setLength(final short length) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpPacket.setLength", length));
        }

        Length.set(getBufferInternal(), length);
    }

    @Override
    public final int getTotalLength() {
        return BASE_HEADER_LENGTH + getPayloadLength();
    }

    /**
     * @return
     */
    public final int getPayloadLength() {
        return this.payload.limit();
    }

    /**
     * @return
     */
    public final int getSourcePort() {
        return SourcePort.get(getBufferInternal()) & 0xFFFF;
    }

    /**
     * @param sourcePort
     */
    public final void setSourcePort(final int sourcePort) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpPacket.setSourcePort", sourcePort));
        }

        SourcePort.set(getBufferInternal(), (short) sourcePort);
    }

    /**
     * @return
     */
    public final int getDestinationPort() {
        return DestinationPort.get(getBufferInternal()) & 0xFFFF;
    }

    /**
     * @param destinationPort
     */
    public final void setDestinationPort(final int destinationPort) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpPacket.setDestinationPort", destinationPort));
        }

        DestinationPort.set(getBufferInternal(), (short) destinationPort);
    }

    /**
     * @param sourcePort
     * @param destinationPort
     */
    public final void setPorts(final int sourcePort, final int destinationPort) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpPacket.setPorts", sourcePort, destinationPort));
        }

        SourcePort.set(getBufferInternal(), (short) sourcePort);
        DestinationPort.set(getBufferInternal(), (short) destinationPort);
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
            logger.finer(this.log.entry("UdpPacket.setChecksum", checksum));
        }

        Checksum.set(getBufferInternal(), checksum);
    }

    /**
     * Returns a ByteBuffer that references the underlying byte array (if any)
     * currently attached to this packet.
     * 
     * @return
     */
    public final ByteBuffer getPayload() {
        return this.payload.slice();
    }

    /**
     * Attaches the specified byte array to this packet.
     * Note: This method stores a reference to the underlying byte array - the array is
     * NOT copied.
     * 
     * @param payload
     *            - The byte array that will become the packet payload.
     */
    public final void setPayload(final ByteBuffer payload) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("UdpPacket.setPayload", payload));
        }

        this.payload = payload.slice();
        setLength((short) (getHeaderLength() + this.payload.limit()));
    }

}
