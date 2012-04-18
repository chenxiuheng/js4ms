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
 * An AMT Membership Update Message.
 * <pre>
 * 6.5. AMT Membership Update
 * 
 *    An AMT Membership Update is sent to report a membership after a valid
 *    Response MAC has been received.  It contains the original IGMP/MLD
 *    Membership/Listener Report or Leave/Done received over the AMT
 *    pseudo-interface including the original IP header.  It echoes the
 *    Response MAC received in the AMT Membership Query so the respondent
 *    can verify return routability to the originator.
 * 
 *    It is sent from the destination address received in the Query to the
 *    source address received in the Query which should both be the same as
 *    the original Request.
 * 
 *    The UDP source and destination port numbers should be the same ones
 *    sent in the original Request.
 * 
 *    The relay is not required to use the IP source address of the IGMP
 *    Membership Report for any particular purpose.
 * 
 *    The same Request Nonce and Response MAC can be used across multiple
 *    AMT Membership Update messages without having to send individual AMT
 *    Membership Query messages.
 * 
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |     Type=0x5  |    Reserved   |         Response MAC          |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |            Response MAC (continued)                           |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |            Request Nonce                                      |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |            IGMP or MLD Message (including IP header)          |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |            ...                                                |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 * 6.5.1. Type
 * 
 *    The type of the message.
 * 
 * 6.5.2. Reserved
 * 
 *    A 8-bit reserved field.  Sent as 0, ignored on receipt.
 * 
 * 6.5.3. Response MAC
 * 
 *    The 48-bit MAC received in the Membership Query and echoed back in
 *    the Membership Update.
 * 
 * 6.5.4. Request Nonce
 * 
 *    A 32-bit identifier used to distinguish this request.
 * 
 * 6.5.5. IGMP/MLD Message (including IP Header)
 * 
 *    The message contains either an IGMP Membership Report, an IGMP
 *    Membership Leave, an MLD Multicast Listener Report, or an MLD
 *    Listener Done.  The IGMP or MLD version sent should be in response
 *    the version of the query received in the AMT Membership Query.  The
 *    IGMP/MLD Message includes a full IP Header.
 * </pre>
 * 
 * @author Gregory Bumgardner
 * 
 */
public final class AmtMembershipUpdateMessage extends AmtEncapsulationMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser extends AmtEncapsulationMessage.Parser {

        public Parser() {
            this(DEFAULT_UPDATE_PACKET_PARSER);
        }

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
    public static final int BASE_MESSAGE_LENGTH = 12;

    /**
     * Singleton instance of parser for IP packets carrying IGMP or MLD membership update messages. 
     */
    public static final IPPacket.BufferParser DEFAULT_UPDATE_PACKET_PARSER = getUpdatePacketParser();

    public static final ByteField       Reserved = new ByteField(1);
    public static final ByteArrayField  ResponseMac = new ByteArrayField(2,6);
    public static final IntegerField    RequestNonce = new IntegerField(8);
 
    
    /*-- Static Functions ---------------------------------------------------*/
    
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

    public static AmtMembershipUpdateMessage.Parser constructParser() {
        AmtMembershipUpdateMessage.Parser parser = new AmtMembershipUpdateMessage.Parser();
        parser.setIPPacketParser(getUpdatePacketParser());
        return parser;
    }

    /*-- Member Functions---------------------------------------------------*/
    
    /**
     * 
     * @param responseMac
     * @param requestNonce
     * @param updatePacket
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
     * 
     * @param buffer
     * @throws ParseException
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
            logger.finer(Logging.entering(ObjectId,"AmtMembershipUpdateMessage.setResponseMac", Logging.mac(responseMac)));
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
            logger.finer(Logging.entering(ObjectId, "AmtMembershipUpdateMessage.setRequestNonce", requestNonce));
        }
        
        RequestNonce.set(getBufferInternal(),requestNonce);
    }
    
}
