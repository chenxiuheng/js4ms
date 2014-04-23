package org.js4ms.ip.ipv4;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPv4Packet.java [org.js4ms.jsdk:ip]
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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.field.BooleanField;
import org.js4ms.common.util.buffer.field.ByteArrayField;
import org.js4ms.common.util.buffer.field.ByteBitField;
import org.js4ms.common.util.buffer.field.ByteField;
import org.js4ms.common.util.buffer.field.FixedBufferField;
import org.js4ms.common.util.buffer.field.ShortBitField;
import org.js4ms.common.util.buffer.field.ShortField;
import org.js4ms.common.util.buffer.parser.MissingParserException;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.ip.IPEndOfListOption;
import org.js4ms.ip.IPHeaderOption;
import org.js4ms.ip.IPMessage;
import org.js4ms.ip.IPPacket;
import org.js4ms.ip.IPPayload;




/**
 * An IPv4 packet as described in [<a
 * href="http://tools.ietf.org/html/rfc791">RFC-791</a>]. <h3>Packet Format</h3>
 * <blockquote>
 * 
 * <pre>
 *   0                   1                   2                   3   
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |Version|  IHL  |Type of Service|          Total Length         |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |         Identification        |Flags|      Fragment Offset    |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |  Time to Live |    Protocol   |         Header Checksum       |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                       Source Address                          |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                    Destination Address                        |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                    Options                    |    Padding    |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * <dl>
 * <dt><u>Version</u></dt></h2>
 * <p>
 * <dd>The Version field indicates the format of the internet header. This document
 * describes version 4.
 * <p>
 * See {@link #getVersion()}.</dd>
 * <p>
 * <dt><u>IHL</u></dt>
 * <p>
 * <dd>Internet Header Length is the length of the internet header in 32 bit words, and
 * thus points to the beginning of the data. Note that the minimum value for a correct
 * header is 5.
 * <p>
 * See {@link #getHeaderLength()}.</dd>
 * <p>
 * <dt><u>Type of Service or Differentiated Services Field</u></dt>
 * <p>
 * <dd><i>Note: The original definition of the TOS field has been superseded. This field
 * has be relabeled in part to become the Differentiated Services Code Point as described
 * in [<a href="http://tools.ietf.org/html/rfc2474">RFC-2474</a>].</i>
 * <p>
 * <p>
 * The Type of Service provides an indication of the abstract parameters of the quality of
 * service desired. These parameters are to be used to guide the selection of the actual
 * service parameters when transmitting a datagram through a particular network. Several
 * networks offer service precedence, which somehow treats high precedence traffic as more
 * important than other traffic (generally by accepting only traffic above a certain
 * precedence at time of high load). The major choice is a three way tradeoff between
 * low-delay, high-reliability, and high-throughput.
 * 
 * <pre>
 *       Bits 0-2:  Precedence.
 *       Bit    3:  0 = Normal Delay,      1 = Low Delay.
 *       Bits   4:  0 = Normal Throughput, 1 = High Throughput.
 *       Bits   5:  0 = Normal Relibility, 1 = High Relibility.
 *       Bit  6-7:  Reserved for Future Use.
 * 
 *          0     1     2     3     4     5     6     7
 *       +-----+-----+-----+-----+-----+-----+-----+-----+
 *       |                 |     |     |     |     |     |
 *       |   PRECEDENCE    |  D  |  T  |  R  |  0  |  0  |
 *       |                 |     |     |     |     |     |
 *       +-----+-----+-----+-----+-----+-----+-----+-----+
 * 
 *         Precedence
 * 
 *           111 - Network Control
 *           110 - Internetwork Control
 *           101 - CRITIC/ECP
 *           100 - Flash Override
 *           011 - Flash
 *           010 - Immediate
 *           001 - Priority
 *           000 - Routine
 * </pre>
 * 
 * The use of the Delay, Throughput, and Reliability indications may increase the cost (in
 * some sense) of the service. In many networks better performance for one of these
 * parameters is coupled with worse performance on another. Except for very unusual cases
 * at most two of these three indications should be set.
 * <p>
 * The type of service is used to specify the treatment of the datagram during its
 * distribution through the internet system. Example mappings of the internet type of
 * service to the actual service provided on networks such as AUTODIN II, ARPANET, SATNET,
 * and PRNET is given in &quot;Service Mappings&quot; [8].
 * <p>
 * The Network Control precedence designation is intended to be used within a network
 * only. The actual use and control of that designation is up to each network. The
 * Internetwork Control designation is intended for use by gateway control originators
 * only. If the actual use of these precedence designations is of concern to a particular
 * network, it is the responsibility of that network to control the access to, and use of,
 * those precedence designations.
 * <p>
 * See {@link #getTypeOfService()}, {@link #setTypeOfService(byte)}.</dd>
 * <p>
 * <dt><u>Total Length</u></dt>
 * <p>
 * <dd>Total Length is the length of the datagram, measured in octets, including internet
 * header and data. This field allows the length of a datagram to be up to 65,535 octets.
 * Such long datagrams are impractical for most hosts and networks. All hosts must be
 * prepared to accept datagrams of up to 576 octets (whether they arrive whole or in
 * fragments). It is recommended that hosts only send datagrams larger than 576 octets if
 * they have assurance that the destination is prepared to accept the larger datagrams.
 * <p>
 * The number 576 is selected to allow a reasonable sized data block to be transmitted in
 * addition to the required header information. For example, this size allows a data block
 * of 512 octets plus 64 header octets to fit in a datagram. The maximal internet header
 * is 60 octets, and a typical internet header is 20 octets, allowing a margin for headers
 * of higher level protocols.
 * <p>
 * See {@link #getTotalLength()}.</dd>
 * <p>
 * <dt><u>Identification</u></h2>
 * <p>
 * <dd>An identifying value assigned by the sender to aid in assembling the fragments of a
 * datagram.
 * <p>
 * See {@link #getIdentification()}, {@link #setIdentification(short)}.</dd>
 * <dt><u>Flags</u></dt>
 * <p>
 * <dd>Various Control Flags.
 * 
 * <pre>
 *       Bit 0: reserved, must be zero
 *       Bit 1: (DF) 0 = May Fragment,  1 = Don't Fragment.
 *       Bit 2: (MF) 0 = Last Fragment, 1 = More Fragments.
 *           0   1   2
 *         +---+---+---+
 *         |   | D | M |
 *         | 0 | F | F |
 *         +---+---+---+
 * </pre>
 * 
 * <p>
 * See {@link #getDoNotFragment()}, {@link #setDoNotFragment(boolean)},
 * {@link #getMoreFragments()}, {@link #setMoreFragments(boolean)}.</dd>
 * <p>
 * <dt><u>Fragment Offset</u></dt>
 * <p>
 * <dd>This field indicates where in the datagram this fragment belongs. The fragment
 * offset is measured in units of 8 octets (64 bits). The first fragment has offset zero.
 * <p>
 * See {@link #getFragmentOffset()}, {@link #setFragmentOffset(short)}.</dd>
 * <p>
 * <dt><u>Time to Live</u></dt>
 * <p>
 * <dd>This field indicates the maximum time the datagram is allowed to remain in the
 * internet system. If this field contains the value zero, then the datagram must be
 * destroyed. This field is modified in internet header processing. The time is measured
 * in units of seconds, but since every module that processes a datagram must decrease the
 * TTL by at least one even if it process the datagram in less than a second, the TTL must
 * be thought of only as an upper bound on the time a datagram may exist. The intention is
 * to cause undeliverable datagrams to be discarded, and to bound the maximum datagram
 * lifetime.
 * <p>
 * See {@link #getTTL()}, {@link #setTTL(byte)}.</dd>
 * <p>
 * <dt><u>Protocol</u></dt>
 * <p>
 * <dd>This field indicates the next level protocol used in the data portion of the
 * internet datagram. The values for various protocols are specified in &quot;Assigned
 * Numbers&quot; [9].
 * <p>
 * See {@link #getProtocol()}, {@link #setProtocol(byte)},
 * {@link #addProtocolMessage(IPMessage)}.</dd>
 * <p>
 * <dt><u>Header Checksum</u></dt>
 * <p>
 * <dd>A checksum on the header only. Since some header fields change (e.g., time to
 * live), this is recomputed and verified at each point that the internet header is
 * processed.
 * <p>
 * The checksum field is the 16 bit one's complement of the one's complement sum of all 16
 * bit words in the header. For purposes of computing the checksum, the value of the
 * checksum field is zero.
 * <p>
 * This is a simple to compute checksum and experimental evidence indicates it is
 * adequate, but it is provisional and may be replaced by a CRC procedure, depending on
 * further experience.
 * <p>
 * See {@link #getHeaderChecksum()}, {@link #setHeaderChecksum(short)}.</dd>
 * <p>
 * <dt><u>Source Address</u></dt>
 * <p>
 * <dd>The source address. See [RFC-791 Section 3.2].
 * <p>
 * See {@link #getSourceAddress()}, {@link #setSourceAddress(byte[])}.</dd>
 * <p>
 * <dt><u>Destination Address</u></dt>
 * <p>
 * <dd>The destination address. See [RFC-791 Section 3.2].
 * <p>
 * See {@link #getDestinationAddress()}, {@link #setDestinationAddress(byte[])}.</dd>
 * <p>
 * <dt><u>Options</u></dt>
 * <p>
 * <dd>The options may appear or not in datagrams. They must be implemented by all IP
 * modules (host and gateways). What is optional is their distribution in any particular
 * datagram, not their implementation.
 * <p>
 * See {@link #getOptions()}, {@link #addOption(IPHeaderOption)}.</dd>
 * </dl>
 * </blockquote>
 * 
 * @author Gregory Bumgardner
 */
