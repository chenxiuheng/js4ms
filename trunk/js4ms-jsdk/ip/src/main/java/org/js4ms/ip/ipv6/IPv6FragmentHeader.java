package org.js4ms.ip.ipv6;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPv6FragmentHeader.java [org.js4ms.jsdk:ip]
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
import org.js4ms.common.util.buffer.field.BooleanField;
import org.js4ms.common.util.buffer.field.IntegerField;
import org.js4ms.common.util.buffer.field.ShortBitField;
import org.js4ms.common.util.buffer.parser.MissingParserException;
import org.js4ms.ip.IPExtensionHeader;
import org.js4ms.ip.IPMessage;





/**
 * Represents an IPv6 fragment header.
 * A fragment header is used by an IPv6 source to send packets larger
 * than would fit in the path MTU. A Fragment
 * header is identified by a Next Header value of 44 in the
 * immediately preceding header. <h3>Header Format</h3> <blockquote>
 * 
 * <pre>
 *   0               1               2               3          
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |  Next Header  |   Reserved    |      Fragment Offset    |Res|M|
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                         Identification                        |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * <dl>
 * <dt><u>Next Header</u></dt>
 * <p>
 * <dd>Identifies the initial header type of the Fragmentable Part of the original packet
 * (defined below). Uses the same values as the IPv4 Protocol field [RFC-1700 et seq.].
 * <p>
 * See {@link #getNextProtocolNumber()}, {@link #getNextMessage()},
 * {@link #setNextMessage(IPMessage)}.</dd>
 * <p>
 * <dt><u>Reserved</u></dt>
 * <p>
 * <dd>Reserved field. Initialized to zero for transmission; ignored on reception.</dd>
 * <p>
 * <dt><u>Fragment Offset</u></dt>
 * <p>
 * <dd>The offset, in 8-octet units, of the data following this header, relative to the
 * start of the Fragmentable Part of the original packet.
 * <p>
 * See {@link #getFragmentOffset()}, {@link #setFragmentOffset(short)}.</dd>
 * <p>
 * <dt><u>Reserved (Res)</u></dt>
 * <p>
 * <dd>Reserved field. Initialized to zero for transmission; ignored on reception.</dd>
 * <p>
 * <dt><u>More Fragments (M) Flag</u></dt>
 * <p>
 * <dd>
 * 
 * <pre>
 *   1 = more fragments;
 *   0 = last fragment.
 * </pre>
 * 
 * <p>
 * See {@link #getMoreFragments()}, {@link #setMoreFragments(boolean)}.</dd>
 * <p>
 * <dt><u>Identification</u></dt>
 * <p>
 * <dd>For every packet that is to be fragmented, the source node generates an
 * Identification value. The Identification must be different than that of any other
 * fragmented packet sent recently* with the same Source Address and Destination Address.
 * If a Routing header is present, the Destination Address of concern is that of the final
 * destination.
 * <p>
 * See {@link #getIdentification()}, {@link #setIdentification(int)}.</dd>
 * </dl>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class IPv6FragmentHeader
                extends IPExtensionHeader {

    /*-- Inner Classes ---------------------------------------------------*/

    /**
     * 
     */
    public static class Parser
                    implements IPMessage.ParserType {

        @Override
        public IPMessage parse(final ByteBuffer buffer) throws ParseException {
            return new IPv6FragmentHeader(buffer);
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

    /*-- Static Variables ---------------------------------------------------*/

    /** Protocol number for IPv6 Fragment Headers */
    public static final byte IP_PROTOCOL_NUMBER = 44;

    /** */
    public static final ShortBitField FragmentOffset = new ShortBitField(2, 3, 13);

    /** */
    public static final BooleanField MoreFragments = new BooleanField(3, 0);

    /** */
    public static final IntegerField Identification = new IntegerField(4);

    /** */
    public static final int BASE_HEADER_LENGTH = 8;

    /*-- Member Variables ---------------------------------------------------*/

    /** */
    ByteBuffer fragment;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param fragmentOffset
     * @param moreFragments
     * @param identification
     */
    public IPv6FragmentHeader(final short fragmentOffset,
                              final boolean moreFragments,
                              final int identification) {
        super(IP_PROTOCOL_NUMBER);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6FragmentHeader.IPv6FragmentHeader", fragmentOffset, moreFragments,
                                        identification));
        }

        setFragmentOffset(fragmentOffset);
        setMoreFragments(moreFragments);
        setIdentification(identification);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger,Level.FINER);
        }
    }

    /**
     * @param buffer
     * @throws ParseException
     */
    public IPv6FragmentHeader(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_HEADER_LENGTH), IP_PROTOCOL_NUMBER);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6FragmentHeader.IPv6FragmentHeader", buffer));
        }

        this.fragment = consume(buffer, buffer.remaining());

        if (logger.isLoggable(Level.FINER)) {
            logState(logger,Level.FINER);
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
        logger.log(level,this.log.msg(": more-fragments=" + getMoreFragments()));
        logger.log(level,this.log.msg(": fragment-offset=" + getFragmentOffset()));
        logger.log(level,this.log.msg(": identification=" + getIdentification()));
        logger.log(level,this.log.msg(": fragment array=" + this.fragment.array() +
                                 " offset=" + this.fragment.arrayOffset() +
                                 " limit=" + this.fragment.limit()));
    }

    /**
     * Gets the total length of this header in bytes.
     * Some extension headers override this method to return a fixed value.
     */
    @Override
    public final int getHeaderLength() {
        return BASE_HEADER_LENGTH;
    }

    /**
     * @return
     */
    public ByteBuffer getFragment() {
        return this.fragment.slice();
    }

    /**
     * @return
     */
    public short getFragmentOffset() {
        return FragmentOffset.get(getBufferInternal());
    }

    /**
     * @param offset
     */
    public void setFragmentOffset(final short offset) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6FragmentHeader.setFragmentOffset", offset));
        }

        FragmentOffset.set(getBufferInternal(), offset);
    }

    /**
     * @return
     */
    public boolean getMoreFragments() {
        return MoreFragments.get(getBufferInternal());
    }

    /**
     * @param moreFragments
     */
    public void setMoreFragments(final boolean moreFragments) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6FragmentHeader.setMoreFragments", moreFragments));
        }

        MoreFragments.set(getBufferInternal(), moreFragments);
    }

    /**
     * @return
     */
    public int getIdentification() {
        return Identification.get(getBufferInternal());
    }

    /**
     * @param identification
     */
    public void setIdentification(final int identification) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6FragmentHeader.setIdentification", identification));
        }

        Identification.set(getBufferInternal(), identification);
    }
}
