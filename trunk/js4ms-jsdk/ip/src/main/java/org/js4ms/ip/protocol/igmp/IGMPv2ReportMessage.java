package org.js4ms.ip.protocol.igmp;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IGMPv2ReportMessage.java [org.js4ms.jsdk:ip]
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

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.parser.MissingParserException;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.ip.IPMessage;
import org.js4ms.ip.IPPacket;
import org.js4ms.ip.ipv4.IPv4Packet;




/**
 * Represents an IGMPv2 Membership Report Message.
 * Version 2 Membership Reports are sent by IP systems to report (to
 * neighboring routers) the current multicast reception state, or
 * changes in the multicast reception state, of their interfaces.
 * [See <a
 * href="http://www.ietf.org/rfc/rfc2236.txt">RFC-2236</a>]
 * <p>
 * <h3>Message Format</h3>
 * <blockquote>
 * <pre>
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |  Type = 0x16  | Max Resp Code |           Checksum            |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                         Group Address                         |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <dl>
 * <dt><u>Max Response Code</u></dt>
 * <p>
 * <dd>The Max Response Code field is meaningful only in Membership Query messages.</dd>
 * <p>
 * <dt><u>Checksum</u></dt>
 * <dd>The checksum is the 16-bit one's complement of the one's complement sum of the
 * whole IGMP message (the entire IP payload). For computing the checksum, the checksum
 * field is set to zero. When transmitting packets, the checksum MUST be computed and
 * inserted into this field. When receiving packets, the checksum MUST be verified before
 * processing a packet.
 * <p>
 * See {@link #getChecksum()}, {@link #setChecksum(short)},
 * {@link #calculateChecksum(ByteBuffer, int)}, {@link #verifyChecksum(ByteBuffer)}.</dd>
 * <p>
 * <dt><u>Group Address</u></dt>
 * <p>
 * <dd>In a Membership Report message, the group address field holds the IP multicast
 * group address of the group being or currently joined.</dd>
 * <p>
 * See {@link #getGroupAddress()}, {@link #setGroupAddress(byte[])}.
 * </dl>
 * </blockquote>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class IGMPv2ReportMessage
                extends IGMPGroupMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * 
     */
    public static final class Parser
                    implements IGMPMessage.ParserType {

        @Override
        public IGMPMessage parse(final ByteBuffer buffer) throws ParseException {
            return new IGMPv2ReportMessage(buffer);
        }

        @Override
        public boolean verifyChecksum(final ByteBuffer buffer) throws MissingParserException, ParseException {
            return IGMPv2ReportMessage.verifyChecksum(buffer);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    /** */
    public static final byte MESSAGE_TYPE = 0x16;

    /** */
    public static final int BASE_MESSAGE_LENGTH = 8;

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return
     */
    public static IGMPMessage.Parser getIGMPMessageParser() {
        return getIGMPMessageParser(new IGMPv2ReportMessage.Parser());
    }

    /**
     * @return
     */
    public static IPMessage.Parser getIPMessageParser() {
        return getIPMessageParser(new IGMPv2ReportMessage.Parser());
    }

    /**
     * @return
     */
    public static IPv4Packet.Parser getIPv4PacketParser() {
        return getIPv4PacketParser(new IGMPv2ReportMessage.Parser());
    }

    /**
     * @return
     */
    public static IPPacket.BufferParser getIPPacketParser() {
        return getIPPacketParser(new IGMPv2ReportMessage.Parser());
    }

    /**
     * Verifies the IGMP message checksum. Called by the parser prior to constructing the
     * packet.
     * 
     * @param buffer
     *            - the buffer containing the IGMP message.
     */
    public static boolean verifyChecksum(final ByteBuffer buffer) {
        return Checksum.get(buffer) == IGMPMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH);
    }

    /**
     * Writes the IGMP message checksum into a buffer containing an IGMP message.
     * 
     * @param buffer
     */
    public static void setChecksum(final ByteBuffer buffer) {
        Checksum.set(buffer, IGMPMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH));
    }

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param groupAddress
     */
    public IGMPv2ReportMessage(final byte[] groupAddress) {
        super(BASE_MESSAGE_LENGTH, MESSAGE_TYPE, (byte) 0, groupAddress);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPv2ReportMessage.IGMPv2ReportMessage", Logging.address(groupAddress)));
        }
    }

    /**
     * @param buffer
     * @throws ParseException
     */
    public IGMPv2ReportMessage(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPv2ReportMessage.IGMPv2ReportMessage", buffer));
        }
    }

    @Override
    public void writeChecksum(final ByteBuffer buffer,
                              final byte[] sourceAddress,
                              final byte[] destinationAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPv2ReportMessage.writeChecksum", buffer, Logging.address(sourceAddress),
                                          Logging.address(destinationAddress)));
        }

        IGMPv2ReportMessage.setChecksum(buffer);
    }

    @Override
    public int getMessageLength() {
        return BASE_MESSAGE_LENGTH;
    }

    @Override
    public byte getType() {
        return MESSAGE_TYPE;
    }

}
