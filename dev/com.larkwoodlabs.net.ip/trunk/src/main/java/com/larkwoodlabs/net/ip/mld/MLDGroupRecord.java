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

package com.larkwoodlabs.net.ip.mld;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.BufferBackedObject;
import com.larkwoodlabs.util.buffer.fields.ByteArrayField;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.buffer.fields.ShortField;
import com.larkwoodlabs.util.logging.Logging;


public final class MLDGroupRecord extends BufferBackedObject {

    /**
     * MODE_IS_INCLUDE - indicates that the interface has a filter mode of
     * INCLUDE for the specified multicast address. The Source Address [i]
     * fields in this Group Record contain the interface's source list for
     * the specified multicast address, if it is non-empty.
     */
    public final static byte MODE_IS_INCLUDE = 1;

    /**
     * MODE_IS_EXCLUDE - indicates that the interface has a filter mode of
     * EXCLUDE for the specified multicast address. The Source Address [i]
     * fields in this Group Record contain the interface's source list for
     * the specified multicast address, if it is non-empty.
     */
    public final static byte MODE_IS_EXCLUDE = 2;

    /**
     * CHANGE_TO_INCLUDE_MODE - indicates that the interface changed to
     * INCLUDE filter mode for the specified address. The Source Address [i]
     * fields this Group Record contain the interface's new list for the
     * specified multicast address, if it is non-empty.
     */
    public final static byte CHANGE_TO_INCLUDE_MODE = 3;

    /**
     * CHANGE_TO_EXCLUDE_MODE - indicates that the interface has changed to
     * EXCLUDE filter mode for the specified multicast address. The Source
     * Address [i] fields in this Group Record contain the interface's new
     * source list for the specified multicast address, if it is non-empty.
     */
    public final static byte CHANGE_TO_EXCLUDE_MODE = 4;

    /**
     * ALLOW_NEW_SOURCES - indicates that the Source Address [i] fields in
     * this Group Record contain a list of the additional sources that the
     * system wishes to hear from, for packets sent to the specified
     * multicast address. If the change was to an INCLUDE source list, these
     * are the addresses that were added to the list; if the change was to
     * an EXCLUDE source list, these are the addresses that were deleted
     * from the list.
     */
    public final static byte ALLOW_NEW_SOURCES = 5;

    /**
     * BLOCK_OLD_SOURCES - indicates that the Source Address [i] fields in
     * this Group Record contain a list of the sources that the system no
     * longer wishes to hear from, for packets sent to the specified
     * multicast address. If the change was to an INCLUDE source list, these
     * are the addresses that were deleted from the list; if the change was
     * to an EXCLUDE source list, these are the addresses that were added to
     * the list.
     */
    public final static byte BLOCK_OLD_SOURCES = 6;

    public static final int BASE_RECORD_LENGTH = 8;

    public static final ByteField       RecordType = new ByteField(0);
    public static final ByteField       AuxDataLen = new ByteField(1);
    public static final ShortField      NumberOfSources = new ShortField(2);
    public static final ByteArrayField  GroupAddress = new ByteArrayField(4,16);

    public static String getTypeName(byte type) {
        switch(type) {
            case MODE_IS_INCLUDE: return "MODE_IS_INCLUDE";
            case MODE_IS_EXCLUDE: return "MODE_IS_EXCLUDE";
            case CHANGE_TO_INCLUDE_MODE: return "CHANGE_TO_INCLUDE_MODE";
            case CHANGE_TO_EXCLUDE_MODE: return "CHANGE_TO_EXCLUDE_MODE";
            case ALLOW_NEW_SOURCES: return "ALLOW_NEW_SOURCES";
            case BLOCK_OLD_SOURCES: return "BLOCK_OLD_SOURCES";
            default: return "UNRECOGINIZED TYPE!";
        }
    }

    public static short calculateGroupRecordSize(ByteBuffer buffer) {
        return (short)(BASE_RECORD_LENGTH + NumberOfSources.get(buffer) * 16 + AuxDataLen.get(buffer) * 4);
    }