public final class IPv4Packet
                extends IPPacket {

    /*-- Inner Classes ---------------------------------------------------*/

    /**
     * @author gbumgard
     */
    public static class Parser
                    implements IPPacket.ParserType {

        /** */
        IPHeaderOption.Parser optionParser;

        /** */
        IPMessage.Parser protocolParser;

        /**
         * 
         */
        public Parser() {
            this(null, null);
        }

        /**
         * @param optionParser
         * @param protocolParser
         */
        public Parser(final IPHeaderOption.Parser optionParser, final IPMessage.Parser protocolParser) {
            setOptionParser(optionParser);
            setProtocolParser(protocolParser);
        }

        /**
         * @param optionParser
         */
        public void setOptionParser(final IPHeaderOption.Parser optionParser) {
            this.optionParser = optionParser;
        }

        /**
         * @return
         */
        public IPHeaderOption.Parser getOptionParser() {
            return this.optionParser;
        }

        /**
         * @param protocolParser
         */
        public void setProtocolParser(final IPMessage.Parser protocolParser) {
            this.protocolParser = protocolParser;
        }

        /**
         * @return
         */
        public IPMessage.Parser getProtocolParser() {
            return this.protocolParser;
        }

        @Override
        public IPPacket parse(final ByteBuffer buffer) throws ParseException, MissingParserException {
            IPv4Packet header = new IPv4Packet(buffer);

            if (this.optionParser != null) {
                // Parse IP header options
                header.parseOptions(this.optionParser);
            }

            if (this.protocolParser != null) {
                header.parsePayload(this.protocolParser);
            }

            return header;
        }

        @Override
        public IPPacket parse(final InputStream is) throws ParseException, MissingParserException, IOException {
            IPv4Packet header = new IPv4Packet(is);

            if (this.optionParser != null) {
                // Parse IP header options
                header.parseOptions(this.optionParser);
            }

            if (this.protocolParser != null) {
                header.parsePayload(this.protocolParser);
            }

            return header;
        }

        @Override
        public boolean verifyChecksum(final ByteBuffer buffer) throws MissingParserException, ParseException {
            return IPv4Packet.verifyChecksum(buffer);
        }

        @Override
        public Object getKey() {
            return INTERNET_PROTOCOL_VERSION;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    /** Logger used to generate IPv4Packet log entries. */
    public static final Logger logger = Logger.getLogger(IPv4Packet.class.getName());

    /**
     * Protocol version for IPv4 headers.
     */
    public static final byte INTERNET_PROTOCOL_VERSION = 4;

    /** */
    public static final int BASE_HEADER_LENGTH = 20;

    /** */
    public static final FixedBufferField BaseHeader = new FixedBufferField(0, BASE_HEADER_LENGTH);

    /** */
    public static final ByteBitField Version = new ByteBitField(0, 4, 4);

    /** */
    public static final ByteBitField HeaderLength = new ByteBitField(0, 0, 4);

    /** */
    public static final ByteField TypeOfService = new ByteField(1);

    /** */
    public static final ByteBitField Precedence = new ByteBitField(1, 5, 3);

    /** */
    public static final BooleanField MinimizeDelay = new BooleanField(1, 4);

    /** */
    public static final BooleanField MaximizeThroughput = new BooleanField(1, 3);

    /** */
    public static final BooleanField MaximizeReliability = new BooleanField(1, 2);

    /** */
    public static final BooleanField MinimizeMonetaryCost = new BooleanField(1, 1);

    /** */
    public static final ShortField TotalLength = new ShortField(2);

    /** */
    public static final ShortField Identification = new ShortField(4);

    /** */
    public static final ByteBitField Flags = new ByteBitField(6, 5, 3);

    /** */
    public static final BooleanField MoreFragments = new BooleanField(6, 5);

    /** */
    public static final BooleanField DontFragment = new BooleanField(6, 6);

    /** */
    public static final ShortBitField FragmentOffset = new ShortBitField(6, 0, 13);

    /** */
    public static final ByteField TTL = new ByteField(8);

    /** */
    public static final ByteField Protocol = new ByteField(9);

    /** */
    public static final ShortField HeaderChecksum = new ShortField(10);

    /** */
    public static final ByteArrayField SourceAddressBytes = new ByteArrayField(12, 4);

    /** */
    public static final ByteArrayField DestinationAddressBytes = new ByteArrayField(16, 4);

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * Verifies the IPv4 header checksum. Called by the parser prior to constructing the
     * packet.
     * 
     * @param buffer
     *            - the buffer containing the IPv4 header.
     */
    public static boolean verifyChecksum(final ByteBuffer buffer) {
        short checksum = HeaderChecksum.get(buffer);
        short computedChecksum = calculateChecksum(buffer);
        if (checksum != computedChecksum) {
            logger.warning("received IPv4 packet with invalid checksum: received=" + checksum + " computed=" + computedChecksum);
            return false;
        }
        return true;
    }

    /**
     * Calculates the IPv4 header checksum for an IPv4 header contained in a buffer.
     * 
     * @param buffer
     *            - the buffer containing the IPv4 header.
     */
    public static short calculateChecksum(final ByteBuffer buffer) {
        return IPPacket.calculateChecksum(buffer, HeaderChecksum, HeaderLength.get(buffer) * 4);
    }

    /**
     * Writes the IPv4 header checksum into a buffer containing an IPv4 header.
     * 
     * @param buffer
     *            - a byte array.
     */
    public static void setChecksum(final ByteBuffer buffer) {
        HeaderChecksum.set(buffer, IPv4Packet.calculateChecksum(buffer));
    }

    /*-- Member Variables ---------------------------------------------------*/

    /** */
    protected ByteBuffer unparsedOptions = null;

    /** */
    protected ByteBuffer unparsedPayload = null;

    /** */
    protected Vector<IPHeaderOption> options = null;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param precedence
     * @param minimizeDelay
     * @param maximizeThroughput
     * @param maximizeReliability
     * @param minimizeMonetaryCost
     * @param identification
     * @param doNotFragment
     * @param moreFragments
     * @param fragmentOffset
     * @param ttl
     * @param sourceAddress
     * @param destinationAddress
     * @param firstProtocolHeader
     */
    public IPv4Packet(final byte precedence,
                      final boolean minimizeDelay,
                      final boolean maximizeThroughput,
                      final boolean maximizeReliability,
                      final boolean minimizeMonetaryCost,
                      final short identification,
                      final boolean doNotFragment,
                      final boolean moreFragments,
                      final short fragmentOffset,
                      final byte ttl,
                      final byte[] sourceAddress,
                      final byte[] destinationAddress,
                      final IPMessage firstProtocolHeader) {
        super(BASE_HEADER_LENGTH);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.IPv4Packet",
                                        precedence,
                                        minimizeDelay,
                                        maximizeThroughput,
                                        maximizeReliability,
                                        minimizeMonetaryCost,
                                        identification,
                                        doNotFragment,
                                        moreFragments,
                                        fragmentOffset,
                                        ttl,
                                        sourceAddress,
                                        destinationAddress,
                                        firstProtocolHeader));
        }
        setVersion(INTERNET_PROTOCOL_VERSION);
        setHeaderLength(BASE_HEADER_LENGTH);
        setPrecedence(precedence);
        setMinimizeDelay(minimizeDelay);
        setMaximizeThroughput(maximizeThroughput);
        setMaximizeReliability(maximizeReliability);
        setMinimizeMonetaryCost(minimizeMonetaryCost);
        setIdentification(identification);
        setDoNotFragment(doNotFragment);
        setMoreFragments(moreFragments);
        setFragmentOffset(fragmentOffset);
        setTTL(ttl);
        setSourceAddress(sourceAddress);
        setDestinationAddress(destinationAddress);
        setFirstProtocolMessage(firstProtocolHeader);
        if (logger.isLoggable(Level.FINER)) {
            logState(logger,Level.FINER);
        }
    }

    /**
     * @param tos
     * @param identification
     * @param doNotFragment
     * @param moreFragments
     * @param fragmentOffset
     * @param ttl
     * @param sourceAddress
     * @param destinationAddress
     * @param firstProtocolHeader
     */
    public IPv4Packet(final byte tos,
                      final short identification,
                      final boolean doNotFragment,
                      final boolean moreFragments,
                      final short fragmentOffset,
                      final byte ttl,
                      final byte[] sourceAddress,
                      final byte[] destinationAddress,
                      final IPMessage firstProtocolHeader) {
        super(BASE_HEADER_LENGTH);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.IPv4Packet",
                                        tos,
                                        identification,
                                        doNotFragment,
                                        moreFragments,
                                        fragmentOffset,
                                        ttl,
                                        sourceAddress,
                                        destinationAddress,
                                        firstProtocolHeader));
        }
        setVersion(INTERNET_PROTOCOL_VERSION);
        setTypeOfService(tos);
        setIdentification(identification);
        setDoNotFragment(doNotFragment);
        setMoreFragments(moreFragments);
        setFragmentOffset(fragmentOffset);
        setTTL(ttl);
        setSourceAddress(sourceAddress);
        setDestinationAddress(destinationAddress);
        setFirstProtocolMessage(firstProtocolHeader);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger,Level.FINER);
        }
    }

    /**
     * Constructs an IPv4 packet representation from the contents of the
     * specified ByteBuffer.
     * 
     * @param buffer
     * @throws ParseException
     */
    public IPv4Packet(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_HEADER_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.IPv4Packet", buffer));
        }

        this.unparsedOptions = consume(buffer, getHeaderLength() - BASE_HEADER_LENGTH);
        this.unparsedPayload = consume(buffer, getTotalLength() - getHeaderLength());

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
    public IPv4Packet(final InputStream is) throws ParseException, IOException {
        super(consume(is, BASE_HEADER_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.IPv4Packet", is));
        }

        this.unparsedOptions = consume(is, getHeaderLength() - BASE_HEADER_LENGTH);
        this.unparsedPayload = consume(is, getTotalLength() - getHeaderLength());

        if (logger.isLoggable(Level.FINER)) {
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
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        if (getFragmentOffset() != 0 || getMoreFragments()) {
            logger.log(level,this.log.msg(": *** Fragmented Packet ***"));
        }
        logger.log(level,this.log.msg(": total-length=" + getTotalLength()));
        logger.log(level,this.log.msg(": header-length=" + getHeaderLength()));
        logger.log(level,this.log.msg(": payload-length=" + getPayloadLength()));
        logger.log(level,this.log.msg(": precedence=" + getPrecedence()));
        logger.log(level,this.log.msg(": minimize-delay=" + getMinimizeDelay()));
        logger.log(level,this.log.msg(": maximize-throughput=" + getMaximizeThroughput()));
        logger.log(level,this.log.msg(": maximize-reliability=" + getMaximizeReliability()));
        logger.log(level,this.log.msg(": minimize-cost=" + getMinimizeMonetaryCost()));
        logger.log(level,this.log.msg(": identification=" + getIdentification()));
        logger.log(level,this.log.msg(": don't-fragment=" + getDoNotFragment()));
        logger.log(level,this.log.msg(": more-fragments=" + getMoreFragments()));
        logger.log(level,this.log.msg(": fragment-offset=" + getFragmentOffset()));
        logger.log(level,this.log.msg(": TTL=" + getTTL()));
        logger.log(level,this.log.msg(": protocol=" + getProtocol()));
        logger.log(level,this.log.msg(": checksum=" + getHeaderChecksum()));
        logger.log(level,this.log.msg(": source-address=" + Logging.address(getSourceAddress())));
        logger.log(level,this.log.msg(": destination-address=" + Logging.address(getDestinationAddress())));
        if (this.unparsedOptions != null) {
            logger.log(level,this.log.msg(": unparsed options offset=" + this.unparsedOptions.arrayOffset() + " limit="
                                     + this.unparsedOptions.limit()));
        }
        if (this.options != null && this.options.size() > 0) {
            logger.log(level,this.log.msg("----> header options"));
            Iterator<IPHeaderOption> iter = this.options.iterator();
            while (iter.hasNext()) {
                iter.next().log(logger,level);
            }
            logger.log(level,this.log.msg("<---- header options"));
        }
        if (this.unparsedPayload != null) {
            logger.log(level,this.log.msg(": unparsed payload offset=" + this.unparsedPayload.arrayOffset() + " limit="
                                     + this.unparsedPayload.limit()));
        }
        logger.log(level,this.log.msg("----> protocol messages"));
        IPMessage nextMessage = getFirstProtocolMessage();
        while (nextMessage != null) {
            nextMessage.log(logger,level);
            nextMessage = nextMessage.getNextMessage();
        }
        logger.log(level,this.log.msg("<---- protocol messages"));
    }

    @Override
    public void writeTo(final ByteBuffer buffer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.writeTo", buffer));
        }

        // Create a separate view of the underlying bytes to use for writing the checksum
        ByteBuffer slice = buffer.slice();

        // Precondition.checkBounds(buffer.length, offset, getTotalLength());
        updateHeaderLength();
        super.writeTo(buffer);
        if (this.unparsedOptions != null) {
            buffer.put(this.unparsedOptions);
            this.unparsedOptions.rewind();
        }
        else {
            Iterator<IPHeaderOption> iter = this.options.iterator();
            while (iter.hasNext()) {
                iter.next().writeTo(buffer);
            }
        }
        int padding = getPaddingLength();
        for (int i = 0; i < padding; i++) {
            buffer.put((byte) 0);
        }

        writeChecksum(slice);

        if (this.unparsedPayload != null) {
            buffer.put(this.unparsedPayload);
        }
        else {
            IPMessage nextMessage = getFirstProtocolMessage();
            while (nextMessage != null) {
                // Create a separate view of the underlying bytes to use for writing the
                // checksum (if any) in each protocol message
                slice = buffer.slice();
                nextMessage.writeTo(buffer);
                nextMessage.writeChecksum(slice, getSourceAddress(), getDestinationAddress());
                nextMessage = nextMessage.getNextMessage();
            }
        }
    }

    @Override
    public void writeChecksum(final ByteBuffer buffer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.writeChecksum", buffer));
        }

        // Precondition.checkReference(buffer);
        IPv4Packet.setChecksum(buffer);
    }

    @Override
    public byte getNextProtocolNumber() {
        return getProtocol();
    }

    @Override
    protected void setNextProtocolNumber(final byte protocolNumber) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setNextProtocolNumber", protocolNumber));
        }

        setProtocol(protocolNumber);
    }

    /**
     * Gets the computed length of the header including options expressed in
     * bytes. This value is stored in the header as the length in 4-byte words.
     * 
     * @return
     */
    public int getComputedHeaderLength() {
        return (((BASE_HEADER_LENGTH + getOptionsLength() + 3) / 4) * 4);
    }

    /**
     * Gets the current header length field value expressed in bytes. This value
     * is stored in the header as the length in 4-byte words.
     * 
     * <pre>
     *   0               1               2               3 
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |       |  IHL  |               |                               |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return
     */
    public int getHeaderLength() {
        return HeaderLength.get(getBufferInternal()) * 4;
    }

    /**
     * @param length
     */
    private void setHeaderLength(final int length) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setHeaderLength", length));
        }

        HeaderLength.set(getBufferInternal(), (byte) (length / 4));
    }

    /**
     * Updates the header length field using the value returned by
     * {@link #getComputedHeaderLength()}.
     */
    private void updateHeaderLength() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.updateHeaderLength"));
        }

        int headerLength = getHeaderLength();
        setHeaderLength(getComputedHeaderLength());
        setTotalLength((short) (getTotalLength() + (getHeaderLength() - headerLength)));
    }

    /**
     * Gets the current Type of Service (TOS) flags in single byte. Specifies
     * the parameters for the type of service requested. These flags may be
     * utilized by networks to define the handling of the datagram during
     * transport.
     * 
     * <pre>
     *   0               1               2               3  
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |       |       |Type of Service|                               |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  
     *    0   1   2   3   4   5   6   7
     *  +---+---+---+---+---+---+---+---+
     *  |Precedence | D | T | R | M | 0 |
     *  +---+---+---+---+---+---+---+---+
     * </pre>
     * 
     * @return
     */
    public byte getTypeOfService() {
        return TypeOfService.get(getBufferInternal());
    }

    /**
     * Sets the Type of Service flags from a single byte. See {@link #getTypeOfService()}.
     * 
     * @param tos
     */
    public void setTypeOfService(final byte tos) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setTypeOfService", tos));
        }

        TypeOfService.set(getBufferInternal(), tos);
    }

    /** */
    public static final byte PRECEDENCE_ROUTINE = 0;

    /** */
    public static final byte PRECEDENCE_PRIORITY = 1;

    /** */
    public static final byte PRECEDENCE_IMMEDIATE = 2;

    /** */
    public static final byte PRECEDENCE_FLASH = 3;

    /** */
    public static final byte PRECEDENCE_FLASH_OVERRIDE = 4;

    /** */
    public static final byte PRECEDENCE_CRITICAL = 5;

    /** */
    public static final byte PRECEDENCE_INTERNETWORK_CONTROL = 6;

    /** */
    public static final byte PRECEDENCE_NETWORK_CONTROL = 7;

    /**
     * Gets current Precedence field value. See {@link #getTypeOfService()}.
     * 
     * <pre>
     *    1
     *    0   1   2   3   4   5   6   7
     *  +---+---+---+---+---+---+---+---+
     *  |Precedence |   |   |   |   |   |
     *  +---+---+---+---+---+---+---+---+
     * </pre>
     * <ul>
     * <li>0: Routine ({@link #PRECEDENCE_ROUTINE})
     * <li>1: Priority ({@link #PRECEDENCE_PRIORITY})
     * <li>2: Immediate ({@link #PRECEDENCE_IMMEDIATE})
     * <li>3: Flash ({@link #PRECEDENCE_FLASH})
     * <li>4: Flash override ({@link #PRECEDENCE_FLASH_OVERRIDE})
     * <li>5: CRITIC/ECP ({@link #PRECEDENCE_CRITICAL})
     * <li>6: Internetwork control ({@link #PRECEDENCE_INTERNETWORK_CONTROL})
     * <li>7: Network control ({@link #PRECEDENCE_NETWORK_CONTROL}) </ol>
     * 
     * @return
     */
    public byte getPrecedence() {
        return Precedence.get(getBufferInternal());
    }

    /**
     * Sets precedence value. See {@link #getPrecedence()}.
     * 
     * @param precedence
     */
    public void setPrecedence(final byte precedence) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setPrecedence", precedence));
        }

        Precedence.set(getBufferInternal(), precedence);
    }

    /**
     * Gets current Minimize Delay flag value. See {@link #getTypeOfService()}.
     * 
     * <pre>
     *    1
     *    0   1   2   3   4   5   6   7
     *  +---+---+---+---+---+---+---+---+
     *  |           | D |   |   |   |   |
     *  +---+---+---+---+---+---+---+---+
     * </pre>
     * <ul>
     * <li>false = Normal delay.
     * <li>true = Low delay.
     * </ul>
     * 
     * @return
     */
    public boolean getMinimizeDelay() {
        return MinimizeDelay.get(getBufferInternal());
    }

    /**
     * Sets Minimize Delay flag value. See {@link #getMinimizeDelay()}.
     * 
     * @param minimizeDelay
     */
    public void setMinimizeDelay(final boolean minimizeDelay) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setMinimizeDelay", minimizeDelay));
        }

        MinimizeDelay.set(getBufferInternal(), minimizeDelay);
    }

    /**
     * Gets current Maximize Throughput flag value. See {@link #getTypeOfService()}.
     * 
     * <pre>
     *    1
     *    0   1   2   3   4   5   6   7
     *  +---+---+---+---+---+---+---+---+
     *  |           |   | T |   |   |   |
     *  +---+---+---+---+---+---+---+---+
     * </pre>
     * <ul>
     * <li>false = Normal throughput.
     * <li>true = High throughput.
     * </ul>
     * 
     * @return
     */
    public boolean getMaximizeThroughput() {
        return MaximizeThroughput.get(getBufferInternal());
    }

    /**
     * Sets Maximize Throughput flag value.
     * See {@link #getMaximizeThroughput()}.
     * 
     * @param maximizeThroughput
     */
    public void setMaximizeThroughput(final boolean maximizeThroughput) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setMaximizeThroughput", maximizeThroughput));
        }

        MaximizeThroughput.set(getBufferInternal(), maximizeThroughput);
    }

    /**
     * Gets current Maximize Reliability flag value. See {@link #getTypeOfService()}.
     * 
     * <pre>
     *    0   1   2   3   4   5   6   7
     *  +---+---+---+---+---+---+---+---+
     *  |           |   |   | R |   |   |
     *  +---+---+---+---+---+---+---+---+
     * </pre>
     * <ul>
     * <li>false = Normal monetary cost.
     * <li>true = Minimize monetary cost.
     * </ul>
     * 
     * @return
     */
    public boolean getMaximizeReliability() {
        return MaximizeReliability.get(getBufferInternal());
    }

    /**
     * Sets Maximize Reliability flag value.
     * See {@link #getMaximizeReliability()}.
     * 
     * @param maximizeReliability
     */
    public void setMaximizeReliability(final boolean maximizeReliability) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setMaximizeReliability", maximizeReliability));
        }

        MaximizeReliability.set(getBufferInternal(), maximizeReliability);
    }

    /**
     * Gets current Minimize Monetary Cost flag value.
     * See {@link #getTypeOfService()}.
     * 
     * <pre>
     *    0   1   2   3   4   5   6   7
     *  +---+---+---+---+---+---+---+---+
     *  |           |   |   |   | M |   |
     *  +---+---+---+---+---+---+---+---+
     * </pre>
     * <ul>
     * <li>false = Normal monetary cost.
     * <li>true = Minimize monetary cost.
     * </ul>
     * 
     * @return
     */
    public boolean getMinimizeMonetaryCost() {
        return MinimizeMonetaryCost.get(getBufferInternal());
    }

    /**
     * Sets Minimize Monetary Cost flag value.
     * See {@link #getMinimizeMonetaryCost()}.
     * 
     * @param minimizeMonetaryCost
     */
    public void setMinimizeMonetaryCost(final boolean minimizeMonetaryCost) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("setMinimizeMonetaryCost", minimizeMonetaryCost));
        }

        MinimizeMonetaryCost.set(getBufferInternal(), minimizeMonetaryCost);
    }

    /**
     * Gets the current total length in bytes. Includes IP header and payload.
     * 
     * <pre>
     *   0               1               2             3   
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |       |       |               |          Total Length         |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return
     */
    public int getTotalLength() {
        return TotalLength.get(getBufferInternal());
    }

    /**
     * Sets the total length value.
     * See {@link #getTotalLength()}.
     * 
     * @param totalLength
     */
    protected void setTotalLength(final short totalLength) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setTotalLength", totalLength));
        }

        TotalLength.set(getBufferInternal(), totalLength);
    }

    @Override
    public int getPayloadLength() {
        return getTotalLength() - getHeaderLength();
    }

    @Override
    protected void setPayloadLength(final int length) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setPayloadLength", length));
        }

        setTotalLength((short) (getHeaderLength() + length));
    }

    /**
     * Gets the current Identification field value.
     * 
     * <pre>
     *   4               5
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |         Identification        |     |                         |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return
     */
    public short getIdentification() {
        return Identification.get(getBufferInternal());
    }

    /**
     * Sets the Identification field value.
     * See {@link #getIdentification()}.
     * 
     * @param identification
     */
    public void setIdentification(final short identification) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setIdentification", identification));
        }

        Identification.set(getBufferInternal(), identification);
    }

    /**
     * Gets the current Don't Fragment flag value.
     * 
     * <pre>
     *   6
     * +---+---+---+---+---+---+---+---+
     * |   | D |                       $
     * |   | F |                       $
     * +---+---+---+---+---+---+---+---+
     * </pre>
     * <ul>
     * <li>false = do not fragment
     * <li>true = may fragment
     * </ul>
     * 
     * @return
     */
    public boolean getDoNotFragment() {
        return DontFragment.get(getBufferInternal());
    }

    /**
     * Sets the Don't Fragment flag value.
     * See {@link #getDoNotFragment()}.
     * 
     * @param doNotFragment
     */
    public void setDoNotFragment(final boolean doNotFragment) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setDoNotFragment", doNotFragment));
        }

        DontFragment.set(getBufferInternal(), doNotFragment);
    }

    /**
     * Gets the current More Fragments flag value.
     * 
     * <pre>
     *   6
     * +---+---+---+---+---+---+---+---+
     * |       | M |                   $
     * |       | F |                   $
     * +---+---+---+---+---+---+---+---+
     * </pre>
     * <ul>
     * <li>false = last fragment
     * <li>true = more fragments
     * </ul>
     * See {@link #getFlags()}.
     * 
     * @return
     */
    public boolean getMoreFragments() {
        return MoreFragments.get(getBufferInternal());
    }

    /**
     * Sets the More Fragments flag value.
     * See {@link #getMoreFragments()}.
     * 
     * @param moreFragments
     */
    public void setMoreFragments(final boolean moreFragments) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setMoreFragments", moreFragments));
        }

        MoreFragments.set(getBufferInternal(), moreFragments);
    }

    @Override
    public boolean isMoreFragments() {
        return getMoreFragments();
    }

    /**
     * Gets the current Fragment Offset field value.
     * 
     * <pre>
     *                                  6               7
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                               |     |      Fragment Offset    |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * Used to identify the fragments of one datagram from those of another.
     * The originating protocol module of an internet datagram sets the
     * identification field to a value that must be unique for that
     * source-destination pair and protocol for the time the datagram will
     * be active in the internet system. The value of this field is typically
     * incremented by one for each datagram distribution.
     * 
     * @return
     */
    @Override
    public int getFragmentOffset() {
        return FragmentOffset.get(getBufferInternal());
    }

    /**
     * Sets the Fragment Offset field value.
     * See {@link #getFragmentOffset()}.
     * 
     * @param fragmentOffset
     */
    public void setFragmentOffset(final short fragmentOffset) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setFragmentOffset", fragmentOffset));
        }

        FragmentOffset.set(getBufferInternal(), fragmentOffset);
    }

    /**
     * Gets the current Time to Live (TTL) field value.
     * 
     * <pre>
     *   8               9               10              11
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |  Time to Live |               |                               |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * The TTL field indicates the maximum time the datagram is allowed to
     * remain in the Internet system. If this field contains the value zero,
     * then the datagram must be destroyed. This field is modified in IP header
     * processing. The time is measured in units of seconds, but since every
     * module that processes a datagram must decrease the TTL by at least one
     * even if it process the datagram in less than a second, the TTL must be
     * thought of only as an upper bound on the time a datagram may exist.
     * The intention is to cause undeliverable datagrams to be discarded and
     * to establish a limit the maximum datagram lifetime.
     * 
     * @return
     */
    public byte getTTL() {
        return TTL.get(getBufferInternal());
    }

    /**
     * Sets the Time to Live (TTL) field value. See {@link #getTTL()}.
     * 
     * @param ttl
     */
    public void setTTL(final byte ttl) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setTTL", ttl));
        }

        TTL.set(getBufferInternal(), ttl);
    }

    /**
     * Gets the current Protocol field value.
     * See assigned protocol numbers in [RFC-1700].
     * 
     * <pre>
     *   8               9               10              11
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |               |    Protocol   |                               |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * The protocol indicates the higher-level protocol used in the
     * payload portion of the packet.
     * 
     * @return
     */
    public byte getProtocol() {
        return Protocol.get(getBufferInternal());
    }

    /**
     * Sets the Protocol field value.
     * See {@link #getProtocol()}.
     * 
     * @param protocol
     */
    protected void setProtocol(final byte protocol) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setProtocol", protocol));
        }

        Protocol.set(getBufferInternal(), protocol);
    }

    /**
     * Gets the current Header Checksum field value.
     * 
     * <pre>
     *   8               9               10              11
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |               |               |         Header Checksum       |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return
     */
    public short getHeaderChecksum() {
        return HeaderChecksum.get(getBufferInternal());
    }

    /**
     * Sets the Checksum field value.
     * See {@link #getChecksum()}.
     * 
     * @param checksum
     */
    public void setHeaderChecksum(final short checksum) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setHeaderChecksum", checksum));
        }

        HeaderChecksum.set(getBufferInternal(), checksum);
    }

    /**
     * Gets the current Source Address field value.
     * 
     * <pre>
     *   12
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |                       Source Address                          |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return
     */
    @Override
    public byte[] getSourceAddress() {
        return SourceAddressBytes.get(getBufferInternal());
    }

    /**
     * Gets the current Source Address field value as an Inet4Address.
     * 
     * <pre>
     *   12
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |                       Source Address                          |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return
     */
    @Override
    public InetAddress getSourceInetAddress() {
        try {
            return (Inet4Address) InetAddress.getByAddress(SourceAddressBytes.get(getBufferInternal()));
        }
        catch (UnknownHostException e) {
            throw new Error(e);
        }
    }

    /**
     * Sets the Source Address field value.
     * See {@link #getSourceAddress()}.
     * 
     * @param sourceAddress
     *            - an IPv4 address.
     */
    public void setSourceAddress(final Inet4Address sourceAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setSourceAddress", sourceAddress));
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
     *            - an IPv4 address.
     */
    public void setSourceAddress(final byte[] address) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setSourceAddress", Logging.address(address)));
        }

        // Precondition.checkIPv4Address(address);
        SourceAddressBytes.set(getBufferInternal(), address);
    }

    /**
     * Gets the current Destination Address field value.
     * 
     * <pre>
     *   16
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |                   Destination Address                         |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return
     */
    @Override
    public byte[] getDestinationAddress() {
        return DestinationAddressBytes.get(getBufferInternal());
    }

    /**
     * Gets the current Destination Address field value as an Inet4Address.
     * 
     * <pre>
     *   16
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |                   Destination Address                         |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * 
     * @return
     */
    @Override
    public InetAddress getDestinationInetAddress() {
        try {
            return (Inet4Address) InetAddress.getByAddress(DestinationAddressBytes.get(getBufferInternal()));
        }
        catch (UnknownHostException e) {
            throw new Error(e);
        }
    }

    /**
     * Sets the Destination Address field value.
     * See {@link #getDestinationAddress()}.
     * 
     * @param destinationAddress
     *            - an IPv4 address.
     */
    public void setDestinationAddress(final Inet4Address destinationAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setDestinationAddress", destinationAddress));
        }

        // Precondition.checkReference(destinationAddress);
        byte[] address = destinationAddress.getAddress();
        setDestinationAddress(address);
    }

    /**
     * Sets the Destination Address field value.
     * See {@link #getDestinationAddress()}.
     * 
     * @param destinationAddress
     *            - an IPv4 address.
     */
    public void setDestinationAddress(final byte[] address) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.setDestinationAddress", Logging.address(address)));
        }

        // Precondition.checkIPv4Address(address);
        DestinationAddressBytes.set(getBufferInternal(), address);
    }

    /**
     * @param option
     */
    public void addOption(final IPHeaderOption option) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.addOption", option));
        }

        // Precondition.checkReference(option);
        if (this.options == null) this.options = new Vector<IPHeaderOption>();
        this.options.add(option);
        updateHeaderLength();
    }

    /**
     * @param option
     */
    public void removeOption(final IPHeaderOption option) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.removeOption", option));
        }

        if (this.options != null) {
            this.options.remove(option);
            updateHeaderLength();
        }
    }

    /**
     * Calculates the total bytes required by all options currently attached to
     * the packet header.
     * 
     * @return
     */
    public int getOptionsLength() {
        if (this.options == null) {
            if (this.unparsedOptions == null) {
                return 0;
            }
            else {
                return this.unparsedOptions.limit();
            }
        }
        else {
            int length = 0;
            for (IPHeaderOption option : this.options) {
                length += option.getOptionLength();
            }
            return length;
        }
    }

    /**
     * @return
     */
    public ByteBuffer getUnparsedOptions() {
        return this.unparsedOptions.slice();
    }

    /**
     * @param optionParser
     * @throws ParseException
     * @throws MissingParserException
     */
    public void parseOptions(final IPHeaderOption.Parser optionParser) throws ParseException, MissingParserException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.parseOptions", optionParser));
        }

        if (this.unparsedOptions != null) {

            if (this.options == null) {
                this.options = new Vector<IPHeaderOption>();
            }
            else {
                this.options.clear();
            }

            this.unparsedOptions.rewind();

            while (this.unparsedOptions.remaining() > 0) {
                IPHeaderOption option = optionParser.parse(this.unparsedOptions);
                this.options.add(option);
                if (option instanceof IPEndOfListOption) {
                    break;
                }
            }

            this.unparsedOptions.rewind();

            updateHeaderLength();
        }
    }

    /**
     * @return
     */
    public Enumeration<IPHeaderOption> getOptions() {
        return this.options != null ? this.options.elements() : null;
    }

    /**
     * Calculates the number of zero-padding bytes required to make IP header
     * end on 32-bit word boundary.
     */
    public int getPaddingLength() {
        return getHeaderLength() - BASE_HEADER_LENGTH - getOptionsLength();
    }

    @Override
    public int getFragmentIdentifier() {
        return getIdentification();
    }

    @Override
    public ByteBuffer getUnparsedPayload() {
        return this.unparsedPayload.slice();
    }

    @Override
    public ByteBuffer getFragment() {
        return this.unparsedPayload.slice();
    }

    @Override
    public void setReassembledPayload(final ByteBuffer reassembledPayload) {
        this.unparsedPayload = reassembledPayload.slice();
        setPayloadLength(this.unparsedPayload.limit());
        setFragmentOffset((short) 0);
        setMoreFragments(false);
    }

    /**
     * @param protocolParser
     * @throws ParseException
     * @throws MissingParserException
     */
    public void parsePayload(final IPMessage.Parser protocolParser) throws ParseException, MissingParserException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv4Packet.parsePayload", protocolParser));
        }

        this.unparsedPayload.rewind();

        if (this.unparsedPayload != null && this.unparsedPayload.remaining() > 0) {

            // Parse IP protocol headers
            byte lastProtocolNumber = getLastProtocolNumber();

            // Check checksum before we consume the payload
            if (!protocolParser.verifyChecksum(this.unparsedPayload, lastProtocolNumber, getSourceAddress(),
                                               getDestinationAddress())) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(this.log.msg("invalid checksum detected in IP protocol packet"));
                }
                throw new ParseException("invalid checksum detected in IP protocol packet");
            }

            IPMessage nextHeader = protocolParser.parse(this.unparsedPayload, lastProtocolNumber);
            if (nextHeader == null) {
                addProtocolMessage(new IPPayload(lastProtocolNumber, consume(this.unparsedPayload, getPayloadLength())));
            }
            else {
                addProtocolMessage(nextHeader);
                while (nextHeader != null && nextHeader.getNextProtocolNumber() != IPMessage.NO_NEXT_HEADER) {
                    lastProtocolNumber = nextHeader.getNextProtocolNumber();
                    if (!protocolParser.verifyChecksum(this.unparsedPayload, lastProtocolNumber, getSourceAddress(),
                                                       getDestinationAddress())) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine(this.log.msg("invalid checksum detected in IP protocol packet"));
                        }
                        throw new ParseException("invalid checksum detected in IP protocol packet");
                    }
                    nextHeader = protocolParser.parse(this.unparsedPayload, lastProtocolNumber);
                    addProtocolMessage(nextHeader);
                }
            }

            this.unparsedPayload.rewind();
        }
    }

}
