/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: AmtRelayDiscoveryMessage.java (com.larkwoodlabs.net.amt)
 * 
 * Copyright � 2010-2012 Cisco Systems, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.larkwoodlabs.net.amt;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.fields.IntegerField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Represents an AMT Relay Discovery message.
 * The following description is excerpted from the <a
 * href="http://tools.ietf.org/html/draft-ietf-mboned-auto-multicast">Automatic Multicast
 * Tunneling (AMT)</a> specification.
 * <p>
 * A Relay Discovery message is used to solicit a response from a relay in the form of a
 * Relay Advertisement message.
 * <p>
 * The UDP/IP datagram containing this message MUST carry a valid, non- zero UDP checksum
 * and carry the following IP address and UDP port values: <blockquote>
 * <dl>
 * <dt><u>Source IP Address</u></dt>
 * <p>
 * <dd>The IP address of the gateway interface on which the gateway will listen for a
 * relay response. Note: The value of this field may be changed as a result of network
 * address translation before arriving at the relay.</dd>
 * <p>
 * <dt><u>Source UDP Port</u></dt>
 * <p>
 * <dd>The UDP port number on which the gateway will listen for a relay response. Note:
 * The value of this field may be changed as a result of network address translation
 * before arriving at the relay.</dd>
 * <p>
 * <dt><u>Destination IP Address</u></dt>
 * <p>
 * <dd>An anycast or unicast IP address, i.e. the Relay Discovery Address advertised by a
 * relay.</dd>
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
 *    |  V=0  |Type=1 |     Reserved                                  |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                        Discovery Nonce                        |
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
 * <dd>The type number for this message is 1.</dd>
 * <p>
 * <dt><u>Reserved</u></dt>
 * <p>
 * <dd>Reserved bits that MUST be set to zero by the gateway and ignored by the relay.</dd>
 * <p>
 * <dt><u>Discovery Nonce</u></dt>
 * <p>
 * <dd>A 32-bit random value generated by the gateway and echoed by the relay in a Relay
 * Advertisement message. This value is used by the gateway to correlate Relay
 * Advertisement messages with Relay Discovery messages. Discovery nonce generation is
 * described in Section 5.2.3.4.5.</dd>
 * </dl>
 * </blockquote>
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class AmtRelayDiscoveryMessage
                extends AmtMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * An AMT Relay Discovery message parser/factory.
     */
    public static class Parser
                    implements AmtMessage.ParserType {

        @Override
        public AmtMessage parse(ByteBuffer buffer) throws ParseException {
            return new AmtRelayDiscoveryMessage(buffer);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    public static final byte MESSAGE_TYPE = 0x1;

    private static final int BASE_MESSAGE_LENGTH = 8;

    private static final IntegerField RequestNonce = new IntegerField(4);

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return
     */
    public static AmtRelayDiscoveryMessage.Parser constructParser() {
        return new AmtRelayDiscoveryMessage.Parser();
    }

    /**
     * @return
     */
    public static int createNonce() {
        double rand = Math.random();
        long range = 0x7FFFFFFF;
        double result = rand * range;
        return (int) result;
    }

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs an instance using a randomly generated discovery nonce value.
     */
    public AmtRelayDiscoveryMessage() {
        this(createNonce());

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(Logging.entering(ObjectId, "AmtRelayDiscoveryMessage.AmtRelayDiscoveryMessage"));
        }
    }

    /**
     * Constructs an instance using the specified discovery nonce value.
     * 
     * @param discoveryNonce
     *            An integer discovery nonce value.
     */
    public AmtRelayDiscoveryMessage(int discoveryNonce) {
        super(BASE_MESSAGE_LENGTH, MESSAGE_TYPE);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtRelayDiscoveryMessage.AmtRelayDiscoveryMessage", discoveryNonce));
        }

        setDiscoveryNonce(discoveryNonce);

        if (logger.isLoggable(Level.FINE)) {
            logState(logger);
        }
    }

    /**
     * Constructs an instance from the contents of a ByteBuffer.
     * 
     * @param buffer
     *            A ByteBuffer containing a single AMT Relay Discovery message.
     * @throws ParseException
     *             The buffer could not be parsed to produce a valid AMT Relay Discovery
     *             message.
     */
    public AmtRelayDiscoveryMessage(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtRelayDiscoveryMessage.AmtRelayDiscoveryMessage", buffer));
            logState(logger);
        }
    }

    @Override
    public void log(Logger logger) {
        super.log(logger);
        logState(logger);
    }

    /**
     * @param logger
     */
    private void logState(Logger logger) {
        logger.info(ObjectId + " : discovery-nonce=" + getDiscoveryNonce());
    }

    @Override
    public Byte getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public int getTotalLength() {
        return BASE_MESSAGE_LENGTH;
    }

    /**
     * Gets the discovery nonce field value.
     * 
     * @return The integer value of the discovery nonce field.
     */
    public int getDiscoveryNonce() {
        return RequestNonce.get(getBufferInternal());
    }

    /**
     * Sets the discovery nonce field to the specified value.
     * 
     * @param discoveryNonce
     *            A random integer nonce value.
     */
    public void setDiscoveryNonce(int discoveryNonce) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtRelayDiscoveryMessage.setDiscoveryNonce", discoveryNonce));
        }

        RequestNonce.set(getBufferInternal(), discoveryNonce);
    }

}
