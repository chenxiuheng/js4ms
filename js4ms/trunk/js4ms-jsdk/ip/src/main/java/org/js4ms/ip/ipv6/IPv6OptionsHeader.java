package org.js4ms.ip.ipv6;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPv6OptionsHeader.java [org.js4ms.jsdk:ip]
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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.parser.MissingParserException;
import org.js4ms.ip.IPEndOfListOption;
import org.js4ms.ip.IPExtensionHeader;
import org.js4ms.ip.IPHeaderOption;
import org.js4ms.ip.IPMessage;




/**
 * Base class for IPv6 options headers.
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public abstract class IPv6OptionsHeader
                extends IPExtensionHeader {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * 
     * 
     */
    public static abstract class Parser
                    implements IPMessage.ParserType {

        /** */
        IPHeaderOption.Parser headerOptionParser;

        /**
         * 
         */
        public Parser() {
            this(null);
        }

        /**
         * @param headerOptionParser
         */
        public Parser(final IPHeaderOption.Parser headerOptionParser) {
            setHeaderOptionParser(headerOptionParser);
        }

        /**
         * @param headerOptionParser
         */
        public void setHeaderOptionParser(final IPHeaderOption.Parser headerOptionParser) {
            this.headerOptionParser = headerOptionParser;
        }

        /**
         * @param parser
         */
        public void add(final IPHeaderOption.ParserType parser) {
            // Precondition.checkReference(parser);
            if (this.headerOptionParser == null) {
                setHeaderOptionParser(new IPHeaderOption.Parser());
            }
            this.headerOptionParser.add(parser.getKey(), parser);
        }

        /**
         * @param optionCode
         * @param parser
         */
        public void add(final Object optionCode, final IPHeaderOption.ParserType parser) {
            // Precondition.checkReference(parser);
            if (this.headerOptionParser == null) {
                setHeaderOptionParser(new IPHeaderOption.Parser());
            }
            this.headerOptionParser.add(optionCode, parser);
        }

        /**
         * @param optionCode
         */
        public void remove(final Object optionCode) {
            if (this.headerOptionParser != null) {
                this.headerOptionParser.remove(optionCode);
            }
        }

        /**
         * @param buffer
         * @return
         * @throws ParseException
         */
        public abstract IPv6OptionsHeader constructHeader(final ByteBuffer buffer) throws ParseException;

        @Override
        public IPMessage parse(final ByteBuffer buffer) throws ParseException, MissingParserException {
            IPv6OptionsHeader header = constructHeader(buffer);

            // Parse IP header options
            header.parseOptions(this.headerOptionParser);

            return header;
        }

    }

    /*-- Member Variables---------------------------------------------------*/

    /** */
    protected ByteBuffer unparsedOptions = null;

    /** */
    protected Vector<IPHeaderOption> options = null;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param protocolNumber
     */
    protected IPv6OptionsHeader(final byte protocolNumber) {
        super(protocolNumber);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.log.entry("IPv6OptionsHeader.IPv6OptionsHeader", protocolNumber));
        }
    }

    /**
     * @param buffer
     * @param protocolNumber
     * @throws ParseException
     */
    public IPv6OptionsHeader(final ByteBuffer buffer, final byte protocolNumber) throws ParseException {
        super(consume(buffer, BASE_HEADER_LENGTH), protocolNumber);

        int headerLength = HeaderLength.get(getBufferInternal());
        this.unparsedOptions = consume(buffer, (MIN_HEADER_LENGTH - BASE_HEADER_LENGTH) + headerLength * 8);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6OptionsHeader.IPv6OptionsHeader", buffer));
        }
    }

    @Override
    public void writeTo(final ByteBuffer buffer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6OptionsHeader.writeTo", buffer));
        }

        // Precondition.checkBounds(buffer.length, offset, getTotalLength());
        updateHeaderLength();
        super.writeTo(buffer);
        if (this.options == null) {
            buffer.put(this.unparsedOptions);
        }
        else {
            Iterator<IPHeaderOption> iter = this.options.iterator();
            while (iter.hasNext()) {
                iter.next().writeTo(buffer);
            }
        }
        int padding = getPaddingLength();
        for (int i = 0; i < padding; i++) {
            buffer.put((byte) 0);
        }
    }

    @Override
    public void setProtocolNumber(final byte protocolNumber) {
        // Do nothing - value is set by constructors
    }

    /**
     * Gets the computed length of the header including options expressed in
     * bytes. This value is stored in the header as the length in 8-byte words minus 8
     * bytes.
     * 
     * @return
     */
    public int getComputedHeaderLength() {
        return (((BASE_HEADER_LENGTH + getOptionsLength() + 7) / 8) * 8);
    }

    /**
     * Updates the header length field using the value returned by
     * {@link #getComputedHeaderLength()}.
     */
    private void updateHeaderLength() {
        setHeaderLength(getComputedHeaderLength());
    }

    /**
     * @param option
     */
    public final void addOption(final IPHeaderOption option) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6OptionsHeader.addOption", option));
        }

        if (this.options == null) this.options = new Vector<IPHeaderOption>();
        this.options.add(option);
        updateHeaderLength();
    }

    /**
     * @param option
     */
    public final void removeOption(final IPHeaderOption option) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6OptionsHeader.removeOption", option));
        }

        this.options.remove(option);
        updateHeaderLength();
    }

    /**
     * Calculates the total bytes required by all options currently attached to
     * the packet header.
     * 
     * @return
     */
    public final int getOptionsLength() {
        if (this.options == null) {
            if (this.unparsedOptions == null) {
                return 0;
            }
            else {
                return this.unparsedOptions.limit();
            }
        }
        else {
            int length = 0;
            for (IPHeaderOption option : this.options) {
                length += option.getOptionLength();
            }
            return length;
        }
    }

    /**
     * @param parser
     * @throws ParseException
     * @throws MissingParserException
     */
    public void parseOptions(final IPHeaderOption.Parser parser) throws ParseException, MissingParserException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IPv6OptionsHeader.parseOptions", parser));
        }

        // Precondition.checkReference(parser);
        if (this.unparsedOptions != null && parser != null) {

            if (this.options == null) {
                this.options = new Vector<IPHeaderOption>();
            }
            else {
                this.options.clear();
            }

            while (this.unparsedOptions.limit() > 0) {
                IPHeaderOption option = parser.parse(this.unparsedOptions);
                this.options.add(option);
                if (option instanceof IPEndOfListOption) {
                    break;
                }
            }

            this.unparsedOptions.rewind();

            updateHeaderLength();
        }
    }

    /**
     * @return
     */
    public Enumeration<IPHeaderOption> getOptions() {
        return this.options != null ? this.options.elements() : null;
    }

    /**
     * Calculates the number of zero-padding bytes required to make IP header
     * end on 64-bit word boundary.
     * 
     * @return
     */
    public int getPaddingLength() {
        return getHeaderLength() - BASE_HEADER_LENGTH - getOptionsLength();
    }

}
