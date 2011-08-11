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
import com.larkwoodlabs.util.buffer.fields.IntegerField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An AMT request message.
 * <pre>
 * 6.3. AMT Request
 * 
 *    A Request packet is sent to begin a 3-way handshake for sending an
 *    IGMP/MLD Membership/Listener Report or Leave/Done.  It can be sent
 *    from a gateway to a relay, from a gateway to another gateway, or from
 *    a relay to a gateway.
 * 
 *    It is sent from the originator's unique unicast address to the
 *    respondents' unique unicast address.
 * 
 *    The UDP source port is uniquely selected by the local host operating
 *    system.  It can be different for each Request and different from the
 *    source port used in Discovery messages but does not have to be.  The
 *    UDP destination port is the IANA reserved AMT port number.  The UDP
 *    checksum MUST be valid in AMT control messages.
 * 
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |     Type=0x3  |     Reserved                                  |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |            Request Nonce                                      |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 * 6.3.1. Type
 * 
 *    The type of the message.
 * 
 * 6.3.2. Reserved
 * 
 *    A 24-bit reserved field.  Sent as 0, ignored on receipt.
 * 
 * 6.3.3. Request Nonce
 * 
 *    A 32-bit identifier used to distinguish this request.
 * </pre>
 * 
 * @author Gregory Bumgardner
 * 
 */
public final class AmtRequestMessage extends AmtMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser implements AmtMessage.ParserType {

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
    
    public static final IntegerField RequestNonce = new IntegerField(4);

    public static int nextRequestNonce = 1;


    /*-- Static Functions ---------------------------------------------------*/

    public static AmtRequestMessage.Parser constructParser() {
        return new AmtRequestMessage.Parser();
    }

    public static synchronized int getNextRequestNonce() {
        return nextRequestNonce++;
    }

    /*-- Member Functions ---------------------------------------------------*/

    public AmtRequestMessage() {
        this(getNextRequestNonce());
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtRequestMessage.AmtRequestMessage"));
        }
    }

    public AmtRequestMessage(int requestNonce) {
        super(MESSAGE_LENGTH,MESSAGE_TYPE);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtRequestMessage.AmtRequestMessage", requestNonce));
        }
        
        setRequestNonce(requestNonce);
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    public AmtRequestMessage(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtRequestMessage.AmtRequestMessage", buffer));
            logState(logger);
        }
    }

    @Override
    public void log(Logger logger) {
        super.log(logger);
        logState(logger);
    }
    
    /**
     * Logs value of member variables declared or maintained by this class.
     * @param logger
     */
    private void logState(Logger logger) {
        logger.info(ObjectId + " : request-nonce="+getRequestNonce());
    }

    @Override
    public Byte getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public int getTotalLength() {
        return MESSAGE_LENGTH;
    }

    public int getRequestNonce() {
        return RequestNonce.get(getBufferInternal());
    }

    public void setRequestNonce(int requestNonce) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtRequestMessage.setRequestNonce", requestNonce));
        }
        
        RequestNonce.set(getBufferInternal(),requestNonce);
    }

}
