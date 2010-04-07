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

package com.larkwoodlabs.net.ip.mld;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.ipv6.IPv6Packet;
import com.larkwoodlabs.util.buffer.fields.ShortField;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

/**
 * 
 *
 * @author Gregory Bumgardner
 */
public final class MLDv2ReportMessage extends MLDMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser implements MLDMessage.ParserType {

        @Override
        public MLDMessage parse(ByteBuffer buffer) throws ParseException {
            return new MLDv2ReportMessage(buffer);
        }

        @Override
        public boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) throws MissingParserException, ParseException {
            return MLDv2ReportMessage.verifyChecksum(buffer, sourceAddress, destinationAddress);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/
    
    public static final byte MESSAGE_TYPE = (byte)143;
    public static final int BASE_MESSAGE_LENGTH = 8;

    public static final ShortField Reserved = new ShortField(4);
    public static final ShortField NumberOfGroupRecords = new ShortField(6);


    /*-- Static Functions ---------------------------------------------------*/
    
    public static MLDMessage.Parser getMLDMessageParser() {
        return getMLDMessageParser(new MLDv2ReportMessage.Parser());
    }

    public static IPMessage.Parser getIPMessageParser() {
        return getIPMessageParser(new MLDv2ReportMessage.Parser());
    }

    public static IPv6Packet.Parser getIPv6PacketParser() {
        return getIPv6PacketParser(new MLDv2ReportMessage.Parser());
    }

    public static IPPacket.Parser getIPPacketParser() {
        return getIPPacketParser(new MLDv2ReportMessage.Parser());
    }

    /**
     * Verifies the MLD message checksum. Called by the parser prior to constructing the packet.
     * @param segment - the buffer segment containing the MLD message.
     * @param sourceAddress An IPv6 (16-byte) address..
     * @param destinationAddress An IPv6 (16-byte) address.
     */
    public static boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        return Checksum.get(buffer) == MLDMessage.calculateChecksum(buffer, calculateMessageSize(buffer), sourceAddress, destinationAddress);
    }

    /**
     * Writes the MLD message checksum into a buffer containing an MLD message.
     * @param buffer - a byte array.
     * @param offset - the offset within the array at which to write the message.
     * @param sourceAddress An IPv6 (16-byte) address..
     * @param destinationAddress An IPv6 (16-byte) address.
     */
    public static void setChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        Checksum.set(buffer, MLDMessage.calculateChecksum(buffer, calculateMessageSize(buffer), sourceAddress, destinationAddress));
    }

    public static short calculateMessageSize(ByteBuffer buffer) {
        short total = BASE_MESSAGE_LENGTH;
        short numberOfGroupRecords = NumberOfGroupRecords.get(buffer);
        ByteBuffer message = buffer.slice();
        for (int i = 0; i < numberOfGroupRecords; i++) {
            message.position(total);
            total += MLDGroupRecord.calculateGroupRecordSize(message.slice());
        }
        return total;
    }


    /*-- Member Variables ---------------------------------------------------*/
    
    private Vector<MLDGroupRecord> groupRecords = new Vector<MLDGroupRecord>();
    

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     */
    public MLDv2ReportMessage() {
        super(BASE_MESSAGE_LENGTH, MESSAGE_TYPE);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,"MLDv2ReportMessage.MLDv2ReportMessage"));
        }
        
        Reserved.set(getBufferInternal(), (short)0);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public MLDv2ReportMessage(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDv2ReportMessage.MLDv2ReportMessage", buffer));
        }

        int numberOfGroupRecords = getNumberOfGroupRecords();
        if (numberOfGroupRecords > 0) {
            for (int i = 0; i < numberOfGroupRecords; i++) {
                MLDGroupRecord record = new MLDGroupRecord(buffer);
                this.groupRecords.add(record);
            }
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
        logger.info(ObjectId + " : number-of-group-records="+getNumberOfGroupRecords());
        logger.info(ObjectId + " ----> start group records");
        int numberOfGroupRecords = getNumberOfGroupRecords();
        if (numberOfGroupRecords > 0) {
            for (int i = 0; i < numberOfGroupRecords; i++) {
                logger.info(ObjectId + " : group record["+i+"]:");
                this.groupRecords.get(i).log();
            }
        }
        logger.info(ObjectId + " <---- end group records");
    }


    /**
     * NOTE: You must call {@link #updateChecksum(byte[],byte[],int)} to
     * write the checksum prior to calling this method!
     */
    @Override
    public void writeTo(ByteBuffer buffer) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDv2ReportMessage.writeTo", buffer));
        }
        
        //Precondition.checkReference(buffer);
        //Precondition.checkBounds(buffer.length, offset, getMessageLength());
        setNumberOfGroupRecords((short)this.groupRecords.size());
        super.writeTo(buffer);
        Iterator<MLDGroupRecord> iter = this.groupRecords.iterator();
        while (iter.hasNext()) {
            iter.next().writeTo(buffer);
        }
    }

    @Override
    public void writeChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                        "MLDv2ReportMessage.writeChecksum",
                                        buffer,
                                        Logging.address(sourceAddress),
                                        Logging.address(destinationAddress)));
        }
        
        MLDv2ReportMessage.setChecksum(buffer, sourceAddress, destinationAddress);
    }
    
    @Override
    public byte getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public int getMessageLength() {
        int messageLength = BASE_MESSAGE_LENGTH;
        Iterator<MLDGroupRecord> iter = this.groupRecords.iterator();
        while (iter.hasNext()) {
            messageLength += iter.next().getRecordLength();
        }
        return messageLength;
    }

    /**
     * 
     * @return
     */
    public int getNumberOfGroupRecords() {
        return NumberOfGroupRecords.get(getBufferInternal());
    }

    /**
     * 
     * @param numberOfGroupRecords
     */
    protected void setNumberOfGroupRecords(short numberOfGroupRecords) {
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(Logging.entering(ObjectId, "MLDv2ReportMessage.setNumberOfGroupRecords", numberOfGroupRecords));
        }
        
        NumberOfGroupRecords.set(getBufferInternal(),numberOfGroupRecords);
    }

    /**
     * 
     * @param groupRecord
     * @return
     */
    public int addGroupRecord(MLDGroupRecord groupRecord) {
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(Logging.entering(ObjectId, "MLDv2ReportMessage.addGroupRecord", groupRecord));
        }
        
        //Precondition.checkReference(groupRecord);
        int index = this.groupRecords.size();
        this.groupRecords.add(groupRecord);
        setNumberOfGroupRecords((short)this.groupRecords.size());
        return index;
    }

    /**
     * 
     * @param index
     */
    public void removeGroupRecord(int index) {
        this.groupRecords.remove(index);
    }

    /**
     * 
     * @param index
     * @return
     */
    public MLDGroupRecord getGroupRecord(int index) {
        return this.groupRecords.get(index);
    }

}
