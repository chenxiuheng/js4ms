/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: IPPayload.java (org.js4ms.net.ip)
 * 
 * Copyright © 2009-2012 Cisco Systems, Inc.
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

package org.js4ms.net.ip;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exceptions.ParseException;
import org.js4ms.util.buffer.BufferBackedObject;
import org.js4ms.util.buffer.parser.MissingParserException;




/**
 * Represents an opaque IP message.
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public class IPPayload
                extends BufferBackedObject
                implements IPMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * 
     */
    public static class Parser
                    implements IPMessage.ParserType {

        @Override
        public IPPayload parse(final ByteBuffer buffer) {
            return new IPPayload(buffer);
        }

        @Override
        public boolean verifyChecksum(final ByteBuffer buffer,
                                      final byte[] sourceAddress,
                                      final byte[] destinationAddress) throws MissingParserException, ParseException {
            return true; // Does nothing in this class
        }

        @Override
        public Object getKey() {
            return null; // Any Protocol
        }

    }

    /*-- Member Variables ---------------------------------------------------*/

    /** Logger used to generate IPPayload log entries. */
    public static final Logger logger = Logger.getLogger(IPPayload.class.getName());

    /** */
    private byte protocolNumber;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param buffer
     */
    public IPPayload(final ByteBuffer buffer) {
        this((byte) 0, buffer);
    }

    /**
     * @param protocolNumber
     * @param buffer
     */
    public IPPayload(final byte protocolNumber, final ByteBuffer buffer) {
        super(buffer);
        this.protocolNumber = protocolNumber;

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.log.entry("IPPayload.IPPayload", protocolNumber, buffer));
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
     * 
     * @param logger
     */
    private void logState(final Logger logger) {
        logger.info(this.log.msg(": protocol=" + getProtocolNumber()));
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void writeChecksum(final ByteBuffer buffer,
                              final byte[] sourceAddress,
                              final byte[] destinationAddress) {
        // Does nothing in this class
    }

    @Override
    public byte getProtocolNumber() {
        return this.protocolNumber;
    }

    @Override
    public void setProtocolNumber(final byte protocolNumber) {
        this.protocolNumber = protocolNumber;
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
        // Does nothing
    }

    @Override
    public int getHeaderLength() {
        return getBufferInternal().limit();
    }

    @Override
    public int getTotalLength() {
        return getBufferInternal().limit();
    }

}
