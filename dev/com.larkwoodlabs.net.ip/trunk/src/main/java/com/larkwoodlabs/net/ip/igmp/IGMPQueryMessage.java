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

package com.larkwoodlabs.net.ip.igmp;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Base class for v2 and v3 Membership Query message classes.
 * The two versions are differentiated by their length.
 * 
 * @author Gregory Bumgardner
 */
public abstract class IGMPQueryMessage extends IGMPGroupMessage {

    /*-- Inner Classes ------------------------------------------------------*/
    
    /**
     * Parser used to construct and parse the appropriate Membership Query messages.
     * 
     * The IGMP version of a Membership Query message is determined as follows:
     * 
     * <li>IGMPv2 Query: length = 8 octets and Max Resp Code is zero.
     * <li>IGMPv3 Query: length >= 12 octets
     * 
     * Query messages that do not match any of the above conditions MUST be silently ignored.     * 
     */
    public static class Parser implements IGMPMessage.ParserType {

        IGMPv2QueryMessage.Parser v2Parser = new IGMPv2QueryMessage.Parser();
        IGMPv3QueryMessage.Parser v3Parser = new IGMPv3QueryMessage.Parser();
        
        public Parser() {
            this.v2Parser = new IGMPv2QueryMessage.Parser();
            this.v3Parser = new IGMPv3QueryMessage.Parser();
        }

        @Override
        public IGMPMessage parse(ByteBuffer buffer) throws ParseException {
            if (buffer.limit() == IGMPv2QueryMessage.BASE_MESSAGE_LENGTH) {
                return this.v2Parser.parse(buffer);
            }
            else if (buffer.limit() >= IGMPv3QueryMessage.BASE_MESSAGE_LENGTH) {
                return this.v3Parser.parse(buffer);
            }
            else {
                throw new ParseException("the length of the Membership Query message is invalid");
            }
        }

        @Override
        public boolean verifyChecksum(ByteBuffer buffer) throws MissingParserException, ParseException {
            if (buffer.limit() == IGMPv2QueryMessage.BASE_MESSAGE_LENGTH) {
                return this.v2Parser.verifyChecksum(buffer);
            }
            else if (buffer.limit() >= IGMPv3QueryMessage.BASE_MESSAGE_LENGTH) {
                return this.v3Parser.verifyChecksum(buffer);
            }
            else {
                throw new ParseException("the length of the Membership Query message is invalid");
            }
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

   }

    /*-- Static Variables ---------------------------------------------------*/
    
    public static final byte MESSAGE_TYPE = 0x11;

    /**
     * Field that specifies the maximum response time (or Reserved).
     */
    public static final ByteField MaxRespCode = new ByteField(1);
    

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param size
     * @param maximumResponseTime
     * @param groupAddress
     */
    protected IGMPQueryMessage(int size, short maximumResponseTime, byte[] groupAddress) {
        super(size, MESSAGE_TYPE, maximumResponseTime, groupAddress);
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(Logging.entering(ObjectId, "IGMPQueryMessage.IGMPQueryMessage", size, maximumResponseTime, Logging.address(groupAddress)));
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    protected IGMPQueryMessage(ByteBuffer buffer) throws ParseException {
        super(buffer);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(Logging.entering(ObjectId, "IGMPQueryMessage.IGMPQueryMessage", buffer));
            logState(logger);
        }
    }

    @Override
    public void log(Logger logger) {
        super.log(logger);
        logState(logger);
    }
    
    /**
     * 
     * @param logger
     */
    private void logState(Logger logger) {
        logger.info(ObjectId + " : max-resp-code="+String.format("%02X",getMaxRespCode())+" max-response-time="+getMaximumResponseTime()+"ms");
    }

    @Override
    public byte getType() {
        return MESSAGE_TYPE;
    }

    /**
     * Returns the value of the message {@linkplain #MaxRespCode Max Resp Code} field.
     * The specifies the maximum time allowed for an IGMP response.
     */
    public byte getMaxRespCode() {
        return MaxRespCode.get(getBufferInternal());
    }

    /**
     * Sets the value of the message {@linkplain #MaxRespCode Max Resp Code} field.
     */
    public void setMaxRespCode(byte maxRespCode) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPQueryMessage.setMaxRespCode", maxRespCode));
        }
        
        MaxRespCode.set(getBufferInternal(), maxRespCode);
    }

    /**
     * 
     * @return
     */
    public abstract int getMaximumResponseTime();

    /**
     * 
     * @param milliseconds
     */
    public abstract void setMaximumResponseTime(int milliseconds);

}