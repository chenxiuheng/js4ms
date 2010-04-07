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

import com.larkwoodlabs.util.buffer.BufferBackedObject;
import com.larkwoodlabs.util.buffer.fields.IntegerField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An RTCP Source Description "chunck".
 * See <a href="http://www.ietf.org/rfc/rfc1889.txt">[RFC-1889]</a>
 * <pre>
 *  0               1               2               3
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           SSRC/CSRC                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+  
 * |                           SDES items                          |
 * |                              ...                              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+  
 * </pre>
 * Each chunk consists of an SSRC/CSRC identifier followed by a list of
 * zero or more items, which carry information about the SSRC/CSRC.
 * Each chunk starts on a 32-bit boundary.
 * <p>
 * @see {@link RtcpSourceDescriptionPacket}
 * @see {@link RtcpSourceDescriptionItem}
 * @author Gregory Bumgardner
 */
public final class RtcpSourceDescriptionChunk extends BufferBackedObject {

    /*-- Static Variables ---------------------------------------------------*/

    public final static Logger logger = RtcpPacket.logger;
    
    public final static IntegerField SourceIdentifier = new IntegerField(0);

    public final static int BASE_CHUNK_LENGTH = 4;


    /*-- Member Variables ---------------------------------------------------*/

    LinkedList<RtcpSourceDescriptionItem> items = new LinkedList<RtcpSourceDescriptionItem>();
    

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     */
    public RtcpSourceDescriptionChunk() {
        super(BASE_CHUNK_LENGTH);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpReceptionReport.RtcpReceptionReport"));
            logState(logger);
        }
    }

    /**
     * 
     * @param sourceIdentifier
     */
    public RtcpSourceDescriptionChunk(int sourceIdentifier) {
        super(BASE_CHUNK_LENGTH);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "RtcpReceptionReport.RtcpReceptionReport",
                                          sourceIdentifier));
        }
        
        setSourceIdentifier(sourceIdentifier);
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    public RtcpSourceDescriptionChunk(ByteBuffer buffer) {
        super(consume(buffer, BASE_CHUNK_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpReceptionReport.RtcpReceptionReport"));
        }

        int startPosition = buffer.position();
        
        // Read the variable length items - items end when item type == 0.
        while (buffer.get(buffer.position()) != 0) {
            RtcpSourceDescriptionItem item = new RtcpSourceDescriptionItem(buffer);
            this.items.add(item);
        }
        
        // Remove the terminating item type zero.
        buffer.get();

        // Chunk must end on 32-bit boundary, but item lengths are variable.
        // Advance the buffer position to next 32-bit word boundary if necessary.
        int padding = (4 - ((buffer.position() - startPosition) % 4)) % 4;
        buffer.position(buffer.position() + padding);

        if (logger.isLoggable(Level.FINER)) {
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
        logger.info(ObjectId + " : source-identifier=" + String.format("%08X", getSourceIdentifier()));
        logger.info(ObjectId + " : ----> Source Description Items");
        for (RtcpSourceDescriptionItem item : this.items) {
            item.log(logger);
        }
        logger.info(ObjectId + " : <---- Source Description Items");
    }

    @Override
    public void writeTo(ByteBuffer buffer) {

        super.writeTo(buffer);

        int startPosition = buffer.position();

        for (RtcpSourceDescriptionItem item : this.items) {
            item.writeTo(buffer);
        }

        // Write last item token
        buffer.put((byte)0);
        
        // Pad to next 32-bit word boundary.
        while(((buffer.position() - startPosition) % 4) != 0) {
            buffer.put((byte)0);
        }
    }

    /**
     * 
     * @return
     */
    public int getTotalLength() {
        int totalLength = BASE_CHUNK_LENGTH;
        for (RtcpSourceDescriptionItem item : this.items) {
            totalLength += item.getTotalLength();
        }
        // For last item token
        totalLength += 1;
        // For padding to next word
        totalLength += (totalLength % 4);
        return totalLength;
    }
    
    /**
     * 
     * @return
     */
    public int getSourceIdentifier() {
        return SourceIdentifier.get(getBufferInternal());
    }
    
    /**
     * 
     * @param sourceIdentifier
     */
    public void setSourceIdentifier(int sourceIdentifier) {
        SourceIdentifier.set(getBufferInternal(), sourceIdentifier);
    }

    /**
     * 
     * @param item
     */
    public void addItem(RtcpSourceDescriptionItem item) {
        this.items.add(item);
    }
    
    /**
     * 
     * @return
     */
    public Iterator<RtcpSourceDescriptionItem> getItemIterator() {
        return this.items.iterator();
    }
}
