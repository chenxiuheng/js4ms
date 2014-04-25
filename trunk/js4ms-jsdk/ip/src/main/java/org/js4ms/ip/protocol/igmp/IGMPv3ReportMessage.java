package org.js4ms.ip.protocol.igmp;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IGMPv3ReportMessage.java [org.js4ms.jsdk:ip]
 * %%
 * Copyright (C) 2009 - 2014 Cisco Systems, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.exception.ParseException;
import org.js4ms.common.util.buffer.field.ShortField;
import org.js4ms.common.util.buffer.parser.MissingParserException;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.ip.IPMessage;
import org.js4ms.ip.IPPacket;
import org.js4ms.ip.ipv4.IPv4Packet;




/**
 * Represents an IGMPv3 Membership Report Message.
 * Version 3 Membership Reports are sent by IP systems to report (to
 * neighboring routers) the current multicast reception state, or
 * changes in the multicast reception state, of their interfaces.
 * See [<a
 * href="http://www.ietf.org/rfc/rfc3376.txt">RFC-3376</a>].
 * <p>
 * <h3>Message Format</h3> <blockquote>
 * 
 * <pre>
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |  Type = 0x22  |    Reserved   |           Checksum            |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |           Reserved            |  Number of Group Records (M)  |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  .                                                               .
 *  .                        Group Record [1]                       .
 *  .                                                               .
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  .                                                               .
 *  .                        Group Record [2]                       .
 *  .                                                               .
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                               .                               |
 *  .                               .                               .
 *  |                               .                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  .                                                               .
 *  .                        Group Record [M]                       .
 *  .                                                               .
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <dl>
 * <dt><u>Reserved</u></dt>
 * <p>
 * <dd>The Reserved fields are set to zero on transmission, and ignored on reception.</dd>
 * <p>
 * <dt><u>Checksum</u></dt>
 * <p>
 * <dd>The Checksum is the 16-bit one's complement of the one's complement sum of the
 * whole IGMP message (the entire IP payload). For computing the checksum, the Checksum
 * field is set to zero. When receiving packets, the checksum MUST be verified before
 * processing a message.
 * <p>
 * See {@link #getChecksum()}, {@link #setChecksum(short)},
 * {@link #calculateChecksum(ByteBuffer, int)}.</dd>
 * <p>
 * <dt><u>Number of Group Records (M)</u></dt>
 * <p>
 * <dd>The Number of Group Records (M) field specifies how many Group Records are present
 * in this Report.
 * <p>
 * See {@link #getNumberOfGroupRecords()}.</dd>
 * <p>
 * <dt><u>Group Record</u></dt>
 * <p>
 * <dd>Each Group Record is a block of fields containing information pertaining to the
 * sender's membership in a single multicast group on the interface from which the Report
 * is sent.
 * <p>
 * See {@link #getGroupRecord(int)}, {@link #addGroupRecord(IGMPGroupRecord)}.
 * <p>
 * See {@link IGMPGroupRecord}.</dd>
 * </dl>
 * </blockquote>
 * <p>
 * <h3>IP Source Addresses for Reports</h3>
 * <p>
 * An IGMP report is sent with a valid IP source address for the destination subnet. The
 * 0.0.0.0 source address may be used by a system that has not yet acquired an IP address.
 * Note that the 0.0.0.0 source address may simultaneously be used by multiple systems on
 * a LAN. Routers MUST accept a report with a source address of 0.0.0.0.
 * <p>
 * <h3>IP Destination Addresses for Reports</h3>
 * <p>
 * Version 3 Reports are sent with an IP destination address of 224.0.0.22, to which all
 * IGMPv3-capable multicast routers listen. A system that is operating in version 1 or
 * version 2 compatibility modes sends version 1 or version 2 Reports to the multicast
 * group specified in the Group Address field of the Report. In addition, a system MUST
 * accept and process any version 1 or version 2 Report whose IP Destination Address field
 * contains *any* of the addresses (unicast or multicast) assigned to the interface on
 * which the Report arrives.
 * <p>
 * <h3>Membership Report Size</h3>
 * <p>
 * If the set of Group Records required in a Report does not fit within the size limit of
 * a single Report message (as determined by the MTU of the network on which it will be
 * sent), the Group Records are sent in as many Report messages as needed to report the
 * entire set.
 * <p>
 * If a single Group Record contains so many source addresses that it does not fit within
 * the size limit of a single Report message, if its Type is not MODE_IS_EXCLUDE or
 * CHANGE_TO_EXCLUDE_MODE, it is split into multiple Group Records, each containing a
 * different subset of the source addresses and each sent in a separate Report message. If
 * its Type is MODE_IS_EXCLUDE or CHANGE_TO_EXCLUDE_MODE, a single Group Record is sent,
 * containing as many source addresses as can fit, and the remaining source addresses are
 * not reported; though the choice of which sources to report is arbitrary, it is
 * preferable to report the same set of sources in each subsequent report, rather than
 * reporting different sources each time.
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class IGMPv3ReportMessage
                extends IGMPMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * @author gbumgard
     */
    public static final class Parser
                    implements IGMPMessage.ParserType {

        @Override
        public IGMPMessage parse(final ByteBuffer buffer) throws ParseException {
            return new IGMPv3ReportMessage(buffer);
        }

        @Override
        public boolean verifyChecksum(final ByteBuffer buffer) throws MissingParserException, ParseException {
            return IGMPv3ReportMessage.verifyChecksum(buffer);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    /** */
    public static final byte MESSAGE_TYPE = 0x22;

    /** */
    public static final int BASE_MESSAGE_LENGTH = 8;

    /** */
    public static final ShortField NumberOfGroupRecords = new ShortField(6);

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return
     */
    public static IGMPMessage.Parser getIGMPMessageParser() {
        return getIGMPMessageParser(new IGMPv3ReportMessage.Parser());
    }

    /**
     * @return
     */
    public static IPMessage.Parser getIPMessageParser() {
        return getIPMessageParser(new IGMPv3ReportMessage.Parser());
    }

    /**
     * @return
     */
    public static IPv4Packet.Parser getIPv4PacketParser() {
        return getIPv4PacketParser(new IGMPv3ReportMessage.Parser());
    }

    /**
     * @return
     */
    public static IPPacket.BufferParser getIPPacketParser() {
        return getIPPacketParser(new IGMPv3ReportMessage.Parser());
    }

    /**
     * Verifies the IGMP message checksum. Called by the parser prior to constructing the
     * packet.
     * 
     * @param buffer
     *            - the buffer containing the IGMP message.
     */
    public static boolean verifyChecksum(final ByteBuffer buffer) {
        return Checksum.get(buffer) == IGMPMessage.calculateChecksum(buffer, IGMPv3ReportMessage.calculateMessageSize(buffer));
    }

    /**
     * Writes the IGMP message checksum into a buffer containing an IGMP message.
     * 
     * @param buffer
     *            - a byte array.
     */
    public static void setChecksum(final ByteBuffer buffer) {
        Checksum.set(buffer, IGMPMessage.calculateChecksum(buffer, IGMPv3ReportMessage.calculateMessageSize(buffer)));
    }

    /**
     * @param buffer
     * @return
     */
    public static short calculateMessageSize(final ByteBuffer buffer) {
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

    /**
     * 
     */
    final private Vector<IGMPGroupRecord> groupRecords = new Vector<IGMPGroupRecord>();

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     */
    public IGMPv3ReportMessage() {
        super(BASE_MESSAGE_LENGTH, MESSAGE_TYPE, (byte) 0);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPv3ReportMessage.IGMPv3ReportMessage"));
        }

        getBufferInternal().put(1, (byte) 0); // Reserved
        getBufferInternal().put(4, (byte) 0); // Reserved
        getBufferInternal().put(5, (byte) 0); // Reserved

        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    /**
     * @param buffer
     * @throws ParseException
     */
    public IGMPv3ReportMessage(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPv3ReportMessage.IGMPv3ReportMessage", buffer));
        }

        int numberOfGroupRecords = getNumberOfGroupRecords();
        if (numberOfGroupRecords > 0) {
            for (int i = 0; i < numberOfGroupRecords; i++) {
                IGMPGroupRecord record = new IGMPGroupRecord(buffer);
                this.groupRecords.add(record);
            }
        }

        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    @Override
    public void log(final Logger logger, final Level level) {
        super.log(logger, level);
        logState(logger, level);
    }

    /**
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": number-of-group-records=" + getNumberOfGroupRecords()));
        logger.log(level,this.log.msg(": ----> group records"));
        int numberOfGroupRecords = getNumberOfGroupRecords();
        if (numberOfGroupRecords > 0) {
            for (int i = 0; i < numberOfGroupRecords; i++) {
                logger.log(level,this.log.msg(": group record[" + i + "]:"));
                this.groupRecords.get(i).log(logger,level);
            }
        }
        logger.log(level,this.log.msg("<---- end group records"));
    }

    @Override
    public void writeTo(final ByteBuffer buffer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPv3ReportMessage.writeTo", buffer));
        }

        // Precondition.checkReference(buffer);
        // Precondition.checkBounds(buffer.length, offset, getMessageLength());
        setNumberOfGroupRecords((short) this.groupRecords.size());
        super.writeTo(buffer);
        Iterator<IGMPGroupRecord> iter = this.groupRecords.iterator();
        while (iter.hasNext()) {
            IGMPGroupRecord record = iter.next();
            record.writeTo(buffer);
        }
    }

    @Override
    public void writeChecksum(final ByteBuffer buffer,
                              final byte[] sourceAddress,
                              final byte[] destinationAddress) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPv3ReportMessage.writeChecksum",
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
     * @return
     */
    public int getNumberOfGroupRecords() {
        return NumberOfGroupRecords.get(getBufferInternal());
    }

    /**
     * @param numberOfGroupRecords
     */
    protected void setNumberOfGroupRecords(final short numberOfGroupRecords) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPv3ReportMessage.setNumberOfGroupRecords", numberOfGroupRecords));
        }

        NumberOfGroupRecords.set(getBufferInternal(), numberOfGroupRecords);
    }

    /**
     * @param groupRecord
     * @return
     */
    public int addGroupRecord(final IGMPGroupRecord groupRecord) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPv3ReportMessage.addGroupRecord", groupRecord));
        }

        int index = this.groupRecords.size();
        this.groupRecords.add(groupRecord);
        setNumberOfGroupRecords((short) this.groupRecords.size());
        return index;
    }

    /**
     * @param index
     */
    public void removeGroupRecord(final int index) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("IGMPv3ReportMessage.removeGroupRecord", index));
        }

        this.groupRecords.remove(index);
    }

    /**
     * @param index
     * @return
     */
    public IGMPGroupRecord getGroupRecord(final int index) {
        return this.groupRecords.get(index);
    }

}
