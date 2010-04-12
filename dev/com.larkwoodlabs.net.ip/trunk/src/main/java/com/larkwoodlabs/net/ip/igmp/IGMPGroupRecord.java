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

package com.larkwoodlabs.net.ip.igmp;

import java.net.InetAddress;
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

/**
 * An IGMP Membership Report Group Record.
 * 
 * <pre>
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |  Record Type  |  Aux Data Len |     Number of Sources (N)     |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |                       Multicast Address                       |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |                       Source Address [1]                      |
 *       +-                                                             -+
 *       |                       Source Address [2]                      |
 *       +-                                                             -+
 *       .                               .                               .
 *       .                               .                               .
 *       .                               .                               .
 *       +-                                                             -+
 *       |                       Source Address [N]                      |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |                                                               |
 *       .                                                               .
 *       .                         Auxiliary Data                        .
 *       .                                                               .
 *       |                                                               |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 * 4.2.4. Group Record
 * 
 *    Each Group Record is a block of fields containing information
 *    pertaining to the sender's membership in a single multicast group on
 *    the interface from which the Report is sent.
 * 
 * 4.2.5. Record Type
 * 
 *    See section 4.2.12, below.
 * 
 * 4.2.6. Aux Data Len
 * 
 *    The Aux Data Len field contains the length of the Auxiliary Data
 *    field in this Group Record, in units of 32-bit words.  It may contain
 *    zero, to indicate the absence of any auxiliary data.
 * 
 * 4.2.7. Number of Sources (N)
 * 
 *    The Number of Sources (N) field specifies how many source addresses
 *    are present in this Group Record.
 * 
 * 4.2.8. Multicast Address
 * 
 *    The Multicast Address field contains the IP multicast address to
 *    which this Group Record pertains.
 * 
 * 4.2.9. Source Address [i]
 * 
 *    The Source Address [i] fields are a vector of n IP unicast addresses,
 *    where n is the value in this record's Number of Sources (N) field.
 * 
 * 4.2.10. Auxiliary Data
 * 
 *    The Auxiliary Data field, if present, contains additional information
 *    pertaining to this Group Record.  The protocol specified in this
 *    document, IGMPv3, does not define any auxiliary data.  Therefore,
 *    implementations of IGMPv3 MUST NOT include any auxiliary data (i.e.,
 *    MUST set the Aux Data Len field to zero) in any transmitted Group
 *    Record, and MUST ignore any auxiliary data present in any received
 *    Group Record.  The semantics and internal encoding of the Auxiliary
 *    Data field are to be defined by any future version or extension of
 *    IGMP that uses this field.
 * 
 * 4.2.11. Additional Data
 * 
 *    If the Packet Length field in the IP header of a received Report
 *    indicates that there are additional octets of data present, beyond
 *    the last Group Record, IGMPv3 implementations MUST include those
 *    octets in the computation to verify the received IGMP Checksum, but
 *    MUST otherwise ignore those additional octets.  When sending a
 *    Report, an IGMPv3 implementation MUST NOT include additional octets
 *    beyond the last Group Record.
 * 
 * 4.2.12. Group Record Types
 * 
 *    There are a number of different types of Group Records that may be
 *    included in a Report message:
 * 
 *    o A &quot;Current-State Record&quot; is sent by a system in response to a Query
 *      received on an interface.  It reports the current reception state
 *      of that interface, with respect to a single multicast address.  The
 *      Record Type of a Current-State Record may be one of the following
 *      two values:
 * 
 *         Value  Name and Meaning
 *         -----  ----------------
 * 
 *           1    MODE_IS_INCLUDE - indicates that the interface has a
 *                filter mode of INCLUDE for the specified multicast
 *                address.  The Source Address [i] fields in this Group
 *                Record contain the interface's source list for the
 *                specified multicast address, if it is non-empty.
 * 
 *           2    MODE_IS_EXCLUDE - indicates that the interface has a
 *                filter mode of EXCLUDE for the specified multicast
 *                address.  The Source Address [i] fields in this Group
 *                Record contain the interface's source list for the
 *                specified multicast address, if it is non-empty.
 * 
 *    o A &quot;Filter-Mode-Change Record&quot; is sent by a system whenever a local
 *      invocation of IPMulticastListen causes a change of the filter mode
 *      (i.e., a change from INCLUDE to EXCLUDE, or from EXCLUDE to
 *      INCLUDE), of the interface-level state entry for a particular
 *      multicast address.  The Record is included in a Report sent from
 *      the interface on which the change occurred.  The Record Type of a
 *      Filter-Mode-Change Record may be one of the following two values:
 * 
 *           3    CHANGE_TO_INCLUDE_MODE - indicates that the interface
 *                has changed to INCLUDE filter mode for the specified
 *                multicast address.  The Source Address [i] fields
 *                in this Group Record contain the interface's new
 *                source list for the specified multicast address,
 *                if it is non-empty.
 * 
 *           4    CHANGE_TO_EXCLUDE_MODE - indicates that the interface
 *                has changed to EXCLUDE filter mode for the specified
 *                multicast address.  The Source Address [i] fields
 *                in this Group Record contain the interface's new
 *                source list for the specified multicast address,
 *                if it is non-empty.
 * 
 *    o A &quot;Source-List-Change Record&quot; is sent by a system whenever a local
 *      invocation of IPMulticastListen causes a change of source list that
 *      is *not* coincident with a change of filter mode, of the
 *      interface-level state entry for a particular multicast address.
 *      The Record is included in a Report sent from the interface on which
 *      the change occurred.  The Record Type of a Source-List-Change
 *      Record may be one of the following two values:
 * 
 *           5    ALLOW_NEW_SOURCES - indicates that the Source Address
 *                [i] fields in this Group Record contain a list of the
 *                additional sources that the system wishes to
 *                hear from, for packets sent to the specified
 *                multicast address.  If the change was to an INCLUDE
 *                source list, these are the addresses that were added
 *                to the list; if the change was to an EXCLUDE source
 *                list, these are the addresses that were deleted from
 *                the list.
 * 
 *           6    BLOCK_OLD_SOURCES - indicates that the Source Address
 *                [i] fields in this Group Record contain a list of the
 *                sources that the system no longer wishes to
 *                hear from, for packets sent to the specified
 *                multicast address.  If the change was to an INCLUDE
 *                source list, these are the addresses that were
 *                deleted from  the list; if the change was to an
 *                EXCLUDE source list, these are the addresses that
 *                were added to the list.
 * 
 *    If a change of source list results in both allowing new sources and
 *    blocking old sources, then two Group Records are sent for the same
 *    multicast address, one of type ALLOW_NEW_SOURCES and one of type
 *    BLOCK_OLD_SOURCES.
 * </pre>
 */
