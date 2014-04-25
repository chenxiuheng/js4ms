package org.js4ms.ip.protocol.mld;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MLDv2ReportMessage.java [org.js4ms.jsdk:ip]
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
import org.js4ms.ip.ipv6.IPv6Packet;




/**
 * A Multicast Listener Report Message as described in [<a
 * href="http://tools.ietf.org/html/rfc3810">RFC-3810</a>].
 * <p>
 * Version 2 Multicast Listener Reports are sent by IP nodes to report (to neighboring
 * routers) the current multicast listening state, or changes in the multicast listening
 * state, of their interfaces.
 * <h3>Message Format</h3> <blockquote>
 * 
 * <pre>
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |  Type = 143   |    Reserved   |           Checksum            |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |           Reserved            |Nr of Mcast Address Records (M)|
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  .                                                               .
 *  .                  Multicast Address Record [1]                 .
 *  .                                                               .
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  .                                                               .
 *  .                  Multicast Address Record [2]                 .
 *  .                                                               .
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                               .                               |
 *  .                               .                               .
 *  |                               .                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  .                                                               .
 *  .                  Multicast Address Record [M]                 .
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
 * <dd>The standard ICMPv6 checksum; it covers the entire MLDv2 message, plus a
 * "pseudo-header" of IPv6 header fields [RFC2460, RFC2463]. In order to compute the
 * checksum, the Checksum field is set to zero. When a packet is received, the checksum
 * MUST be verified before processing it.
 * <p>
 * See {@link #getChecksum()}, {@link #setChecksum(short)},
 * {@link #calculateChecksum(ByteBuffer, int, byte[], byte[])}, and
 * {@link #verifyChecksum(ByteBuffer, byte[], byte[])}.</dd>
 * <p>
 * <dt><u>Number of Multicast Address Records (M)</u></dt>
 * <p>
 * <dd>The Number of Multicast Address Records (M) field specifies how many Multicast
 * Address Records are present in this Report.
 * <p>
 * See {@link #getNumberOfGroupRecords()}.</dd>
 * <p>
 * <dt><u>Multicast Address Record</u></dt>
 * <p>
 * <dd>Each Multicast Address Record is a block of fields that contain information on the
 * sender listening to a single multicast address on the interface from which the Report
 * is sent.
 * <p>
 * See {@link MLDGroupRecord}, {@link #getGroupRecord(int)},
 * {@link #addGroupRecord(MLDGroupRecord)}.</dd>
 * <p>
 * <dt><u>Additional Data</u></dt>
 * <p>
 * <dd>If the Payload Length field in the IPv6 header of a received Report indicates that
 * there are additional octets of data present, beyond the last Multicast Address Record,
 * MLDv2 implementations MUST include those octets in the computation to verify the
 * received MLD Checksum, but MUST otherwise ignore those additional octets. When sending
 * a Report, an MLDv2 implementation MUST NOT include additional octets beyond the last
 * Multicast Address Record.</dd>
 * </dl>
 * </blockquote>
 * <h3>Source Addresses for Reports</h3> An MLDv2 Report MUST be sent with a valid IPv6
 * link-local source address, or the unspecified address (::), if the sending interface
 * has not acquired a valid link-local address yet. Sending reports with the unspecified
 * address is allowed to support the use of IP multicast in the Neighbor Discovery
 * Protocol [RFC2461]. For stateless autoconfiguration, as defined in [RFC2462], a node is
 * required to join several IPv6 multicast groups, in order to perform Duplicate Address
 * Detection (DAD). Prior to DAD, the only address the reporting node has for the sending
 * interface is a tentative one, which cannot be used for communication. Thus, the
 * unspecified address must be used.
 * <p>
 * On the other hand, routers MUST silently discard a message that is not sent with a
 * valid link-local address, without taking any action on the contents of the packet.
 * Thus, a Report is discarded if the router cannot identify the source address of the
 * packet as belonging to a link connected to the interface on which the packet was
 * received. A Report sent with the unspecified address is also discarded by the router.
 * This enhances security, as unidentified reporting nodes cannot influence the state of
 * the MLDv2 router(s). Nevertheless, the reporting node has modified its listening state
 * for multicast addresses that are contained in the Multicast Address Records of the
 * Report message. From now on, it will treat packets sent to those multicast addresses
 * according to this new listening state. Once a valid link-local address is available, a
 * node SHOULD generate new MLDv2 Report messages for all multicast addresses joined on
 * the interface.
 * <h3>Destination Addresses for Reports</h3> Version 2 Multicast Listener Reports are
 * sent with an IP destination address of FF02:0:0:0:0:0:0:16, to which all MLDv2-capable
 * multicast routers listen (see section 11 for IANA considerations related to this
 * special destination address). A node that operates in version 1 compatibility mode (see
 * details in section 8) sends version 1 Reports to the multicast address specified in the
 * Multicast Address field of the Report. In addition, a node MUST accept and process any
 * version 1 Report whose IP Destination Address field contains *any* of the IPv6
 * addresses (unicast or multicast) assigned to the interface on which the Report arrives.
 * This might be useful, e.g., for debugging purposes.
 * <p>
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class MLDv2ReportMessage
                extends MLDMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    /**
     * 
     */
    public static class Parser
                    implements MLDMessage.ParserType {

        @Override
        public MLDMessage parse(final ByteBuffer buffer) throws ParseException {
            return new MLDv2ReportMessage(buffer);
        }

        @Override
        public boolean verifyChecksum(final ByteBuffer buffer,
                                      final byte[] sourceAddress,
                                      final byte[] destinationAddress) throws MissingParserException, ParseException {
            return MLDv2ReportMessage.verifyChecksum(buffer, sourceAddress, destinationAddress);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    /** */
    public static final byte MESSAGE_TYPE = (byte) 143;

    /** */
    public static final int BASE_MESSAGE_LENGTH = 8;

    /** */
    public static final ShortField Reserved = new ShortField(4);

    /** */
    public static final ShortField NumberOfGroupRecords = new ShortField(6);

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return
     */
    public static MLDMessage.Parser getMLDMessageParser() {
        return getMLDMessageParser(new MLDv2ReportMessage.Parser());
    }

    /**
     * @return
     */
    public static IPMessage.Parser getIPMessageParser() {
        return getIPMessageParser(new MLDv2ReportMessage.Parser());
    }

    /**
     * @return
     */
    public static IPv6Packet.Parser getIPv6PacketParser() {
        return getIPv6PacketParser(new MLDv2ReportMessage.Parser());
    }

    /**
     * @return
     */
    public static IPPacket.BufferParser getIPPacketParser() {
        return getIPPacketParser(new MLDv2ReportMessage.Parser());
    }

    /**
     * Verifies the MLD message checksum. Called by the parser prior to constructing the
     * packet.
     * 
     * @param buffer
     *            - the buffer containing the MLD message.
     * @param sourceAddress
     *            An IPv6 (16-byte) address..
     * @param destinationAddress
     *            An IPv6 (16-byte) address.
     * @return
     */
    public static boolean verifyChecksum(final ByteBuffer buffer,
                                         final byte[] sourceAddress,
                                         final byte[] destinationAddress) {
        return Checksum.get(buffer) == MLDMessage.calculateChecksum(buffer, calculateMessageSize(buffer), sourceAddress,
                                                                    destinationAddress);
    }

    /**
     * Writes the MLD message checksum into a buffer containing an MLD message.
     * 
     * @param buffer
     *            - a byte array.
     * @param offset
     *            - the offset within the array at which to write the message.
     * @param sourceAddress
     *            An IPv6 (16-byte) address..
     * @param destinationAddress
     *            An IPv6 (16-byte) address.
     */
    public static void setChecksum(final ByteBuffer buffer,
                                   final byte[] sourceAddress,
                                   final byte[] destinationAddress) {
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

    /** */
    final private Vector<MLDGroupRecord> groupRecords = new Vector<MLDGroupRecord>();

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     */
    public MLDv2ReportMessage() {
        super(BASE_MESSAGE_LENGTH, MESSAGE_TYPE);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("MLDv2ReportMessage.MLDv2ReportMessage"));
        }

        Reserved.set(getBufferInternal(), (short) 0);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger, Level.FINER);
        }
    }

    /**
     * @param buffer
     * @throws ParseException
     */
    public MLDv2ReportMessage(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("MLDv2ReportMessage.MLDv2ReportMessage", buffer));
        }

        int numberOfGroupRecords = getNumberOfGroupRecords();
        if (numberOfGroupRecords > 0) {
            for (int i = 0; i < numberOfGroupRecords; i++) {
                MLDGroupRecord record = new MLDGroupRecord(buffer);
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
     * Logs value of member variables declared or maintained by this class.
     * 
     * @param logger
     */
    private void logState(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(": number-of-group-records=" + getNumberOfGroupRecords()));
        logger.log(level,this.log.msg("----> start group records"));
        int numberOfGroupRecords = getNumberOfGroupRecords();
        if (numberOfGroupRecords > 0) {
            for (int i = 0; i < numberOfGroupRecords; i++) {
                logger.log(level,this.log.msg(": group record[" + i + "]:"));
                this.groupRecords.get(i).log(level);
            }
        }
        logger.log(level,this.log.msg("<---- end group records"));
    }

    /**
     * NOTE: You must call {@link #updateChecksum(byte[],byte[],int)} to
     * write the checksum prior to calling this method!
     * 
     * @param buffer
     */
    @Override
    public void writeTo(final ByteBuffer buffer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("MLDv2ReportMessage.writeTo", buffer));
        }

        // Precondition.checkReference(buffer);
        // Precondition.checkBounds(buffer.length, offset, getMessageLength());
        setNumberOfGroupRecords((short) this.groupRecords.size());
        super.writeTo(buffer);
        Iterator<MLDGroupRecord> iter = this.groupRecords.iterator();
        while (iter.hasNext()) {
            iter.next().writeTo(buffer);
        }
    }

    @Override
    public void writeChecksum(final ByteBuffer buffer,
                              final byte[] sourceAddress,
                              final byte[] destinationAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("MLDv2ReportMessage.writeChecksum",
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
     * @return
     */
    public int getNumberOfGroupRecords() {
        return NumberOfGroupRecords.get(getBufferInternal());
    }

    /**
     * @param numberOfGroupRecords
     */
    protected void setNumberOfGroupRecords(final short numberOfGroupRecords) {

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.log.entry("MLDv2ReportMessage.setNumberOfGroupRecords", numberOfGroupRecords));
        }

        NumberOfGroupRecords.set(getBufferInternal(), numberOfGroupRecords);
    }

    /**
     * @param groupRecord
     * @return
     */
    public int addGroupRecord(final MLDGroupRecord groupRecord) {

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.log.entry("MLDv2ReportMessage.addGroupRecord", groupRecord));
        }

        // Precondition.checkReference(groupRecord);
        int index = this.groupRecords.size();
        this.groupRecords.add(groupRecord);
        setNumberOfGroupRecords((short) this.groupRecords.size());
        return index;
    }

    /**
     * @param index
     */
    public void removeGroupRecord(final int index) {
        this.groupRecords.remove(index);
    }

    /**
     * @param index
     * @return
     */
    public MLDGroupRecord getGroupRecord(final int index) {
        return this.groupRecords.get(index);
    }

}
