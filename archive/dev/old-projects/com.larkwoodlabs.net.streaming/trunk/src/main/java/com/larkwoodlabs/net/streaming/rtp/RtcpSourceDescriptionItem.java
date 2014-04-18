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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.buffer.BufferBackedObject;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An RTCP Source Description Item.
 * See <a href="http://www.ietf.org/rfc/rfc1889.txt">[RFC-1889]</a>
 * <h2>SDES Item</h2>
 * Items are contiguous, i.e., items are not individually padded to a
 * 32-bit boundary. Text is not null terminated because some multi-octet
 * encodings include null octets. The list of items in each chunk is
 * terminated by one or more null octets, the first of which is
 * interpreted as an item type of zero to denote the end of the list,
 * and the remainder as needed to pad until the next 32-bit boundary. A
 * chunk with zero items (four null octets) is valid but useless.
 * <pre>
 *   0               1               2               
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-/ /-+-+-+
 *  |      type     |     length    |     ...     |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-/ /-+-+-+
 * </pre>
 * 
 * <h3>Item type: 8-bits</h3>
 * Identifies the type of information contained the item:
 * <pre>
 *  1 = CNAME - Canonical end-point identifier
 *  2 = NAME  - User name
 *  3 = EMAIL - Electronic mail address
 *  4 = PHONE - Phone number
 *  5 = LOC   - Geographic user location
 *  6 = TOOL  - Application or tool name
 *  7 = NOTE  - Notice/status
 *  8 = PRIV  - Private extensions
 * </pre>
 * <h3>length: 8-bits</h3>
 * An  octet count describing the length of the text, not including
 * the two-octet header.
 * 
 * <h3>text: 0-255 bytes</h3>
 * The item text. Note that the text can be no longer than 255 octets,
 * but this is consistent with the need to limit RTCP bandwidth consumption.
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
 * @see {@link RtcpSourceDescriptionPacket}
 * @see {@link RtcpSourceDescriptionChunk}
 * 
 * @author Gregory Bumgardner
 */
public class RtcpSourceDescriptionItem extends BufferBackedObject {

    /*-- Inner Classes ------------------------------------------------------*/

    public enum ItemType {
        UNDEFINED(0),
        CNAME(1),
        NAME(2),
        EMAIL(3),
        PHONE(4),
        LOC(5),
        TOOL(6),
        NOTE(7),
        PRIV(8);

        private int type;

        private ItemType(int type) {
            this.type = type;
        }

        public int getValue() {
            return this.type;
        }
    }
    
    /*-- Static Variables ---------------------------------------------------*/

    public final static Logger logger = RtcpPacket.logger;
    
    public final static ByteField Type = new ByteField(0);
    public final static ByteField Length = new ByteField(1);

    public final static int BASE_ITEM_LENGTH = 2;
    

    /*-- Member Variables ---------------------------------------------------*/

    private ByteBuffer itemText;
    

    /*-- Member Functions ---------------------------------------------------*/

