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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.ipv4.IPv4Packet;
import com.larkwoodlabs.util.buffer.fields.ShortField;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An IGMPv3 Membership Report Message. [<a
 * href="http://www.ietf.org/rfc/rfc3376.txt">RFC-3376</a>]
 * 
 * <pre>
 * 4.2. Version 3 Membership Report Message
 * 
 *    Version 3 Membership Reports are sent by IP systems to report (to
 *    neighboring routers) the current multicast reception state, or
 *    changes in the multicast reception state, of their interfaces.
 *    Reports have the following format:
 * 
 *        0                   1                   2                   3
 *        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |  Type = 0x22  |    Reserved   |           Checksum            |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |           Reserved            |  Number of Group Records (M)  |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |                                                               |
 *       .                                                               .
 *       .                        Group Record [1]                       .
 *       .                                                               .
 *       |                                                               |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |                                                               |
 *       .                                                               .
 *       .                        Group Record [2]                       .
 *       .                                                               .
 *       |                                                               |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |                               .                               |
 *       .                               .                               .
 *       |                               .                               |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |                                                               |
 *       .                                                               .
 *       .                        Group Record [M]                       .
 *       .                                                               .
 *       |                                                               |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 * 4.2.1. Reserved
 * 
 *    The Reserved fields are set to zero on distribution, and ignored on
 *    reception.
 * 
 * 4.2.2. Checksum
 * 
 *    The Checksum is the 16-bit one's complement of the one's complement
 *    sum of the whole IGMP message (the entire IP payload).  For computing
 *    the checksum, the Checksum field is set to zero.  When receiving
 *    packets, the checksum MUST be verified before processing a message.
 * 
 * 4.2.3. Number of Group Records (M)
 * 
 *    The Number of Group Records (M) field specifies how many Group
 *    Records are present in this Report.
 * 
 * 4.2.4. Group Record
 * 
 *    Each Group Record is a block of fields containing information
 *    pertaining to the sender's membership in a single multicast group on
 *    the interface from which the Report is sent.
 *    
 *    ...See nested GroupRecord class...
 * 
 * 4.2.13. IP Source Addresses for Reports
 * 
 *    An IGMP report is sent with a valid IP source address for the
 *    destination subnet.  The 0.0.0.0 source address may be used by a
 *    system that has not yet acquired an IP address.  Note that the
 *    0.0.0.0 source address may simultaneously be used by multiple systems
 *    on a LAN.  Routers MUST accept a report with a source address of
 *    0.0.0.0.
 * 
 * 4.2.14. IP Destination Addresses for Reports
 * 
 *    Version 3 Reports are sent with an IP destination address of
 *    224.0.0.22, to which all IGMPv3-capable multicast routers listen.  A
 *    system that is operating in version 1 or version 2 compatibility
 *    modes sends version 1 or version 2 Reports to the multicast group
 *    specified in the Group Address field of the Report.  In addition, a
 *    system MUST accept and process any version 1 or version 2 Report
 *    whose IP Destination Address field contains *any* of the addresses
 *    (unicast or multicast) assigned to the interface on which the Report
 *    arrives.
 * 
 * 4.2.16. Membership Report Size
 * 
 *    If the set of Group Records required in a Report does not fit within
 *    the size limit of a single Report message (as determined by the MTU
 *    of the network on which it will be sent), the Group Records are sent
 *    in as many Report messages as needed to report the entire set.
 * 
 *    If a single Group Record contains so many source addresses that it
 *    does not fit within the size limit of a single Report message, if its
 *    Type is not MODE_IS_EXCLUDE or CHANGE_TO_EXCLUDE_MODE, it is split
 *    into multiple Group Records, each containing a different subset of
 *    the source addresses and each sent in a separate Report message.  If
 *    its Type is MODE_IS_EXCLUDE or CHANGE_TO_EXCLUDE_MODE, a single Group
 *    Record is sent, containing as many source addresses as can fit, and
 * 
 *    the remaining source addresses are not reported; though the choice of
 *    which sources to report is arbitrary, it is preferable to report the
 *    same set of sources in each subsequent report, rather than reporting
 *    different sources each time.
 * </pre>
 * 
 * @see com.larkwoodlabs.net.ip.igmp.IGMPv3GroupRecord.
 * 
 * @author Gregory Bumgardner
 * 
 */
public final class IGMPv3ReportMessage extends IGMPMessage {

    /*-- Inner Classes ------------------------------------------------------*/
    
    public static final class Parser implements IGMPMessage.ParserType {

        @Override
        public IGMPMessage parse(ByteBuffer buffer) throws ParseException {
            return new IGMPv3ReportMessage(buffer);
        }

        @Override
        public boolean verifyChecksum(ByteBuffer buffer) throws MissingParserException, ParseException {
            return IGMPv3ReportMessage.verifyChecksum(buffer);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }


    /*-- Static Variables ---------------------------------------------------*/
    
    public static final byte MESSAGE_TYPE = 0x22;

    public static final int BASE_MESSAGE_LENGTH = 8;

    public static final ShortField NumberOfGroupRecords = new ShortField(6);


    /*-- Static Functions ---------------------------------------------------*/
    
    public static IGMPMessage.Parser getIGMPMessageParser() {
        return getIGMPMessageParser(new IGMPv3ReportMessage.Parser());
    }

