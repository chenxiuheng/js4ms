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

package com.larkwoodlabs.net.ip.ipv6;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPExtensionHeader;
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.buffer.fields.IntegerField;
import com.larkwoodlabs.util.buffer.fields.SelectorField;
import com.larkwoodlabs.util.buffer.parser.BufferParserSelector;
import com.larkwoodlabs.util.buffer.parser.KeyedBufferParser;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

/**
 * <pre>
 * <h1>4.4  Routing Header</h1>
 * 
 *    The Routing header is used by an IPv6 source to list one or more
 *    intermediate nodes to be &quot;visited&quot; on the way to a packet's
 *    destination.  This function is very similar to IPv4's Loose Source
 *    and Record Route option.  The Routing header is identified by a Next
 *    Header value of 43 in the immediately preceding header, and has the
 *    following format:
 * 
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |  Next Header  |  Hdr Ext Len  |  Routing Type | Segments Left |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *     |                                                               |
 *     .                                                               .
 *     .                       type-specific data                      .
 *     .                                                               .
 *     |                                                               |
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 *    <h2>Next Header (8-bits)</h2>
 *    Identifies the type of header immediately following the Routing
 *    header.  Uses the same values as the IPv4 Protocol field [RFC-1700].
 * 
 *    <h2>Hdr Ext Len (8-bits)</h2>
 *    An unsigned integer. Length of the Routing header in 8-octet units,
 *    not including the first 8 octets.
 * 
 *    <h2>Routing Type (8-bits)</h2>
 *    Identifies the particular Routing header variant.
 * 
 *    <h2>Segments Left (8-bits)</h2>
 *    An unsigned integer.  Number of route segments remaining, i.e.,
 *    the number of explicitly listed intermediate nodes still to be
 *    visited before reaching the final destination.
 * 
 *    <h2>type-specific data (variable length)</h2>
 *    Variable-length field, of format determined by the Routing Type,
 *    and of length such that the complete Routing header is an integer
 *    multiple of 8 octets long.
 * 
 *    If, while processing a received packet, a node encounters a Routing
 *    header with an unrecognized Routing Type value, the required behavior
 *    of the node depends on the value of the Segments Left field, as
 *    follows:
 * 
 *       If Segments Left is zero, the node must ignore the Routing header
 *       and proceed to process the next header in the packet, whose type
 *       is identified by the Next Header field in the Routing header.
 * 
 *       If Segments Left is non-zero, the node must discard the packet and
 *       send an ICMP Parameter Problem, Code 0, message to the packet's
 *       Source Address, pointing to the unrecognized Routing Type.
 * 
 *    If, after processing a Routing header of a received packet, an
 *    intermediate node determines that the packet is to be forwarded onto
 *    a link whose link MTU is less than the size of the packet, the node
 *    must discard the packet and send an ICMP Packet Too Big message to
 *    the packet's Source Address.
 * </pre>
 * @see <a href="http://www.rfc-editor.org/rfc/rfc2460.txt">[RFC-2460]</a>
 * 
 * @author Gregory Bumgardner
 * 
 */
public class IPv6RoutingHeader extends IPExtensionHeader {

    /**
     * 
     */
    public static interface ParserType extends KeyedBufferParser<IPv6RoutingHeader> {
        
    }

    /**
     * 
     */
    public static class Parser extends BufferParserSelector<IPMessage> implements IPMessage.ParserType {

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
    public static final ByteField    RoutingType = new ByteField(2);
    /** */
    public static final ByteField    SegmentsLeft = new ByteField(3);
    /** */
    public static final IntegerField Reserved = new IntegerField(4);


    /**
     * 
     * @param routingType
     * @param segmentsLeft
     */
    protected IPv6RoutingHeader(final byte routingType, final byte segmentsLeft) {
        super(IP_PROTOCOL_NUMBER);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6RoutingHeader.IPv6RoutingHeader", routingType, segmentsLeft));
        }
        
        setRoutingType(routingType);
        setSegmentsLeft(segmentsLeft);
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public IPv6RoutingHeader(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, MIN_HEADER_LENGTH + HeaderLength.get(buffer) * 8), IP_PROTOCOL_NUMBER);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6RoutingHeader.IPv6RoutingHeader", buffer));
            logState(logger);
        }
    }

    @Override
    public void log(final Logger logger) {
        super.log(logger);
        logState(logger);
    }
    
    /**
     * Logs value of member variables declared or maintained by this class.
     * @param logger
     */
    private void logState(final Logger logger) {
        logger.info(ObjectId + " : routing-type="+getRoutingType());
        logger.info(ObjectId + " : segments-left="+getSegmentsLeft());
    }

    /**
     * 
     * @return
     */
    public final byte getRoutingType() {
        return RoutingType.get(getBufferInternal());
    }

    /**
     * 
     * @param routingType
     */
    public final void setRoutingType(final byte routingType) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6RoutingHeader.setRoutingType", routingType));
        }
        
        RoutingType.set(getBufferInternal(),routingType);
    }

    /**
     * 
     * @return
     */
    public final byte getSegmentsLeft() {
        return SegmentsLeft.get(getBufferInternal());
    }

    /**
     * 
     * @param segmentsLeft
     */
    public final void setSegmentsLeft(final byte segmentsLeft) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6RoutingHeader.setSegmentsLeft", segmentsLeft));
        }
        
        SegmentsLeft.set(getBufferInternal(),segmentsLeft);
    }
}
