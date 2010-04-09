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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.igmp.IGMPMessage;
import com.larkwoodlabs.util.buffer.fields.BooleanField;
import com.larkwoodlabs.util.buffer.fields.ByteBitField;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.buffer.fields.ShortField;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

/**
 * A Multicast Listener Query Message as described in
 * [<a href="http://tools.ietf.org/html/rfc3810">RFC-3810</a>].
 * 
 * <h2>5.1.  Multicast Listener Query Message</h2>
 *    Multicast Listener Queries are sent by multicast routers in Querier
 *    State to query the multicast listening state of neighboring
 *    interfaces.  Queries have the following format:
 * <pre> 
 *   0               1               2               3
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |  Type = 130   |      Code     |           Checksum            |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |    Maximum Response Code      |           Reserved            |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +                      Multicast Address                        +
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  | Resv  |S| QRV |     QQIC      |     Number of Sources (N)     |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +                      Source Address [1]                       +
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +                      Source Address [2]                       +
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  .                               .                               .
 *  .                               .                               .
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +                      Source Address [N]                       +
 *  |                                                               |
 *  +                                                               +
 *  |                                                               |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * <h3>5.1.1.  Code</h3>
 * 
 *    Initialized to zero by the sender; ignored by receivers.
 * 
 * <h3>5.1.2.  Checksum</h3>
 * 
 *    The standard ICMPv6 checksum; it covers the entire MLDv2 message,
 *    plus a &quot;pseudo-header&quot; of IPv6 header fields [RFC2463].  For
 *    computing the checksum, the Checksum field is set to zero.  When a
 *    packet is received, the checksum MUST be verified before processing
 *    it.<p>
 *    See {@link #getChecksum()}, {@link #setChecksum(short)},
 *    {@link #calculateChecksum(ByteBuffer, int, byte[], byte[])} and
 *    {@link #verifyChecksum(ByteBuffer, byte[], byte[])}.
 * 
 * <h3>5.1.3.  Maximum Response Code</h3>
 * 
 *    The Maximum Response Code field specifies the maximum time allowed
 *    before sending a responding Report.  The actual time allowed, called
 *    the Maximum Response Delay, is represented in units of milliseconds,
 *    and is derived from the Maximum Response Code as follows:<p>
 * 
 *    If Maximum Response Code &lt; 32768,
 *       Maximum Response Delay = Maximum Response Code<p>
 * 
 *    If Maximum Response Code &gt;=32768, Maximum Response Code represents a
 *    floating-point value as follows:
 * <pre>
 *     0 1 2 3 4 5 6 7 8 9 A B C D E F
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |1| exp |          mant         |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *    Maximum Response Delay = (mant | 0x1000) &lt;&lt; (exp+3)<p>
 *    
 *    Small values of Maximum Response Delay allow MLDv2 routers to tune
 *    the &quot;leave latency&quot; (the time between the moment the last node on a
 *    link ceases to listen to a specific multicast address and the moment
 *    the routing protocol is notified that there are no more listeners for
 *    that address).  Larger values, especially in the exponential range,
 *    allow the tuning of the burstiness of MLD traffic on a link.<p>
 *    See {@link #getMaximumResponseDelay()} and {@link #setMaximumResponseDelay(short)}.
 * 
 * <h3>5.1.4.  Reserved</h3>
 * 
 *    Initialized to zero by the sender; ignored by receivers.
 * 
 * <h3>5.1.5.  Multicast Address</h3>
 * 
 *    For a General Query, the Multicast Address field is set to zero.  For
 *    a Multicast Address Specific Query or Multicast Address and Source
 *    Specific Query, it is set to the multicast address being queried (see
 *    section 5.1.10, below).<p>
 *    See {@link #getGroupAddress()}, {@link #setGroupAddress(byte[])}
 *    and {@link #setGroupAddress(InetAddress)}.
 * 
 * <h3>5.1.7.  S Flag (Suppress Router-Side Processing)</h3>
 * 
 *    When set to one, the S Flag indicates to any receiving multicast
 *    routers that they have to suppress the normal timer updates they
 *    perform upon hearing a Query.  Nevertheless, it does not suppress the
 *    querier election or the normal &quot;host-side&quot; processing of a Query that
 *    a router may be required to perform as a consequence of itself being
 *    a multicast listener.<p>
 *    See {@link #getSuppressRouterSideProcessing()} and {@link #setSuppressRouterSideProcessing(boolean)}.
 * 
 * <h3>5.1.8.  QRV (Querier's Robustness Variable)</h3>
 * 
 *    If non-zero, the QRV field contains the [Robustness Variable] value
 *    used by the Querier.  If the Querier's [Robustness Variable] exceeds
 *    7 (the maximum value of the QRV field), the QRV field is set to zero.
 * 
 *    Routers adopt the QRV value from the most recently received Query as
 *    their own [Robustness Variable] value, unless that most recently
 *    received QRV was zero, in which case they use the default [Robustness
 *    Variable] value specified in section 9.1, or a statically configured
 *    value.<p>
 *    See {@link #getQuerierRobustnessVariable()} and {@link #setQuerierRobustnessVariable(byte)}.
 * 
 * <h3>5.1.9.  QQIC (Querier's Query Interval Code)</h3>
 * 
 *    The Querier's Query Interval Code field specifies the [Query
 *    Interval] used by the Querier.  The actual interval, called the
 *    Querier's Query Interval (QQI), is represented in units of seconds,
 *    and is derived from the Querier's Query Interval Code as follows:<p>
 * 
 *    If QQIC &lt; 128, QQI = QQIC<p>
 * 
 *    If QQIC &gt;= 128, QQIC represents a floating-point value as follows:
 * <pre>
 *        0 1 2 3 4 5 6 7
 *    +-+-+-+-+-+-+-+-+
 *    |1| exp | mant  |
 *    +-+-+-+-+-+-+-+-+
 * </pre>
 *    QQI = (mant | 0x10) &lt;&lt; (exp + 3)<p>
 * 
 *    Multicast routers that are not the current Querier adopt the QQI
 *    value from the most recently received Query as their own [Query
 *    Interval] value, unless that most recently received QQI was zero, in
 *    which case the receiving routers use the default [Query Interval]
 *    value specified in section 9.2.<p>
 *    See {@link #getQuerierQueryIntervalCode()}, {@link #setQuerierQueryIntervalCode(byte)},
 *    {@link #getQueryIntervalTime()} and {@link #setQueryIntervalTime(int)}.
 * 
 * <h3>5.1.10.  Number of Sources (N)</h3>
 * 
 *    The Number of Sources (N) field specifies how many source addresses
 *    are present in the Query.  This number is zero in a General Query or
 *    a Multicast Address Specific Query, and non-zero in a Multicast
 *    Address and Source Specific Query.  This number is limited by the MTU
 *    of the link over which the Query is transmitted.  For example, on an
 *    Ethernet link with an MTU of 1500 octets, the IPv6 header (40 octets)
 *    together with the Hop-By-Hop Extension Header (8 octets) that
 *    includes the Router Alert option consume 48 octets; the MLD fields up
 *    to the Number of Sources (N) field consume 28 octets; thus, there are
 *    1424 octets left for source addresses, which limits the number of
 *    source addresses to 89 (1424/16).<p>
 *    See {@link #getNumberOfSources()}.
 * 
 * <h3>5.1.11.  Source Address [i]</h3>
 * 
 *    The Source Address [i] fields are a vector of n unicast addresses,
 *    where n is the value in the Number of Sources (N) field.<p>
 *    See {@link #getSource(int)}, {@link #getSourceIterator()},
 *    {@link #addSource(byte[])} and {@link #addSource(InetAddress)}.
 * 
 * <h3>5.1.12.  Additional Data</h3>
 * 
 *    If the Payload Length field in the IPv6 header of a received Query
 *    indicates that there are additional octets of data present, beyond
 *    the fields described here, MLDv2 implementations MUST include those
 *    octets in the computation to verify the received MLD Checksum, but
 *    MUST otherwise ignore those additional octets.  When sending a Query,
 *    an MLDv2 implementation MUST NOT include additional octets beyond the
 *    fields described above.<p>
 * 
 * @author Gregory Bumgardner
 * 
 */
