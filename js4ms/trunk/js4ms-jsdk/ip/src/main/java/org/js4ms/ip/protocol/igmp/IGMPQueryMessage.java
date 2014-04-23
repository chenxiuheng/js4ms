package org.js4ms.ip.protocol.igmp;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IGMPQueryMessage.java [org.js4ms.jsdk:ip]
 * %%
 * Copyright (C) 2009 - 2014 Cisco Systems, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.field.ByteField;
import org.js4ms.common.util.buffer.parser.MissingParserException;
import org.js4ms.common.util.logging.Logging;




/**
 * Base class for IGMPv2 and IGMPv3 Membership Query Message classes.
 * The two versions are differentiated by their length.
 * 
 * @see IGMPv2QueryMessage
 * @see IGMPv3QueryMessage
 * @author Gregory Bumgardner (gbumgard)
 */
public abstract class IGMPQueryMessage
                extends IGMPGroupMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * Parser used to construct and parse the appropriate Membership Query messages.
     * The IGMP version of a Membership Query message is determined as follows:
     * <ul>
     * <li>IGMPv2 Query: length = 8 octets and Max Resp Code is zero.
     * <li>IGMPv3 Query: length >= 12 octets
     * </ul>
     * Query messages that do not match any of the above conditions MUST be silently
     * ignored. *
     */
    public static class Parser
                    implements IGMPMessage.ParserType {

        /** */
        final IGMPv2QueryMessage.Parser v2Parser = new IGMPv2QueryMessage.Parser();

        /** */
        final IGMPv3QueryMessage.Parser v3Parser = new IGMPv3QueryMessage.Parser();

        /**
         * 
         */
        public Parser() {
        }

        @Override
        public IGMPMessage parse(final ByteBuffer buffer) throws ParseException {
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

    /** */
    public static final byte MESSAGE_TYPE = 0x11;

    /**
     * Field that specifies the maximum response time (or Reserved).
     */
    public static final ByteField MaxRespCode = new ByteField(1);

    /** */
    public static final byte[] GENERAL_QUERY_GROUP = new byte[4];

    /** */
    public static final byte[] QUERY_DESTINATION_ADDRESS;

    static {
        byte[] address = new byte[4];
        address[0] = (byte) 224;
        address[1] = 0;
        address[2] = 0;
        address[3] = 1;
        QUERY_DESTINATION_ADDRESS = address;
    }

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs a general query message
     * 
     * @param size
     * @param maximumResponseTime
     */
    protected IGMPQueryMessage(final int size, final short maximumResponseTime) {
        super(size, MESSAGE_TYPE, maximumResponseTime, GENERAL_QUERY_GROUP);

        if (logger.isLoggable(Level.FINER)) {
            logger.fine(this.log.entry("IGMPQueryMessage.IGMPQueryMessage", size, maximumResponseTime));
            logState(logger, Level.FINER);
        }
    }

    /**
     * @param size
     * @param maximumResponseTime
     * @param groupAddress
     */
    protected IGMPQueryMessage(final int size,
                               final short maximumResponseTime,
                               final byte[] groupAddress) {
        super(size, MESSAGE_TYPE, maximumResponseTime, groupAddress);

        if (logger.isLoggable(Level.FINER)) {
            logger.fine(this.log.entry("IGMPQueryMessage.IGMPQueryMessage", size, maximumResponseTime,
                                       Logging.address(groupAddress)));
            logState(logger, Level.FINER);
        }
    }

    /**
     * @param buffer
     * @throws ParseException
     */
    protected IGMPQueryMessage(final ByteBuffer buffer) throws ParseException {
        super(buffer);

        if (logger.isLoggable(Level.FINER)) {
            logger.fine(this.log.entry("IGMPQueryMessage.IGMPQueryMessage", buffer));
            logState(logger, Level.FINER);
        }
    }

    @Override
    public void log(final Logger logger, final Level level) {
        super.log(logger, level);
        logState(logger, level);
    }

    /**
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(" : max-resp-code=" + String.format("%02X", getMaxRespCode()) + " max-response-time="
                                 + getMaximumResponseTime() + "ms"));
    }

    @Override
    public byte getType() {
        return MESSAGE_TYPE;
    }

    /**
     * Returns the value of the message {@linkplain #MaxRespCode Max Resp Code} field.
     * The specifies the maximum time allowed for an IGMP response.
     * 
     * @return
     */
    public byte getMaxRespCode() {
        return MaxRespCode.get(getBufferInternal());
    }

    /**
     * Sets the value of the message {@linkplain #MaxRespCode Max Resp Code} field.
     * 
     * @param maxRespCode
     */
    public void setMaxRespCode(final byte maxRespCode) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPQueryMessage.setMaxRespCode", maxRespCode));
        }

        MaxRespCode.set(getBufferInternal(), maxRespCode);
    }

    /**
     * @return
     */
    public abstract int getMaximumResponseTime();

    /**
     * @param milliseconds
     */
    public abstract void setMaximumResponseTime(int milliseconds);

}