public final class IGMPGroupRecord extends BufferBackedObject {

    /*-- Static Variables ---------------------------------------------------*/

    public static final int BASE_RECORD_LENGTH = 8;

    /**
     * MODE_IS_INCLUDE - indicates that the interface has a filter mode of
     * INCLUDE for the specified multicast address. The Source Address [i]
     * fields in this Group Record contain the interface's source list for
     * the specified multicast address, if it is non-empty.
     */
    public static final byte MODE_IS_INCLUDE = 1;

    /**
     * MODE_IS_EXCLUDE - indicates that the interface has a filter mode of
     * EXCLUDE for the specified multicast address. The Source Address [i]
     * fields in this Group Record contain the interface's source list for
     * the specified multicast address, if it is non-empty.
     */
    public static final byte MODE_IS_EXCLUDE = 2;

    /**
     * CHANGE_TO_INCLUDE_MODE - indicates that the interface changed to
     * INCLUDE filter mode for the specified address. The Source Address [i]
     * fields this Group Record contain the interface's new list for the
     * specified multicast address, if it is non-empty.
     */
    public static final byte CHANGE_TO_INCLUDE_MODE = 3;

    /**
     * CHANGE_TO_EXCLUDE_MODE - indicates that the interface has changed to
     * EXCLUDE filter mode for the specified multicast address. The Source
     * Address [i] fields in this Group Record contain the interface's new
     * source list for the specified multicast address, if it is non-empty.
     */
    public static final byte CHANGE_TO_EXCLUDE_MODE = 4;

    /**
     * ALLOW_NEW_SOURCES - indicates that the Source Address [i] fields in
     * this Group Record contain a list of the additional sources that the
     * system wishes to hear from, for packets sent to the specified
     * multicast address. If the change was to an INCLUDE source list, these
     * are the addresses that were added to the list; if the change was to
     * an EXCLUDE source list, these are the addresses that were deleted
     * from the list.
     */
    public static final byte ALLOW_NEW_SOURCES = 5;

