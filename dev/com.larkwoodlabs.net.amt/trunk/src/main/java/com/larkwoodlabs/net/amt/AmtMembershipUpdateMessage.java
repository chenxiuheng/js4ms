/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: AmtMembershipUpdateMessage.java (com.larkwoodlabs.net.amt)
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
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.igmp.IGMPMessage;
import com.larkwoodlabs.net.ip.igmp.IGMPv2LeaveMessage;
import com.larkwoodlabs.net.ip.igmp.IGMPv3ReportMessage;
import com.larkwoodlabs.net.ip.mld.MLDMessage;
import com.larkwoodlabs.net.ip.mld.MLDv1DoneMessage;
import com.larkwoodlabs.net.ip.mld.MLDv1ReportMessage;
import com.larkwoodlabs.util.buffer.fields.ByteArrayField;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.buffer.fields.IntegerField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Represents an AMT Membership Update message.
 * The following description is excerpted from <a
 * href="http://tools.ietf.org/html/draft-ietf-mboned-auto-multicast">Automatic Multicast
 * Tunneling (AMT)</a> specification.
 * 
 * <pre>
 * 5.1.5.  Membership Update
 * 
 *    A gateway sends a Membership Update message to a relay to report a
 *    change in group membership state, or to report the current group
 *    membership state in response to receiving a Membership Query message.
 *    The gateway encapsulates the IGMP or MLD message as an IP datagram
 *    within a Membership Update message and sends it to the relay, where
 *    it may (see below) be decapsulated and processed by the relay to
 *    update group membership and forwarding state.
 * 
 *    A gateway cannot send a Membership Update message until a receives a
 *    Membership Query from a relay because the gateway must copy the
 *    Request Nonce and Response MAC values carried by a Membership Query
 *    into any subsequent Membership Update messages it sends back to that
 *    relay.  These values are used by the relay to verify that the sender
 *    of the Membership Update message was the recipient of the Membership
 *    Query message from which these values were copied.
 * 
 *    The successful delivery of this message to the relay marks the start
 *    of the final stage in the three-way handshake.  This stage concludes
 *    when the relay successfully verifies that sender of the Message
 *    Update message was the recipient of a Membership Query message sent
 *    earlier.  At this point, the relay may proceed to process the
 *    encapsulated IGMP or MLD message to create or update group membership
 *    and forwarding state on behalf of the gateway.
 * 
 *    The UDP/IP datagram containing this message MUST carry a valid, non-
 *    zero UDP checksum and carry the following IP address and UDP port
 *    values:
 * 
 *    Source IP Address -  The IP address of the gateway interface on which
 *       the gateway will listen for Multicast Data messages from the
 *       relay.  The address must be the same address used to send the
 *       initial Request message or the message will be ignored.  Note: The
 *       value of this field may be changed as a result of network address
 *       translation before arriving at the relay.
 * 
 *    Source UDP Port -  The UDP port number on which the gateway will
 *       listen for Multicast Data messages from the relay.  This port must
 *       be the same port used to send the initial Request message or the
 *       message will be ignored.  Note: The value of this field may be
 *       changed as a result of network address translation before arriving
 *       at the relay.
 * 
 *    Destination IP Address -  The unicast IP address of the relay.
 * 
 *    Destination UDP Port -  The IANA-assigned AMT UDP port number.
 * 
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
 * 
 *                      Membership Update Message Format
 * 
 * 5.1.5.1.  Version (V)
 * 
 *    The protocol version number for this message is 0.
 * 
 * 5.1.5.2.  Type
 * 
 *    The type number for this message is 5.
 * 
 * 5.1.5.3.  Reserved
 * 
 *    Reserved bits that MUST be set to zero by the gateway and ignored by
 *    the relay.
 * 
 * 5.1.5.4.  Response MAC
 * 
 *    A 48-bit value copied from the Response MAC field (Section 5.1.4.6)
 *    in a Membership Query message.  Used by the relay to perform source
 *    authentication.
 * 
 * 5.1.5.5.  Request Nonce
 * 
 *    A 32-bit value copied from the Request Nonce field in a Request or
 *    Membership Query message.  Used by the relay to perform source
 *    authentication.
 * 
 * 5.1.5.6.  Encapsulated Group Membership Update Message
 * 
 *    An IP-encapsulated IGMP or MLD message produced by the host-mode IGMP
 *    or MLD protocol running on a gateway pseudo-interface.  This field
 *    will contain of one of the following IP datagrams:
 * 
 *       IPv4:IGMPv2 Membership Report
 * 
 *       IPv4:IGMPv2 Leave Group
 * 
 *       IPv4:IGMPv3 Membership Report
 * 
 *       IPv6:MLDv1 Multicast Listener Report
 * 
 *       IPv6:MLDv1 Multicast Listener Done
 * 
 *       IPv6:MLDv2 Multicast Listener Report
 * 
 *    The source address carried by the message should be set as described
 *    in Section 5.2.1.
 * </pre>
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
            logger.finer(Logging.entering(ObjectId,
                                          "AmtMembershipUpdateMessage.AmtMembershipUpdateMessage",
                                          Logging.mac(responseMac),
                                          requestNonce,
                                          updatePacket));
        }

        setResponseMac(responseMac);
        setRequestNonce(requestNonce);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
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
            logger.finer(Logging.entering(ObjectId, "AmtMembershipUpdateMessage.AmtMembershipUpdateMessage", buffer));
            logState(logger);
        }
    }

    @Override
    public void log(final Logger logger) {
        super.log(logger);
        logState(logger);
    }

    /**
     * Logs value of member variables declared or maintained by this class.
     * 
     * @param logger The logger to use when generating log messages.
     */
    private void logState(final Logger logger) {
        logger.info(ObjectId + " : response-MAC=" + Logging.mac(getResponseMac()));
        logger.info(ObjectId + " : request-nonce=" + getRequestNonce());
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
            logger.finer(Logging.entering(ObjectId, "AmtMembershipUpdateMessage.setResponseMac", Logging.mac(responseMac)));
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
            logger.finer(Logging.entering(ObjectId, "AmtMembershipUpdateMessage.setRequestNonce", requestNonce));
        }

        RequestNonce.set(getBufferInternal(), requestNonce);
    }

}
