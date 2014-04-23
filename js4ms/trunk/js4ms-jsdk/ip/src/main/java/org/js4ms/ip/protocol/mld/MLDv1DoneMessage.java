package org.js4ms.ip.protocol.mld;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MLDv1DoneMessage.java [org.js4ms.jsdk:ip]
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
 * A Multicast Listener Done Message as described in
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
 *  |  Type = 132   |     Code      |          Checksum             |
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
 * <dd>A Multicast Listener Done Message has a type value of decimal 132.
 * <p>
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
 * <dd>The Maximum Response Delay field is meaningful only in Query messages.
 * <p>
 * See {@link MLDQueryMessage#getMaximumResponseDelay()}.</dd>
 * <p>
 * <dt><u>Reserved</u></dt>
 * <p>
 * <dd>Initialized to zero by the sender; ignored by receivers.</dd>
 * <p>
 * <dt><u>Multicast Address</u></dt>
 * <p>
 * <dd>In a Done message, the Multicast Address field holds a specific IPv6 multicast
 * address to which the message sender is ceasing to listen.
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
 * first 24 octets.</dd>
 * </dl>
 * </blockquote>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class MLDv1DoneMessage
                extends MLDGroupMessage {

    /**
     * 
     */
    public static class Parser
                    implements MLDMessage.ParserType {

        @Override
        public MLDMessage parse(final ByteBuffer buffer) throws ParseException {
            return new MLDv1DoneMessage(buffer);
        }

        @Override
        public boolean verifyChecksum(final ByteBuffer buffer,
                                      final byte[] sourceAddress,
                                      final byte[] destinationAddress) throws MissingParserException, ParseException {
            return MLDv1DoneMessage.verifyChecksum(buffer, sourceAddress, destinationAddress);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    /** */
    public static final byte MESSAGE_TYPE = (byte) 132;

    /** */
    public static final int BASE_MESSAGE_LENGTH = 24;

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return
     */
    public static MLDMessage.Parser getMLDMessageParser() {
        return getMLDMessageParser(new MLDv1DoneMessage.Parser());
    }

    /**
     * @return
     */
    public static IPMessage.Parser getIPMessageParser() {
        return getIPMessageParser(new MLDv1DoneMessage.Parser());
    }

    /**
     * @return
     */
    public static IPv6Packet.Parser getIPv6PacketParser() {
        return getIPv6PacketParser(new MLDv1DoneMessage.Parser());
    }

    /**
     * @return
     */
    public static IPPacket.BufferParser getIPPacketParser() {
        return getIPPacketParser(new MLDv1DoneMessage.Parser());
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
     * @param groupAddress
     */
    public MLDv1DoneMessage(final byte[] groupAddress) {
        super(BASE_MESSAGE_LENGTH, (byte) 0, groupAddress);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("MLDv1DoneMessage.MLDv1DoneMessage", Logging.address(groupAddress)));
        }
    }

    /**
     * @param buffer
     * @throws ParseException
     */
    public MLDv1DoneMessage(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("MLDv1DoneMessage.MLDv1DoneMessage", buffer));
        }
    }

    @Override
    public void writeChecksum(final ByteBuffer buffer,
                              final byte[] sourceAddress,
                              final byte[] destinationAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("MLDv1DoneMessage.writeChecksum",
                                        buffer,
                                        Logging.address(sourceAddress),
                                        Logging.address(destinationAddress)));
        }

        MLDv1DoneMessage.setChecksum(buffer, sourceAddress, destinationAddress);
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
