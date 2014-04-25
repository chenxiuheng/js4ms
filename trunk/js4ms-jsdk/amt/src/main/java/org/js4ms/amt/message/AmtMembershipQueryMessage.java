package org.js4ms.amt.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * AmtMembershipQueryMessage.java [org.js4ms.jsdk:amt]
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


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.field.BooleanField;
import org.js4ms.common.util.buffer.field.ByteArrayField;
import org.js4ms.common.util.buffer.field.IntegerField;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.ip.IPPacket;
import org.js4ms.ip.ipv4.IPv4Packet;
import org.js4ms.ip.ipv6.IPv6Packet;
import org.js4ms.ip.protocol.igmp.IGMPMessage;
import org.js4ms.ip.protocol.igmp.IGMPv3QueryMessage;
import org.js4ms.ip.protocol.mld.MLDMessage;
import org.js4ms.ip.protocol.mld.MLDv2QueryMessage;



/**
 * Represents an AMT Membership Query message.
 * The following description is excerpted from the <a
 * href="http://tools.ietf.org/html/draft-ietf-mboned-auto-multicast">Automatic Multicast
 * Tunneling (AMT)</a> specification.
 * <p>
 * A relay sends a Membership Query message to a gateway to solicit a Membership Update
 * response, but only after receiving a Request message from the gateway. The successful
 * delivery of this message to a gateway marks the start of the second-stage in the
 * three-way handshake used to create or update tunnel state within a relay. The UDP/IP
 * datagram containing this message MUST carry a valid, non- zero UDP checksum and carry
 * the following IP address and UDP port values: <blockquote>
 * <dl>
 * <dt><u>Source IP Address</u></dt>
 * <p>
 * <dd>The destination IP address carried by the Request message (i.e. the unicast IP
 * address of the relay).</dd>
 * <p>
 * <dt><u>Source UDP Port</u></dt>
 * <p>
 * <dd>The destination UDP port carried by the Request message (i.e. the IANA-assigned AMT
 * port number).</dd>
 * <p>
 * <dt><u>Destination IP Address</u></dt>
 * <p>
 * <dd>The source IP address carried by the Request message. Note: The value of this field
 * may be changed as a result of network address translation before arriving at the
 * gateway.</dd>
 * <p>
 * <dt><u>Destination UDP Port</u></dt>
 * <p>
 * <dd>The source UDP port carried by the Request message. Note: The value of this field
 * may be changed as a result of network address translation before arriving at the
 * gateway.</dd>
 * </dl>
 * </blockquote>
 * <h3>Message Format</h3> <blockquote>
 * 
 * <pre>
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |  V=0  |Type=4 | Reserved  |L|G|         Response MAC          |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                               +
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                         Request Nonce                         |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                                                               |
 *    |               Encapsulated General Query Message              |
 *    ~                 IPv4:IGMPv3(Membership Query)                 ~
 *    |                  IPv6:MLDv2(Listener Query)                   |
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |     Gateway Port Number       |                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                               +
 *    |                                                               |
 *    +                                                               +
 *    |                Gateway IP Address (IPv4 or IPv6)              |
 *    +                                                               +
 *    |                                                               |
 *    +                               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <dl>
 * <dt><u>Version (V)</u></dt>
 * <p>
 * <dd>The protocol version number for this message is 0.</dd>
 * <p>
 * <dt><u>Type</u></dt>
 * <p>
 * <dd>The type number for this message is 4.</dd>
 * <p>
 * <dt><u>Reserved</u></dt>
 * <p>
 * <dd>Reserved bits that MUST be set to zero by the relay and ignored by the gateway.</dd>
 * <p>
 * <dt><u>Limit (L) Flag</u></dt>
 * <p>
 * <dd>A 1-bit flag set to 1 to indicate that the relay is NOT accepting Membership Update
 * messages from new gateway tunnel endpoints and that it will ignore any that are. A
 * value of 0 has no special significance - the relay may or may not be accepting
 * Membership Update messages from new gateway tunnel endpoints. A gateway checks this
 * flag before attempting to create new group subscription state on the relay to determine
 * whether it should restart relay discovery. A gateway that has already created group
 * subscriptions on the relay may ignore this flag. Support for this flag is RECOMMENDED.</dd>
 * <p>
 * <dt><u>Gateway Address (G) Flag</u></dt>
 * <p>
 * <dd>A 1-bit flag set to 0 to indicate that the message does NOT carry the Gateway Port
 * and Gateway IP Address fields, and 1 to indicate that it does. A relay implementation
 * that supports the optional teardown procedure (See Section 5.3.3.5) SHOULD set this
 * flag and and the Gateway Address field values. If a relay sets this flag, it MUST also
 * include the Gateway Address fields in the message. A gateway implementation that does
 * not support the optional teardown procedure (See Section 5.2.3.7) MAY ignore this flag
 * and the Gateway Address fields if they are present.</dd>
 * <p>
 * <dt><u>Response MAC</u></dt>
 * <p>
 * <dd>A 48-bit source authentication hash generated by the relay as described in Section
 * 5.3.5. The gateway echoes this value in subsequent Membership Update messages to allow
 * the relay to verify that the sender of a Membership Update message was the intended
 * receiver of a Membership Query sent by the relay.</dd>
 * <p>
 * <dt><u>Request Nonce</u></dt>
 * <p>
 * <dd>A 32-bit value copied from the Request Nonce field (Section 5.1.3.5) carried by a
 * Request message. The relay will have included this value in the Response MAC hash
 * computation. The gateway echoes this value in subsequent Membership Update messages.
 * The gateway also uses this value to match a Membership Query to a Request message.</dd>
 * <p>
 * <dt><u>Encapsulated General Query Message</u></dt>
 * <p>
 * <dd>An IP-encapsulated IGMP or MLD message generated by the relay. This field will
 * contain one of the following IP datagrams: IPv4:IGMPv3 Membership Query IPv6:MLDv2
 * Listener Query The source address carried by the query message should be set as
 * described in Section 5.3.3.3. The Querier's Query Interval Code (QQIC) field in the
 * general query is used by a relay to specify the time offset a gateway should use to
 * schedule a new three-way handshake to refresh the group membership state within the
 * relay (current time + Query Interval). The Querier's Robustness Variable (QRV) field in
 * the general query is used by a relay to specify the number of times a gateway should
 * retransmit unsolicited membership reports, encapsulated within Membership Update
 * messages, and optionally, the number of times to send a Teardown message.</dd>
 * <p>
 * <dt><u>Gateway Address Fields</u></dt>
 * <p>
 * <dd>The Gateway Port Number and Gateway Address fields are present in the Membership
 * Query message if, and only if, the "G" flag is set. A gateway need not parse the
 * encapsulated IP datagram to determine the position of these fields within the UDP
 * datagram containing the Membership Query message - if the G-flag is set, the gateway
 * may simply subtract the total length of the fields (18 bytes) from the total length of
 * the UDP datagram to obtain the offset. <blockquote>
 * <dl>
 * <dt><u>Gateway Port Number</u></dt>
 * <p>
 * <dd>A 16-bit UDP port containing a UDP port value. The Relay sets this field to the
 * value of the UDP source port of the Request message that triggered the Query message.</dd>
 * <p>
 * <dt><u>Gateway IP Address</u></dt>
 * <p>
 * <dd>A 16-byte IP address that, when combined with the value contained in the Gateway
 * Port Number field, forms the gateway endpoint address that the relay will use to
 * identify the tunnel instance, if any, created by a subsequent Membership Update
 * message. This field may contain an IPv6 address or an IPv4 address stored as an IPv4-
 * compatible IPv6 address, where the IPv4 address is prefixed with 96 bits set to zero
 * (See [RFC4291]). This address must match that used by the relay to compute the value
 * stored in the Response MAC field.</dd>
 * </dl>
 * </blockquote>
 * </dl>
 * </blockquote>
 * 
 * @see <a
 *      href="http://tools.ietf.org/html/draft-ietf-mboned-auto-multicast">draft-ietf-auto-multicast</a>
 * @author Greg Bumgardner (gbumgard)
 */
