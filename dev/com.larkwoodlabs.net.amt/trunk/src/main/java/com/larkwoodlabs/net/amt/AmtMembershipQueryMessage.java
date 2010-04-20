/*
 * Copyright � 2009-2010 Larkwood Labs Software.
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
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.igmp.IGMPMessage;
import com.larkwoodlabs.net.ip.mld.MLDMessage;
import com.larkwoodlabs.util.buffer.fields.ByteArrayField;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.buffer.fields.IntegerField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An AMT membership query message.
 * <pre>
 * 6.4. AMT Membership Query
 * 
 *    An AMT Membership Query packet is sent from the respondent back to
 *    the originator to solicit an AMT Membership Update while confirming
 *    the source of the original request.  It contains a relay Message
 *    Authentication Code (MAC) that is a cryptographic hash of a private
 *    secret, the originators address, and the request nonce.
 * 
 *    It is sent from the destination address received in the Request to
 *    the source address received in the Request which is the same address
 *    used in the Relay Advertisement.
 * 
 *    The UDP source port is the IANA reserved AMT port number and the UDP
 *    destination port is the source port received in the Request message.
 *    The UDP checksum MUST be valid in AMT control messages.
 * 
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |     Type=0x4  |    Reserved   |         Response MAC          |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |            Response MAC (continued)                           |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |            Request Nonce                                      |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |            IGMP Membership Query or MLD Listener Query        |
 *    |            (including IP Header)                              |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |            ...                                                |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 * 6.4.1. Type
 * 
 *    The type of the message.
 * 
 * 6.4.2. Reserved
 * 
 *    A 8-bit reserved field.  Sent as 0, ignored on receipt.
 * 
 * 6.4.3. Response MAC
 * 
 *    A 48-bit hash generated by the respondent and sent to the originator
 *    for inclusion in the AMT Membership Update.  The algorithm used for
 *    this is chosen by the respondent but an algorithm such as HMAC-MD5-48
 *    [RFC2104] SHOULD be used at a minimum.
 * 
 * 6.4.4. Request Nonce
 * 
 *    A 32-bit identifier used to distinguish this request echoed back to
 *    the originator.
 * 
 * 6.4.5. IGMP/MLD Query (including IP Header)
 * 
 *    The message contains either an IGMP Query or an MLD Multicast
 *    Listener Query.  The IGMP or MLD version sent should default to
 *    IGMPv3 or MLDv2 unless explicitly configured to use IGMPv2 or MLDv1.
 *    The IGMP/MLD Query includes a full IP Header.  The IP source address
 *    of the query would match the anycast address on the pseudo interface.
 *    The TTL of the outer header should be sufficient to reach the tunnel
 *    endpoint and not mimic the inner header TTL which is typically 1 for
 *    IGMP/MLD messages.
 * </pre>
 * 
 * @author Gregory Bumgardner
 * 
 */
final class AmtMembershipQueryMessage extends AmtEncapsulationMessage {

    /*-- Inner Classes ------------------------------------------------------*/
    
    public static class Parser extends AmtEncapsulationMessage.Parser {

        public Parser() {
            this(DEFAULT_QUERY_PACKET_PARSER);
        }

        public Parser(IPPacket.Parser ipParser) {
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
    public static final int BASE_MESSAGE_LENGTH = 12;

    /**
     * Singleton instance of parser for IP packets carrying IGMP or MLD query messages. 
     */
    public static final IPPacket.Parser DEFAULT_QUERY_PACKET_PARSER = getQueryPacketParser();

    public static final ByteField       Reserved = new ByteField(1);
    public static final ByteArrayField  ResponseMac = new ByteArrayField(2,6);
    public static final IntegerField    RequestNonce = new IntegerField(8);
    
    /*-- Static Functions ---------------------------------------------------*/
    
    public static IPPacket.Parser getQueryPacketParser() {
        IPPacket.Parser parser = new IPPacket.Parser();
        parser.add(IGMPMessage.getIPv4PacketParser());
        parser.add(MLDMessage.getIPv6PacketParser());
        return parser;
    }

    public static AmtMembershipQueryMessage.Parser constructParser() {
        AmtMembershipQueryMessage.Parser parser = new AmtMembershipQueryMessage.Parser();
        parser.setIPPacketParser(getQueryPacketParser());
        return parser;
    }

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param responseMac
     * @param requestNonce
     * @param queryPacket
     */
    public AmtMembershipQueryMessage(final byte[] responseMac, final int requestNonce, final IPPacket queryPacket) {
        super(BASE_MESSAGE_LENGTH, MESSAGE_TYPE, queryPacket);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                        "AmtMembershipQueryMessage.AmtMembershipQueryMessage",
                                        Logging.mac(responseMac),
                                        requestNonce,
                                        queryPacket));
        }
        
        Reserved.set(getBufferInternal(), (byte)0);
        setResponseMac(responseMac);
        setRequestNonce(requestNonce);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param segment
     * @throws ParseException
     */
    public AmtMembershipQueryMessage(final ByteBuffer segment) throws ParseException {
        super(segment, BASE_MESSAGE_LENGTH);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMembershipQueryMessage.AmtMembershipQueryMessage", segment));
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
     * @param logger
     */
    private void logState(final Logger logger) {
        logger.info(ObjectId + " : response-MAC="+Logging.mac(getResponseMac()));
        logger.info(ObjectId + " : request-nonce="+getRequestNonce());
    }

    @Override
    public Byte getType() {
        return MESSAGE_TYPE;
    }

    /**
     * 
     * @return
     */
    public byte[] getResponseMac() {
        return ResponseMac.get(getBufferInternal());
    }

    /**
     * 
     * @param responseMac
     */
    public void setResponseMac(final byte[] responseMac) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMembershipQueryMessage.setResponseMac", Logging.mac(responseMac)));
        }
        
        ResponseMac.set(getBufferInternal(),responseMac);
    }

    /**
     * 
     * @return
     */
    public int getRequestNonce() {
        return RequestNonce.get(getBufferInternal());
    }

    /**
     * 
     * @param requestNonce
     */
    public void setRequestNonce(final int requestNonce) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMembershipQueryMessage.setRequestNonce", requestNonce));
        }
        
        RequestNonce.set(getBufferInternal(),requestNonce);
    }

}