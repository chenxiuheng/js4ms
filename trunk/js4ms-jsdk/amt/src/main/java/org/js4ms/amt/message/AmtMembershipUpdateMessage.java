package org.js4ms.amt.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * AmtMembershipUpdateMessage.java [org.js4ms.jsdk:amt]
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
import org.js4ms.common.util.buffer.field.ByteArrayField;
import org.js4ms.common.util.buffer.field.ByteField;
import org.js4ms.common.util.buffer.field.IntegerField;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.ip.IPMessage;
import org.js4ms.ip.IPPacket;
import org.js4ms.ip.protocol.igmp.IGMPMessage;
import org.js4ms.ip.protocol.igmp.IGMPv2LeaveMessage;
import org.js4ms.ip.protocol.igmp.IGMPv3ReportMessage;
import org.js4ms.ip.protocol.mld.MLDMessage;
import org.js4ms.ip.protocol.mld.MLDv1DoneMessage;
import org.js4ms.ip.protocol.mld.MLDv1ReportMessage;



/**
 * Represents an AMT Membership Update message.
 * The following description is excerpted from <a
 * href="http://tools.ietf.org/html/draft-ietf-mboned-auto-multicast">Automatic Multicast
 * Tunneling (AMT)</a> specification.
 * <p>
 * A gateway sends a Membership Update message to a relay to report a change in group
 * membership state, or to report the current group membership state in response to
 * receiving a Membership Query message. The gateway encapsulates the IGMP or MLD message
 * as an IP datagram within a Membership Update message and sends it to the relay, where
 * it may (see below) be decapsulated and processed by the relay to update group
 * membership and forwarding state.
 * <p>
 * A gateway cannot send a Membership Update message until a receives a Membership Query
 * from a relay because the gateway must copy the Request Nonce and Response MAC values
 * carried by a Membership Query into any subsequent Membership Update messages it sends
 * back to that relay. These values are used by the relay to verify that the sender of the
 * Membership Update message was the recipient of the Membership Query message from which
 * these values were copied.
 * <p>
 * The successful delivery of this message to the relay marks the start of the final stage
 * in the three-way handshake. This stage concludes when the relay successfully verifies
 * that sender of the Message Update message was the recipient of a Membership Query
 * message sent earlier. At this point, the relay may proceed to process the encapsulated
 * IGMP or MLD message to create or update group membership and forwarding state on behalf
 * of the gateway.
 * <p>
 * The UDP/IP datagram containing this message MUST carry a valid, non- zero UDP checksum
 * and carry the following IP address and UDP port values: <blockquote>
 * <dl>
 * <dt><u>Source IP Address</u></dt>
 * <p>
 * <dd>The IP address of the gateway interface on which the gateway will listen for
 * Multicast Data messages from the relay. The address must be the same address used to
 * send the initial Request message or the message will be ignored. Note: The value of
 * this field may be changed as a result of network address translation before arriving at
 * the relay.</dd>
 * <p>
 * <dt><u>Source UDP Port</u></dt>
 * <p>
 * <dd>The UDP port number on which the gateway will listen for Multicast Data messages
 * from the relay. This port must be the same port used to send the initial Request
 * message or the message will be ignored. Note: The value of this field may be changed as
 * a result of network address translation before arriving at the relay.</dd>
 * <p>
 * <dt><u>Destination IP Address</u></dt>
 * <p>
 * <dd>The unicast IP address of the relay.</dd>
 * <p>
 * <dt><u>Destination UDP Port</u></dt>
 * <p>
 * <dd>The IANA-assigned AMT UDP port number.</dd>
 * </dl>
 * </blockquote>
 * <h3>Message Format</h3> <blockquote>
 * 
 * <pre>
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |  V=0  |Type=5 |  Reserved     |        Response MAC           |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                               +
 *    |                                                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                         Request Nonce                         |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                                                               |
 *    |         Encapsulated Group Membership Update Message          |
 *    ~           IPv4:IGMP(Membership Report|Leave Group)            ~
 *    |            IPv6:MLD(Listener Report|Listener Done)            |
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
 * <dd>The type number for this message is 5.</dd>
 * <p>
 * <dt><u>Reserved</u></dt>
 * <p>
 * <dd>Reserved bits that MUST be set to zero by the gateway and ignored by the relay.</dd>
 * <p>
 * <dt><u>Response MAC</u></dt>
 * <p>
 * <dd>A 48-bit value copied from the Response MAC field (Section 5.1.4.6) in a Membership
 * Query message. Used by the relay to perform source authentication.</dd>
 * <p>
 * <dt><u>Request Nonce</u></dt>
 * <p>
 * <dd>A 32-bit value copied from the Request Nonce field in a Request or Membership Query
 * message. Used by the relay to perform source authentication.</dd>
 * <p>
 * <dt><u>Encapsulated Group Membership Update Message</u></dt>
 * <p>
 * <dd>An IP-encapsulated IGMP or MLD message produced by the host-mode IGMP or MLD
 * protocol running on a gateway pseudo-interface. This field will contain of one of the
 * following IP datagrams:
 * <ul>
 * <li>IPv4:IGMPv2 Membership Report</li>
 * <li>IPv4:IGMPv2 Leave Group</li>
 * <li>IPv4:IGMPv3 Membership Report</li>
 * <li>IPv6:MLDv1 Multicast Listener Report</li>
 * <li>IPv6:MLDv1 Multicast Listener Done</li>
 * <li>IPv6:MLDv2 Multicast Listener Report</li>
 * </ul>
 * </dd>
 * </dl>
 * </blockquote>
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class AmtMembershipUpdateMessage
                extends AmtEncapsulationMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * An AMT Membership Update message parser/factory.
     */
    public static class Parser
                    extends AmtEncapsulationMessage.Parser {

        /**
         * 
         */
        public Parser() {
            this(DEFAULT_UPDATE_PACKET_PARSER);
        }

        /**
         * @param ipParser
         */
        public Parser(IPPacket.BufferParser ipParser) {
            super(ipParser);
        }

        @Override
        public AmtEncapsulationMessage constructMessage(ByteBuffer buffer) throws ParseException {
            return new AmtMembershipUpdateMessage(buffer);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    public static final byte MESSAGE_TYPE = 0x5;

    private static final int BASE_MESSAGE_LENGTH = 12;

    /**
     * Singleton instance of parser for IP packets carrying IGMP or MLD membership update
     * messages.
     */
    public static final IPPacket.BufferParser DEFAULT_UPDATE_PACKET_PARSER = getUpdatePacketParser();

    @SuppressWarnings("unused")
    private static final ByteField Reserved = new ByteField(1);

    private static final ByteArrayField ResponseMac = new ByteArrayField(2, 6);

    private static final IntegerField RequestNonce = new IntegerField(8);

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return A parser that constructs an IPPacket from a buffer containing
     *         the encapsulated packet from an AMT Membership Update message.
     */
    public static IPPacket.BufferParser getUpdatePacketParser() {
        IGMPMessage.Parser igmpMessageParser = new IGMPMessage.Parser();
        igmpMessageParser.add(new IGMPv3ReportMessage.Parser());
        igmpMessageParser.add(new IGMPv2LeaveMessage.Parser());
        MLDMessage.Parser mldMessageParser = new MLDMessage.Parser();
        mldMessageParser.add(new MLDv1ReportMessage.Parser());
        mldMessageParser.add(new MLDv1DoneMessage.Parser());
        IPMessage.Parser ipv4MessageParser = new IPMessage.Parser();
        ipv4MessageParser.add(igmpMessageParser);
        IPMessage.Parser ipv6MessageParser = new IPMessage.Parser();
        ipv6MessageParser.add(mldMessageParser);
        IPPacket.BufferParser ipParser = new IPPacket.BufferParser();
        return ipParser;
    }

    /**
     * @return A parser that constructs an AmtMembershipUpdateMessage from the
     *         contents of a ByteBuffer.
     */
    public static AmtMembershipUpdateMessage.Parser constructParser() {
        AmtMembershipUpdateMessage.Parser parser = new AmtMembershipUpdateMessage.Parser();
        parser.setIPPacketParser(getUpdatePacketParser());
        return parser;
    }

    /*-- Member Functions---------------------------------------------------*/

    /**
     * Constructs an instance from the specified response MAC, request Nonce and
     * IP packet.
     * 
     * @param responseMac
     *            A 6-byte array containing a 48-bit MAC.
     * @param requestNonce
     *            An integer nonce value.
     * @param updatePacket
     *            The IP packet containing an IGMP or MLD membership report
     *            that will be encapsulated in the AMT Membership Update message.
     */
    public AmtMembershipUpdateMessage(final byte[] responseMac, final int requestNonce, final IPPacket updatePacket) {
        super(BASE_MESSAGE_LENGTH, MESSAGE_TYPE, updatePacket);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry(
                                        "AmtMembershipUpdateMessage.AmtMembershipUpdateMessage",
                                        Logging.mac(responseMac),
                                        requestNonce,
                                        updatePacket));
        }

        setResponseMac(responseMac);
        setRequestNonce(requestNonce);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    /**
     * Constructs an instance from the contents of a ByteBuffer.
     * 
     * @param buffer
     *            A buffer containing a single AMT Membership Update message.
     * @throws ParseException
     *             The buffer could not be parsed to produce a valid AMT Membership Update
     *             messag.
     */
    public AmtMembershipUpdateMessage(final ByteBuffer buffer) throws ParseException {
        super(buffer, BASE_MESSAGE_LENGTH);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMembershipUpdateMessage.AmtMembershipUpdateMessage", buffer));
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
     *            The logger to use when generating log messages.
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": response-MAC=" + Logging.mac(getResponseMac())));
        logger.log(level,this.log.msg(": request-nonce=" + getRequestNonce()));
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
            logger.finer(this.log.entry("AmtMembershipUpdateMessage.setResponseMac", Logging.mac(responseMac)));
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
     *            corresponding field in an {@link AmtMembershipQueryMessage}.
     */
    public void setRequestNonce(final int requestNonce) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMembershipUpdateMessage.setRequestNonce", requestNonce));
        }

        RequestNonce.set(getBufferInternal(), requestNonce);
    }

}