    /**
     * BLOCK_OLD_SOURCES - indicates that the Source Address [i] fields in
     * this Group Record contain a list of the sources that the system no
     * longer wishes to hear from, for packets sent to the specified
     * multicast address. If the change was to an INCLUDE source list, these
     * are the addresses that were deleted from the list; if the change was
     * to an EXCLUDE source list, these are the addresses that were added to
     * the list.
     */
    public static final byte BLOCK_OLD_SOURCES = 6;

    public static final ByteField       RecordType = new ByteField(0);
    public static final ByteField       AuxDataLen = new ByteField(1);
    public static final ShortField      NumberOfSources = new ShortField(2);
    public static final ByteArrayField  GroupAddress = new ByteArrayField(4,4);


    /*-- Static Functions ---------------------------------------------------*/
    
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
        return (short)(BASE_RECORD_LENGTH + NumberOfSources.get(buffer) * 4 + AuxDataLen.get(buffer) * 4);
    }


    /*-- Member Variables ---------------------------------------------------*/

    private Vector<byte[]> sources = new Vector<byte[]>();

    private ByteBuffer auxData;


    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param type
     * @param groupAddress
     */
    public IGMPGroupRecord(byte type, byte[] groupAddress) {
        this(type, groupAddress, null);
        
        if (IGMPMessage.logger.isLoggable(Level.FINER)) {
            IGMPMessage.logger.finer(Logging.entering(ObjectId, "IGMPv3GroupRecord.IGMPv3GroupRecord", type, Logging.address(groupAddress)));
        }
    }

    /**
     * 
     * @param type
     * @param groupAddress
     * @param auxData
     */
    public IGMPGroupRecord(byte type, byte[] groupAddress, ByteBuffer auxData) {
        super(BASE_RECORD_LENGTH);

        if (IGMPMessage.logger.isLoggable(Level.FINER)) {
            IGMPMessage.logger.finer(Logging.entering(ObjectId, "IGMPv3GroupRecord.IGMPv3GroupRecord", type, Logging.address(groupAddress), auxData));
        }
        
        setType(type);
        setGroupAddress(groupAddress);
        setAuxData(auxData);
        
        if (IGMPMessage.logger.isLoggable(Level.FINER)) {
            logState(IGMPMessage.logger);
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public IGMPGroupRecord(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_RECORD_LENGTH));
        
        if (IGMPMessage.logger.isLoggable(Level.FINER)) {
            IGMPMessage.logger.finer(Logging.entering(ObjectId, "IGMPv3GroupRecord.IGMPv3GroupRecord", buffer));
        }
        
        int count = getNumberOfSources();
        for(int i=0; i<count; i++) {
            byte[] address = new byte[4];
            buffer.get(address);
            this.sources.add(address);
        }
        
        this.auxData = consume(buffer, getAuxDataLength()*4);
        
        if (IGMPMessage.logger.isLoggable(Level.FINER)) {
            logState(IGMPMessage.logger);
        }
    }

    @Override
    public Logger getLogger() {
        return IGMPMessage.logger;
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
        logger.info(ObjectId + " : record-length="+getRecordLength());
        logger.info(ObjectId + " : record-type="+getType()+" "+getTypeName(getType()));
        logger.info(ObjectId + " : aux-data-length="+getAuxDataLength());
        logger.info(ObjectId + " : number-of-sources="+getNumberOfSources());
        logger.info(ObjectId + " : group-address="+Logging.address(getGroupAddress()));
        logger.info(ObjectId + " ----> sources");
        for(int i= 0; i<getNumberOfSources(); i++) {
            logger.info(ObjectId+" : source["+i+"]="+Logging.address(getSource(i)));
        }
        logger.info(ObjectId+" <---- end sources");
        logger.info(ObjectId+" : aux-data="+getAuxData());
    }

    @Override
    public void writeTo(ByteBuffer buffer) {
        
        if (IGMPMessage.logger.isLoggable(Level.FINER)) {
            IGMPMessage.logger.finer(Logging.entering(ObjectId, "IGMPv3GroupRecord.writeTo", buffer));
        }
        
        //Precondition.checkReference(buffer);
        // Update fields
        setAuxDataLength(this.auxData != null ? this.auxData.limit() : 0);
        setNumberOfSources((short)this.sources.size());
        //Precondition.checkBounds(buffer.length, offset, getRecordLength());
        super.writeTo(buffer);

        Iterator<byte[]> iter = this.sources.iterator();
        while (iter.hasNext()) {
            byte[] address = iter.next();
            buffer.put(address);
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
        return BASE_RECORD_LENGTH + getNumberOfSources() * 4 + getAuxDataLength();
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
        
        if (IGMPMessage.logger.isLoggable(Level.FINER)) {
            IGMPMessage.logger.finer(Logging.entering(ObjectId, "IGMPv3GroupRecord.setType", type));
        }
        
        if (type == MODE_IS_INCLUDE || type == MODE_IS_EXCLUDE || type == CHANGE_TO_INCLUDE_MODE
                || type == CHANGE_TO_EXCLUDE_MODE || type == ALLOW_NEW_SOURCES || type == BLOCK_OLD_SOURCES) {
            RecordType.set(getBufferInternal(),type);
        } else {
            if (IGMPMessage.logger.isLoggable(Level.FINE)) {
                IGMPMessage.logger.fine(ObjectId+"invalid group record type specified");
            }
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
        
        if (IGMPMessage.logger.isLoggable(Level.FINER)) {
            IGMPMessage.logger.finer(Logging.entering(ObjectId, "IGMPv3GroupRecord.setNumberOfSources", numberOfSources));
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
        
        if (IGMPMessage.logger.isLoggable(Level.FINER)) {
            IGMPMessage.logger.finer(Logging.entering(ObjectId, "IGMPv3GroupRecord.setAuxDataLength", length));
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
        
        if (IGMPMessage.logger.isLoggable(Level.FINER)) {
            IGMPMessage.logger.finer(Logging.entering(ObjectId, "IGMPv3GroupRecord.setGroupAddress", Logging.address(groupAddress)));
        }
        
        //Precondition.checkReference(groupAddress);
        setGroupAddress(groupAddress.getAddress());
    }

    /**
     * 
     * @param groupAddress
     */
    public void setGroupAddress(byte[] groupAddress) {
        
        if (IGMPMessage.logger.isLoggable(Level.FINER)) {
            IGMPMessage.logger.finer(Logging.entering(ObjectId, "IGMPv3GroupRecord.setGroupAddress", Logging.address(groupAddress)));
        }
        
        if (groupAddress.length != 4) {
            if (IGMPMessage.logger.isLoggable(Level.FINE)) {
                IGMPMessage.logger.fine(ObjectId+"invalid group address - IGMPv3 messages only allow use of IPv4 addresses");
            }
            throw new IllegalArgumentException("invalid group address - IGMPv3 messages only allow use of IPv4 addresses");
        }
        GroupAddress.set(getBufferInternal(),groupAddress);
    }

    /**
     * 
     * @param sourceAddress
     */
    public void addSource(InetAddress sourceAddress) {
        
        if (IGMPMessage.logger.isLoggable(Level.FINER)) {
            IGMPMessage.logger.finer(Logging.entering(ObjectId, "IGMPv3GroupRecord.addSource", Logging.address(sourceAddress)));
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
        
        if (IGMPMessage.logger.isLoggable(Level.FINER)) {
            IGMPMessage.logger.finer(Logging.entering(ObjectId, "IGMPv3GroupRecord.addSource", Logging.address(sourceAddress)));
        }
        
        //Precondition.checkIPv4Address(sourceAddress);
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
        
        if (IGMPMessage.logger.isLoggable(Level.FINER)) {
            IGMPMessage.logger.finer(Logging.entering(ObjectId, "IGMPv3GroupRecord.removeSource", index));
        }

        this.sources.remove(index);
    }

    /**
     * 
     * @return
     */
    public ByteBuffer getAuxData() {
        return this.auxData == null ? null : this.auxData.slice();
    }
    
    /**
     * 
     * @param auxData
     */
    public void setAuxData(ByteBuffer auxData) {
        
        if (IGMPMessage.logger.isLoggable(Level.FINER)) {
            IGMPMessage.logger.finer(Logging.entering(ObjectId, "IGMPv3GroupRecord.setAuxData", auxData));
        }
        
        this.auxData = auxData == null ? null : auxData.slice();
        setAuxDataLength(this.auxData != null ? (this.auxData.limit() + 3) / 4 : 0);
    }
    
}

