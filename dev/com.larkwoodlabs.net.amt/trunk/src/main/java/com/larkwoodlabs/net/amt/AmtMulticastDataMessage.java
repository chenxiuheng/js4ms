/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: AmtMulticastDataMessage.java (com.larkwoodlabs.net.amt)
 * 
 * Copyright © 2010-2012 Cisco Systems, Inc.
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

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.ipv4.IPv4Packet;
import com.larkwoodlabs.net.ip.ipv6.IPv6Packet;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Represents an AMT Multicast Data message.
 * The following description is excerpted from the
 * <a href="http://tools.ietf.org/html/draft-ietf-mboned-auto-multicast">Automatic
 * Multicast Tunneling (AMT)</a> specification.
 * 
 * <pre>
 * 5.1.6.  Multicast Data
 * 
 *    A relay sends a Multicast Data message to deliver an IP multicast
 *    packet to a gateway.
 * 
 *    The checksum field in the UDP header of this message MAY contain a
 *    value of zero when sent over IPv4 but SHOULD, if possible, contain a
 *    valid, non-zero value when sent over IPv6 (See Section 4.2.2.3).
 * 
 *    The UDP/IP datagram containing this message MUST carry the following
 *    IP address and UDP port values:
 * 
 *    Source IP Address -  The unicast IP address of the relay.
 * 
 *    Source UDP Port -  The IANA-assigned AMT port number.
 * 
 *    Destination IP Address -  A tunnel endpoint IP address, i.e. the
 *       source IP address carried by the Membership Update message sent by
 *       a gateway to indicate an interest in receiving the multicast
 *       packet.  Note: The value of this field may be changed as a result
 *       of network address translation before arriving at the gateway.
 * 
 *    Destination UDP Port -  A tunnel endpoint UDP port, i.e. the source
 *       UDP port carried by the Membership Update message sent by a
 *       gateway to indicate an interest in receiving the multicast packet.
 *       Note: The value of this field may be changed as a result of
 *       network address translation before arriving at the gateway.
 * 
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |  V=0  |Type=6 |    Reserved   |                               |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                               +
 *    |                                                               |
 *    ~                     IP Multicast Packet                       ~
 *    |                                                               |
 *    +                - - - - - - - - - - - - - - - - - - - - - - - -+
 *    |               :               :               :               :
 *    +-+-+-+-+-+-+-+-+- - - - - - - - - - - - - - - - - - - - - - - -
 * 
 *                        Multicast Data Message Format
 * 
 * 5.1.6.1.  Version (V)
 * 
 *    The protocol version number for this message is 0.
 * 
 * 5.1.6.2.  Type
 * 
 *    The type number for this message is 6.
 * 
 * 5.1.6.3.  Reserved
 * 
 *    Bits that MUST be set to zero by the relay and ignored by the
 *    gateway.
 * 
 * 5.1.6.4.  IP Multicast Data
 * 
 *    A complete IPv4 or IPv6 Multicast datagram.
 * 
 * </pre>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class AmtMulticastDataMessage
                extends AmtEncapsulationMessage {

    /*-- Inner Classes ---------------------------------------------------*/

    /**
     * An AMT Multicast Data message parser/factory.
     */
    public static class Parser
                    extends AmtEncapsulationMessage.Parser {

        /**
         * 
         */
        public Parser() {
            this(DEFAULT_DATA_PACKET_PARSER);
        }

        /**
         * @param ipParser
         */
        public Parser(IPPacket.BufferParser ipParser) {
            super(ipParser);
        }

        @Override
        public AmtEncapsulationMessage constructMessage(final ByteBuffer buffer) throws ParseException {
            return new AmtMulticastDataMessage(buffer);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    public static final byte MESSAGE_TYPE = 0x6;

    private static final int BASE_MESSAGE_LENGTH = 2;

    @SuppressWarnings("unused")
    private static final ByteField Reserved = new ByteField(1);

    /**
     * Singleton instance of parser for IP packets carrying UDP protocol messages.
     */
    public static final IPPacket.BufferParser DEFAULT_DATA_PACKET_PARSER = getDataPacketParser();

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return A parser that constructs an IPPacket object from the contents
     *         of a ByteBuffer.
     */
    public static IPPacket.BufferParser getDataPacketParser() {
        // UdpPacket.Parser udpParser = new UdpPacket.Parser();
        // IPMessage.Parser ipMessageParser = new IPMessage.Parser();
        // ipMessageParser.add(udpParser);
        IPv4Packet.Parser ipv4Parser = new IPv4Packet.Parser();
        // TODO header options?
        // ipv4Parser.setProtocolParser(ipMessageParser);
        IPv6Packet.Parser ipv6Parser = IPv6Packet.getIPv6MessageParser(); // Adds
                                                                          // extension
                                                                          // headers (or
                                                                          // skip?)
        // ipv6Parser.setProtocolParser(ipMessageParser);
        IPPacket.BufferParser ipParser = new IPPacket.BufferParser();
        ipParser.add(ipv4Parser);
        ipParser.add(ipv6Parser);
        return ipParser;
    }

    /**
     * @return A parser that construct an AmtMulticastDataMessage object from the
     *         contents of a ByteBuffer.
     */
    public static AmtMulticastDataMessage.Parser constructParser() {
        AmtMulticastDataMessage.Parser parser = new AmtMulticastDataMessage.Parser();
        parser.setIPPacketParser(getDataPacketParser());
        return parser;
    }

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs an instance that encapsulates the specified IP packet.
     * 
     * @param dataPacket
     */
    public AmtMulticastDataMessage(final IPPacket dataPacket) {
        super(BASE_MESSAGE_LENGTH, MESSAGE_TYPE, dataPacket);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMulticastDataMessage.AmtMulticastDataMessage", dataPacket));
        }
    }

    /**
     * Constructs an instance from the contents of the specified ByteBuffer.
     * 
     * @param buffer
     *            The ByteBuffer containing the message.
     * @throws ParseException
     *             The buffer could not be parsed to produce a value AMT Multicast Data
     *             message.
     */
    public AmtMulticastDataMessage(final ByteBuffer buffer) throws ParseException {
        super(buffer, BASE_MESSAGE_LENGTH);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMulticastDataMessage.AmtMulticastDataMessage", buffer));
        }
    }

    @Override
    public final Byte getType() {
        return MESSAGE_TYPE;
    }

}
