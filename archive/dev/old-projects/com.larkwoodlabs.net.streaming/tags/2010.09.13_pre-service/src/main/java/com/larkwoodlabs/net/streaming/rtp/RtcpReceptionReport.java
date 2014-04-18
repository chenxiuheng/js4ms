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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.buffer.BufferBackedObject;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.buffer.fields.IntegerBitField;
import com.larkwoodlabs.util.buffer.fields.IntegerField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An RTCP Reception Report Block (contained in SR and RR messages).
 * See <a href="http://www.ietf.org/rfc/rfc1889.txt">[RFC-1889]</a>
 * <p>
 * The reception report block appears in both Sender Report and 
 * ReceptionReport packets. Each reception report block conveys 
 * statistics on the reception of RTP packets from a single 
 * synchronization source.
 * <pre>
 *   0               1               2               3
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                              SSRC                             |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  | fraction lost |       cumulative number of packets lost       | 
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |           extended highest sequence number received           |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                      interarrival jitter                      |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                         last SR (LSR)                         |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                   delay since last SR (DLSR)                  |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <h3>SSRC_n (source identifier): 32 bits</h3>
 *   The SSRC identifier of the source to which the information in
 *   this reception report block pertains.
 * 
 * <h3>fraction lost: 8 bits</h3>
 *   The fraction of RTP data packets from source SSRC_n lost since
 *   the previous SR or RR packet was sent, expressed as a fixed
 *   point number with the binary point at the left edge of the
 *   field. (That is equivalent to taking the integer part after
 *   multiplying the loss fraction by 256.) This fraction is defined
 *   to be the number of packets lost divided by the number of
 *   packets expected,  as defined in the next paragraph.  An
 *   implementation is shown in Appendix A.3. If the loss is negative
 *   due to duplicates, the fraction lost is set to zero. Note that a
 *   receiver cannot tell whether any packets were lost after the
 *   last one received, and that there will be no reception report
 *   block issued for a source if all packets from that source sent
 *   during the last reporting interval have been lost.
 * 
 * <h3>cumulative number of packets lost: 24 bits</h3>
 *   The total number of RTP data packets from source SSRC_n that
 *   have been lost since the beginning of reception. This number is
 *   defined to be the number of packets expected less the number of
 *   packets actually received, where the number of packets received
 *   includes any which are late or duplicates. Thus packets that
 *   arrive late are not counted as lost, and the loss may be
 *   negative if there are duplicates.  The number of packets
 *   expected is defined to be the extended last sequence number
 *   received, as defined next, less the initial sequence number
 *   received. This may be calculated as shown in Appendix A.3.
 * 
 * <h3>extended highest sequence number received: 32 bits</h3>
 *   The low 16 bits contain the highest sequence number received in
 *   an RTP data packet from source SSRC_n, and the most significant
 *   16 bits extend that sequence number with the corresponding count
 *   of sequence number cycles, which may be maintained according to
 *   the algorithm in Appendix A.1. Note that different receivers
 *   within the same session will generate different extensions to
 *   the sequence number if their start times differ significantly.
 * 
 * <h3>interarrival jitter: 32 bits</h3>
 *   An estimate of the statistical variance of the RTP data packet
 *   interarrival time, measured in timestamp units and expressed as
 *   an unsigned integer. The interarrival jitter J is defined to be
 *   the mean deviation (smoothed absolute value) of the difference D
 *   in packet spacing at the receiver compared to the sender for a
 *   pair of packets. As shown in the equation below, this is
 *   equivalent to the difference in the "relative transit time" for
 *   the two packets; the relative transit time is the difference
 *   between a packet's RTP timestamp and the receiver's clock at the
 *   time of arrival, measured in the same units.
 * 
 * <h3>last SR timestamp (LSR): 32 bits</h3>
 *   The middle 32 bits out of 64 in the NTP timestamp (as explained
 *   in Section 4) received as part of the most recent RTCP sender
 *   report (SR) packet from source SSRC_n.  If no SR has been
 *   received yet, the field is set to zero.
 * 
 * <h3>delay since last SR (DLSR): 32 bits</h3>
 *   The delay, expressed in units of 1/65536 seconds, between
 *   receiving the last SR packet from source SSRC_n and sending this
 *   reception report block.  If no SR packet has been received yet
 *   from SSRC_n, the DLSR field is set to zero.
 * <p>
 * @see {@link RtcpSenderReportPacket}
 * @see {@link RtcpReceiverReportPacket}
 * 
 * @author Gregory Bumgardner
 */
public class RtcpReceptionReport extends BufferBackedObject {

    /*-- Static Variables ---------------------------------------------------*/

    public final static Logger logger = RtcpPacket.logger;
    
    public final static IntegerField SourceIdentifier = new IntegerField(0);
    public final static ByteField FractionLost = new ByteField(4);
    public final static IntegerBitField PacketsLost = new IntegerBitField(5,0,24);
    public final static IntegerField HighestSequenceNumberReceived = new IntegerField(8);
    public final static IntegerField InterarrivalJitter = new IntegerField(12);
    public final static IntegerField LastSRTimestamp = new IntegerField(16);
    public final static IntegerField DelaySinceLastSRTimestamp = new IntegerField(20);
    
