package org.js4ms.ip;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPinIPHeader.java [org.js4ms.jsdk:ip]
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
import org.js4ms.common.util.buffer.parser.MissingParserException;
import org.js4ms.common.util.logging.LoggableBase;
import org.js4ms.common.util.logging.Logging;




/**
 * Represents and IP-in-IP payload.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public class IPinIPHeader
                extends LoggableBase
                implements IPMessage {

    /**
     * 
     */
    public static class Parser
                    implements IPMessage.ParserType {

        IPPacket.BufferParser ipParser = null;

        /**
         * 
         */
        public Parser() {
            this(new IPPacket.BufferParser());
        }

        /**
         * @param ipParser
         */
        public Parser(final IPPacket.BufferParser ipParser) {
            setIPHeaderParser(ipParser);
        }

        /**
         * @param ipParser
         */
        public void setIPHeaderParser(final IPPacket.BufferParser ipParser) {
            // Precondition.checkReference(ipParser);
            this.ipParser = ipParser;
        }

        /**
         * @return
         */
        public IPPacket.BufferParser getIPHeaderParser() {
            return this.ipParser;
        }

        @Override
        public IPMessage parse(final ByteBuffer buffer) throws ParseException, MissingParserException {
            IPPacket header = this.ipParser.parse(buffer);
            return new IPinIPHeader(header);
        }

        @Override
        public boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) throws MissingParserException, ParseException {
            return true; // Does nothing in this class
        }

        @Override
        public Object getKey() {
            return IP_PROTOCOL_NUMBER;
        }

    }

    /** Logger used to generate IPinIPHeader log entries */
    public static final Logger logger = Logger.getLogger(IPinIPHeader.class.getName());

    /** Protocol number for IP-in-IP headers. */
    public static final byte IP_PROTOCOL_NUMBER = 4;

    /** */
    protected final String ObjectId = Logging.identify(this);

    /** */
    private IPPacket encapsulatedHeader;

    /**
     * @param encapsulatedHeader
     */
    public IPinIPHeader(final IPPacket encapsulatedHeader) {
        // Precondition.checkReference(encapsulatedHeader);
        this.encapsulatedHeader = encapsulatedHeader;

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPinIPHeader.IPinIPHeader", encapsulatedHeader));
            logState(logger, Level.FINER);
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
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
        logger.log(level,ObjectId + " : protocol=" + getProtocolNumber());
        if (this.encapsulatedHeader != null) {
            logger.log(level,ObjectId + " ----> start encapsulated header");
            this.encapsulatedHeader.log(logger,level);
            logger.log(level,ObjectId + " <---- end encapsulated header");
        }
    }

    @Override
    public void writeChecksum(final ByteBuffer buffer,
                              final byte[] sourceAddress,
                              final byte[] destinationAddress) {
        // Does nothing in this class
    }

    @Override
    public byte getProtocolNumber() {
        return IP_PROTOCOL_NUMBER;
    }

    @Override
    public void setProtocolNumber(final byte protocolNumber) {
        // Do nothing - protocol number is set in constructors
    }

    @Override
    public byte getNextProtocolNumber() {
        return NO_NEXT_HEADER;
    }

    @Override
    public IPMessage getNextMessage() {
        return null;
    }

    @Override
    public void setNextMessage(final IPMessage header) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeNextMessage() {
        // Does nothing in this class
    }

    @Override
    public int getHeaderLength() {
        return 0;
    }

    @Override
    public int getTotalLength() {
        return this.encapsulatedHeader.getHeaderLength() + this.encapsulatedHeader.getPayloadLength();
    }

    @Override
    public void writeTo(final ByteBuffer buffer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPinIPHeader.writeTo", buffer));
        }

        this.encapsulatedHeader.writeTo(buffer);
    }

    /**
     * @return
     */
    public IPPacket getEncapsulatedHeader() {
        return this.encapsulatedHeader;
    }

}
