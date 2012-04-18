/*
 * Copyright © 2009-2010 Larkwood Labs Software.
 *
 * Licensed under the Larkwood Labs Software Source Code License, Version 1.0.
 * You may not use this file except in compliance with this License.
 *
 * You may view the Source Code License at
 * http://www.larkwoodlabs.com/source-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the license.
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
 * Class representing an AMT IP Multicast data message.
 * 
 * The AMT Data message is a UDP packet encapsulating the IP Multicast
 * data requested by the originator based on a previous AMT Membership
 * Update message.<p>
 * 
 * It is sent from the unicast destination address of the Membership
 * update to the source address of the Membership Update.<p>
 * 
 * The UDP source and destination port numbers should be the same ones
 * sent in the original Query.  The UDP checksum SHOULD be 0 in the AMT
 * IP Multicast Data message.<p>
 * 
 * From the draft specification
 * <a href="http://tools.ietf.org/html/draft-ietf-mboned-auto-multicast-09#section-6.6">[draft-ietf-mboned-auto-multicast-09][6.1]</a><p>
 * The payload of the UDP packet contains the following fields.
 * <pre>
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |     Type=0x6  |    Reserved   |     IP Multicast Data ...     |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |            ...                                                |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * <dl>
 * <dt>Type
 * <dd>The type of the message.<br/>
 *     See {@link AmtMessage#getType() getType()},
 *         {@link AmtMessage#setType(byte) setType()}.
 * <dt>Reserved 
 * <dd>An 8-bit reserved field.  Sent as 0, ignored on receipt.<br/>
 *     See {@link #Reserved}.
 * <dt>IP Multicast Data
 * <dd>The original IP Multicast data packet that is being replicated by the
 *     relay to the gateways including the original IP header.<br/>
 *     See {@link AmtEncapsulationMessage#getPacket() getPacket()},
 *         {@link AmtEncapsulationMessage#setPacket(IPPacket) setPacket(IPPacket)},
 *         {@link AmtEncapsulationMessage#parsePacket(IPPacket.BufferParser) parsePacket(IPPacket.Parser)}.
 * </dl><p>
 * @author Gregory Bumgardner
 */
public final class AmtMulticastDataMessage extends AmtEncapsulationMessage {

    /*-- Inner Classes ---------------------------------------------------*/
    
    public static class Parser extends AmtEncapsulationMessage.Parser {

        public Parser() {
            this(DEFAULT_DATA_PACKET_PARSER);
        }

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
    public static final int BASE_MESSAGE_LENGTH = 2;
    
    public static final ByteField Reserved = new ByteField(1); 

    /**
     * Singleton instance of parser for IP packets carrying UDP protocol messages. 
     */
    public static final IPPacket.BufferParser DEFAULT_DATA_PACKET_PARSER = getDataPacketParser();


    /*-- Static Functions ---------------------------------------------------*/
    
    public static IPPacket.BufferParser getDataPacketParser() {
        //UdpPacket.Parser udpParser = new UdpPacket.Parser();
        //IPMessage.Parser ipMessageParser = new IPMessage.Parser();
        //ipMessageParser.add(udpParser);
        IPv4Packet.Parser ipv4Parser = new IPv4Packet.Parser();
        // TODO header options?
        //ipv4Parser.setProtocolParser(ipMessageParser);
        IPv6Packet.Parser ipv6Parser = IPv6Packet.getIPv6MessageParser(); // Adds extension headers (or skip?)
        //ipv6Parser.setProtocolParser(ipMessageParser);
        IPPacket.BufferParser ipParser = new IPPacket.BufferParser();
        ipParser.add(ipv4Parser);
        ipParser.add(ipv6Parser);
        return ipParser;
    }

    public static AmtMulticastDataMessage.Parser constructParser() {
        AmtMulticastDataMessage.Parser parser = new AmtMulticastDataMessage.Parser();
        parser.setIPPacketParser(getDataPacketParser());
        return parser;
    }

    
    /*-- Member Functions ---------------------------------------------------*/

    /**
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
     * 
     * @param segment
     * @throws ParseException
     */
    public AmtMulticastDataMessage(final ByteBuffer segment) throws ParseException {
        super(segment, BASE_MESSAGE_LENGTH);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMulticastDataMessage.AmtMulticastDataMessage", segment));
        }
    }

    @Override
    public final Byte getType() {
        return MESSAGE_TYPE;
    }

}