    public final static int REPORT_LENGTH = 24;
    
    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     */
    public RtcpReceptionReport() {
        super(REPORT_LENGTH);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpReceptionReport.RtcpReceptionReport"));
            logState(logger);
        }
    }

    /**
     * 
     * @param sourceIdentifier
     * @param fractionLost
     * @param packetsLost
     * @param highestSequenceNumberReceived
     * @param interarrivalJitter
     * @param lastSRTimestamp
     * @param delaySinceLastSRTimestamp
     */
    public RtcpReceptionReport(int sourceIdentifier,
                               double fractionLost,
                               int packetsLost,
                               int highestSequenceNumberReceived,
                               int interarrivalJitter,
                               int lastSRTimestamp,
                               int delaySinceLastSRTimestamp) {
        super(REPORT_LENGTH);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "RtcpReceptionReport.RtcpReceptionReport",
                                          sourceIdentifier,
                                          fractionLost,
                                          packetsLost,
                                          highestSequenceNumberReceived,
                                          interarrivalJitter,
                                          lastSRTimestamp,
                                          delaySinceLastSRTimestamp));
        }
        
        setSourceIdentifier(sourceIdentifier);
        setFractionLost(fractionLost);
        setPacketsLost(packetsLost);
        setHighestSequenceNumberReceived(highestSequenceNumberReceived);
        setInterarrivalJitter(interarrivalJitter);
        setLastSRTimestamp(lastSRTimestamp);
        setDelaySinceLastSRTimestamp(delaySinceLastSRTimestamp);
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     */
    public RtcpReceptionReport(ByteBuffer buffer) {
        super(consume(buffer, REPORT_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpReceptionReport.RtcpReceptionReport"));
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
        logger.info(ObjectId + " : fraction-lost=" + getFractionLost());
        logger.info(ObjectId + " : packets-lost=" + getPacketsLost());
        logger.info(ObjectId + " : highest-sequence-number-received=" + getHighestSequenceNumberReceived());
        logger.info(ObjectId + " : interarrival-jitter=" + getInterarrivalJitter());
        logger.info(ObjectId + " : last-SR-timestamp=" + getLastSRTimestamp());
        logger.info(ObjectId + " : delay-since-last-SR-timestamp=" + getDelaySinceLastSRTimestamp());
    }

    /**
     * 
     * @return
     */
    public int getTotalLength() {
        return REPORT_LENGTH;
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
     * @return
     */
    public double getFractionLost() {
        return ((double)(FractionLost.get(getBufferInternal()) & 0xFF) / 256.0);
    }

    /**
     * 
     * @param fractionLost
     */
    public void setFractionLost(double fractionLost) {
        byte value;
        if (fractionLost < 0) {
            value = 0;
        }
        else {
            value = (byte)((int)(fractionLost * 256) & 0xFF);
        }
        FractionLost.set(getBufferInternal(), value);
    }

    /**
     * 
     * @return
     */
    public int getPacketsLost() {
        return PacketsLost.get(getBufferInternal());
    }

    /**
     * 
     * @param packetsLost
     */
    public void setPacketsLost(int packetsLost) {
        PacketsLost.set(getBufferInternal(), packetsLost);
    }

    /**
     * 
     * @return
     */
    public int getHighestSequenceNumberReceived() {
        return HighestSequenceNumberReceived.get(getBufferInternal());
    }

    /**
     * 
     * @param sequenceNumber
     */
    public void setHighestSequenceNumberReceived(int sequenceNumber) {
        HighestSequenceNumberReceived.set(getBufferInternal(), sequenceNumber);
    }

    /**
     * 
     * @return
     */
    public int getInterarrivalJitter() {
        return InterarrivalJitter.get(getBufferInternal());
    }

    /**
     * 
     * @param interarrivalJitter
     */
    public void setInterarrivalJitter(int interarrivalJitter) {
        InterarrivalJitter.set(getBufferInternal(), interarrivalJitter);
    }

    /**
     * 
     * @return
     */
    public int getLastSRTimestamp() {
        return LastSRTimestamp.get(getBufferInternal());
    }

    /**
     * 
     * @param lastSRTimestamp
     */
    public void setLastSRTimestamp(int lastSRTimestamp) {
        LastSRTimestamp.set(getBufferInternal(), lastSRTimestamp);
    }

    /**
     * 
     * @return
     */
    public int getDelaySinceLastSRTimestamp() {
        return DelaySinceLastSRTimestamp.get(getBufferInternal());
    }

    /**
     * 
     * @param delaySinceLastSRTimestamp
     */
    public void setDelaySinceLastSRTimestamp(int delaySinceLastSRTimestamp) {
        DelaySinceLastSRTimestamp.set(getBufferInternal(), delaySinceLastSRTimestamp);
    }

}
