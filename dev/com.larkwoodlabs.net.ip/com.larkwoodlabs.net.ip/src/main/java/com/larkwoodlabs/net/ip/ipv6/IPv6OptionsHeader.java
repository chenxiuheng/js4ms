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

package com.larkwoodlabs.net.ip.ipv6;

import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPEndOfListOption;
import com.larkwoodlabs.net.ip.IPExtensionHeader;
import com.larkwoodlabs.net.ip.IPHeaderOption;
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

public abstract class IPv6OptionsHeader extends IPExtensionHeader {

    /*-- Inner Classes ------------------------------------------------------*/
    
    public static abstract class Parser implements IPMessage.ParserType {

        IPHeaderOption.Parser headerOptionParser;
        
        public Parser() {
            this(null);
        }

        public Parser(IPHeaderOption.Parser headerOptionParser) {
            setHeaderOptionParser(headerOptionParser);
        }

        public void setHeaderOptionParser(IPHeaderOption.Parser headerOptionParser) {
            this.headerOptionParser = headerOptionParser;
        }

        public void add(IPHeaderOption.ParserType parser) {
            //Precondition.checkReference(parser);
            if (this.headerOptionParser == null) {
                setHeaderOptionParser(new IPHeaderOption.Parser());
            }
            this.headerOptionParser.add(parser.getKey(), parser);
        }

        public void add(Object optionCode, IPHeaderOption.ParserType parser) {
            //Precondition.checkReference(parser);
            if (this.headerOptionParser == null) {
                setHeaderOptionParser(new IPHeaderOption.Parser());
            }
            this.headerOptionParser.add(optionCode, parser);
        }

        public void remove(Object optionCode) {
            if (this.headerOptionParser != null) {
                this.headerOptionParser.remove(optionCode);
            }
        }

        public abstract IPv6OptionsHeader constructHeader(ByteBuffer buffer) throws ParseException;

        @Override
        public IPMessage parse(ByteBuffer buffer) throws ParseException, MissingParserException {
            IPv6OptionsHeader header = constructHeader(buffer);

            // Parse IP header options
            header.parseOptions(this.headerOptionParser);

            return header;
        }

    }


    /*-- Member Variables---------------------------------------------------*/

    protected ByteBuffer unparsedOptions = null;

    protected Vector<IPHeaderOption> options = null;
    

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param protocolNumber
     */
    protected IPv6OptionsHeader(byte protocolNumber) {
        super(protocolNumber);
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(Logging.entering(ObjectId, "IPv6OptionsHeader.IPv6OptionsHeader", protocolNumber));
        }
    }

    /**
     * 
     * @param buffer
     * @param protocolNumber
     * @throws ParseException
     */
    public IPv6OptionsHeader(ByteBuffer buffer, byte protocolNumber) throws ParseException {
        super(consume(buffer,BASE_HEADER_LENGTH), protocolNumber);
        
        this.unparsedOptions = consume(buffer, HeaderLength.get(getBufferInternal()) - BASE_HEADER_LENGTH);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6OptionsHeader.IPv6OptionsHeader", buffer));
        }
    }
        
    @Override
    public void writeTo(ByteBuffer buffer) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6OptionsHeader.writeTo", buffer));
        }
        
        //Precondition.checkBounds(buffer.length, offset, getTotalLength());
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
        for (int i=0; i<padding; i++) {
            buffer.put((byte)0);
        }
    }
    
    @Override
    public void setProtocolNumber(byte protocolNumber) {
        // Do nothing - value is set by constructors
    }

    /**
     * Gets the computed length of the header including options expressed in
     * bytes. This value is stored in the header as the length in 8-byte words minus 8 bytes.
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
     * 
     * @param option
     */
    public final void addOption(IPHeaderOption option) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6OptionsHeader.addOption", option));
        }
        
        if (this.options == null) this.options = new Vector<IPHeaderOption>();
        this.options.add(option);
        updateHeaderLength();
    }

    /**
     * 
     * @param option
     */
    public final void removeOption(IPHeaderOption option) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6OptionsHeader.removeOption", option));
        }
        
        this.options.remove(option);
        updateHeaderLength();
    }

    /**
     * Calculates the total bytes required by all options currently attached to
     * the packet header.
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
     * 
     * @param parser
     * @throws ParseException
     * @throws MissingParserException
     */
    public void parseOptions(IPHeaderOption.Parser parser) throws ParseException, MissingParserException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6OptionsHeader.parseOptions", parser));
        }
        
        //Precondition.checkReference(parser);
        if (this.unparsedOptions != null) {

            if (this.options == null) {
                this.options = new Vector<IPHeaderOption>();
            }
            else {
                this.options.clear();
            }

            while(this.unparsedOptions.limit() > 0) {
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
     * 
     * @return
     */
    public Enumeration<IPHeaderOption> getOptions() {
        return this.options != null ? this.options.elements() : null;
    }

    /**
     * Calculates the number of zero-padding bytes required to make IP header
     * end on 64-bit word boundary.
     */
    public int getPaddingLength() {
        return getHeaderLength() - BASE_HEADER_LENGTH - getOptionsLength();
    }

}
