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

package com.larkwoodlabs.net.ip.mld;

import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Base class for v1 and v2 Multicast Listener Query classes.
 * The two versions are differentiated by their length.
 * 
 * @author Gregory Bumgardner
 * 
 */
public abstract class MLDQueryMessage extends MLDGroupMessage {

    /**
     * Parser used to construct and parse the appropriate Multicast Listener Query messages.
     * 
     * The MLD version of a Multicast Listener Query message is determined as follows:
     * 
     * <li>MLDv1 Query: length = 24 octets
     * <li>MLDv2 Query: length >= 28 octets
     * 
     * Query messages that do not match any of the above conditions MUST be silently ignored.
     * 
     * 
     * @author Gregory Bumgardner
     * 
     */
    public static class Parser implements MLDMessage.ParserType {

        MLDv1QueryMessage.Parser v1Parser = new MLDv1QueryMessage.Parser();
        MLDv2QueryMessage.Parser v2Parser = new MLDv2QueryMessage.Parser();
        
        public Parser() {
            this.v1Parser = new MLDv1QueryMessage.Parser();
            this.v2Parser = new MLDv2QueryMessage.Parser();
        }

        @Override
        public MLDMessage parse(ByteBuffer buffer) throws ParseException {
            if (buffer.limit() == MLDv1QueryMessage.BASE_MESSAGE_LENGTH) {
                return this.v1Parser.parse(buffer);
            }
            else if (buffer.limit() >= MLDv2QueryMessage.BASE_MESSAGE_LENGTH) {
                return this.v2Parser.parse(buffer);
            }
            else {
                throw new ParseException("the length of the Multicast Listener Query is invalid");
            }
        }

        @Override
        public boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) throws MissingParserException, ParseException {
            if (buffer.limit() == MLDv1QueryMessage.BASE_MESSAGE_LENGTH) {
                return this.v1Parser.verifyChecksum(buffer, sourceAddress, destinationAddress);
            }
            else if (buffer.limit() > MLDv2QueryMessage.BASE_MESSAGE_LENGTH) {
                return this.v2Parser.verifyChecksum(buffer, sourceAddress, destinationAddress);
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
    
    public static final byte MESSAGE_TYPE = (byte)130;

    /**
     * 
     */
    public static final byte[] GENERAL_QUERY_GROUP = new byte[16];

    public static final byte[] QUERY_DESTINATION_ADDRESS;
    
    static {
        byte[] address = new byte[16];
        address[0] = (byte)0xFF;
        address[1] = (byte)0x02;
        address[15] = 1;
        QUERY_DESTINATION_ADDRESS = address;
    }

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs a general query
     * @param size
     * @param groupAddress
     */
    protected MLDQueryMessage(int size) {
        super(size,(byte)0,GENERAL_QUERY_GROUP);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDQueryMessage.MLDQueryMessage"));
        }
    }

    /**
     * 
     * @param size
     * @param groupAddress
     */
    protected MLDQueryMessage(int size, byte[] groupAddress) {
        super(size,(byte)0,groupAddress);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDQueryMessage.MLDQueryMessage", Logging.address(groupAddress)));
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    protected MLDQueryMessage(ByteBuffer buffer) throws ParseException {
        super(buffer);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDQueryMessage.MLDQueryMessage", buffer));
        }
    }

    @Override
    public byte getType() {
        return MESSAGE_TYPE;
    }

}
