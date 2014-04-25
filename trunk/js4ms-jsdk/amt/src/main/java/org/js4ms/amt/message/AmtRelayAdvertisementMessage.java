package org.js4ms.amt.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * AmtRelayAdvertisementMessage.java [org.js4ms.jsdk:amt]
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
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.field.ByteArrayField;
import org.js4ms.common.util.buffer.field.IntegerField;
import org.js4ms.common.util.logging.Logging;



/**
 * Represents an AMT Relay Advertisement message.
 * The following description is excerpted from the <a
 * href="http://tools.ietf.org/html/draft-ietf-mboned-auto-multicast">Automatic Multicast
 * Tunneling (AMT)</a> specification.
 * <p>
 * The Relay Advertisement message is used to supply a gateway with a unicast IP address
 * of a relay. A relay sends this message to a gateway when it receives a Relay Discovery
 * message from that gateway. The UDP/IP datagram containing this message MUST carry a
 * valid, non- zero UDP checksum and carry the following IP address and UDP port values:
 * <blockquote>
 * <dl>
 * <dt><u>Source IP Address</u></dt>
 * <p>
 * <dd>The destination IP address carried by the Relay Discovery message (i.e. the Relay
 * Discovery Address advertised by the relay).</dd>
 * <p>
 * <dt><u>Source UDP Port</u></dt>
 * <p>
 * <dd>The destination UDP port carried by the Relay Discovery message (i.e. the
 * IANA-assigned AMT port number).</dd>
 * <p>
 * <dt><u>Destination IP Address</u></dt>
 * <p>
 * <dd>The source IP address carried by the Relay Discovery message. Note: The value of
 * this field may be changed as a result of network address translation before arriving at
 * the gateway.</dd>
 * <p>
 * <dt><u>Destination UDP Port</u></dt>
 * <p>
 * <dd>The source UDP port carried by the Relay Discovery message. Note: The value of this
 * field may be changed as a result of network address translation before arriving at the
 * gateway.</dd>
 * </dl>
 * </blockquote>
 * <h3>Message Format</h3> <blockquote>
 * 
 * <pre>
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |  V=0  |Type=2 |                   Reserved                    |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                        Discovery Nonce                        |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                                                               |
 *    ~                  Relay Address (IPv4 or IPv6)                 ~
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <dl>
 * <dt><u>Version (V)</u></dt>
 * <p>
 * <dd>The protocol version number for this message is 0.</dd>
 * <p>
 * <dt><u>Type</u></dt>
 * <p>
 * <dd>The type number for this message is 2.</dd>
 * <p>
 * <dt><u>Reserved</u></dt>
 * <p>
 * <dd>Reserved bits that MUST be set to zero by the relay and ignored by the gateway.</dd>
 * <p>
 * <dt><u>Discovery Nonce</u></dt>
 * <p>
 * <dd>A 32-bit value copied from the Discovery Nonce field (Section 5.1.1.4) contained in
 * the Relay Discovery message. The gateway uses this value to match a Relay Advertisement
 * to a Relay Discovery message.</dd>
 * <p>
 * <dt><u>Relay Address</u></dt>
 * <p>
 * <dd>The unicast IPv4 or IPv6 address of the relay. A gateway uses the length of the UDP
 * datagram containing the Relay Advertisement message to determine the address family;
 * i.e. length - 8 = 4 (IPv4) or 16 (IPv6).</dd>
 * </dl>
 * </blockquote>
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class AmtRelayAdvertisementMessage
                extends AmtMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * An AMT Relay Advertisement message parser/factory.
     */
    public static class Parser
                    implements AmtMessage.ParserType {

        @Override
        public AmtMessage parse(ByteBuffer buffer) throws ParseException {
            return new AmtRelayAdvertisementMessage(buffer);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    public static final byte MESSAGE_TYPE = 0x2;

    private static final int BASE_MESSAGE_LENGTH = 12;

    private static final int MIN_MESSAGE_LENGTH = 12;

    private static final int MAX_MESSAGE_LENGTH = 24;

    private static final IntegerField DiscoveryNonce = new IntegerField(4);

    private static final ByteArrayField IPv4RelayAddress = new ByteArrayField(8, 4);

    private static final ByteArrayField IPv6RelayAddress = new ByteArrayField(8, 16);

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return A parser that constructs an AmtRelayAdvertisementMessage from the
     *         contents of a ByteBuffer.
     */
    public static AmtRelayAdvertisementMessage.Parser constructParser() {
        return new AmtRelayAdvertisementMessage.Parser();
    }

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs an instance with the specified discovery nonce and relay address values.
     * 
     * @param discoveryNonce
     *            An integer nonce value.
     * @param relayAddress
     *            An InetAddress containing the relay unicast address.
     */
    public AmtRelayAdvertisementMessage(int discoveryNonce, byte[] relayAddress) {
        super(BASE_MESSAGE_LENGTH + relayAddress.length, MESSAGE_TYPE);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtRelayAdvertisementMessage.AmtRelayAdvertisementMessage",
                                        discoveryNonce,
                                        Logging.address(relayAddress)));
        }

        setDiscoveryNonce(discoveryNonce);
        setRelayAddress(relayAddress);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    /**
     * Constructs an instance from the contents of a ByteBuffer.
     * 
     * @param buffer
     *            A ByteBuffer containing a single AMT Relay Advertisement message.
     * @throws ParseException
     *             The buffer could not be parsed to produce a valid AMT Relay
     *             Advertisement
     *             message.
     */
    public AmtRelayAdvertisementMessage(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, buffer.limit() > MIN_MESSAGE_LENGTH ? MAX_MESSAGE_LENGTH : MIN_MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtRelayAdvertisementMessage.AmtRelayAdvertisementMessage", buffer));
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
        logger.log(level,this.log.msg(": discovery-nonce=" + getDiscoveryNonce()));
        logger.log(level,this.log.msg(": relay-address=" + Logging.address(getRelayAddress())));
    }

    @Override
    public Byte getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public int getTotalLength() {
        return getBufferInternal().limit();
    }

    /**
     * Gets the discovery nonce field value.
     * 
     * @return The integer value of the discovery nonce field.
     */
    public int getDiscoveryNonce() {
        return DiscoveryNonce.get(getBufferInternal());
    }

    /**
     * Sets the discovery nonce field to the specified value.
     * 
     * @param discoveryNonce
     *            An integer nonce value. Typically copied from the
     *            corresponding field in an {@link AmtRelayDiscoveryMessage}.
     */
    public void setDiscoveryNonce(int discoveryNonce) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtRelayAdvertisementMessage.setDiscoveryNonce", discoveryNonce));
        }

        DiscoveryNonce.set(getBufferInternal(), discoveryNonce);
    }

    /**
     * Gets the current value of the Relay Address field.
     * May be IPv4 (4 byte) or IPv6 (16 byte) address.
     */
    public byte[] getRelayAddress() {
        if (getBufferInternal().limit() > MIN_MESSAGE_LENGTH) {
            return IPv6RelayAddress.get(getBufferInternal());
        }
        else {
            return IPv4RelayAddress.get(getBufferInternal());
        }
    }

    /**
     * Sets the Relay Address field value.
     * See {@link #getRelayAddress()}.
     * 
     * @param relayAddress
     *            - an IPv4 address.
     */
    public void setRelayAddress(InetAddress relayAddress) {
        byte[] address = relayAddress.getAddress();
        setRelayAddress(address);
    }

    /**
     * Sets the Relay Address field value.
     * See {@link #getRelayAddress()}.
     * 
     * @param relayAddress
     *            - an IPv4 address.
     */
    public void setRelayAddress(byte[] relayAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtRelayAdvertisementMessage.setRelayAddress", Logging.address(relayAddress)));
        }

        // Precondition.checkAddress(relayAddress);
        if (relayAddress.length == 4) {
            IPv4RelayAddress.set(getBufferInternal(), relayAddress);
        }
        else {
            IPv6RelayAddress.set(getBufferInternal(), relayAddress);
        }
    }

}
