package org.js4ms.ip.ipv6;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPv6RoutingHeader.java [org.js4ms.jsdk:ip]
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
import org.js4ms.common.util.buffer.field.ByteField;
import org.js4ms.common.util.buffer.field.IntegerField;
import org.js4ms.common.util.buffer.field.SelectorField;
import org.js4ms.common.util.buffer.parser.BufferParserSelector;
import org.js4ms.common.util.buffer.parser.KeyedBufferParser;
import org.js4ms.common.util.buffer.parser.MissingParserException;
import org.js4ms.ip.IPExtensionHeader;
import org.js4ms.ip.IPMessage;




/**
 * Represents an IPv6 Routing header.
 * <p>
 * The Routing header is used by an IPv6 source to list one or more intermediate nodes to
 * be &quot;visited&quot; on the way to a packet's destination. This function is very
 * similar to IPv4's Loose Source and Record Route option. The Routing header is
 * identified by a Next Header value of 43 in the immediately preceding header. See <a
 * href="http://www.rfc-editor.org/rfc/rfc2460.txt">[RFC-2460]</a>.
 * <h3>Header Format</h3> <blockquote>
 * 
 * <pre>
 *   0                   1                   2                   3   
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |  Next Header  |  Hdr Ext Len  |  Routing Type | Segments Left |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  .                                                               .
 *  .                       type-specific data                      .
 *  .                                                               .
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <dl>
 * <dt><u>Next Header</u></dt>
 * <p>
 * <dd>Identifies the type of header immediately following the Routing header. Uses the
 * same values as the IPv4 Protocol field [RFC-1700].
 * <p>
 * See {@link #getNextProtocolNumber()}, {@link #getNextMessage()},
 * {@link #setNextMessage(IPMessage)}.</dd>
 * <p>
 * <dt><u>Hdr Ext Len</u></dt>
 * <p>
 * <dd>An unsigned integer. Length of the Routing header in 8-octet units, not including
 * the first 8 octets.
 * <p>
 * See {@link #getHeaderLength()}, {@link #getTotalLength()}.</dd>
 * <p>
 * <dt><u>Routing Type</u></dt>
 * <p>
 * <dd>Identifies the particular Routing header variant.
 * <p>
 * See {@link #getRoutingType()}, {@link #setRoutingType(byte)}.</dd>
 * <p>
 * <dt><u>Segments Left</u></dt>
 * <p>
 * <dd>An unsigned integer. Number of route segments remaining, i.e., the number of
 * explicitly listed intermediate nodes still to be visited before reaching the final
 * destination.
 * <p>
 * See {@link #getSegmentsLeft()}, {@link #setSegmentsLeft(byte)}.</dd>
 * <p>
 * <dt><u>Type-specific Data</u></dt>
 * <p>
 * <dd>Variable-length field, of format determined by the Routing Type, and of length such
 * that the complete Routing header is an integer multiple of 8 octets long.
 * <p>
 * If, while processing a received packet, a node encounters a Routing header with an
 * unrecognized Routing Type value, the required behavior of the node depends on the value
 * of the Segments Left field, as follows:
 * <ul>
 * <li>If Segments Left is zero, the node must ignore the Routing header and proceed to
 * process the next header in the packet, whose type is identified by the Next Header
 * field in the Routing header.</li>
 * <li>If Segments Left is non-zero, the node must discard the packet and send an ICMP
 * Parameter Problem, Code 0, message to the packet's Source Address, pointing to the
 * unrecognized Routing Type.</li>
 * </ul>
 * If, after processing a Routing header of a received packet, an intermediate node
 * determines that the packet is to be forwarded onto a link whose link MTU is less than
 * the size of the packet, the node must discard the packet and send an ICMP Packet Too
 * Big message to the packet's Source Address.</li> </ul></dd>
 * </dl>
 * </blockquote>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public class IPv6RoutingHeader
                extends IPExtensionHeader {

    /**
     * 
     */
    public static interface ParserType
                    extends KeyedBufferParser<IPv6RoutingHeader> {

    }

    /**
     * 
     */
    public static class Parser
                    extends BufferParserSelector<IPMessage>
                    implements IPMessage.ParserType {

        /**
         * 
         */
        public Parser() {
            super(new SelectorField<Byte>(IPv6RoutingHeader.RoutingType));
        }

        @Override
        public boolean verifyChecksum(final ByteBuffer buffer,
                                      final byte[] sourceAddress,
                                      final byte[] destinationAddress) throws MissingParserException, ParseException {
            return true; // Does nothing in this class
        }

        @Override
        public Object getKey() {
            return IP_PROTOCOL_NUMBER;
        }

    }

    /** Protocol number for IPv6 Routing headers. */
    public static final byte IP_PROTOCOL_NUMBER = 43;

    /** */
    public static final ByteField RoutingType = new ByteField(2);

    /** */
    public static final ByteField SegmentsLeft = new ByteField(3);

    /** */
    public static final IntegerField Reserved = new IntegerField(4);

    /**
     * @param routingType
     * @param segmentsLeft
     */
    protected IPv6RoutingHeader(final byte routingType, final byte segmentsLeft) {
        super(IP_PROTOCOL_NUMBER);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6RoutingHeader.IPv6RoutingHeader", routingType, segmentsLeft));
        }

        setRoutingType(routingType);
        setSegmentsLeft(segmentsLeft);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    /**
     * @param buffer
     * @throws ParseException
     */
    public IPv6RoutingHeader(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, MIN_HEADER_LENGTH + HeaderLength.get(buffer) * 8), IP_PROTOCOL_NUMBER);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6RoutingHeader.IPv6RoutingHeader", buffer));
            logState(logger, Level.FINER);
        }
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
        logger.log(level,this.log.msg(": routing-type=" + getRoutingType()));
        logger.log(level,this.log.msg(": segments-left=" + getSegmentsLeft()));
    }

    /**
     * @return
     */
    public final byte getRoutingType() {
        return RoutingType.get(getBufferInternal());
    }

    /**
     * @param routingType
     */
    public final void setRoutingType(final byte routingType) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6RoutingHeader.setRoutingType", routingType));
        }

        RoutingType.set(getBufferInternal(), routingType);
    }

    /**
     * @return
     */
    public final byte getSegmentsLeft() {
        return SegmentsLeft.get(getBufferInternal());
    }

    /**
     * @param segmentsLeft
     */
    public final void setSegmentsLeft(final byte segmentsLeft) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6RoutingHeader.setSegmentsLeft", segmentsLeft));
        }

        SegmentsLeft.set(getBufferInternal(), segmentsLeft);
    }
}
