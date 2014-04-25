package org.js4ms.amt.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * AmtTeardownMessage.java [org.js4ms.jsdk:amt]
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
import org.js4ms.common.util.buffer.field.ByteArrayField;
import org.js4ms.common.util.buffer.field.ByteField;
import org.js4ms.common.util.buffer.field.IntegerField;
import org.js4ms.common.util.buffer.field.ShortField;
import org.js4ms.common.util.logging.Logging;



/**
 * Represents an AMT Teardown message.
 * The following description is excerpted from <a
 * href="http://tools.ietf.org/html/draft-ietf-mboned-auto-multicast">Automatic Multicast
 * Tunneling (AMT)</a> specification.
 * <p>
 * A gateway sends a Teardown message to a relay to request that it stop sending Multicast
 * Data messages to a tunnel endpoint created by an earlier Membership Update message. A
 * gateway sends this message when it detects that a Request message sent to the relay
 * carries an address that differs from that carried by a previous Request message. The
 * gateway uses the Gateway IP Address and Gateway Port Number Fields in the Membership
 * Query message to detect these address changes.
 * <p>
 * To provide backwards compatibility with early implementations of the AMT protocol,
 * support for this message and associated procedures is considered OPTIONAL - gateways
 * are not required to send this message and relays are not required to act upon it.
 * <p>
 * The UDP/IP datagram containing this message MUST carry a valid, non- zero UDP checksum
 * and carry the following IP address and UDP port values: <blockquote>
 * <dl>
 * <dt><u>Source IP Address</u></dt>
 * <p>
 * <dd>The IP address of the gateway interface used to send the message. This address may
 * differ from that used to send earlier messages. Note: The value of this field may be
 * changed as a result of network address translation before arriving at the relay.</dd>
 * <p>
 * <dt><u>Source UDP Port</u></dt>
 * <p>
 * <dd>The UDP port number. This port number may differ from that used to send earlier
 * messages. Note: The value of this field may be changed as a result of network address
 * translation before arriving at the relay.</dd>
 * <p>
 * <dt><u>Destination IP Address</u></dt>
 * <p>
 * <dd>The unicast IP address of the relay.</dd>
 * <p>
 * <dt><u>Destination UDP Port</u></dt>
 * <p>
 * <dd>The IANA-assigned AMT port number.</dd>
 * </dl>
 * </blockquote>
 * <h3>Message Format</h3> <blockquote>
 * 
 * <pre>
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |  V=0  |Type=7 |  Reserved     |         Response MAC          |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                               +
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                         Request Nonce                         |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |     Gateway Port Number       |                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                               +
 *    |                                                               |
 *    +                                                               +
 *    |              Gateway IP Address (IPv4 or IPv6)                |
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
 * <dd>The type number for this message is 7.</dd>
 * <p>
 * <dt><u>Reserved</u></dt>
 * <p>
 * <dd>Reserved bits that MUST be set to zero by the gateway and ignored by the relay.</dd>
 * <p>
 * <dt><u>Response MAC</u></dt>
 * <p>
 * <dd>A 48-bit value copied from the Response MAC field (Section 5.1.4.6) in the last
 * Membership Query message the relay sent to the gateway endpoint address of the tunnel
 * to be torn down. The gateway endpoint address is provided by the Gateway IP Address and
 * Gateway Port Number fields carried by the Membership Query message.</dd>
 * <p>
 * <dt><u>Request Nonce</u></dt>
 * <p>
 * <dd>A 32-bit value copied from the Request Nonce field (Section 5.1.4.7) in the last
 * Membership Query message the relay sent to the gateway endpoint address of the tunnel
 * to be torn down. The gateway endpoint address is provided by the Gateway IP Address and
 * Gateway Port Number fields carried by the Membership Query message. This value must
 * match that used by the relay to compute the value stored in the Response MAC field.</dd>
 * <p>
 * <dt><u>Gateway Port Number</u></dt>
 * <p>
 * <dd>A 16-bit UDP port number that, when combined with the value contained in the
 * Gateway IP Address field, forms the tunnel endpoint address that the relay will use to
 * identify the tunnel instance to tear down. The relay provides this value to the gateway
 * using the Gateway Port Number field (Section 5.1.4.9.1) in a Membership Query message.
 * This port number must match that used by the relay to compute the value stored in the
 * Response MAC field.</dd>
 * <p>
 * <dt><u>Gateway IP Address</u></dt>
 * <p>
 * <dd>A 16-byte IP address that, when combined with the value contained in the Gateway
 * Port Number field, forms the tunnel endpoint address that the relay will used to
 * identify the tunnel instance to tear down. The relay provides this value to the gateway
 * using the Gateway IP Address field (Section 5.1.4.9.2) in a Membership Query message.
 * This field may contain an IPv6 address or an IPv4 address stored as an IPv4-compatible
 * IPv6 address, where the IPv4 address is prefixed with 96 bits set to zero (See
 * [RFC4291]). This address must match that used by the relay to compute the value stored
 * in the Response MAC field.</dd>
 * </dl>
 * </blockquote>
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class AmtTeardownMessage
                extends AmtMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * An AMT Teardown message parser/factory.
     */
    public static class Parser
                    implements AmtMessage.ParserType {

        @Override
        public AmtTeardownMessage parse(ByteBuffer buffer) throws ParseException {
            return new AmtTeardownMessage(buffer);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    public static final byte MESSAGE_TYPE = 0x7;

    private static final int MESSAGE_LENGTH = 30;

    @SuppressWarnings("unused")
    private static final ByteField Reserved = new ByteField(1);

    private static final ByteArrayField ResponseMac = new ByteArrayField(2, 6);

    private static final IntegerField RequestNonce = new IntegerField(8);

    private static final ShortField GatewayPort = new ShortField(12);

    private static final ByteArrayField GatewayAddress = new ByteArrayField(14, 16);

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return A parser that constructs an AmtTeardownMessage object from the contents
     *         of a ByteBuffer.
     */
    public static AmtTeardownMessage.Parser constructParser() {
        AmtTeardownMessage.Parser parser = new AmtTeardownMessage.Parser();
        return parser;
    }

    /*-- Member Functions---------------------------------------------------*/

    /**
     * Constructs an instance using the specified values to initialize the
     * corresponding message fields.
     * 
     * @param responseMac
     *            A 6-byte array containing a 48-bit response MAC.
     * @param requestNonce
     *            An integer request nonce value.
     * @param gatewayAddress
     *            An InetSocketAddress object containing the
     *            gateway IP address and UDP port number values.
     */
    public AmtTeardownMessage(final byte[] responseMac,
                              final int requestNonce,
                              final InetSocketAddress gatewayAddress) {
        super(MESSAGE_LENGTH, MESSAGE_TYPE);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry(
                                        "AmtTeardownMessage.AmtTeardownMessage",
                                        Logging.mac(responseMac),
                                        requestNonce,
                                        Logging.address(gatewayAddress)));
        }

        setResponseMac(responseMac);
        setRequestNonce(requestNonce);
        setGatewayAddress(gatewayAddress);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger,Level.FINER);
        }
    }

    /**
     * Constructs an instance from the contents of a ByteBuffer.
     * 
     * @param buffer
     *            A ByteBuffer containing a single AMT Teardown message.
     * @throws ParseException
     *             The buffer could not be parsed to produce a valid AMT Teardown
     *             message.
     */
    public AmtTeardownMessage(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTeardownMessage.AmtTeardownMessage", buffer));
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
     *            The logger that will be used to generate the log messages.
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": response-MAC=" + Logging.mac(getResponseMac())));
        logger.log(level,this.log.msg(": request-nonce=" + getRequestNonce()));
        logger.log(level,this.log.msg(": gateway-address=" + Logging.address(getGatewayAddress())));
    }

    @Override
    public Byte getType() {
        return MESSAGE_TYPE;
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
            logger.finer(this.log.entry("AmtTeardownMessage.setResponseMac", Logging.mac(responseMac)));
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
     * Sets the request nonce field to the specified value.
     * 
     * @param requestNonce
     *            An integer nonce value. Typically copied from the
     *            corresponding field in an {@link AmtRequestMessage} or
     *            {@link AmtMembershipQueryMessage}.
     */
    public void setRequestNonce(final int requestNonce) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTeardownMessage.setRequestNonce", requestNonce));
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
        try {
            return new InetSocketAddress(InetAddress.getByAddress(GatewayAddress.get(getBufferInternal())),
                                         GatewayPort.get(getBufferInternal()));
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Sets the gateway IP address and and UDP port number field values.
     * 
     * @param gatewayAddress
     *            An InetSocketAddress object containing the gateway IP Address and UDP
     *            port number.
     */
    public void setGatewayAddress(final InetSocketAddress gatewayAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTeardownMessage.setGatewayAddress", gatewayAddress));
        }

        short port = (short) gatewayAddress.getPort();
        GatewayPort.set(getBufferInternal(), port);

        byte[] address = gatewayAddress.getAddress().getAddress();
        if (address.length == 4) {
            byte[] ipv6Address = new byte[16];
            for (int i = 0; i < 4; i++) {
                ipv6Address[i + 12] = address[i];
            }
            address = ipv6Address;
        }
        GatewayAddress.set(getBufferInternal(), address);
    }
}
