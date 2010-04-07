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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.fields.ByteBitField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An RTCP Source Description (SDES) message.
 * See <a href="http://www.ietf.org/rfc/rfc1889.txt">[RFC-1889]</a>
 * <h2>6.4 SDES:Source description RTCP packet</h2>
 * The SDES packet is a three-level structure composed of a header and
 * zero or more chunks, each of of which is composed of items describing
 * the source identified in that chunk.
 * <pre>
 *  0               1               2               3
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V=2|P|    SC   |  PT=SDES=202  |             length            | 1) Header
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * |                          SSRC/CSRC_1                          | 2) Chunk
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+  
 * |                           SDES items                          | < Chunk 1
 * |                              ...                              |
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * |                          SSRC/CSRC_2                          | 
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ 
 * |                           SDES items                          | < Chunk 2..N
 * |                              ...                              |
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * </pre>
 * <h2>1) Header</h2>
 * <h3>version (V), padding (P), length:</h3>
 *   As described for the SR packet (see Section 6.3.1).
 * 
 * <h3>packet type (PT): 8 bits</h3>
 *   Contains the constant 202 to identify this as an RTCP SDES packet.
 * 
 * <h3>source count (SC): 5 bits</h3>
 *   The number of SSRC/CSRC chunks contained in this SDES packet.
 *   A value of zero is valid but useless.
 *   
 * <h2>2) Chunk(s)</h2>
 * Each chunk consists of an SSRC/CSRC identifier followed by a list of
 * zero or more items, which carry information about the SSRC/CSRC. Each
 * chunk starts on a 32-bit boundary. Each item consists of an 8-bit
 * type field, an 8-bit octet count describing the length of the text
 * (thus, not including this two-octet header), and the text itself.
 * Note that the text can be no longer than 255 octets, but this is
 * consistent with the need to limit RTCP bandwidth consumption.
 * <p>
 * The text is encoded according to the UTF-2 encoding specified in
 * Annex F of ISO standard 10646 [12,13]. This encoding is also known as
 * UTF-8 or UTF-FSS. It is described in "File System Safe UCS
 * Transformation Format (FSS_UTF)", X/Open Preliminary Specification,
 * Document Number P316 and Unicode Technical Report #4. US-ASCII is a
 * subset of this encoding and requires no additional encoding. The
 * presence of multi-octet encodings is indicated by setting the most
 * significant bit of a character to a value of one.
 * <p>
 * Items are contiguous, i.e., items are not individually padded to a
 * 32-bit boundary. Text is not null terminated because some multi-octet
 * encodings include null octets. The list of items in each chunk is
 * terminated by one or more null octets, the first of which is
 * interpreted as an item type of zero to denote the end of the list,
 * and the remainder as needed to pad until the next 32-bit boundary. A
 * chunk with zero items (four null octets) is valid but useless.
 * <p>
 * End systems send one SDES packet containing their own source
 * identifier (the same as the SSRC in the fixed RTP header). A mixer
 * sends one SDES packet containing a chunk for each contributing source
 * from which it is receiving SDES information, or multiple complete
 * SDES packets in the format above if there are more than 31 such
 * sources (see Section 7).
 * <p>
 * @see {@link RtcpSourceDescriptionChunk}
 * @see {@link RtcpSourceDescriptionItem}
 * 
 * @author Gregory Bumgardner
 */
public final class RtcpSourceDescriptionPacket extends RtcpPacket {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser implements RtcpPacket.ParserType {

        @Override
        public RtcpPacket parse(ByteBuffer buffer) throws ParseException {
            return new RtcpSourceDescriptionPacket(buffer);
        }

        @Override
        public Object getKey() {
            return PACKET_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    public final static ByteBitField SourceCount = new ByteBitField(0,0,5);

    public final static byte PACKET_TYPE = (byte)202;
    
    public final static int BASE_MESSAGE_LENGTH = 4;

    /*-- Static Functions ---------------------------------------------------*/

    public static RtcpSourceDescriptionPacket.Parser constructParser() {
        return new RtcpSourceDescriptionPacket.Parser();
    }

    
    /*-- Member Variables ---------------------------------------------------*/

    LinkedList<RtcpSourceDescriptionChunk> chunks = new LinkedList<RtcpSourceDescriptionChunk>();

    /*-- Member Functions ---------------------------------------------------*/

    public RtcpSourceDescriptionPacket() {
        super(BASE_MESSAGE_LENGTH, PACKET_TYPE);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpSourceDescriptionPacket.RtcpSourceDescriptionPacket"));
            logState(logger);
        }
    }

    public RtcpSourceDescriptionPacket(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpSourceDescriptionPacket.RtcpSourceDescriptionPacket", buffer));
        }

        int count = SourceCount.get(getBufferInternal());
        
        for (int i = 0; i < count; i++) {
            this.chunks.add(new RtcpSourceDescriptionChunk(buffer));
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
        logger.info(ObjectId + " : source-count=" + getSourceCount());
        logger.info(ObjectId + " : ----> Chunks");
        for (RtcpSourceDescriptionChunk chunk : this.chunks) {
            chunk.log(logger);
        }
        logger.info(ObjectId + " : <---- Chunks");
    }

    @Override
    public void writeTo(ByteBuffer buffer) {
        super.writeTo(buffer);
        for (RtcpSourceDescriptionChunk chunk : this.chunks) {
            chunk.writeTo(buffer);
        }
    }

    @Override
    public int getTotalLength() {
        int totalLength = BASE_MESSAGE_LENGTH;
        for (RtcpSourceDescriptionChunk chunk : this.chunks) {
            totalLength += chunk.getTotalLength();
        }
        return totalLength;
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
     * @param chunk
     */
    public void addChunk(RtcpSourceDescriptionChunk chunk) {
        this.chunks.add(chunk);
        setSourceCount(this.chunks.size());
        setTotalLength(getTotalLength());
    }
    
    /**
     * 
     * @return
     */
    public Iterator<RtcpSourceDescriptionChunk> getChunkIterator() {
        return this.chunks.iterator();
    }
    
}