public final class MLDv2QueryMessage extends MLDQueryMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser implements MLDMessage.ParserType {

        @Override
        public MLDMessage parse(ByteBuffer buffer) throws ParseException {
            return new MLDv1QueryMessage(buffer);
        }

        @Override
        public boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) throws MissingParserException, ParseException {
            return MLDv2QueryMessage.verifyChecksum(buffer, sourceAddress, destinationAddress);
        }

        @Override
        public Object getKey() {
            return MessageType;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/
    
    public static final int BASE_MESSAGE_LENGTH = 28;
    
    public static final ShortField      MaximumResponseCode = new ShortField(4);
    public static final ByteBitField    Reserved = new ByteBitField(24,4,4);
    public static final BooleanField    SuppressRouterSideProcessing = new BooleanField(24,3);
    public static final ByteBitField    QuerierRobustnessVariable = new ByteBitField(24,0,3);
    public static final ByteField       QuerierQueryIntervalCode = new ByteField(25);
    public static final ShortField      NumberOfSources = new ShortField(27);


    /*-- Static Functions ---------------------------------------------------*/
    
    /**
     * Verifies the MLD message checksum. Called by the parser prior to constructing the packet.
     * @param segment - the buffer segment containing the MLD message.
     * @param sourceAddress An IPv6 (16-byte) address..
     * @param destinationAddress An IPv6 (16-byte) address.
     */
    public static boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        return Checksum.get(buffer) == MLDMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH + (NumberOfSources.get(buffer) * 16), sourceAddress, destinationAddress);
    }

    /**
     * Writes the MLD message checksum into a buffer containing an MLD message.
     * @param buffer - a byte array.
     * @param offset - the offset within the array at which to write the message.
     * @param sourceAddress An IPv6 (16-byte) address..
     * @param destinationAddress An IPv6 (16-byte) address.
     */
    public static void setChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        Checksum.set(buffer, MLDMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH + (NumberOfSources.get(buffer) * 16), sourceAddress, destinationAddress));
    }


    /*-- Member Variables ---------------------------------------------------*/

    private Vector<byte[]> sources = new Vector<byte[]>();


    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param groupAddress
     */
    public MLDv2QueryMessage(byte[] groupAddress) {
        super(BASE_MESSAGE_LENGTH,groupAddress);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDv2QueryMessage.MLDv2QueryMessage", Logging.address(groupAddress)));
        }
        
        Reserved.set(getBufferInternal(), (byte)0);
        setSuppressRouterSideProcessing(false);
        setQuerierRobustnessVariable((byte)0);
        setQuerierQueryIntervalCode((byte)0);
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public MLDv2QueryMessage(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDv2QueryMessage.MLDv2QueryMessage", buffer));
        }

        int numberOfSources = getNumberOfSources();
        if (numberOfSources > 0) {
            for (int i = 0; i < numberOfSources; i++) {
                byte[] address = new byte[16];
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
    
    /**
     * 
     * @param logger
     */
    private void logState(Logger logger) {
        logger.info(ObjectId + " : suppress-router-side-processing="+getSuppressRouterSideProcessing());
        logger.info(ObjectId + " : querier-robustness-variable="+getQuerierRobustnessVariable());
        logger.info(ObjectId + " : querier-query-interval-code="+getQuerierQueryIntervalCode()+" "+getQueryIntervalTime()+"ms");
        logger.info(ObjectId + " : number-of-sources="+getNumberOfSources());
        logger.info(ObjectId + " ----> start sources");
        for(int i= 0; i<getNumberOfSources(); i++) {
            logger.info(ObjectId +" : source["+i+"]="+Logging.address(getSource(i)));
        }
        logger.info(ObjectId +" <---- end sources");
    }

    
    /**
     * Writes the message into a buffer.
     * Call {@link #updateChecksum(byte[], byte[], int)} prior to calling this method.
     */
    @Override
    public void writeTo(ByteBuffer buffer) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDv2QueryMessage.writeTo", buffer));
        }
        
        super.writeTo(buffer);
        Iterator<byte[]> iter = this.sources.iterator();
        while (iter.hasNext()) {
            buffer.put(iter.next());
        }
    }
    
    @Override
    public void writeChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                        "MLDv2QueryMessage.writeChecksum",
                                        buffer,
                                        Logging.address(sourceAddress),
                                        Logging.address(destinationAddress)));
        }
        
        MLDv2QueryMessage.setChecksum(buffer, sourceAddress, destinationAddress);
    }
    
    @Override
    public int getMessageLength() {
        return BASE_MESSAGE_LENGTH + getNumberOfSources() * 16;
    }

    /**
     * Returns the "<code>Maximum Response Delay</code>" value in milliseconds.
     * This value is calculated from {@linkplain #MaximumResponseCode Maximum Response Code} value.
     * 
     * The Maximum Response Code field specifies the maximum time allowed
     * before sending a responding Report. The actual time allowed, called
     * the Maximum Response Delay, is represented in units of milliseconds,
     * and is derived from the Maximum Response Code as follows:
     * 
     * If Maximum Response Code < 32768, Maximum Response Delay = Maximum
     * Response Code
     * 
     * If Maximum Response Code >=32768, Maximum Response Code represents a
     * floating-point value as follows:
     * <pre>
     *   0 1 2 3 4 5 6 7 8 9 A B C D E F
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |1| exp |         mant          |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     * Maximum Response Delay = (mant | 0x1000) << (exp+3)
     */
    @Override
    public short getMaximumResponseDelay() {
        /*
         * The Maximum Response Code field specifies the maximum time allowed
         * before sending a responding Report. The actual time allowed, called
         * the Maximum Response Delay, is represented in units of milliseconds,
         * and is derived from the Maximum Response Code as follows:
         * 
         * If Maximum Response Code < 32768, Maximum Response Delay = Maximum
         * Response Code
         * 
         * If Maximum Response Code >=32768, Maximum Response Code represents a
         * floating-point value as follows:
         * 
         *   0 1 2 3 4 5 6 7 8 9 A B C D E F
         *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *  |1| exp |         mant          |
         *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         * 
         * Maximum Response Delay = (mant | 0x1000) << (exp+3)
         */
        short maxRespCode = MaximumResponseCode.get(getBufferInternal());
        if (maxRespCode < 32768) {
            return maxRespCode;
        }
        else {
            int exponent = ((maxRespCode >> 12 ) & 0x7);
            int mantissa = maxRespCode & 0xFFF;
            return (short)((mantissa | 0x1000) << (exponent + 3)); // Floating point milliseconds.
        }
    }

    /**
     * Sets the "<code>Maximum Response Delay</code>" value in milliseconds.
     * This value is converted into a {@linkplain #MaximumResponseCode Maximum Response Code} value.
     * @see #getMaximumResponseDelay()
     * @return
     */
    @Override
    public void setMaximumResponseDelay(short milliseconds) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDv2QueryMessage.setMaximumResponseDelay", milliseconds));
        }
        
        if (milliseconds < 32768) {
            MaximumResponseCode.set(getBufferInternal(), milliseconds);
        }
        else {
            // convert to fixed then floating point
            int fp = milliseconds << 16; // fixed point 16.16
            int exponent = 3;
            while(fp > 0x01FFFFFF) {
                fp = ((fp >> 1) & 0x7FFFFFFF);
                exponent++;
            }
            short mantissa = (short)((fp >> 8) & 0x00FF);
            MaximumResponseCode.set(getBufferInternal(), (short)(0x8000 | (exponent << 12) | mantissa));
        }
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
            logger.finer(Logging.entering(ObjectId,"MLDv2QueryMessage.setSuppressRouterSideProcessing", suppressRouterSideProcessing));
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
            logger.finer(Logging.entering(ObjectId, "MLDv2QueryMessage.setQuerierRobustnessVariable", querierRobustnessVariable));
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
            logger.finer(Logging.entering(ObjectId, "MLDv2QueryMessage.setQuerierQueryIntervalCode", querierQueryIntervalCode));
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
            logger.finer(Logging.entering(ObjectId, "MLDv2QueryMessage.setQueryIntervalTime", seconds));
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
            logger.finer(Logging.entering(ObjectId, "MLDv2QueryMessage.setNumberOfSources", numberOfSources));
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
            logger.finer(Logging.entering(ObjectId, "MLDv2QueryMessage.addSource", sourceAddress));
        }
        
        addSource(sourceAddress.getAddress());
    }

    /**
     * 
     * @param sourceAddress
     */
    public void addSource(byte[] sourceAddress) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDv2QueryMessage.addSource", Logging.address(sourceAddress)));
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
            byte[] address = new byte[16];
            ByteBuffer buffer = getBufferInternal();
            buffer.position(BASE_MESSAGE_LENGTH + (index*16));
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
