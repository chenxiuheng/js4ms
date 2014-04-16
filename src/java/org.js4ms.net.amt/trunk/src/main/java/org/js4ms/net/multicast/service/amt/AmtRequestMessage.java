/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: AmtRequestMessage.java (org.js4ms.net.amt)
 * 
 * Copyright (C) 2010-2012 Cisco Systems, Inc.
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

package org.js4ms.net.multicast.service.amt;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.util.buffer.field.BooleanField;
import org.js4ms.util.buffer.field.IntegerField;



/**
 * Represents an AMT Request message.
 * The following description is excerpted from the
 * <a href="http://tools.ietf.org/html/draft-ietf-mboned-auto-multicast">Automatic
 * Multicast Tunneling (AMT)</a> specification.
 * <p>
 * A gateway sends a Request message to a relay to solicit a Membership Query response.
 * <p>
 * The successful delivery of this message marks the start of the first stage in the
 * three-way handshake used to create or update state within a relay.
 * <p>
 * The UDP/IP datagram containing this message MUST carry a valid, non- zero UDP checksum
 * and carry the following IP address and UDP port values: <blockquote>
 * <dl>
 * <dt><u>Source IP Address</u></dt>
 * <p>
 * <dd>The IP address of the gateway interface on which the gateway will listen for a
 * response from the relay. Note: The value of this field may be changed as a result of
 * network address translation before arriving at the relay.</dd>
 * <p>
 * <dt><u>Source UDP Port</u></dt>
 * <p>
 * <dd>The UDP port number on which the gateway will listen for a response from the relay.
 * Note: The value of this field may be changed as a result of network address translation
 * before arriving at the relay.</dd>
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
 *    |  V=0  |Type=3 |   Reserved  |P|            Reserved           |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                         Request Nonce                         |
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
 * <dd>The type number for this message is 3.</dd>
 * <p>
 * <dt><u>Reserved</u></dt>
 * <p>
 * <dd>Reserved bits that MUST be set to zero by the gateway and ignored by the relay.</dd>
 * <p>
 * <dt><u>P Flag</u></dt>
 * <p>
 * <dd>The "P" flag is set to indicate which group membership protocol the gateway wishes
 * the relay to use in the Membership Query response: Value Meaning 0 The relay MUST
 * respond with a Membership Query message that contains an IPv4 packet carrying an IGMPv3
 * general query message. 1 The relay MUST respond with a Membership Query message that
 * contains an IPv6 packet carrying an MLDv2 general query message.</dd>
 * <p>
 * <dt><u>Request Nonce</u></dt>
 * <p>
 * <dd>A 32-bit random value generated by the gateway and echoed by the relay in a
 * Membership Query message. This value is used by the relay to compute the Response MAC
 * value and is used by the gateway to correlate Membership Query messages with Request
 * messages. Request nonce generation is described in Section 5.2.3.5.6.</dd>
 * </dl>
 * </blockquote>
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class AmtRequestMessage
                extends AmtMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * An AMT Request message parser/factory.
     */
    public static class Parser
                    implements AmtMessage.ParserType {

        @Override
        public AmtMessage parse(ByteBuffer buffer) throws ParseException {
            return new AmtRequestMessage(buffer);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    public static final byte MESSAGE_TYPE = 0x3;

    public static final int MESSAGE_LENGTH = 8;

    private static final BooleanField ProtocolFlag = new BooleanField(1, 0);

    private static final IntegerField RequestNonce = new IntegerField(4);

    public static int nextRequestNonce = 1;

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return A parser that constructs an AmtRequestMessage object from the
     *         contents of a ByteBuffer.
     */
    public static AmtRequestMessage.Parser constructParser() {
        return new AmtRequestMessage.Parser();
    }

    /**
     * @return The next value in an static integer sequence.
     */
    public static synchronized int getNextRequestNonce() {
        return nextRequestNonce++;
    }

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs an instance using a sequential nonce value.
     */
    public AmtRequestMessage(boolean requestMLD) {
        this(getNextRequestNonce(), requestMLD);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtRequestMessage.AmtRequestMessage"));
        }
    }

    /**
     * Constructs and instance using the specified nonce value.
     * 
     * @param requestNonce
     *            A random integer value.
     */
    public AmtRequestMessage(int requestNonce, boolean requestMLD) {
        super(MESSAGE_LENGTH, MESSAGE_TYPE);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtRequestMessage.AmtRequestMessage", requestNonce));
        }

        setRequestNonce(requestNonce);
        this.setProtocolFlag(requestMLD);
        if (logger.isLoggable(Level.FINER)) {
            logState(logger,Level.FINER);
        }
    }

    /**
     * Constructs an instance from the contents of a ByteBuffer.
     * 
     * @param buffer
     *            A buffer containing a single AMT Request message.
     * @throws ParseException
     *             The buffer could not be parsed to produce a valid AMT Request message.
     */
    public AmtRequestMessage(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtRequestMessage.AmtRequestMessage", buffer));
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
     *            The logger to use when generating log messages.
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg("request-nonce=" + getRequestNonce()));
    }

    @Override
    public Byte getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public int getTotalLength() {
        return MESSAGE_LENGTH;
    }

    /**
     * Gets the protocol (P) flag field value.
     * 
     * @return The boolean value of the protocol (P) flag field.
     */
    public boolean getProtocolFlag() {
        return ProtocolFlag.get(getBufferInternal());
    }

    /**
     * Sets the protocol (P) flag field to the specified value.
     * 
     * @param requestMLD
     *            A boolean value where <code>false</code> indicates which group
     *            membership protocol the gateway wishes to use for this request
     *            where <code>false</code> is IGMPv3 and <code>true</code> is MLDv2.
     */
    public void setProtocolFlag(final boolean requestMLD) {
        ProtocolFlag.set(getBufferInternal(), requestMLD);
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
     *            An opaque integer nonce value..
     */
    public void setRequestNonce(int requestNonce) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtRequestMessage.setRequestNonce", requestNonce));
        }

        RequestNonce.set(getBufferInternal(), requestNonce);
    }

}
