package org.js4ms.ip.protocol.mld;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MLDv1QueryMessage.java [org.js4ms.jsdk:ip]
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
import org.js4ms.ip.ipv6.IPv6Packet;




/**
 * A Multicast Listener Query Message as described in
 * [<a href="http://tools.ietf.org/html/rfc2710">RFC-2710</a>].
 * <p>
 * MLD is a sub-protocol of ICMPv6, that is, MLD message types are a subset of the set of
 * ICMPv6 messages, and MLD messages are identified in IPv6 packets by a preceding Next
 * Header value of 58. All MLD messages described in this document are sent with a
 * link-local IPv6 Source Address, an IPv6 Hop Limit of 1, and an IPv6 Router Alert option
 * [RTR-ALERT] in a Hop-by-Hop Options header. (The Router Alert option is necessary to
 * cause routers to examine MLD messages sent to multicast addresses in which the routers
 * themselves have no interest.)
 * <h3>Message Format</h3> <blockquote>
 * 
 * <pre>
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |  Type = 130   |     Code      |          Checksum             |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |     Maximum Response Delay    |          Reserved             |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +                       Multicast Address                       +
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <dl>
 * <dt><u>Type</u></dt>
 * <p>
 * <dd>A Multicast Listener Query has a type value of decimal 130. These are two subtypes
 * of listener queries. They are differentiated by the contents of the Multicast Address
 * field:
 * <ul>
 * <li>A General Query, used to learn which multicast addresses have listeners on an
 * attached link.
 * <li>A Multicast-Address-Specific Query, used to learn if a particular multicast address
 * has any listeners on an attached link.
 * </ul>
 * See {@link #getType()}.</dd>
 * <p>
 * <dt><u>Code</u></dt>
 * <p>
 * <dd>Initialized to zero by the sender; ignored by receivers.</dd>
 * <p>
 * <dt><u>Checksum</u></dt>
 * <p>
 * <dd>The standard ICMPv6 checksum, covering the entire MLD message plus a
 * &quot;pseudo-header&quot; of IPv6 header fields [ICMPv6,IPv6].
 * <p>
 * See {@link #getChecksum()}, {@link #setChecksum(short)},
 * {@link #calculateChecksum(ByteBuffer, int, byte[], byte[])} and
 * {@link #verifyChecksum(byte[], byte[], int)}.</dd>
 * <p>
 * <dt><u>Maximum Response Delay</u></dt>
 * <p>
 * <dd>The Maximum Response Delay field is meaningful only in Query messages, and
 * specifies the maximum allowed delay before sending a responding Report, in units of
 * milliseconds. In all other messages, it is set to zero by the sender and ignored by
 * receivers.
 * <p>
 * Varying this value allows the routers to tune the &quot;leave latency&quot; (the time
 * between the moment the last node on a link ceases listening to a particular multicast
 * address and moment the routing protocol is notified that there are no longer any
 * listeners for that address), as discussed in section 7.8. It also allows tuning of the
 * burstiness of MLD traffic on a link, as discussed in section 7.3.
 * <p>
 * See {@link #getMaximumResponseDelay()} and {@link #setMaximumResponseDelay(short)}.</dd>
 * <p>
 * <dt><u>Reserved</u></dt>
 * <p>
 * <dd>Initialized to zero by the sender; ignored by receivers.</dd>
 * <p>
 * <dt><u>Multicast Address</u></dt>
 * <p>
 * <dd>In a Query message, the Multicast Address field is set to zero when sending a
 * General Query, and set to a specific IPv6 multicast address when sending a
 * Multicast-Address-Specific Query.
 * <p>
 * See {@link #getGroupAddress()}, {@link #setGroupAddress(byte[])} and
 * {@link #setGroupAddress(java.net.InetAddress)}.</dd>
 * <p>
 * <dt><u>Other fields</u></dt>
 * <p>
 * <dd>The length of a received MLD message is computed by taking the IPv6 Payload Length
 * value and subtracting the length of any IPv6 extension headers present between the IPv6
 * header and the MLD message. If that length is greater than 24 octets, that indicates
 * that there are other fields present beyond the fields described above, perhaps
 * belonging to a future backwards-compatible version of MLD. An implementation of the
 * version of MLD specified in this document MUST NOT send an MLD message longer than 24
 * octets and MUST ignore anything past the first 24 octets of a received MLD message. In
 * all cases, the MLD checksum MUST be computed over the entire MLD message, not just the
 * first 24 octets.
 * <p>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class MLDv1QueryMessage
                extends MLDQueryMessage {

    /**
     * 
     */
    public static class Parser
                    implements MLDMessage.ParserType {

        @Override
        public MLDMessage parse(final ByteBuffer buffer) throws ParseException {
            return new MLDv1QueryMessage(buffer);
        }

        @Override
        public boolean verifyChecksum(final ByteBuffer buffer,
                                      final byte[] sourceAddress,
                                      final byte[] destinationAddress) throws MissingParserException, ParseException {
            return MLDv1QueryMessage.verifyChecksum(buffer, sourceAddress, destinationAddress);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    /** */
    public static final int BASE_MESSAGE_LENGTH = 24;

    /** */
    public static final short QUERY_RESPONSE_INTERVAL = 10 * 1000; // 10 secs as ms

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return
     */
    public static MLDMessage.Parser getMLDMessageParser() {
        return getMLDMessageParser(new MLDv1QueryMessage.Parser());
    }

    /**
     * @return
     */
    public static IPMessage.Parser getIPMessageParser() {
        return getIPMessageParser(new MLDv1QueryMessage.Parser());
    }

    /**
     * @return
     */
    public static IPv6Packet.Parser getIPv6PacketParser() {
        return getIPv6PacketParser(new MLDv1QueryMessage.Parser());
    }

    /**
     * @return
     */
    public static IPPacket.BufferParser getIPPacketParser() {
        return getIPPacketParser(new MLDv1QueryMessage.Parser());
    }

    /**
     * @param sourceAddress
     * @return
     */
    public static IPv6Packet constructGeneralQueryPacket(final byte[] sourceAddress) {
        return constructGroupQueryPacket(sourceAddress, IPv6GeneralQueryGroupAddress);
    }

    /**
     * @param sourceAddress
     * @param groupAddress
     * @return
     */
    public static IPv6Packet constructGroupQueryPacket(final byte[] sourceAddress,
                                                       final byte[] groupAddress) {
        MLDv1QueryMessage message = new MLDv1QueryMessage(groupAddress);
        message.setMaximumResponseDelay(QUERY_RESPONSE_INTERVAL);
        return constructIPv6Packet(sourceAddress, groupAddress, message);
    }

    /**
     * Verifies the MLD message checksum. Called by the parser prior to constructing the
     * packet.
     * 
     * @param buffer
     *            - the buffer containing the MLD message.
     * @param sourceAddress
     *            An IPv6 (16-byte) address..
     * @param destinationAddress
     *            An IPv6 (16-byte) address.
     * @return
     */
    public static boolean verifyChecksum(final ByteBuffer buffer,
                                         final byte[] sourceAddress,
                                         final byte[] destinationAddress) {
        return Checksum.get(buffer) == MLDMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH, sourceAddress, destinationAddress);
    }

    /**
     * Writes the MLD message checksum into a buffer containing an MLD message.
     * 
     * @param buffer
     *            - a byte array.
     * @param offset
     *            - the offset within the array at which to write the message.
     * @param sourceAddress
     *            An IPv6 (16-byte) address..
     * @param destinationAddress
     *            An IPv6 (16-byte) address.
     */
    public static void setChecksum(final ByteBuffer buffer,
                                   final byte[] sourceAddress,
                                   final byte[] destinationAddress) {
        Checksum.set(buffer, MLDMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH, sourceAddress, destinationAddress));
    }

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs a general query
     * 
     * @param groupAddress
     */
    public MLDv1QueryMessage() {
        super(BASE_MESSAGE_LENGTH);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("MLDv1QueryMessage.MLDv1QueryMessage"));
        }
    }

    /**
     * @param groupAddress
     */
    public MLDv1QueryMessage(final byte[] groupAddress) {
        super(BASE_MESSAGE_LENGTH, groupAddress);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("MLDv1QueryMessage.MLDv1QueryMessage", Logging.address(groupAddress)));
        }
    }

    /**
     * @param buffer
     * @throws ParseException
     */
    public MLDv1QueryMessage(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("MLDv1QueryMessage.MLDv1QueryMessage", buffer));
        }
    }

    @Override
    public void writeChecksum(final ByteBuffer buffer,
                              final byte[] sourceAddress,
                              final byte[] destinationAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("MLDv1QueryMessage.writeChecksum",
                                        buffer,
                                        Logging.address(sourceAddress),
                                        Logging.address(destinationAddress)));
        }

        MLDv1QueryMessage.setChecksum(buffer, sourceAddress, destinationAddress);
    }

    @Override
    public int getMessageLength() {
        return BASE_MESSAGE_LENGTH;
    }

}
