package org.js4ms.ip.ipv6;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPv6Packet.java [org.js4ms.jsdk:ip]
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


import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.field.ByteArrayField;
import org.js4ms.common.util.buffer.field.ByteBitField;
import org.js4ms.common.util.buffer.field.ByteField;
import org.js4ms.common.util.buffer.field.FixedBufferField;
import org.js4ms.common.util.buffer.field.IntegerBitField;
import org.js4ms.common.util.buffer.field.ShortBitField;
import org.js4ms.common.util.buffer.field.ShortField;
import org.js4ms.common.util.buffer.parser.MissingParserException;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.ip.IPAuthenticationHeader;
import org.js4ms.ip.IPExtensionHeader;
import org.js4ms.ip.IPMessage;
import org.js4ms.ip.IPPacket;




/**
 * Represents an IPv6 datagram header. See [<a
 * href="http://tools.ietf.org/html/rfc2460">RFC-2460</a>]. <h3>Header Format</h3>
 * <blockquote>
 * 
 * <pre>
 *   0                   1                   2                   3   
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |Version| Traffic Class |           Flow Label                  |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |         Payload Length        |  Next Header  |   Hop Limit   |
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
 * </pre>
 * <dl>
 * <dt><u>Version</u></dt></p>
 * <dd>4-bit Internet Protocol version number = 6.
 * <p>
 * See {@link #getVersion()}.</dd>
 * <p>
 * <dt><u>Traffic Class</u></dt>
 * </p>
 * <dd>8-bit traffic class field. See section 7 in [RFC2460].
 * <p>
 * See {@link #getTrafficClass()}, {@link #setTrafficClass(byte)}.</dd>
 * <p>
 * <dt><u>Flow Label</u></dt>
 * </p>
 * <dd>20-bit flow label. See section 6 in [RFC2460].
 * <p>
 * See {@link #getFlowLabel()}, {@link #setFlowLabel(int)}.</dd>
 * <p>
 * <dt><u>Payload Length</u></dt>
 * </p>
 * <dd>16-bit unsigned integer. Length of the IPv6 payload, i.e., the rest of the packet
 * following this IPv6 header, in octets. (Note that any extension headers [section 4]
 * present are considered part of the payload, i.e., included in the length count.)
 * <p>
 * See {@link #getPayloadLength()}.</dd>
 * <p>
 * <dt><u>Next Header</u></dt>
 * </p>
 * <dd>8-bit selector. Identifies the type of header immediately following the IPv6
 * header. Uses the same values as the IPv4 Protocol field [RFC-1700 et seq.].
 * <p>
 * See {@link #getNextHeader()}, {@link #getNextProtocolNumber()},
 * {@link #addProtocolMessage(IPMessage)}.</dd>
 * <p>
 * <dt><u>Hop Limit</u></dt>
 * </p>
 * <dd>8-bit unsigned integer. Decremented by 1 by each node that forwards the packet. The
 * packet is discarded if Hop Limit is decremented to zero.
 * <p>
 * See {@link #getHopLimit()}, {@link #setHopLimit(byte)}.</dd>
 * <p>
 * <dt><u>Source Address</u></dt>
 * </p>
 * <dd>128-bit address of the originator of the packet.
 * <p>
 * See {@link #getSourceAddress()}, {@link #setSourceAddress(byte[])}.</dd>
 * <p>
 * <dt><u>Destination Address</u></dt>
 * </p>
 * <dd>128-bit address of the intended recipient of the packet (possibly not the ultimate
 * recipient, if a Routing header is present). See section 4.4 in [RFC2460].
 * <p>
 * See {@link #getDestinationAddress()}, {@link #setDestinationAddress(byte[])}.</dd>
 * </dl>
 * </blockquote>
 * 
 * @author Gregory Bumgardner
 */
