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

package com.larkwoodlabs.net.ip.ipv6;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPExtensionHeader;
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.util.buffer.fields.BooleanField;
import com.larkwoodlabs.util.buffer.fields.IntegerField;
import com.larkwoodlabs.util.buffer.fields.ShortBitField;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

/**
 * The Fragment header is used by an IPv6 source to send packets larger
 * than would fit in the path MTU. The Fragment
 * header is identified by a Next Header value of 44 in the
 * immediately preceding header, and has the following format:
 * <pre>
 *  0               1               2               3          
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Next Header  |   Reserved    |      Fragment Offset    |Res|M|
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Identification                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre> 
 * <dl>
 * <dt>Next Header: 8-bits<dd>
 *  
 *      Identifies the initial header type of the Fragmentable Part
 *      of the original packet (defined below).  Uses the same values
 *      as the IPv4 Protocol field [RFC-1700 et seq.].<p>
 * 
 * <dt>Reserved: 8-bits<d>
 *
 *      Reserved field.  Initialized to zero for distribution; ignored on reception.<p>
 * 
 * <dt>Fragment Offset: 13-bits<dd>
 *
 *      The offset, in 8-octet units, of the data following this header,
 *      relative to the start of the Fragmentable Part of the original packet.<p>
 * 
 * <dt>Res: 2-bits<dd>
 * 
 *      Reserved field.  Initialized to zero for distribution; ignored on reception.<p>
 * 
 * <dt>More Fragments flag: 1 bit<dd>
 *
 *      1 = more fragments; 0 = last fragment.<p>
 * 
 * <dt>Identification: 32 bits<dd>
 * 
 *      For every packet that is to be fragmented, the source node generates
 *      an Identification value. The Identification must be different than
 *      that of any other fragmented packet sent recently* with the same
 *      Source Address and Destination Address.  If a Routing header is
 *      present, the Destination Address of concern is that of the final
 *      destination.<p>
 * </dl>
 * 
 * @author Gregory Bumgardner
 * 
 */
public final class IPv6FragmentHeader extends IPExtensionHeader {

    /*-- Inner Classes ---------------------------------------------------*/

    /**
     * 
     */
    public static class Parser implements IPMessage.ParserType {

        @Override
        public IPMessage parse(final ByteBuffer buffer) throws ParseException {
            return new IPv6FragmentHeader(buffer);
        }

        @Override
        public boolean verifyChecksum(final ByteBuffer buffer,
                                      final byte[] sourceAddress,
                                      final byte[] destinationAddress) throws MissingParserException, ParseException {
            return true; // Does nothing in this class
        }

        @Override
        public Object getKey() {
            return IP_PROTOCOL_NUMBER;
        }

    }


    /*-- Static Variables ---------------------------------------------------*/

    /** Protocol number for IPv6 Fragment Headers */
    public static final byte IP_PROTOCOL_NUMBER = 44;

    /** */
    public static final ShortBitField   FragmentOffset = new ShortBitField(2,3,13);
    /** */
    public static final BooleanField    MoreFragments = new BooleanField(3,0);
    /** */
    public static final IntegerField    Identification = new IntegerField(4);

    /** */
    public static final int BASE_HEADER_LENGTH = 8;
    
    /*-- Member Variables ---------------------------------------------------*/

    /** */
    ByteBuffer fragment;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param fragmentOffset
     * @param moreFragments
     * @param identification
     */
    public IPv6FragmentHeader(final short fragmentOffset,
                              final boolean moreFragments,
                              final int identification) {
        super(IP_PROTOCOL_NUMBER);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6FragmentHeader.IPv6FragmentHeader", fragmentOffset, moreFragments, identification));
        }
        
        setFragmentOffset(fragmentOffset);
        setMoreFragments(moreFragments);
        setIdentification(identification);
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public IPv6FragmentHeader(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer,BASE_HEADER_LENGTH), IP_PROTOCOL_NUMBER);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6FragmentHeader.IPv6FragmentHeader", buffer));
        }
        
        this.fragment = consume(buffer, buffer.remaining());

        if (logger.isLoggable(Level.FINER)) {
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
     * @param logger
     */
    private void logState(final Logger logger) {
        logger.fine(ObjectId + " : more-fragments="+getMoreFragments());
        logger.fine(ObjectId + " : fragment-offset="+getFragmentOffset());
        logger.fine(ObjectId + " : identification="+getIdentification());
        logger.fine(ObjectId + " : fragment array=" + this.fragment.array() +
                               " offset=" + this.fragment.arrayOffset() +
                               " limit=" + this.fragment.limit());
    }

    /**
     * Gets the total length of this header in bytes.
     * Some extension headers override this method to return a fixed value.
     */
    @Override
    public final int getHeaderLength() {
        return BASE_HEADER_LENGTH;
    }

    /**
     * 
     * @return
     */
    public ByteBuffer getFragment() {
        return this.fragment.slice();
    }

    /**
     * 
     * @return
     */
    public short getFragmentOffset() {
        return FragmentOffset.get(getBufferInternal());
    }
    
    /**
     * 
     * @param offset
     */
    public void setFragmentOffset(final short offset) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6FragmentHeader.setFragmentOffset", offset));
        }

        FragmentOffset.set(getBufferInternal(), offset);
    }

    /**
     * 
     * @return
     */
    public boolean getMoreFragments() {
        return MoreFragments.get(getBufferInternal());
    }

    /**
     * 
     * @param moreFragments
     */
    public void setMoreFragments(final boolean moreFragments) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6FragmentHeader.setMoreFragments", moreFragments));
        }
        
        MoreFragments.set(getBufferInternal(), moreFragments);
    }

    /**
     * 
     * @return
     */
    public int getIdentification() {
        return Identification.get(getBufferInternal());
    }
    
    /**
     * 
     * @param identification
     */
    public void setIdentification(final int identification) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IPv6FragmentHeader.setIdentification", identification));
        }
        
        Identification.set(getBufferInternal(), identification);
    }
}
