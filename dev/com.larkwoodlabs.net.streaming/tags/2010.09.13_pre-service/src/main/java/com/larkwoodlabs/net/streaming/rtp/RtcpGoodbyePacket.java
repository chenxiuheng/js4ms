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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.fields.ByteBitField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An RTCP Goodbye (BYE) message.
 * See <a href="http://www.ietf.org/rfc/rfc1889.txt">[RFC-1889]</a>
 * <h2>6.5 BYE: Goodbye RTCP packet</h2> 
 * <pre>
 *   0               1               2               3
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |V=2|P|    SC   |   PT=BYE=203  |             length            | 1) Header
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                           SSRC/CSRC                           | 2) SSRC/CSRC
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+    list
 *  :                              ...                              :
 *  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 *  |     length    |               reason for leaving           ...  3) Reason (opt)
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * <h2>1) Packet Header</h2>
 * The first section, the header, is 8 octets long. The fields have the
 * following meaning:
 * 
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
 * <h3>source count (SC): 5 bits</h3>
 *   The number of reception SSRC/CSRC identifiers contained in this packet.
 *   A value of zero is valid but useless.
 * 
 * <h3>packet type (PT): 8 bits</h3>
 *   Contains the constant 203 to identify this as an RTCP BYE packet.
 * 
 * <h3>length: 16 bits</h3>
 *   The length of this RTCP packet in 32-bit words minus one,
 *   including the header and any padding. (The offset of one makes
 *   zero a valid length and avoids a possible infinite loop in
 *   scanning a compound RTCP packet, while counting 32-bit words
 *   avoids a validity check for a multiple of 4.)
 * 
 * <h2>2) SSRC/CSRC List: SC * 32 bits</h2>
 *   A list of identifiers for synchronization sources that are
 *   shutting down.
 * <h2>3) Reason for Leaving: 1 + (length * 8 bits)</h2>
 *   The BYE packet may include an 8-bit octet count followed by that
 *   many octets of text indicating the reason for leaving, e.g.,
 *   "camera malfunction" or "RTP loop detected". The string has the
 *   same encoding as that described for SDES (UTF-8). If the string
 *   fills the packet to the next 32-bit boundary, the string is not
 *   null terminated. If not, the BYE packet is padded with null octets.
 * <p>
 * 
 * @author Gregory Bumgardner
 */
public final class RtcpGoodbyePacket extends RtcpPacket {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser implements RtcpPacket.ParserType {

        @Override
        public RtcpPacket parse(ByteBuffer buffer) throws ParseException {
            return new RtcpGoodbyePacket(buffer);
        }

        @Override
        public Object getKey() {
            return PACKET_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    public final static ByteBitField SourceCount = new ByteBitField(0,0,5);

    public final static byte PACKET_TYPE = (byte)203;
    
    public final static int BASE_MESSAGE_LENGTH = 4;
    

    /*-- Static Functions ---------------------------------------------------*/

    public static RtcpGoodbyePacket.Parser constructParser() {
        return new RtcpGoodbyePacket.Parser();
    }

    
    /*-- Member Variables ---------------------------------------------------*/

    private Vector<Integer> sourceIdentifiers = new Vector<Integer>();
    
    private ByteBuffer reason;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     */
    public RtcpGoodbyePacket() {
        super(BASE_MESSAGE_LENGTH, PACKET_TYPE);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpGoodbyePacket.RtcpGoodbyePacket"));
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public RtcpGoodbyePacket(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpGoodbyePacket.RtcpGoodbyePacket", buffer));
        }
        
        int count = SourceCount.get(getBufferInternal());
        
        for (int i = 0; i < count; i++) {
            this.sourceIdentifiers.add(buffer.getInt());
        }

        if (((Length.get(getBufferInternal()) + 1) * 4) > BASE_MESSAGE_LENGTH + count * 4) {
            int reasonLength = buffer.get();
            this.reason = buffer.slice();
            this.reason.limit(reasonLength);
        }

        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
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
        logger.info(ObjectId + " : ----> Source Identifiers");
        for (Integer identifier : this.sourceIdentifiers) {
            logger.info(ObjectId + " : " + String.format("%08X", identifier));
        }
        logger.info(ObjectId + " : <---- Source Identifiers");
    }

    @Override
    public void writeTo(ByteBuffer buffer) {
        super.writeTo(buffer);
        
        for (Integer identifier : this.sourceIdentifiers ) {
            buffer.putInt(identifier);
        }
        
        if (this.reason != null) {
            buffer.put((byte)this.reason.limit());
            buffer.put(this.reason);
            this.reason.rewind();

            // Pad to next 32-bit word boundary.
            while((buffer.position() % 4) != 0) {
                buffer.put((byte)0);
            }
        }
    }

    @Override
    public int getTotalLength() {
        return BASE_MESSAGE_LENGTH + getSourceCount() * 4 + (this.reason != null ? this.reason.limit() + 1: 0);
    }

    @Override
    public Byte getType() {
        return PACKET_TYPE;
    }
    
    /**
     * 
     * @return
     */
    public int getSourceCount() {
        return SourceCount.get(getBufferInternal());
    }
    
    /**
     * 
     * @param count
     */
    protected void setSourceCount(int count) {
        SourceCount.set(getBufferInternal(), (byte)count);
    }

    /**
     * 
     * @return
     */
    public String getReason() {
        if (this.reason == null) {
            return "";
        }
        else {
            try {
                return new String(this.reason.array(),
                                  this.reason.arrayOffset(),
                                  this.reason.limit(),
                                  "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new Error(e);
            }
        }
    }
    
    /**
     * 
     * @param reason
     */
    public void setReason(String reason) {
        try {
            byte[] bytes = reason.getBytes("UTF-8");
            int length = bytes.length > 255 ? 255 : bytes.length;
            this.reason = ByteBuffer.wrap(bytes, 0, length);
            this.setTotalLength(getTotalLength());
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }
    
    /**
     * 
     * @param identifier
     */
    public void addIdentifier(int identifier) {
        this.sourceIdentifiers.add(identifier);
        setSourceCount(this.sourceIdentifiers.size());
        setTotalLength(getTotalLength());
    }
    
    /**
     * 
     * @return
     */
    public Iterator<Integer> getIdentifierIterator() {
        return this.sourceIdentifiers.iterator();
    }
}
