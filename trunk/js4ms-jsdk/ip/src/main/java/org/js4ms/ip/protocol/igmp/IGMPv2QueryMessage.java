package org.js4ms.ip.protocol.igmp;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IGMPv2QueryMessage.java [org.js4ms.jsdk:ip]
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
import org.js4ms.common.util.logging.Logging;
import org.js4ms.ip.IPMessage;
import org.js4ms.ip.IPPacket;
import org.js4ms.ip.ipv4.IPv4Packet;




/**
 * Represents an IGMPv2 Membership Query Message.
 * Membership Queries are sent by IP multicast routers to query the
 * multicast reception state of neighboring interfaces.
 * See [<a href="http://www.ietf.org/rfc/rfc2236.txt">RFC-2236</a>].
 * 
 * <h3>Message Format</h3>
 * <blockquote>
 * <pre>
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |   Type=0x11   | Max Resp Time |           Checksum            |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                         Group Address                         |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * <dl>
 * <dt><u>Type</u></dt>
 * <p>
 * <dd>An IGMPv2 Query Message is identified by a type value of 0x11.
 * <p>
 * There are two sub-types of Membership Query messages:
 * <ul>
 * <li>General Query, used to learn which groups have members on an attached network.</li>
 * <li>Group-Specific Query, used to learn if a particular group has any members on an
 * attached network.</li>
 * </ul>
 * These two messages are differentiated by the Group Address - a general query has a
 * group address of 0.0.0.0.</dd>
 * <p>
 * <dt><u>Max Response Time</u></dt>
 * <p>
 * <dd>The Max Response Time field specifies the maximum allowed time before sending a
 * responding report in units of 1/10 second. In all other messages, it is set to zero by
 * the sender and ignored by receivers.
 * <p>
 * Varying this setting allows IGMPv2 routers to tune the "leave latency" (the time
 * between the moment the last host leaves a group and when the routing protocol is
 * notified that there are no more members). It also allows tuning of the burstiness of
 * IGMP traffic on a subnet.
 * <p>
 * See {@link #getMaximumResponseTime()}, {@link #setMaximumResponseTime(int)}.</dd>
 * <p>
 * <dt><u>Checksum</u></dt>
 * <p>
 * <dd>The checksum is the 16-bit one's complement of the one's complement sum of the
 * whole IGMP message (the entire IP payload). For computing the checksum, the checksum
 * field is set to zero. When transmitting packets, the checksum MUST be computed and
 * inserted into this field. When receiving packets, the checksum MUST be verified before
 * processing a packet.
 * <p>
 * See {@link #getChecksum()}, {@link #setChecksum(short)},
 * {@link #verifyChecksum(ByteBuffer)}, {@link #calculateChecksum(ByteBuffer, int)}.</dd>
 * <p>
 * <dt><u>Group Address</u></dt>
 * <p>
 * <dd>In a Membership Query message, the group address field is set to zero when sending
 * a General Query, and set to the group address being queried when sending a
 * Group-Specific Query.
 * <p>
 * See {@link #getGroupAddress()}, {@link #setGroupAddress(byte[])}.</dd>
 * <p>
 * <dt><u>Other fields</u></dt>
 * <p>
 * <dd>Note that IGMP messages may be longer than 8 octets, especially future
 * backwards-compatible versions of IGMP. As long as the Type is one that is recognized,
 * an IGMPv2 implementation MUST ignore anything past the first 8 octets while processing
 * the packet. However, the IGMP checksum is always computed over the whole IP payload,
 * not just over the first 8 octets.</dd>
 * </dl>
 * </blockquote>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class IGMPv2QueryMessage
                extends IGMPQueryMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * 
     * 
     */
    public static final class Parser
                    implements IGMPMessage.ParserType {

        @Override
        public IGMPMessage parse(final ByteBuffer buffer) throws ParseException {
            return new IGMPv2QueryMessage(buffer);
        }

        /**
         * @param buffer
         */
        public boolean verifyChecksum(final ByteBuffer buffer) {
            return IGMPv2QueryMessage.verifyChecksum(buffer);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    /** */
    public static final int BASE_MESSAGE_LENGTH = 8;

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return
     */
    public static IGMPMessage.Parser getIGMPMessageParser() {
        return getIGMPMessageParser(new IGMPv2QueryMessage.Parser());
    }

    /**
     * @return
     */
    public static IPMessage.Parser getIPMessageParser() {
        return getIPMessageParser(new IGMPv2QueryMessage.Parser());
    }

    /**
     * @return
     */
    public static IPv4Packet.Parser getIPv4PacketParser() {
        return getIPv4PacketParser(new IGMPv2QueryMessage.Parser());
    }

    /**
     * @return
     */
    public static IPPacket.BufferParser getIPPacketParser() {
        return getIPPacketParser(new IGMPv2QueryMessage.Parser());
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
     * @param maximumResponseTime
     * @param groupAddress
     */
    public IGMPv2QueryMessage(final short maximumResponseTime, final byte[] groupAddress) {
        super(BASE_MESSAGE_LENGTH, maximumResponseTime, groupAddress);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPv2QueryMessage.IGMPv2QueryMessage", maximumResponseTime,
                                          Logging.address(groupAddress)));
        }
    }

    /**
     * @param buffer
     * @throws ParseException
     */
    public IGMPv2QueryMessage(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPv2QueryMessage.IGMPv2QueryMessage", buffer));
        }
    }

    @Override
    public void writeChecksum(final ByteBuffer buffer,
                              final byte[] sourceAddress,
                              final byte[] destinationAddress) {
        IGMPv2QueryMessage.setChecksum(buffer);
    }

    @Override
    public int getMessageLength() {
        return BASE_MESSAGE_LENGTH;
    }

    /**
     * Returns the "<code>Max Resp Time</code>" value in milliseconds.
     * This value is calculated from {@linkplain #MaxRespCode Max Resp Code}.
     * 
     * @return
     */
    public int getMaximumResponseTime() {
        return getMaxRespCode() * 100;
    }

    /**
     * Sets the "<code>Max Resp Time</code>" value in milliseconds.
     * This value is converted into a {@linkplain #MaxRespCode Max Resp Code} value.
     * 
     * @param milliseconds
     */
    public void setMaximumResponseTime(final int milliseconds) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPv2QueryMessage.setMaximumResponseTime", milliseconds));
        }

        short tenths = (short) (milliseconds / 100);
        setMaxRespCode((byte) tenths);
    }
}
