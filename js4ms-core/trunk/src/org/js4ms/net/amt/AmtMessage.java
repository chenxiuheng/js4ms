/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: AmtMessage.java (org.js4ms.net.amt)
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

package org.js4ms.net.amt;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.net.KeyedApplicationMessage;
import org.js4ms.util.buffer.BufferBackedObject;
import org.js4ms.util.buffer.fields.ByteField;
import org.js4ms.util.buffer.fields.SelectorField;



/**
 * Base class for all AMT message classes.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public abstract class AmtMessage
                extends BufferBackedObject
                implements KeyedApplicationMessage<Byte> {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * The parser interface for AMT messages.
     */
    public static interface ParserType
                    extends KeyedApplicationMessage.ParserType {

    }

    /**
     * Base AMT message parser.
     */
    public static class Parser
                    extends KeyedApplicationMessage.Parser {

        /**
         * 
         */
        public Parser() {
            super(new SelectorField<Byte>(AmtMessage.MessageType));
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    /**
     * Logger instance shared by all AMT message classes.
     */
    public static final Logger logger = Logger.getLogger(AmtMessage.class.getName());

    private static final ByteField MessageType = new ByteField(0);

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return A parser that constructs an AMT messages object from the contents of a
     *         ByteBuffer.
     *         Used to parse messages sent from a relay to a gateway.
     */
    public final static AmtMessage.Parser constructAmtGatewayParser() {
        AmtMessage.Parser parser = new AmtMessage.Parser();
        parser.add(AmtRelayAdvertisementMessage.constructParser());
        parser.add(AmtMembershipQueryMessage.constructParser());
        parser.add(AmtMulticastDataMessage.constructParser());
        return parser;
    }

    /**
     * @return A parser that constructs an AMT message object from the contents of a
     *         ByteBuffer.
     *         Used to parse messages sent from a gateway to a relay.
     */
    public final static AmtMessage.Parser constructAmtRelayParser() {
        AmtMessage.Parser parser = new AmtMessage.Parser();
        parser.add(AmtRelayDiscoveryMessage.constructParser());
        parser.add(AmtMembershipUpdateMessage.constructParser());
        return parser;
    }

    /**
     * @return A parser that constructs an AMT message object from the contents of a
     *         ByteBuffer.
     */
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
     * Constructs an instance with the specified message size and type.
     * 
     * @param size
     *            The number of bytes that representing the fixed portion of the message.
     * @param type
     *            The message type.
     */
    protected AmtMessage(final int size, final byte type) {
        super(size);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMessage.AmtMessage", size, type));
        }

        setType(type);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * @param buffer
     */
    protected AmtMessage(final ByteBuffer buffer) {
        super(buffer);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMessage.AmtMessage", buffer));
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
     * 
     * @param logger
     */
    private void logState(final Logger logger) {
        logger.info(this.log.msg(": message-length=" + getTotalLength()));
        logger.info(this.log.msg(": type=" + getType()));
    }

    @Override
    public Byte getType() {
        return MessageType.get(getBufferInternal());
    }

    /**
     * @param type
     */
    protected final void setType(final byte type) {

        if (logger.isLoggable(Level.FINER)) {
            logger.fine(this.log.entry("AmtMessage.setType", type));
        }

        MessageType.set(getBufferInternal(), type);
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