public final class AmtMembershipQueryMessage
                extends AmtEncapsulationMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * An AMT Membership Query message parser/factory.
     */
    public static class Parser
                    extends AmtEncapsulationMessage.Parser {

        /**
         * 
         */
        public Parser() {
            this(DEFAULT_QUERY_PACKET_PARSER);
        }

        /**
         * @param ipParser
         */
        public Parser(IPPacket.BufferParser ipParser) {
            super(ipParser);
        }

        @Override
        public AmtEncapsulationMessage constructMessage(ByteBuffer buffer) throws ParseException {
            return new AmtMembershipQueryMessage(buffer);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    public static final byte MESSAGE_TYPE = 0x4;

    private static final int BASE_MESSAGE_LENGTH = 12;

    /**
     * Singleton instance of parser for IP packets carrying IGMP or MLD query messages.
     */
    private static final IPPacket.BufferParser DEFAULT_QUERY_PACKET_PARSER = getQueryPacketParser();

    private static final BooleanField GatewayAddressFlag = new BooleanField(1, 0);

    private static final BooleanField LimitFlag = new BooleanField(1, 1);

    private static final ByteArrayField ResponseMac = new ByteArrayField(2, 6);

    private static final IntegerField RequestNonce = new IntegerField(8);

    public static final IPv4Packet igmpGeneralQueryPacket;

    public static final IPv6Packet mldGeneralQueryPacket;

    static {
        IGMPv3QueryMessage igmpGeneralQuery = new IGMPv3QueryMessage((short) 0);
        igmpGeneralQueryPacket = IGMPv3QueryMessage.constructIPv4Packet(new byte[4], // Source
                                                                                     // address
                                                                                     // is
                                                                                     // zero
                                                                                     // for
                                                                                     // AMT
                                                                        IGMPv3QueryMessage.QUERY_DESTINATION_ADDRESS,
                                                                        igmpGeneralQuery);

        MLDv2QueryMessage mldGeneralQuery = new MLDv2QueryMessage();
        mldGeneralQueryPacket = MLDv2QueryMessage.constructIPv6Packet(new byte[16], // Source
                                                                                    // address
                                                                                    // is
                                                                                    // zero
                                                                                    // for
                                                                                    // AMT
                                                                      MLDv2QueryMessage.QUERY_DESTINATION_ADDRESS,
                                                                      mldGeneralQuery);
    }

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return A parser that constructs an {@link IGMPMessage} or {@link MLDMessage}
     *         object from the contents of a ByteBuffer.
     */
    public static IPPacket.BufferParser getQueryPacketParser() {
        IPPacket.BufferParser parser = new IPPacket.BufferParser();
        parser.add(IGMPMessage.getIPv4PacketParser());
        parser.add(MLDMessage.getIPv6PacketParser());
        return parser;
    }

    /**
     * @return A parser than constructs an AmtMembershipQueryMessage object from
     *         the contents of a ByteBuffer.
     */
    public static AmtMembershipQueryMessage.Parser constructParser() {
        AmtMembershipQueryMessage.Parser parser = new AmtMembershipQueryMessage.Parser();
        parser.setIPPacketParser(getQueryPacketParser());
        return parser;
    }

    /*-- Member Variables ---------------------------------------------------*/

    private InetSocketAddress gatewayAddress = null;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs an instance using the specified values to initialize the
     * corresponding message fields.
     * 
     * @param responseMac
     *            A 6-byte array containing a 48-bit response MAC.
     * @param requestNonce
     *            An integer request nonce value.
     * @param queryPacket
     *            An IP packet containing an IGMPv3 or MLDv2 general query.
     * @param gatewayAddress
     *            An optional InetSocketAddress object containing the
     *            gateway IP address and UDP port number values.
     */
    public AmtMembershipQueryMessage(final byte[] responseMac,
                                     final int requestNonce,
                                     final IPPacket queryPacket,
                                     final InetSocketAddress gatewayAddress) {
        super(BASE_MESSAGE_LENGTH, MESSAGE_TYPE, queryPacket);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry(
                                        "AmtMembershipQueryMessage.AmtMembershipQueryMessage",
                                        Logging.mac(responseMac),
                                        requestNonce,
                                        queryPacket));
        }

        GatewayAddressFlag.set(getBufferInternal(), gatewayAddress != null);
        setResponseMac(responseMac);
        setRequestNonce(requestNonce);
        setGatewayAddress(gatewayAddress);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    /**
     * Constructs an instance from the contents of a ByteBuffer.
     * 
     * @param buffer
     *            A ByteBuffer containing a single AMT Membership Query message.
     * @throws ParseException
     *             The buffer could not be parsed to produce a valid AMT Membership Query
     *             message.
     */
    public AmtMembershipQueryMessage(final ByteBuffer buffer) throws ParseException {
        super(buffer, BASE_MESSAGE_LENGTH);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMembershipQueryMessage.AmtMembershipQueryMessage", buffer));
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
     *            The logger that will be used to generate the log messages.
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": response-MAC=" + Logging.mac(getResponseMac())));
        logger.log(level,this.log.msg(": request-nonce=" + getRequestNonce()));
    }

    @Override
    public final void writeTo(final ByteBuffer buffer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMembershipQueryMessage.writeTo", buffer));
        }
        super.writeTo(buffer);
        if (this.unparsedPacket == null && this.gatewayAddress != null) {
            buffer.put((byte) 0);
            buffer.put((byte) 0);
            int port = this.gatewayAddress.getPort();
            buffer.put((byte) ((port >> 8) & 0xFF));
            buffer.put((byte) (port & 0xFF));
            byte[] address = this.gatewayAddress.getAddress().getAddress();

            // Is the address an IPv4 address?
            if (address.length == 4) {
                // Write leading zeros for /96 IPv6 mapping
                for (int i = 0; i < 12; i++) {
                    buffer.put((byte) 0);
                }
            }

            // Writes 4 or 16 bytes
            buffer.put(address);
        }
    }

    @Override
    public Byte getType() {
        return MESSAGE_TYPE;
    }

    /**
     * Gets the Limit (L) flag field value.
     * 
     * @return The boolean value of the limit (L) flag field.
     */
    public boolean getProtocolFlag() {
        return LimitFlag.get(getBufferInternal());
    }

    /**
     * Sets the limit (L) flag field to the specified value.
     * 
     * @param limitReached
     *            A boolean value where <code>true</code> indicates that the
     *            relay is no longer accepting group membership requests from
     *            new gateways.
     */
    public void setProtocolFlag(final boolean limitReached) {
        LimitFlag.set(getBufferInternal(), limitReached);
    }

    /**
     * Gets the gateway address flag field value.
     * 
     * @return A boolean value that indicates whether the message includes the gateway
     *         address fields.
     */
    public boolean getGatewayAddressFlag() {
        return GatewayAddressFlag.get(getBufferInternal());
    }

    /**
     * Sets the gateway address flag field value.
     * 
     * @param hasGatewayAddress
     *            A boolean value that indicates whether the message include the gateway
     *            address fields.
     */
    protected void setGatewayAddressFlag(final boolean hasGatewayAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMembershipQueryMessage.setGatewayAddressFlag", hasGatewayAddress));
        }

        GatewayAddressFlag.set(getBufferInternal(), hasGatewayAddress);
    }

    /**
     * Gets the response MAC field value.
     * 
     * @return A 6-byte array containing the 48-bit response MAC field value.
     */
    public byte[] getResponseMac() {
        return ResponseMac.get(getBufferInternal());
    }

    /**
     * Sets the response MAC field value.
     * 
     * @param responseMac
     *            A 6-byte array containing a 48-bit MAC value.
     */
    public void setResponseMac(final byte[] responseMac) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMembershipQueryMessage.setResponseMac", Logging.mac(responseMac)));
        }

        ResponseMac.set(getBufferInternal(), responseMac);
    }

    /**
     * Gets the request nonce field value.
     * 
     * @return The integer value of the request nonce field.
     */
    public int getRequestNonce() {
        return RequestNonce.get(getBufferInternal());
    }

    /**
     * Sets the request nonce field value.
     * 
     * @param requestNonce
     *            An integer nonce value. Typically copied from the
     *            corresponding field in an {@link AmtRequestMessage}.
     */
    public void setRequestNonce(final int requestNonce) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMembershipQueryMessage.setRequestNonce", requestNonce));
        }

        RequestNonce.set(getBufferInternal(), requestNonce);
    }

    /**
     * Gets the gateway IP address and UDP port number field values.
     * 
     * @return An InetSocketAddress object containing the gateway IP Address and UDP port
     *         field values.
     */
    public InetSocketAddress getGatewayAddress() {
        if (this.unparsedPacket != null) {
            int index = this.packet.getTotalLength();
            short port = (short) ((this.unparsedPacket.get(index++) << 8) | this.unparsedPacket.get(index++));
            byte[] address = new byte[16];
            for (int i = 0; i < 16; i++) {
                address[i] = this.unparsedPacket.get(index++);
            }
            try {
                this.gatewayAddress = new InetSocketAddress(InetAddress.getByAddress(address), port);
            }
            catch (UnknownHostException e) {
            }
        }
        return this.gatewayAddress;
    }

    /**
     * Sets the gateway IP address and and UDP port number field values.
     * 
     * @param gatewayAddress
     *            An InetSocketAddress object containing the gateway IP Address and UDP
     *            port number.
     */
    public void setGatewayAddress(final InetSocketAddress gatewayAddress) {
        this.gatewayAddress = gatewayAddress;
        setGatewayAddressFlag(gatewayAddress != null);
        if (this.unparsedPacket != null) {
            int index = this.packet.getTotalLength() + 2;
            int port = this.gatewayAddress.getPort();
            this.unparsedPacket.put(index++, (byte) ((port >> 8) & 0xFF));
            this.unparsedPacket.put(index++, (byte) (port & 0xFF));
            byte[] address = this.gatewayAddress.getAddress().getAddress();
            if (address.length == 4) {
                for (int i = 0; i < 12; i++) {
                    this.unparsedPacket.put(index++, (byte) 0);
                }
                for (int i = 0; i < 4; i++) {
                    this.unparsedPacket.put(index++, address[i]);
                }
            }
            else {
                for (int i = 0; i < 16; i++) {
                    this.unparsedPacket.put(index++, address[i]);
                }
            }
        }
    }
}
