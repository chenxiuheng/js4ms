package org.js4ms.ip;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPExtensionHeader.java [org.js4ms.jsdk:ip]
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
import org.js4ms.common.util.buffer.BufferBackedObject;
import org.js4ms.common.util.buffer.field.ByteField;
import org.js4ms.common.util.buffer.parser.MissingParserException;
import org.js4ms.common.util.logging.Logging;




/**
 * Represents an IP extension header. Used as a base class for classes that represent
 * specific header types, or used to represent a generic, unparsed header. <h3>Header
 * Format</h3> <blockquote>
 * 
 * <pre>
 *   0               1               2               3
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |  Next Header  |  Hdr Ext Len  |                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                               +
 *  |                                                               |
 *  .                                                               .
 *  .                                                               .
 *  .                                                               .
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * <dl>
 * <dt><u>Next Header Type</u></dt>
 * <p>
 * <dd>Identifies the type of header immediately following the extension header. Uses the
 * same values as the IPv4 Protocol field [RFC-1700].</dd>
 * <p>
 * <dt><u>Header Length</u></dt>
 * <p>
 * <dd>Length of the Hop-by-Hop Options header in 8-octet units, not including the first 8
 * octets.</dd>
 * </dl>
 * </blockquote>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public class IPExtensionHeader
                extends BufferBackedObject
                implements IPMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * 
     */
    public static class Parser
                    implements IPMessage.ParserType {

        @Override
        public IPExtensionHeader parse(final ByteBuffer buffer) throws ParseException {
            return new IPExtensionHeader(buffer);
        }

        @Override
        public boolean verifyChecksum(final ByteBuffer buffer, final byte[] sourceAddress, final byte[] destinationAddress) throws MissingParserException, ParseException {
            return true;
        }

        @Override
        public Object getKey() {
            return null; // Any protocol
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    /** Logger used to generate IPExtensionHeader log entries. */
    public static final Logger logger = Logger.getLogger(IPExtensionHeader.class.getName());

    /** */
    public static final ByteField NextHeader = new ByteField(0);

    /** */
    public static final ByteField HeaderLength = new ByteField(1);

    /** */
    protected static final int BASE_HEADER_LENGTH = 2;

    /** */
    protected static final int MIN_HEADER_LENGTH = 8;

    /*-- Member Variables ---------------------------------------------------*/

    private byte protocolNumber;

    private IPMessage nextProtocolHeader = null;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param protocolNumber
     */
    protected IPExtensionHeader(final byte protocolNumber) {
        super(MIN_HEADER_LENGTH);
        this.protocolNumber = protocolNumber;

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPExtensionHeader.IPv6ExtensionHeader", protocolNumber));
            logState(logger,Level.FINER);
        }
    }

    /**
     * @param size
     * @param protocolNumber
     */
    protected IPExtensionHeader(final int size, final byte protocolNumber) {
        super(size);
        this.protocolNumber = protocolNumber;

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPExtensionHeader.IPv6ExtensionHeader", size, protocolNumber));
            logState(logger,Level.FINER);
        }
    }

    /**
     * Constructs an extension header object.
     * 
     * @param buffer
     * @throws ParseException
     */
    public IPExtensionHeader(final ByteBuffer buffer) throws ParseException {
        this(consume(buffer, HeaderLength.get(buffer) * 8 + 8), (byte) 0);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPExtensionHeader.IPv6ExtensionHeader", buffer));
            logState(logger,Level.FINER);
        }
    }

    /**
     * Used by derived classes to construct an extension header object.
     * 
     * @param buffer
     * @param protocolNumber
     */
    protected IPExtensionHeader(final ByteBuffer buffer, final byte protocolNumber) {
        super(buffer);
        this.protocolNumber = protocolNumber;

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPExtensionHeader.IPv6ExtensionHeader", buffer));
            logState(logger,Level.FINER);
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
     * Logs value of member variables declared or maintained by this class.
     * 
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": protocol=" + getProtocolNumber()));
        logger.log(level,this.log.msg(": next-header=" + getNextProtocolNumber()));
        logger.log(level,this.log.msg(": header-length=" + getHeaderLength()));
    }

    @Override
    public final void writeChecksum(final ByteBuffer buffer,
                                    final byte[] sourceAddress,
                                    final byte[] destinationAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPExtensionHeader.writeChecksum",
                                        buffer,
                                        Logging.address(sourceAddress),
                                        Logging.address(destinationAddress)));
        }
        // Does nothing - extension headers do not carry checksums
    }

    @Override
    public final byte getProtocolNumber() {
        return this.protocolNumber;
    }

    @Override
    public void setProtocolNumber(final byte protocolNumber) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPExtensionHeader.setProtocolNumber", protocolNumber));
        }

        this.protocolNumber = protocolNumber;
    }

    @Override
    public final byte getNextProtocolNumber() {
        return NextHeader.get(getBufferInternal());
    }

    /**
     * @param protocolNumber
     */
    protected final void setNextProtocolNumber(final byte protocolNumber) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPExtensionHeader.setNextProtocolNumber", protocolNumber));
        }

        NextHeader.set(getBufferInternal(), protocolNumber);
    }

    /**
     * Gets the total length of this header in bytes.
     * Some extension headers override this method to return a fixed value.
     * 
     * @return
     */
    @Override
    public int getHeaderLength() {
        return HeaderLength.get(getBufferInternal()) * 8 + 8;
    }

    /**
     * Sets the total length of this header in bytes.
     * Must be multiple of 8 and greater than 8 since minimum extension header size is 8
     * octets.
     * 
     * @param length
     */
    protected final void setHeaderLength(final int length) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPExtensionHeader.setHeaderLength", length));
        }

        HeaderLength.set(getBufferInternal(), (byte) Math.min((length / 8) - 1, 0));
    }

    @Override
    public final int getTotalLength() {
        return getHeaderLength();
    }

    @Override
    public final IPMessage getNextMessage() {
        return this.nextProtocolHeader;
    }

    @Override
    public final void setNextMessage(final IPMessage nextProtocolHeader) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPExtensionHeader.setNextMessage", nextProtocolHeader));
        }

        // Null value is allowed
        this.nextProtocolHeader = nextProtocolHeader;
        if (this.nextProtocolHeader != null) {
            setNextProtocolNumber(nextProtocolHeader.getProtocolNumber());
        }
        else {
            setNextProtocolNumber(IPMessage.NO_NEXT_HEADER);
        }
    }

    @Override
    public final void removeNextMessage() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPExtensionHeader.removeNextMessage"));
        }

        IPMessage nextMessage = getNextMessage();
        if (nextMessage != null) {
            setNextMessage(nextMessage.getNextMessage());
            nextMessage.setNextMessage(null);
        }
    }

}
