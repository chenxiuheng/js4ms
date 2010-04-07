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

package com.larkwoodlabs.net.ip;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.BufferBackedObject;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

/**
 * <pre>
 *  0               1               2               3
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Next Header  |  Hdr Ext Len  |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                               +
 * |                                                               |
 * .                                                               .
 * .                                                               .
 * .                                                               .
 * |                                                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * <h2>Next Header Type: 8-bit</h2>
 * 
 *      Identifies the type of header immediately following the 
 *      extension header.  Uses the same values as the IPv4
 *      Protocol field [RFC-1700].
 *
 * <h2>Header Length: 8-bit</h2>
 *      Length of the Hop-by-Hop Options header in 8-octet units,
 *      not including the first 8 octets.
 * 
 * @author Gregory Bumgardner
 * 
 */
public class IPExtensionHeader extends BufferBackedObject implements IPMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser implements IPMessage.ParserType {

        @Override
        public IPExtensionHeader parse(ByteBuffer buffer) throws ParseException {
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

    public static final Logger logger = Logger.getLogger(IPExtensionHeader.class.getName());

    public static final ByteField NextHeader = new ByteField(0); 
    public static final ByteField HeaderLength = new ByteField(1); 

    protected static final int BASE_HEADER_LENGTH = 2;
    protected static final int MIN_HEADER_LENGTH = 8;
    
    /*-- Member Variables ---------------------------------------------------*/

    private byte protocolNumber;
    
    private IPMessage nextProtocolHeader = null;


    /*-- Member Functions ---------------------------------------------------*/

    protected IPExtensionHeader(byte protocolNumber) {
        super(MIN_HEADER_LENGTH);
        this.protocolNumber = protocolNumber;
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPExtensionHeader.IPv6ExtensionHeader", protocolNumber));
            logState(logger);
        }
    }

    protected IPExtensionHeader(int size, byte protocolNumber) {
        super(size);
        this.protocolNumber = protocolNumber;
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPExtensionHeader.IPv6ExtensionHeader", size, protocolNumber));
            logState(logger);
        }
    }


    /**
     * Constructs an extension header object.
     * This constructor can be used to create a header object that provides
     * an opaque representation of header content. 
     */
    public IPExtensionHeader(final ByteBuffer buffer) throws ParseException {
        this(consume(buffer, HeaderLength.get(buffer)*8+8), (byte)0);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPExtensionHeader.IPv6ExtensionHeader", buffer));
            logState(logger);
        }
    }

    /**
     * Used by derived classes to construct an extension header object.
     */
    protected IPExtensionHeader(final ByteBuffer buffer, final byte protocolNumber) {
        super(buffer);
        this.protocolNumber = protocolNumber;
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPExtensionHeader.IPv6ExtensionHeader", buffer));
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
        logger.info(ObjectId + " : protocol="+getProtocolNumber());
        logger.info(ObjectId + " : next-header="+getNextProtocolNumber());
        logger.info(ObjectId + " : header-length="+getHeaderLength());
    }


    @Override
    public final void writeChecksum(final ByteBuffer buffer, final byte[] sourceAddress, final byte[] destinationAddress) {
 
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "IPExtensionHeader.writeChecksum",
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
            logger.finer(Logging.entering(ObjectId, "IPExtensionHeader.setProtocolNumber", protocolNumber));
        }
        
        this.protocolNumber = protocolNumber;
    }

    /**
     * Gets the assigned protocol number for the next header.
     * See protocol field in [RFC-1700].
     */
    @Override
    public final byte getNextProtocolNumber() {
        return NextHeader.get(getBufferInternal());
    }

    protected final void setNextProtocolNumber(final byte protocolNumber) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPExtensionHeader.setNextProtocolNumber", protocolNumber));
        }
        
        NextHeader.set(getBufferInternal(), protocolNumber);
    }

    /**
     * Gets the total length of this header in bytes.
     * Some extension headers override this method to return a fixed value.
     */
    @Override
    public int getHeaderLength() {
        return HeaderLength.get(getBufferInternal())*8 + 8;
    }
    
    /**
     * Sets the total length of this header in bytes.
     * Must be multiple of 8 and greater than 8 since minimum extension header size is 8 octets.
     */
    protected final void setHeaderLength(final int length) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPExtensionHeader.setHeaderLength", length));
        }
        
        HeaderLength.set(getBufferInternal(), (byte)Math.min((length / 8) - 1,0));
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
            logger.finer(Logging.entering(ObjectId, "IPExtensionHeader.setNextMessage", nextProtocolHeader));
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
            logger.finer(Logging.entering(ObjectId, "IPExtensionHeader.removeNextMessage"));
        }
        
        IPMessage nextMessage = getNextMessage();
        if (nextMessage != null) {
            setNextMessage(nextMessage.getNextMessage());
            nextMessage.setNextMessage(null);
        }
    }

}
