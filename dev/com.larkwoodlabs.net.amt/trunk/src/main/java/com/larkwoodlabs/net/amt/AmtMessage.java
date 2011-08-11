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

import com.larkwoodlabs.net.KeyedApplicationMessage;
import com.larkwoodlabs.util.buffer.BufferBackedObject;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.buffer.fields.SelectorField;
import com.larkwoodlabs.util.logging.Logging;

public abstract class AmtMessage extends BufferBackedObject implements KeyedApplicationMessage<Byte> {

    /*-- Inner Classes ------------------------------------------------------*/
    
    public static interface ParserType extends KeyedApplicationMessage.ParserType {

    }

    public static class Parser extends KeyedApplicationMessage.Parser {

        public Parser() {
            super(new SelectorField<Byte>(AmtMessage.MessageType));
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    /**
     * Logger instance shared by all AMT message classes.
     */
    public static final Logger logger = Logger.getLogger(AmtMessage.class.getName());

    public static final ByteField MessageType = new ByteField(0); 


    /*-- Static Functions ---------------------------------------------------*/

    public final static AmtMessage.Parser constructAmtGatewayParser() {
        AmtMessage.Parser parser = new AmtMessage.Parser();
        parser.add(AmtRelayAdvertisementMessage.constructParser());
        parser.add(AmtMembershipQueryMessage.constructParser());
        parser.add(AmtMulticastDataMessage.constructParser());
        return parser;
    }

    public final static AmtMessage.Parser constructAmtRelayParser() {
        AmtMessage.Parser parser = new AmtMessage.Parser();
        parser.add(AmtRelayDiscoveryMessage.constructParser());
        parser.add(AmtMembershipUpdateMessage.constructParser());
        return parser;
    }

    public final static AmtMessage.Parser constructAmtMessageParser() {
        AmtMessage.Parser parser = new AmtMessage.Parser();
        parser.add(AmtRelayDiscoveryMessage.constructParser());
        parser.add(AmtRelayAdvertisementMessage.constructParser());
        parser.add(AmtMembershipQueryMessage.constructParser());
        parser.add(AmtMembershipUpdateMessage.constructParser());
        parser.add(AmtMulticastDataMessage.constructParser());
        return parser;
    }
    

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param size
     * @param type
     */
    protected AmtMessage(final int size, final byte type) {
        super(size);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMessage.AmtMessage", size,type));
        }
        
        setType(type);
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     */
    protected AmtMessage(final ByteBuffer buffer) {
        super(buffer);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMessage.AmtMessage", buffer));
            logState(logger);
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
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
        logger.info(ObjectId + " : message-length="+getTotalLength());
        logger.info(ObjectId + " : type="+getType());
    }

    @Override
    public Byte getType() {
        return MessageType.get(getBufferInternal());
    }
    
    /**
     * 
     * @param type
     */
    protected final void setType(final byte type) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.fine(Logging.entering(ObjectId, "AmtMessage.setType", type));
        }
        
        MessageType.set(getBufferInternal(),type);
    }

    @Override
    public int getHeaderLength() {
        return 0;
    }

    @Override
    public int getTotalLength() {
        // TODO Auto-generated method stub
        return 0;
    }

}