    private Vector<byte[]> sources = new Vector<byte[]>();
    private ByteBuffer auxData;

    public MLDGroupRecord(byte type, byte[] groupAddress) {
        this(type, groupAddress, null);
        
        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(Logging.entering(ObjectId, "MLDGroupRecord.MLDGroupRecord", type, Logging.address(groupAddress)));
        }
    }

    public MLDGroupRecord(byte type, byte[] groupAddress, ByteBuffer auxData) {
        super(BASE_RECORD_LENGTH);
        
        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(Logging.entering(ObjectId, "MLDGroupRecord.MLDGroupRecord", type, Logging.address(groupAddress), auxData));
        }
        
        setType(type);
        setGroupAddress(groupAddress);
        setAuxData(auxData);

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            logState(MLDMessage.logger);
        }
    }

    public MLDGroupRecord(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_RECORD_LENGTH));
        
        if (MLDMessage.logger.isLoggable(Level.FINE)) {
            MLDMessage.logger.fine(Logging.entering(ObjectId, "MLDGroupRecord.MLDGroupRecord", buffer));
        }
        
        int count = getNumberOfSources();
        for(int i=0; i<count; i++) {
            byte[] address = new byte[16];
            buffer.get(address);
            this.sources.add(address);
        }

        this.auxData = consume(buffer, getAuxDataLength()*4);

        if (MLDMessage.logger.isLoggable(Level.FINE)) {
            logState(MLDMessage.logger);
        }
    }

    @Override
    public Logger getLogger() {
        return MLDMessage.logger;
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
        logger.info(ObjectId + " : record-type="+getType()+" "+getTypeName(getType()));
        logger.info(ObjectId + " : aux-data-length="+getAuxDataLength());
        logger.info(ObjectId + " : number-of-sources="+getNumberOfSources());
        logger.info(ObjectId + " : group-address="+Logging.address(getGroupAddress()));
        logger.info(ObjectId + " ----> start sources");
        for(int i= 0; i<getNumberOfSources(); i++) {
            logger.info(ObjectId + " : source["+i+"]="+Logging.address(getSource(i)));
        }
        logger.info(ObjectId + " <---- end sources");
        logger.info(ObjectId + " : aux-data="+getAuxData());
    }

    @Override
    public void writeTo(ByteBuffer buffer) {
        
        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(Logging.entering(ObjectId, "MLDGroupRecord.writeTo", buffer));
        }
        
        // Update fields
        setAuxDataLength(this.auxData != null ? this.auxData.limit() : 0);
        setNumberOfSources((short)this.sources.size());

        super.writeTo(buffer);
        Iterator<byte[]> iter = this.sources.iterator();
        while (iter.hasNext()) {
            buffer.put(iter.next());
        }

        if (this.auxData != null && this.auxData.limit() > 0) {
            buffer.put(this.auxData);
        }
    }

    /**
     * 
     * @return
     */
    public int getRecordLength() {
        return BASE_RECORD_LENGTH + getNumberOfSources() * 16 + getAuxDataLength();
    }

    /**
     * 
     * @return
     */
    public byte getType() {
        return RecordType.get(getBufferInternal());
    }

    /**
     * 
     * @param type
     */
    public void setType(byte type) {
        
        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(Logging.entering(ObjectId, "MLDGroupRecord.setType", type));
        }
        
        if (type == MODE_IS_INCLUDE ||
            type == MODE_IS_EXCLUDE ||
            type == CHANGE_TO_INCLUDE_MODE ||
            type == CHANGE_TO_EXCLUDE_MODE ||
            type == ALLOW_NEW_SOURCES ||
            type == BLOCK_OLD_SOURCES) {
            RecordType.set(getBufferInternal(),type);
        } else {
            throw new IllegalArgumentException("invalid group record type specified");
        }
    }

    /**
     * 
     * @return
     */
    public short getNumberOfSources() {
        return NumberOfSources.get(getBufferInternal());
    }

    /**
     * 
     * @param numberOfSources
     */
    protected void setNumberOfSources(short numberOfSources) {

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(Logging.entering(ObjectId, "MLDGroupRecord.setNumberOfSources", numberOfSources));
        }

        NumberOfSources.set(getBufferInternal(), numberOfSources);
    }

    /**
     * Returns the number of auxiliary data words attached to the group record.
     * @return The data length as a number of 32-bit words.
     */
    public int getAuxDataLength() {
        return this.auxData != null ? (this.auxData.limit()+3)/4 : AuxDataLen.get(getBufferInternal());
    }

    /**
     * Sets the auxiliary data length field.
     * @param length - the data length specified as a number of 32-bit words.
     */
    protected void setAuxDataLength(int length) {
        
        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(Logging.entering(ObjectId, "MLDGroupRecord.setAuxDataLength", length));
        }
        
        AuxDataLen.set(getBufferInternal(),(byte)length);
    }

    /**
     * 
     * @return
     */
    public byte[] getGroupAddress() {
        return GroupAddress.get(getBufferInternal());
    }

    /**
     * 
     * @param groupAddress
     */
    public void setGroupAddress(InetAddress groupAddress) {

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(Logging.entering(ObjectId, "MLDGroupRecord.setGroupAddress", Logging.address(groupAddress)));
        }
        
        setGroupAddress(groupAddress.getAddress());
    }

    /**
     * 
     * @param groupAddress
     */
    public void setGroupAddress(byte[] groupAddress) {
        
        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(Logging.entering(ObjectId, "MLDGroupRecord.setGroupAddress", Logging.address(groupAddress)));
        }
        
        //Precondition.checkIPv6MulticastAddress(groupAddress);
        GroupAddress.set(getBufferInternal(),groupAddress);
    }

    /**
     * 
     * @param sourceAddress
     * @throws UnknownHostException
     */
    public void addSource(InetAddress sourceAddress) throws UnknownHostException {
        
        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(Logging.entering(ObjectId, "MLDGroupRecord.addSource", Logging.address(sourceAddress)));
        }
        
        //Precondition.checkReference(sourceAddress);
        addSource(sourceAddress.getAddress());
    }

    /**
     * 
     * @param sourceAddress
     * @return
     */
    public int addSource(byte[] sourceAddress) {

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(Logging.entering(ObjectId, "MLDGroupRecord.addSource", Logging.address(sourceAddress)));
        }
        
        //Precondition.checkIPv6Address(sourceAddress);
        if (sourceAddress.length != 6) {
            
            if (MLDMessage.logger.isLoggable(Level.FINE)) {
                MLDMessage.logger.fine(ObjectId+" invalid source address - MLD messages only allow use of IPv6 addresses");
            }
            
            throw new IllegalArgumentException("invalid source address - MLD messages only allow use of IPv6 addresses");
        }
        int index = this.sources.size();
        this.sources.add(sourceAddress.clone());
        setNumberOfSources((short)this.sources.size());
        return index;
    }
    
    /**
     * 
     * @param index
     * @return
     */
    public byte[] getSource(int index) {
        return this.sources.get(index);
    }
    
    /**
     * 
     * @param index
     */
    public void removeSource(int index) {
        
        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(Logging.entering(ObjectId, "MLDGroupRecord.removeSource", index));
        }

        this.sources.remove(index);
    }

    /**
     * 
     * @return
     */
    public ByteBuffer getAuxData() {
        return this.auxData;
    }

    /**
     * 
     * @param auxData
     */
    public void setAuxData(ByteBuffer auxData) {

        if (MLDMessage.logger.isLoggable(Level.FINER)) {
            MLDMessage.logger.finer(Logging.entering(ObjectId, "MLDGroupRecord.setAuxData", auxData));
        }

        this.auxData = auxData.slice();
        setAuxDataLength(this.auxData != null ? (this.auxData.limit() + 3) / 4 : 0);
    }

}