    public static IPMessage.Parser getIPMessageParser() {
        return getIPMessageParser(new IGMPv3ReportMessage.Parser());
    }

    public static IPv4Packet.Parser getIPv4PacketParser() {
        return getIPv4PacketParser(new IGMPv3ReportMessage.Parser());
    }

    public static IPPacket.Parser getIPPacketParser() {
        return getIPPacketParser(new IGMPv3ReportMessage.Parser());
    }

    /**
     * Verifies the IGMP message checksum. Called by the parser prior to constructing the packet.
     * @param segment - the buffer segment containing the IGMP message.
     */
    public static boolean verifyChecksum(ByteBuffer buffer) {
        return Checksum.get(buffer) == IGMPMessage.calculateChecksum(buffer, IGMPv3ReportMessage.calculateMessageSize(buffer));
    }

    /**
     * Writes the IGMP message checksum into a buffer containing an IGMP message.
     * @param buffer - a byte array.
     * @param offset - the offset within the array at which to write the message.
     */
    public static void setChecksum(ByteBuffer buffer) {
        Checksum.set(buffer, IGMPMessage.calculateChecksum(buffer, IGMPv3ReportMessage.calculateMessageSize(buffer)));
    }

    public static short calculateMessageSize(ByteBuffer buffer) {
        short total = BASE_MESSAGE_LENGTH;
        short numberOfGroupRecords = NumberOfGroupRecords.get(buffer);
        ByteBuffer message = buffer.slice();
        for (int i = 0; i < numberOfGroupRecords; i++) {
            message.position(total);
            total += IGMPGroupRecord.calculateGroupRecordSize(message.slice());
        }
        return total;
    }
    

    /*-- Member Variables ---------------------------------------------------*/

    private Vector<IGMPGroupRecord> groupRecords = new Vector<IGMPGroupRecord>();


    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     */
    public IGMPv3ReportMessage() {
        super(BASE_MESSAGE_LENGTH, MESSAGE_TYPE, (byte)0);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3ReportMessage.IGMPv3ReportMessage"));
        }
        
        getBufferInternal().put(1,(byte)0); // Reserved
        getBufferInternal().put(4,(byte)0); // Reserved
        getBufferInternal().put(5,(byte)0); // Reserved
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public IGMPv3ReportMessage(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));
        
        if (logger.isLoggable(Level.FINER)) {
            logger.fine(Logging.entering(ObjectId, "IGMPv3ReportMessage.IGMPv3ReportMessage", buffer));
        }
        
        int numberOfGroupRecords = getNumberOfGroupRecords();
        if (numberOfGroupRecords > 0) {
            for (int i = 0; i < numberOfGroupRecords; i++) {
                IGMPGroupRecord record = new IGMPGroupRecord(buffer);
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
     * 
     * @param logger
     */
    private void logState(Logger logger) {
        logger.info(ObjectId + " : number-of-group-records="+getNumberOfGroupRecords());
        logger.info(ObjectId + " : ----> group records");
        int numberOfGroupRecords = getNumberOfGroupRecords();
        if (numberOfGroupRecords > 0) {
            for (int i = 0; i < numberOfGroupRecords; i++) {
                logger.info(ObjectId + " : group record["+i+"]:");
                this.groupRecords.get(i).log(logger);
            }
        }
        logger.info(ObjectId + " <---- end group records");
    }

    @Override
    public void writeTo(ByteBuffer buffer) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3ReportMessage.writeTo", buffer));
        }
        
        //Precondition.checkReference(buffer);
        //Precondition.checkBounds(buffer.length, offset, getMessageLength());
        setNumberOfGroupRecords((short)this.groupRecords.size());
        super.writeTo(buffer);
        Iterator<IGMPGroupRecord> iter = this.groupRecords.iterator();
        while (iter.hasNext()) {
            IGMPGroupRecord record = iter.next();
            record.writeTo(buffer);
        }
    }

    @Override
    public void writeChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "IGMPv3ReportMessage.writeChecksum",
                                          buffer,
                                          Logging.address(sourceAddress),
                                          Logging.address(destinationAddress)));
        }
        
        IGMPv3ReportMessage.setChecksum(buffer);
    }

    @Override
    public byte getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public int getMessageLength() {
        int messageLength = BASE_MESSAGE_LENGTH;
        if (this.groupRecords != null) {
            Iterator<IGMPGroupRecord> iter = this.groupRecords.iterator();
            while (iter.hasNext()) {
                messageLength += iter.next().getRecordLength();
            }
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
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3ReportMessage.setNumberOfGroupRecords", numberOfGroupRecords));
        }
        
        NumberOfGroupRecords.set(getBufferInternal(),numberOfGroupRecords);
    }

    /**
     * 
     * @param groupRecord
     * @return
     */
    public int addGroupRecord(IGMPGroupRecord groupRecord) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3ReportMessage.addGroupRecord", groupRecord));
        }
        
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

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3ReportMessage.removeGroupRecord", index));
        }

        this.groupRecords.remove(index);
    }

    /**
     * 
     * @param index
     * @return
     */
    public IGMPGroupRecord getGroupRecord(int index) {
        return this.groupRecords.get(index);
    }

}