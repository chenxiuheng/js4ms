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

package com.larkwoodlabs.net.streaming.rtp;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.net.KeyedApplicationMessage;
import com.larkwoodlabs.util.buffer.BufferBackedObject;
import com.larkwoodlabs.util.buffer.fields.BooleanField;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.buffer.fields.SelectorField;
import com.larkwoodlabs.util.buffer.fields.ShortField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Base class for RTCP Messages. Handles common RTCP header serialization.
 * See <a href="http://www.ietf.org/rfc/rfc1889.txt">[RFC-1889]</a>
 * <h2>6.1 RTCP Packet Format</h2>
 * This specification defines several RTCP packet types to carry a
 * variety of control information:<p>
 * <blockquote><dl>
 *   <dt>SR: Sender report ({@link RtcpSenderReportPacket})
 *   <dd>Contains transmission and reception statistics from
 *       participants that are active senders.
 *   <dt>RR: Receiver report ({@link RtcpReceiverReportPacket})
 *   <dd>Contains reception statistics from participants that
 *       are not active senders.
 *   <dt>SDES: Source description ({@link RtcpSourceDescriptionPacket})
 *   <dd>Contains descriptive text items, including CNAME.
 *   <dt>BYE: Goodbye ({@link RtcpGoodbyePacket})
 *   <dd>Indicates end of participation.
 *   <dt>APP: Application defined ({@link RtcpApplicationDefinedPacket})
 *   <dd>Contains application specific messages or functions.
 * </dl></blockquote>
 * Each RTCP packet begins with a fixed part similar to that of RTP data
 * packets, followed by structured elements that may be of variable
 * length according to the packet type but always end on a 32-bit
 * boundary. The alignment requirement and a length field in the fixed
 * part are included to make RTCP packets "stackable". Multiple RTCP
 * packets may be concatenated without any intervening separators to
 * form a compound RTCP packet that is sent in a single packet of the
 * lower layer protocol, for example UDP. There is no explicit count of
 * individual RTCP packets in the compound packet since the lower layer
 * protocols are expected to provide an overall length to determine the
 * end of the compound packet.<p>
 * 
 * The fixed part of every RTCP packet has the following format:
 * <pre>
 *     0               1               2               3
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |V=2|P|   TBD   |       PT      |             length            |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * <h3>version (V): 2 bits</h3>
 *   Identifies the version of RTP, which is the same in RTCP packets
 *   as in RTP data packets. The version defined by this
 *   specification is two (2).
 * 
 * <h3>padding (P): 1 bit</h3>
 *   If the padding bit is set, this RTCP packet contains some
 *   additional padding octets at the end which are not part of the
 *   control information. The last octet of the padding is a count of
 *   how many padding octets should be ignored. Padding may be needed
 *   by some encryption algorithms with fixed block sizes. In a
 *   compound RTCP packet, padding should only be required on the
 *   last individual packet because the compound packet is encrypted
 *   as a whole.
 * 
 * <h3>TBD: 5 bits</h3>
 *   Contains value whose interpretation varies with packet type.
 * 
 * <h3>packet type (PT): 8 bits</h3>
 *   Identifies the RTCP packet type:
 * <pre>
 *      200 - Sender Report (SR)
 *      201 - Receiver report (RR)
 *      202 - Source description (SDES)
 *      203 - Goodbye (BYE)
 *      204 - Application-defined (APP)
 * </pre>
 * <h3>length: 16 bits</h3>
 *   The length of this RTCP packet in 32-bit words minus one,
 *   including the header and any padding. (The offset of one makes
 *   zero a valid length and avoids a possible infinite loop in
 *   scanning a compound RTCP packet, while counting 32-bit words
 *   avoids a validity check for a multiple of 4.)<p>
 *   
 * @see {@link RtcpSenderReportPacket}
 * @see {@link RtcpReceiverReportPacket}
 * @see {@link RtcpSourceDescriptionPacket}
 * @see {@link RtcpGoodbyePacket}
 * @see {@link RtcpApplicationDefinedPacket}
 * 
 * @author Gregory Bumgardner
 */
public abstract class RtcpPacket extends BufferBackedObject implements KeyedApplicationMessage<Byte> {

    /*-- Inner Classes ------------------------------------------------------*/

    public static interface ParserType extends KeyedApplicationMessage.ParserType {

    }

    public static class Parser extends KeyedApplicationMessage.Parser {

        public Parser() {
            super(new SelectorField<Byte>(RtcpPacket.PacketType));
        }

    }

    /*-- Static Variables ---------------------------------------------------*/
    
    public static final Logger logger = Logger.getLogger(RtcpPacket.class.getName());

    public static final ByteField PacketType = new ByteField(1);
    public static final BooleanField Padding = new BooleanField(0,5);
    public static final ShortField Length = new ShortField(2);
    
    public static int BASE_HEADER_LENGTH = 4;

    /*-- Static Functions ---------------------------------------------------*/

    public static RtcpPacket.Parser constructRtcpPacketParser() {
        RtcpPacket.Parser parser = new RtcpPacket.Parser();
        parser.add(RtcpSenderReportPacket.constructParser());
        parser.add(RtcpReceiverReportPacket.constructParser());
        parser.add(RtcpSourceDescriptionPacket.constructParser());
        parser.add(RtcpGoodbyePacket.constructParser());
        parser.add(RtcpApplicationDefinedPacket.constructParser());
        return parser;
    }

    /*-- Member Functions ---------------------------------------------------*/

    protected RtcpPacket(int size, byte type) {
        super(size);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpPacket.RtcpPacket", size,type));
        }
        
        setType(type);
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    protected RtcpPacket(ByteBuffer buffer) {
        super(buffer);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpPacket.RtcpPacket", buffer));
            logState(logger);
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void log(Logger logger) {
        super.log(logger);
        logState(logger);
    }
    
    /**
     * Logs value of member variables declared or maintained by this class.
     * @param logger
     */
    private void logState(Logger logger) {
        logger.info(ObjectId + " : packet-length="+getTotalLength());
        logger.info(ObjectId + " : packet-type="+(getType() & 0xFF));
        logger.info(ObjectId + " : padding="+getPadding());
    }

    @Override
    public Byte getType() {
        return PacketType.get(getBufferInternal());
    }
    
    /**
     * 
     * @param type
     */
    protected void setType(byte type) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.fine(Logging.entering(ObjectId, "RtcpPacket.setType", type));
        }
        
        PacketType.set(getBufferInternal(),type);
    }

    /**
     * 
     * @return
     */
    public boolean getPadding() {
        return Padding.get(getBufferInternal());
    }

    /**
     * 
     * @param padding
     */
    public void setPadding(boolean padding) {
        Padding.set(getBufferInternal(),padding);
    }

    /**
     * Returns the length of the RTCP in 32-bit words not including the 4 byte header.
     * @return
     */
    public Short getLength() {
        return Length.get(getBufferInternal());
    }

    /**
     * 
     * @param length
     */
    protected void setLength(short length) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.fine(Logging.entering(ObjectId, "RtcpPacket.setLength", length));
        }
        
        Length.set(getBufferInternal(),length);
    }

    /**
     * 
     * @param byteCount
     */
    protected void setTotalLength(int byteCount) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.fine(Logging.entering(ObjectId, "RtcpPacket.setTotalByteLength", byteCount));
        }
        
        Length.set(getBufferInternal(),(short)((byteCount / 4) - 1));
    }

    @Override
    public int getHeaderLength() {
        return BASE_HEADER_LENGTH;
    }

}
