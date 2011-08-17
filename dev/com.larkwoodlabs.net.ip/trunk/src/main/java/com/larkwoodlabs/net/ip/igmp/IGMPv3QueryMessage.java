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

package com.larkwoodlabs.net.ip.igmp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.ipv4.IPv4Packet;
import com.larkwoodlabs.util.buffer.fields.BooleanField;
import com.larkwoodlabs.util.buffer.fields.ByteBitField;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.buffer.fields.ShortField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An IGMPv3 Membership Query Message.
 * [See <a href="http://www.ietf.org/rfc/rfc3376.txt">RFC-3376</a>]
 * <pre>
 * 4.1. Membership Query Message
 * 
 *    Membership Queries are sent by IP multicast routers to query the
 *    multicast reception state of neighboring interfaces.  Queries have
 *    the following format:
 *    
 *        0                   1                   2                   3
 *        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |  Type = 0x11  | Max Resp Code |           Checksum            |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |                         Group Address                         |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       | Resv  |S| QRV |     QQIC      |     Number of Sources (N)     |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |                       Source Address [1]                      |
 *       +-                                                             -+
 *       |                       Source Address [2]                      |
 *       +-                              .                              -+
 *       .                               .                               .
 *       .                               .                               .
 *       +-                                                             -+
 *       |                       Source Address [N]                      |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 * 4.1.1. Max Resp Code
 * 
 *    The Max Resp Code field specifies the maximum time allowed before
 *    sending a responding report.  The actual time allowed, called the Max
 *    Resp Time, is represented in units of 1/10 second and is derived from
 *    the Max Resp Code as follows:
 * 
 *    If Max Resp Code &lt; 128, Max Resp Time = Max Resp Code
 * 
 *    If Max Resp Code &gt;= 128, Max Resp Code represents a floating-point
 *    value as follows:
 * 
 *        0 1 2 3 4 5 6 7
 *       +-+-+-+-+-+-+-+-+
 *       |1| exp | mant  |
 *       +-+-+-+-+-+-+-+-+
 * 
 *    Max Resp Time = (mant | 0x10) &lt;&lt; (exp + 3)
 * 
 *    Small values of Max Resp Time allow IGMPv3 routers to tune the &quot;leave
 *    latency&quot; (the time between the moment the last host leaves a group
 *    and the moment the routing protocol is notified that there are no
 *    more members).  Larger values, especially in the exponential range,
 *    allow tuning of the burstiness of IGMP traffic on a network.
 * 
 * 4.1.2. Checksum
 * 
 *    The Checksum is the 16-bit one's complement of the one's complement
 *    sum of the whole IGMP message (the entire IP payload).  For computing
 *    the checksum, the Checksum field is set to zero.  When receiving
 *    packets, the checksum MUST be verified before processing a packet.
 *    [RFC-1071]
 * 
 * 4.1.3. Group Address
 * 
 *    The Group Address field is set to zero when sending a General Query,
 *    and set to the IP multicast address being queried when sending a
 *    Group-Specific Query or Group-and-Source-Specific Query (see section
 *    4.1.9, below).
 * 
 * 4.1.4. Resv (Reserved)
 * 
 *    The Resv field is set to zero on distribution, and ignored on
 *    reception.
 * 
 * 4.1.5. S Flag (Suppress Router-Side Processing)
 * 
 *    When set to one, the S Flag indicates to any receiving multicast
 *    routers that they are to suppress the normal timer updates they
 *    perform upon hearing a Query.  It does not, however, suppress the
 *    querier election or the normal &quot;host-side&quot; processing of a Query that
 *    a router may be required to perform as a consequence of itself being
 *    a group member.
 * 
 * 4.1.6. QRV (Querier's Robustness Variable)
 * 
 *    If non-zero, the QRV field contains the [Robustness Variable] value
 *    used by the querier, i.e., the sender of the Query.  If the querier's
 *    [Robustness Variable] exceeds 7, the maximum value of the QRV field,
 *    the QRV is set to zero.  Routers adopt the QRV value from the most
 *    recently received Query as their own [Robustness Variable] value,
 *    unless that most recently received QRV was zero, in which case the
 *    receivers use the default [Robustness Variable] value specified in
 *    section 8.1 or a statically configured value.
 * 
 * 4.1.7. QQIC (Querier's Query Interval Code)
 * 
 *    The Querier's Query Interval Code field specifies the [Query
 *    Interval] used by the querier.  The actual interval, called the
 *    Querier's Query Interval (QQI), is represented in units of seconds
 *    and is derived from the Querier's Query Interval Code as follows:
 * 
 *    If QQIC &lt; 128, QQI = QQIC
 * 
 *    If QQIC &gt;= 128, QQIC represents a floating-point value as follows:
 * 
 *        0 1 2 3 4 5 6 7
 *       +-+-+-+-+-+-+-+-+
 *       |1| exp | mant  |
 *       +-+-+-+-+-+-+-+-+
 * 
 *    QQI = (mant | 0x10) &lt;&lt; (exp + 3)
 * 
 *    Multicast routers that are not the current querier adopt the QQI
 *    value from the most recently received Query as their own [Query
 *    Interval] value, unless that most recently received QQI was zero, in
 *    which case the receiving routers use the default [Query Interval]
 *    value specified in section 8.2.
 * 
 * 4.1.8. Number of Sources (N)
 * 
 *    The Number of Sources (N) field specifies how many source addresses
 *    are present in the Query.  This number is zero in a General Query or
 *    a Group-Specific Query, and non-zero in a Group-and-Source-Specific
 *    Query.  This number is limited by the MTU of the network over which
 *    the Query is transmitted.  For example, on an Ethernet with an MTU of
 *    1500 octets, the IP header including the Router Alert option consumes
 *    24 octets, and the IGMP fields up to including the Number of Sources
 *    (N) field consume 12 octets, leaving 1464 octets for source
 *    addresses, which limits the number of source addresses to 366
 *    (1464/4).
 * 
 * 4.1.9. Source Address [i]
 * 
 *    The Source Address [i] fields are a vector of n IP unicast addresses,
 *    where n is the value in the Number of Sources (N) field.
 * 
 * 4.1.10. Additional Data
 * 
 *    If the Packet Length field in the IP header of a received Query
 *    indicates that there are additional octets of data present, beyond
 *    the fields described here, IGMPv3 implementations MUST include those
 *    octets in the computation to verify the received IGMP Checksum, but
 *    MUST otherwise ignore those additional octets.  When sending a Query,
 *    an IGMPv3 implementation MUST NOT include additional octets beyond
 *    the fields described here.
 * 
 * 4.1.11. Query Variants
 * 
 *    There are three variants of the Query message:
 * 
 *    1. A &quot;General Query&quot; is sent by a multicast router to learn the
 *       complete multicast reception state of the neighboring interfaces
 *       (that is, the interfaces attached to the network on which the
 *       Query is transmitted).  In a General Query, both the Group Address
 *       field and the Number of Sources (N) field are zero.
 * 
 *    2. A &quot;Group-Specific Query&quot; is sent by a multicast router to learn
 *       the reception state, with respect to a *single* multicast address,
 *       of the neighboring interfaces.  In a Group-Specific Query, the
 *       Group Address field contains the multicast address of interest,
 *       and the Number of Sources (N) field contains zero.
 * 
 *    3. A &quot;Group-and-Source-Specific Query&quot; is sent by a multicast router
 *       to learn if any neighboring interface desires reception of packets
 *       sent to a specified multicast address, from any of a specified
 *       list of sources.  In a Group-and-Source-Specific Query, the Group
 *       Address field contains the multicast address of interest, and the
 *       Source Address [i] fields contain the source address(es) of
 *       interest.
 * 
 * 4.1.12. IP Destination Addresses for Queries
 * 
 *    In IGMPv3, General Queries are sent with an IP destination address of
 *    224.0.0.1, the all-systems multicast address.  Group-Specific and
 *    Group-and-Source-Specific Queries are sent with an IP destination
 *    address equal to the  multicast address of interest.  *However*, a
 *    system MUST accept and  process any Query whose IP Destination
 *    Address field contains *any* of the addresses (unicast or multicast)
 *    assigned to the interface on which the Query arrives.
 * </pre>
 * 
 * @author Gregory Bumgardner
 * 
 */
public final class IGMPv3QueryMessage extends IGMPQueryMessage {

    /*-- Inner Classes ------------------------------------------------------*/
    
    public static final class Parser implements IGMPMessage.ParserType {

        @Override
        public IGMPMessage parse(ByteBuffer buffer) throws ParseException {
            return new IGMPv3QueryMessage(buffer);
        }

        @Override
        public boolean verifyChecksum(ByteBuffer segment) {
            return IGMPv3QueryMessage.verifyChecksum(segment);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/
    
    public static final int BASE_MESSAGE_LENGTH = 12;
    public static final int DEFAULT_ROBUSTNESS_VALUE = 2;
    public static final int DEFAULT_QUERY_INTERVAL_VALUE = 125; //secs

    public static final BooleanField SuppressRouterSideProcessing = new BooleanField(8,3);
    public static final ByteBitField QuerierRobustnessVariable = new ByteBitField(8,0,3);
    public static final ByteField    QuerierQueryIntervalCode = new ByteField(9);
    public static final ShortField   NumberOfSources = new ShortField(10);

 
    /*-- Static Functions ---------------------------------------------------*/
    
    public static IGMPMessage.Parser getIGMPMessageParser() {
        return getIGMPMessageParser(new IGMPv3QueryMessage.Parser());
    }

    public static IPMessage.Parser getIPMessageParser() {
        return getIPMessageParser(new IGMPv3QueryMessage.Parser());
    }

    public static IPv4Packet.Parser getIPv4PacketParser() {
        return getIPv4PacketParser(new IGMPv3QueryMessage.Parser());
    }

    public static IPPacket.Parser getIPPacketParser() {
        return getIPPacketParser(new IGMPv3QueryMessage.Parser());
    }

    /**
     * Verifies the IGMP message checksum. Called by the parser prior to constructing the packet.
     * @param segment - the buffer segment containing the IGMP message.
     */
    public static boolean verifyChecksum(ByteBuffer buffer) {
        return Checksum.get(buffer) == IGMPMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH + (NumberOfSources.get(buffer) * 4));
    }

    /**
     * Writes the IGMP message checksum into a buffer containing an IGMP message.
     */
    public static void setChecksum(ByteBuffer buffer) {
        Checksum.set(buffer, IGMPMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH + (NumberOfSources.get(buffer) * 4)));
    }


    /*-- Member Variables ---------------------------------------------------*/

    private Vector<byte[]> sources = new Vector<byte[]>();


    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs general query
     * @param maximumResponseTime
     * @param groupAddress
     */
    public IGMPv3QueryMessage(short maximumResponseTime) {
        super(BASE_MESSAGE_LENGTH, maximumResponseTime);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3QueryMessage.IGMPv3QueryMessage", maximumResponseTime));
        }
        
        setSuppressRouterSideProcessing(false);
        setQuerierRobustnessVariable((byte)DEFAULT_ROBUSTNESS_VALUE);
        setQuerierQueryIntervalCode((byte)DEFAULT_QUERY_INTERVAL_VALUE);
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param maximumResponseTime
     * @param groupAddress
     */
    public IGMPv3QueryMessage(short maximumResponseTime, byte[] groupAddress) {
        super(BASE_MESSAGE_LENGTH, maximumResponseTime, groupAddress);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3QueryMessage.IGMPv3QueryMessage", maximumResponseTime, Logging.address(groupAddress)));
        }
        
        setSuppressRouterSideProcessing(false);
        setQuerierRobustnessVariable((byte)DEFAULT_ROBUSTNESS_VALUE);
        setQuerierQueryIntervalCode((byte)DEFAULT_QUERY_INTERVAL_VALUE);
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public IGMPv3QueryMessage(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3QueryMessage.IGMPv3QueryMessage", buffer));
        }
        
        int numberOfSources = getNumberOfSources();
        if (numberOfSources > 0) {
            for (int i = 0; i < numberOfSources; i++) {
                byte[] address = new byte[4];
                buffer.get(address);
                this.sources.add(address);
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
    
    private void logState(Logger logger) {
        logger.info(ObjectId + " : suppress-router-side-processing="+getSuppressRouterSideProcessing());
        logger.info(ObjectId + " : querier-robustness-variable="+getQuerierRobustnessVariable());
        logger.info(ObjectId + " : querier-query-interval-code="+getQuerierQueryIntervalCode()+" "+getQueryIntervalTime()+"s");
        logger.info(ObjectId + " : number-of-sources="+getNumberOfSources());
        logger.info(ObjectId + " ----> sources");
        for(int i= 0; i<getNumberOfSources(); i++) {
            logger.info(ObjectId + " : source["+i+"]="+Logging.address(getSource(i)));
        }
        logger.info(ObjectId + " <---- sources");
    }

    @Override
    public void writeTo(ByteBuffer buffer) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3QueryMessage.writeTo", buffer));
        }
        
        //Precondition.checkReference(buffer);
        //Precondition.checkBounds(buffer.length, offset, getMessageLength());
        super.writeTo(buffer);
        Iterator<byte[]> iter = this.sources.iterator();
        while (iter.hasNext()) {
            byte[] address = iter.next();
            buffer.put(address);
        }
    }
    
    @Override
    public void writeChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3QueryMessage.writeChecksum", buffer, Logging.address(sourceAddress), Logging.address(destinationAddress)));
        }

        IGMPv3QueryMessage.setChecksum(buffer);
    }

    @Override
    public int getMessageLength() {
        return BASE_MESSAGE_LENGTH + getNumberOfSources() * 4;
    }

    /**
     * Returns the "<code>Max Resp Time</code>" value in milliseconds.
     * This value is calculated from {@linkplain #MaxRespCode Max Resp Code}.
     * @return
     */
    public int getMaximumResponseTime() {
        byte maxRespCode = getMaxRespCode();
        int exponent = ((maxRespCode >> 4 ) & 0x7);
        int mantissa = maxRespCode & 0x0F;
        return 100 * (int)((mantissa | 0x10) << (exponent + 3)); // Floating point tenths to milliseconds.
    }

    /**
     * Sets the "<code>Max Resp Time</code>" value in milliseconds.
     * This value is converted into a {@linkplain #MaxRespCode Max Resp Code} value.
     * @return
     */
    public void setMaximumResponseTime(int milliseconds) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3QueryMessage.setMaximumResponseTime", milliseconds));
        }
        
        short tenths = (short)(milliseconds / 100);
        setMaxRespCode(convertTimeVal(tenths));
    }

    /**
     * Gets the "Suppress Router-Side Processing" flag value.
     * <pre>
     * 4.1.5. S Flag (Suppress Router-Side Processing)
     * 
     *    When set to one, the S Flag indicates to any receiving multicast
     *    routers that they are to suppress the normal timer updates they
     *    perform upon hearing a Query.  It does not, however, suppress the
     *    querier election or the normal &quot;host-side&quot; processing of a Query that
     *    a router may be required to perform as a consequence of itself being
     *    a group member.
     *
     *       +-+-+-+-+-+-+-+-+
     *       |       |S|     | Byte #8
     *       +-+-+-+-+-+-+-+-+
     * </pre>
     */
    public boolean getSuppressRouterSideProcessing() {
        return SuppressRouterSideProcessing.get(getBufferInternal());
    }

    /**
     * Sets the "Suppress Router-Side Processing" flag value.
     * See {@link #getSuppressRouterSideProcessing()}
     * @param suppressRouterSideProcessing
     */
    public void setSuppressRouterSideProcessing(boolean suppressRouterSideProcessing) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3QueryMessage.setSuppressRouterSideProcessing", suppressRouterSideProcessing));
        }
        
        SuppressRouterSideProcessing.set(getBufferInternal(),suppressRouterSideProcessing);
    }


    /**
     * Gets the "Querier's Robustness Variable" field value.
     * <pre>
     * 4.1.6. QRV (Querier's Robustness Variable)
     * 
     *    If non-zero, the QRV field contains the [Robustness Variable] value
     *    used by the querier, i.e., the sender of the Query.  If the querier's
     *    [Robustness Variable] exceeds 7, the maximum value of the QRV field,
     *    the QRV is set to zero.  Routers adopt the QRV value from the most
     *    recently received Query as their own [Robustness Variable] value,
     *    unless that most recently received QRV was zero, in which case the
     *    receivers use the default [Robustness Variable] value specified in
     *    section 8.1 or a statically configured value.
     *
     *       +-+-+-+-+-+-+-+-+
     *       | Resv  |S| QRV | Byte #8
     *       +-+-+-+-+-+-+-+-+
     * </pre>
     * See {@link #setQuerierRobustnessVariable(byte)}.
     */
    public byte getQuerierRobustnessVariable() {
        return QuerierRobustnessVariable.get(getBufferInternal());
    }

    /**
     * Sets the "Querier's Robustness Variable" field value.
     * See {@link #getQuerierRobustnessVariable()}.
     */
    public void setQuerierRobustnessVariable(byte querierRobustnessVariable) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3QueryMessage.setQuerierRobustnessVariable", querierRobustnessVariable));
        }
        
        QuerierRobustnessVariable.set(getBufferInternal(),querierRobustnessVariable);
    }

    /**
     * Gets the "Querier's Query Interval Code" field value.
     * See {@link #setQuerierQueryIntervalCode(byte)} and {@link #setQueryIntervalTime(int)}.
     * <pre>
     * 4.1.7. QQIC (Querier's Query Interval Code)
     * 
     *    The Querier's Query Interval Code field specifies the [Query
     *    Interval] used by the querier.  The actual interval, called the
     *    Querier's Query Interval (QQI), is represented in units of seconds
     *    and is derived from the Querier's Query Interval Code as follows:
     * 
     *    If QQIC &lt; 128, QQI = QQIC
     * 
     *    If QQIC &gt;= 128, QQIC represents a floating-point value as follows:
     * 
     *        0 1 2 3 4 5 6 7
     *       +-+-+-+-+-+-+-+-+
     *       |1| exp | mant  |  Byte #9
     *       +-+-+-+-+-+-+-+-+
     * 
     *    QQI = (mant | 0x10) &lt;&lt; (exp + 3)
     * 
     *    Multicast routers that are not the current querier adopt the QQI
     *    value from the most recently received Query as their own [Query
     *    Interval] value, unless that most recently received QQI was zero, in
     *    which case the receiving routers use the default [Query Interval]
     *    value specified in section 8.2.
     * </pre>
     */
    public byte getQuerierQueryIntervalCode() {
        return QuerierQueryIntervalCode.get(getBufferInternal());
    }

    /**
     * Sets the "Querier's Query Interval Code" field value.
     * See {@link #setQuerierQueryIntervalCode(byte)} and {@link #setQueryIntervalTime(int)}.
     */
    public void setQuerierQueryIntervalCode(byte querierQueryIntervalCode) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3QueryMessage.setQuerierQueryIntervalCode", querierQueryIntervalCode));
        }
        
        QuerierQueryIntervalCode.set(getBufferInternal(),querierQueryIntervalCode);
    }
    
    /**
     * Gets the query interval time in seconds.
     * See {@link #getQuerierQueryIntervalCode()}.
     */
    public int getQueryIntervalTime() {
        byte qqic = getQuerierQueryIntervalCode();
        if (qqic < 128) {
            return qqic;
        }
        else {
            int exponent = ((qqic >> 4 ) & 0x7);
            int mantissa = qqic & 0x0F;
            return (int)((mantissa | 0x10) << (exponent + 3)); // Floating point to seconds.
        }
    }

    /**
     * Sets the query interval time in seconds.
     * See {@link #setQuerierQueryIntervalCode(byte)}.
     */
    public void setQueryIntervalTime(int seconds) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3QueryMessage.setQueryIntervalTime", seconds));
        }
        
        if (seconds < 128) {
            setQuerierQueryIntervalCode((byte)seconds);
        }
        else {
            // convert to floating point
            setQuerierQueryIntervalCode(IGMPMessage.convertTimeVal((short)seconds));
        }
    }

    /**
     * 
     * @return
     */
    public int getNumberOfSources() {
        return NumberOfSources.get(getBufferInternal());
    }

    /**
     * 
     * @param numberOfSources
     */
    protected void setNumberOfSources(int numberOfSources) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3QueryMessage.setNumberOfSources", numberOfSources));
        }
        
        NumberOfSources.set(getBufferInternal(),(short)numberOfSources);
    }
       
    /**
     * 
     * @param sourceAddress
     * @throws UnknownHostException
     */
    public void addSource(InetAddress sourceAddress) throws UnknownHostException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3QueryMessage.addSource", Logging.address(sourceAddress)));
        }
        
        addSource(sourceAddress.getAddress());
    }

    /**
     * 
     * @param sourceAddress
     */
    public void addSource(byte[] sourceAddress) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv3QueryMessage.addSource", Logging.address(sourceAddress)));
        }
        
        this.sources.add(sourceAddress.clone());
        setNumberOfSources((short)this.sources.size());
    }
    
    /**
     * 
     * @param index
     * @return
     */
    public byte[] getSource(int index) {
        if (this.sources.size() > 0) {
            return this.sources.get(index);
        }
        else {
            byte[] address = new byte[4];
            ByteBuffer buffer = getBufferInternal();
            buffer.position(BASE_MESSAGE_LENGTH + (index*4));
            buffer.get(address);
            buffer.rewind();
            return address;
        }
    }
    
    /**
     * 
     * @return
     */
    public Iterator<byte[]> getSourceIterator() {
        return this.sources.iterator();
    }
}