    public RtcpSourceDescriptionItem(ItemType itemType, String itemText) {
        super(BASE_ITEM_LENGTH);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "RtcpReceptionReport.RtcpReceptionReport",
                                          itemType,
                                          itemText));
        }
        
        setItemType(itemType);
        setItemText(itemText);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    public RtcpSourceDescriptionItem(ByteBuffer buffer) {
        super(consume(buffer, BASE_ITEM_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpReceptionReport.RtcpReceptionReport"));
        }

        this.itemText = consume(buffer,Length.get(getBufferInternal()) & 0xFF);
        
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
        logger.info(ObjectId + " : item type=" + getItemType() + " length=" + getLength() + " text='" + getItemText() + "'");
    }

    @Override
    public void writeTo(ByteBuffer buffer) {
        super.writeTo(buffer);
        buffer.put(this.itemText);
        this.itemText.rewind();
    }

    /**
     * 
     * @return
     */
    public int getTotalLength() {
        return this.itemText.limit() + BASE_ITEM_LENGTH;
    }

    /**
     * 
     * @return
     */
    public ItemType getItemType() {
        return ItemType.values()[Type.get(getBufferInternal())];
    }

    /**
     * 
     * @param itemType
     */
    protected void setItemType(ItemType itemType) {
        Type.set(getBufferInternal(), (byte)itemType.getValue());
    }

    /**
     * 
     * @return
     */
    public int getLength() {
        return Length.get(getBufferInternal()) & 0xFF;
    }

    /**
     * 
     * @param length
     */
    protected void setLength(int length) {
        Length.set(getBufferInternal(), (byte)length);
    }

    /**
     * Returns the item text value.
     * If the item type is {@link ItemType.PRIV}, the value returned is
     * formatted as "prefix:text".
     * @return
     */
    public String getItemText() {
        if (getItemType() != ItemType.PRIV) {
            try {
                return new String(this.itemText.array(),
                                  this.itemText.arrayOffset(),
                                  this.itemText.limit(),
                                  "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new Error(e);
            }
        }
        else {
            int prefixLength = this.itemText.get(0);
            try {
                String prefix = new String(this.itemText.array(),
                                           this.itemText.arrayOffset() + 1,
                                           prefixLength,
                                           "UTF-8");
                String text = new String(this.itemText.array(),
                                         this.itemText.arrayOffset() + prefixLength + 1,
                                         this.itemText.limit() - prefixLength - 1,
                                         "UTF-8");
                return prefix + ":" + text;
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new Error(e);
            }
        }
    }

    /**
     * Sets the item text value.
     * If the item type is ({@link ItemType.PRIV}) item, the text string must
     * be formatted as "prefix:text".
     * The item text length cannot exceed 255 bytes after encoding in UTF-8.
     * @param itemType - An {@link ItemType} enumerator.
     * @param itemText - The item text
     */
    public void setItem(ItemType itemType, String itemText) {
        setItemType(itemType);
        if (itemType == ItemType.PRIV) {
            String[] split = itemText.split(":");
            if (split.length == 0) {
                setPrivItem("", "");
            }
            else if (split.length == 1) {
                setPrivItem(split[0], "");
            }
            else {
                setPrivItem(split[0], split[1]);
            }
        }
        else {
            setItemText(itemText);
        }
    }
    
    /**
     * 
     * @param itemText
     */
    protected void setItemText(String itemText) {
        byte[] bytes;
        try {
            bytes = itemText.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            // Should not happen!
            e.printStackTrace();
            throw new Error(e);
        }
        int length = bytes.length > 255 ? 255 : bytes.length;
        Length.set(getBufferInternal(), (byte)length);
        this.itemText = ByteBuffer.wrap(bytes, 0, length);
    }

    /**
     * Set the value of a private extension ({@link ItemType.PRIV}) item.
     * The combined length of the prefix and text cannot
     * exceed 254 bytes after encoding in UTF-8.
     * @param prefix - The extension prefix
     * @param text - The extension text
     */
    public void setPrivItem(String prefix, String text) {
        byte[] prefixBytes;
        byte[] textBytes;
        try {
            prefixBytes = prefix.getBytes("UTF-8");
            textBytes = text.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            // Should not happen!
            e.printStackTrace();
            throw new Error(e);
        }
        int remaining = 254;
        int prefixLength = prefixBytes.length > remaining ? remaining : prefixBytes.length;
        remaining = remaining - prefixLength;
        int textLength = textBytes.length > remaining ? remaining : textBytes.length;
        int combinedLength = prefixLength + textLength;
        this.itemText = ByteBuffer.allocate(combinedLength + 1);
        this.itemText.put((byte)combinedLength);
        this.itemText.put(prefixBytes);
        this.itemText.put(textBytes);
        this.itemText.rewind();
        Length.set(getBufferInternal(), (byte)(combinedLength + 1));
        setItemType(ItemType.PRIV);
    }
}