public final class IPv6Packet
                extends IPPacket {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * 
     */
    public static class Parser
                    implements IPPacket.ParserType {

        /** */
        IPMessage.Parser protocolParser;

        /**
         * 
         */
        public Parser() {
            this(null);
        }

        /**
         * @param parser
         */
        public Parser(final IPMessage.Parser parser) {
            setProtocolParser(parser);
        }

        /**
         * @param parser
         */
        public void setProtocolParser(final IPMessage.Parser parser) {
            if (parser == null) {
                this.protocolParser = new IPMessage.Parser();
            }
            else {
                this.protocolParser = parser;
            }
        }

        /**
         * @return
         */
        public IPMessage.Parser getProtocolParser() {
            return this.protocolParser;
        }

        @Override
        public IPPacket parse(ByteBuffer buffer) throws ParseException, MissingParserException {
            IPv6Packet header = new IPv6Packet(buffer);

            if (this.protocolParser != null) {
                header.parsePayload(this.protocolParser);
            }

            return header;
        }

        @Override
        public IPPacket parse(final InputStream is) throws ParseException, MissingParserException, IOException {
            IPv6Packet header = new IPv6Packet(is);

            if (this.protocolParser != null) {
                header.parsePayload(this.protocolParser);
            }

            return header;
        }

        @Override
        public boolean verifyChecksum(ByteBuffer segment) throws MissingParserException, ParseException {
            return true;
        }

        @Override
        public Object getKey() {
            return IPv6Packet.INTERNET_PROTOCOL_VERSION;
        }
    }

    /*-- Static Variables ---------------------------------------------------*/

    /** Logger used to generate IPv6Packet log entries. */
    public static final Logger logger = Logger.getLogger(IPv6Packet.class.getName());

    /**
     * IPv6 header version.
     */
    public static final byte INTERNET_PROTOCOL_VERSION = 6;

    /** */
    private static final int BASE_HEADER_LENGTH = 40;

    /** */
    public static final FixedBufferField BaseHeader = new FixedBufferField(0, BASE_HEADER_LENGTH);

    /** */
    public static final ByteBitField Version = new ByteBitField(0, 4, 4);

    /** */
    public static final ShortBitField TrafficClass = new ShortBitField(0, 4, 8);

    /** */
    public static final IntegerBitField FlowLabel = new IntegerBitField(0, 24, 0);

    /** */
    public static final ShortField PayloadLength = new ShortField(4);

    /** */
    public static final ByteField NextHeader = new ByteField(6);

    /** */
    public static final ByteField HopLimit = new ByteField(7);

    /** */
    public static final ByteArrayField SourceAddress = new ByteArrayField(8, 16);

    /** */
    public static final ByteArrayField DestinationAddress = new ByteArrayField(24, 16);

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return
     */
    public static IPv6Packet.Parser getIPv6MessageParser() {
        IPMessage.Parser ipMessageParser = new IPMessage.Parser();
        ipMessageParser.add(new IPv6HopByHopOptionsHeader.Parser()); // TODO add standard
                                                                     // options
        ipMessageParser.add(new IPv6DestinationOptionsHeader.Parser());
        IPv6RoutingHeader.Parser routingParser = new IPv6RoutingHeader.Parser(); // TODO
                                                                                 // Add to
                                                                                 // Type0
                                                                                 // class
        routingParser.add(new IPv6RoutingHeader.Parser());
        ipMessageParser.add(routingParser);
        ipMessageParser.add(new IPv6FragmentHeader.Parser());
        ipMessageParser.add(51, new IPExtensionHeader.Parser()); // Authentication
        ipMessageParser.add(50, new IPExtensionHeader.Parser()); // Encapsulating Security
                                                                 // Payload
        IPv6Packet.Parser ipv6Parser = new IPv6Packet.Parser();
        ipv6Parser.setProtocolParser(ipMessageParser);
        return ipv6Parser;
    }

    /*-- Member Variables ---------------------------------------------------*/

    /** */
    private int parsedPayloadLength = 0;

    /** */
    private ByteBuffer unparsedPayload = null;

    /** */
    private IPv6FragmentHeader fragmentHeader = null;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param priority
     * @param flowLabel
     * @param hopLimit
     * @param sourceAddress
     * @param destinationAddress
     * @param firstProtocolHeader
     */
    public IPv6Packet(final byte trafficClass,
                      final int flowLabel,
                      final byte hopLimit,
                      final byte[] sourceAddress,
                      final byte[] destinationAddress,
                      final IPMessage firstProtocolHeader) {
        super(BASE_HEADER_LENGTH);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.log.entry("IPv6Packet.IPv6Packet",
                                       trafficClass,
                                       flowLabel,
                                       hopLimit,
                                       Logging.address(sourceAddress),
                                       Logging.address(destinationAddress),
                                       firstProtocolHeader));
        }
        setVersion(INTERNET_PROTOCOL_VERSION);
        setTrafficClass(trafficClass);
        setFlowLabel(flowLabel);
        setHopLimit(hopLimit);
        setSourceAddress(sourceAddress);
        setDestinationAddress(destinationAddress);
        setFirstProtocolMessage(firstProtocolHeader);
        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    /**
     * Constructs an IPv6 packet representation from the contents of the
     * specified ByteBuffer.
     * 
     * @param buffer
     * @throws ParseException
     */
    public IPv6Packet(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_HEADER_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6Packet.IPv6Packet", buffer));
        }

        ByteBuffer payload = consume(buffer, getPayloadLength());

        // Iterate through extension headers (if any) to look for fragment header
        parseExtensionHeaders(NextHeader.get(getBufferInternal()), payload);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger,Level.FINER);
        }
    }

    /**
     * Constructs an IPv6 packet representation from the specified byte stream..
     * 
     * @param is
     * @throws ParseException
     * @throws IOException
     */
    public IPv6Packet(final InputStream is) throws ParseException, IOException {
        super(consume(is, BASE_HEADER_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6Packet.IPv6Packet", is));
        }

        ByteBuffer payload = consume(is, getPayloadLength());

        // Iterate through extension headers (if any) to look for fragment header
        parseExtensionHeaders(NextHeader.get(getBufferInternal()), payload);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger,Level.FINER);
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
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": length=" + getTotalLength()));
        logger.log(level,this.log.msg(": version=" + getVersion()));
        logger.log(level,this.log.msg(": priority=" + getTrafficClass()));
        logger.log(level,this.log.msg(": flow-label=" + getFlowLabel()));
        logger.log(level,this.log.msg(": hop-limit=" + getHopLimit()));
        logger.log(level,this.log.msg(": payload-length=" + getPayloadLength()));
        logger.log(level,this.log.msg(": source-address=" + Logging.address(getSourceAddress())));
        logger.log(level,this.log.msg(": destination-address=" + Logging.address(getDestinationAddress())));
        logger.log(level,this.log.msg("----> protocol messages"));
        if (getFirstProtocolMessage() != null) {
            getFirstProtocolMessage().log(logger, level);
        }
        logger.log(level,this.log.msg("<---- protocol messages"));
    }

    @Override
    public void writeChecksum(final ByteBuffer buffer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6Packet.writeChecksum", buffer) + " *** EMPTY");
        }

    }

    @Override
    public byte getNextProtocolNumber() {
        return getNextHeader();
    }

    @Override
    protected void setNextProtocolNumber(final byte protocolNumber) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6Packet.setNextProtocolNumber", protocolNumber));
        }

        setNextHeader(protocolNumber);
    }

    @Override
    public int getHeaderLength() {
        return BASE_HEADER_LENGTH;
    }

    /**
     * Gets the current traffic class field value.
     * 
     * <pre>
     *   0               1               2               3 
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |       | Traffic Class |                                       |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return
     */
    public byte getTrafficClass() {
        return (byte) TrafficClass.get(getBufferInternal()).shortValue();
    }

    /**
     * Sets the current traffic class field value.
     * 
     * <pre>
     *   0               1               2               3 
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |       | Traffic Class |                                       |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @param trafficClass
     */
    public void setTrafficClass(final byte trafficClass) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6Packet.setTrafficClass", trafficClass));
        }

        TrafficClass.set(getBufferInternal(), (short) trafficClass);
    }

    /**
     * Gets the current flow label field value.
     * 
     * <pre>
     *  0               1               2               3
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |       |       |                   Flow Label                  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return
     */
    public int getFlowLabel() {
        return FlowLabel.get(getBufferInternal());
    }

    /**
     * Sets the current flow label field value.
     * 
     * <pre>
     *  0               1               2               3
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |       |       |                   Flow Label                  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @param flowLabel
     */
    public void setFlowLabel(final int flowLabel) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6Packet.setFlowLabel", flowLabel));
        }

        FlowLabel.set(getBufferInternal(), flowLabel);
    }

    /**
     * Gets the current payload length field value.
     * 
     * <pre>
     *  4               5               6               7
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |         Payload Length        |               |               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return
     */
    @Override
    public int getPayloadLength() {
        return (int) PayloadLength.get(getBufferInternal());
    }

    /**
     * Sets the payload length field value.
     * 
     * <pre>
     *  4               5               6               7
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |         Payload Length        |               |               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @param length
     */
    @Override
    protected void setPayloadLength(final int length) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6Packet.setPayloadLength", length));
        }

        PayloadLength.set(getBufferInternal(), (short) length);
    }

    /**
     * Gets the current next header type field value.
     * 
     * <pre>
     *  4               5               6               7
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                               |  Next Header  |               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return
     */
    public byte getNextHeader() {
        return NextHeader.get(getBufferInternal());
    }

    /**
     * Sets the next header type field value.
     * 
     * <pre>
     *  4               5               6               7
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                               |  Next Header  |               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @param protocolNumber
     */
    protected void setNextHeader(final byte protocolNumber) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6Packet.setNextHeader", protocolNumber));
        }

        NextHeader.set(getBufferInternal(), protocolNumber);
    }

    /**
     * Gets the current hop limit field value.
     * 
     * <pre>
     *  4               5               6               7
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                               |               |   Hop Limit   |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return
     */
    public byte getHopLimit() {
        return HopLimit.get(getBufferInternal());
    }

    /**
     * Sets the current hop limit field value.
     * 
     * <pre>
     *  4               5               6               7
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                               |               |   Hop Limit   |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @param hopLimit
     */
    public void setHopLimit(final byte hopLimit) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6Packet.setHopLimit", hopLimit));
        }

        HopLimit.set(getBufferInternal(), hopLimit);
    }

    /**
     * Gets the current source address field value.
     * This field contains the 128-bit address of the originator of the packet.
     * See [RFC-1884].
     * 
     * <pre>
     *  8               9               10              11
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * +                                                               +
     * |                                                               |
     * +                         Source Address                        +
     * |                                                               |
     * +                                                               +
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return An <code>Inet6Address</code> object cast as an <code>InetAddress</code> .
     */
    @Override
    public byte[] getSourceAddress() {
        return SourceAddress.get(getBufferInternal());
    }

    /**
     * Gets the current source address field value as an Inet6Address.
     * This field contains the 128-bit address of the originator of the packet.
     * See [RFC-1884].
     * 
     * <pre>
     *  8               9               10              11
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * +                                                               +
     * |                                                               |
     * +                         Source Address                        +
     * |                                                               |
     * +                                                               +
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return An <code>Inet6Address</code> object cast as an <code>InetAddress</code> .
     */
    @Override
    public InetAddress getSourceInetAddress() {
        try {
            return (Inet6Address) InetAddress.getByAddress(SourceAddress.get(getBufferInternal()));
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
            throw new Error(e.toString());
        }
    }

    /**
     * Sets the Source Address field value.
     * See {@link #getSourceAddress()}.
     * 
     * @param sourceAddress
     *            - an IPv6 address.
     */
    public void setSourceAddress(final InetAddress sourceAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6Packet.setSourceAddress", sourceAddress));
        }

        // Precondition.checkReference(sourceAddress);
        byte[] address = sourceAddress.getAddress();
        setSourceAddress(address);
    }

    /**
     * Sets the Source Address field value.
     * See {@link #getSourceAddress()}.
     * 
     * @param sourceAddress
     *            - an IPv6 address.
     */
    public void setSourceAddress(final byte[] address) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6Packet.setSourceAddress", Logging.address(address)));
        }

        // Precondition.checkIPv6Address(address);
        SourceAddress.set(getBufferInternal(), address);
    }

    /**
     * Gets the current destination address field value.
     * 
     * <pre>
     *  24              25              26              27
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * +                                                               +
     * |                                                               |
     * +                     Destination Address                       +
     * |                                                               |
     * +                                                               +
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return An <code>Inet6Address</code> object cast as an <code>InetAddress</code> .
     */
    @Override
    public byte[] getDestinationAddress() {
        return DestinationAddress.get(getBufferInternal());
    }

    /**
     * Gets the current destination address field value as an InetAddress.
     * 
     * <pre>
     *  24              25              26              27
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * +                                                               +
     * |                                                               |
     * +                     Destination Address                       +
     * |                                                               |
     * +                                                               +
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return An <code>Inet6Address</code> object cast as an <code>InetAddress</code> .
     */
    @Override
    public InetAddress getDestinationInetAddress() {
        try {
            return (Inet6Address) InetAddress.getByAddress(DestinationAddress.get(getBufferInternal()));
        }
        catch (UnknownHostException e) {
            throw new Error(e);
        }
    }

    /**
     * Sets the Destination Address field value.
     * See {@link #getDestinationAddress()}.
     * 
     * @param sourceAddress
     *            - an IPv6 address.
     */
    public void setDestinationAddress(final InetAddress destinationAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6Packet.setDestinationAddress", destinationAddress));
        }

        // Precondition.checkReference(destinationAddress);
        byte[] address = destinationAddress.getAddress();
        setDestinationAddress(address);
    }

    /**
     * Sets the Destination Address field value.
     * See {@link #getDestinationAddress()}.
     * 
     * @param sourceAddress
     *            - an IPv6 address.
     */
    public void setDestinationAddress(final byte[] address) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6Packet.setDestinationAddress", Logging.address(address)));
        }

        // Precondition.checkIPv6Address(address);
        DestinationAddress.set(getBufferInternal(), address);
    }

    @Override
    public boolean isMoreFragments() {
        if (this.fragmentHeader != null) {
            return this.fragmentHeader.getMoreFragments();
        }
        return false;
    }

    @Override
    public int getFragmentIdentifier() {
        if (this.fragmentHeader != null) {
            return this.fragmentHeader.getIdentification();
        }
        return 0;
    }

    @Override
    public int getFragmentOffset() {
        if (this.fragmentHeader != null) {
            return this.fragmentHeader.getFragmentOffset();
        }
        return 0;
    }

    @Override
    public ByteBuffer getFragment() {
        if (this.fragmentHeader != null) {
            return this.fragmentHeader.getFragment();
        }
        return this.unparsedPayload;
    }

    /**
     * Returns unparsed portion of packet payload or <code>null</code> if the packet is a
     * datagram fragment.
     * 
     * @return
     */
    public ByteBuffer getUnparsedPayload() {
        return this.unparsedPayload.slice();
    }

    /**
     * Sets unparsed portion of packet payload, updates the total packet length
     * and changes packet state to indicate that the packet is unfragmented.
     * Used in datagram reassembly process.
     * 
     * @param reassembledPayload
     * @throws ParseException
     */
    public void setReassembledPayload(final ByteBuffer reassembledPayload) throws ParseException {
        this.unparsedPayload = reassembledPayload.slice();
        setPayloadLength(this.parsedPayloadLength + unparsedPayload.limit());
        this.fragmentHeader = null;
        parseExtensionHeaders(getLastProtocolNumber(), this.unparsedPayload);
    }

    /**
     * @param protocolParser
     * @throws ParseException
     * @throws MissingParserException
     */
    public void parsePayload(final IPMessage.Parser protocolParser) throws ParseException, MissingParserException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6Packet.parsePayload", protocolParser));
        }

        // Parse IP protocol headers
        byte lastProtocolNumber = getLastProtocolNumber();
        // Check checksum before we consume the payload
        if (!protocolParser.verifyChecksum(this.unparsedPayload, lastProtocolNumber, getSourceAddress(), getDestinationAddress())) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(this.log.msg("invalid checksum detected in IP payload"));
            }
            throw new ParseException("invalid checksum detected in IP protocol packet");
        }
        IPMessage nextHeader = protocolParser.parse(this.unparsedPayload, lastProtocolNumber);
        addProtocolMessage(nextHeader);
        while (nextHeader.getNextProtocolNumber() != IPMessage.NO_NEXT_HEADER) {
            lastProtocolNumber = nextHeader.getNextProtocolNumber();
            if (!protocolParser.verifyChecksum(this.unparsedPayload, lastProtocolNumber, getSourceAddress(),
                                               getDestinationAddress())) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(this.log.msg("invalid checksum detected in IP payload"));
                }
                throw new ParseException("invalid checksum detected in IP payload");
            }
            nextHeader = protocolParser.parse(this.unparsedPayload, lastProtocolNumber);
            addProtocolMessage(nextHeader);
        }

    }

    /**
     * @param nextHeader
     * @param payload
     * @throws ParseException
     */
    private void parseExtensionHeaders(byte nextHeader, final ByteBuffer payload) throws ParseException {
        while (true) {
            if (nextHeader == IPv6HopByHopOptionsHeader.IP_PROTOCOL_NUMBER) {
                IPv6HopByHopOptionsHeader header = new IPv6HopByHopOptionsHeader(payload);
                this.addProtocolMessage(header);
                this.parsedPayloadLength += header.getTotalLength();
                nextHeader = header.getNextProtocolNumber();
            }
            else if (nextHeader == IPv6RoutingHeader.IP_PROTOCOL_NUMBER) {
                IPv6RoutingHeader header = new IPv6RoutingHeader(payload);
                this.addProtocolMessage(header);
                this.parsedPayloadLength += header.getTotalLength();
                nextHeader = header.getNextProtocolNumber();
            }
            else if (nextHeader == IPv6FragmentHeader.IP_PROTOCOL_NUMBER) {
                // Construct the fragment header - it will track what remains of the
                // payload
                this.fragmentHeader = new IPv6FragmentHeader(payload);
                this.unparsedPayload = null;
                nextHeader = this.fragmentHeader.getNextProtocolNumber();
            }
            else if (nextHeader == IPv6DestinationOptionsHeader.IP_PROTOCOL_NUMBER) {
                IPv6DestinationOptionsHeader header = new IPv6DestinationOptionsHeader(payload);
                this.addProtocolMessage(header);
                this.parsedPayloadLength += header.getTotalLength();
                nextHeader = header.getNextProtocolNumber();
            }
            else if (nextHeader == IPAuthenticationHeader.IP_PROTOCOL_NUMBER) {
                IPv6DestinationOptionsHeader header = new IPv6DestinationOptionsHeader(payload);
                this.addProtocolMessage(header);
                this.parsedPayloadLength += header.getTotalLength();
                nextHeader = header.getNextProtocolNumber();
            }
            else {
                // We've reached a protocol header that we don't recognize
                this.unparsedPayload = payload.slice();
                break;
            }
        }
    }

}
