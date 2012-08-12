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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.fields.ByteBitField;
import com.larkwoodlabs.util.buffer.fields.IntegerField;
import com.larkwoodlabs.util.buffer.fields.LongField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An RTCP Sender Report (SR) message.
 * See <a href="http://www.ietf.org/rfc/rfc1889.txt">[RFC-1889]</a>
 * <h2>6.3.1 SR: Sender report RTCP packet</h2>
 * The sender report packet consists of three sections, possibly
 * followed by a fourth profile-specific extension section if defined.
 * <pre>
 *   0               1               2               3
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |V=2|P|    RC   |   PT=SR=200   |             length            | 1) Header 
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                         SSRC of sender                        |
 *  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 *  |              NTP timestamp, most significant word             | 2) Sender
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+    information
 *  |             NTP timestamp, least significant word             |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                         RTP timestamp                         |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                     sender's packet count                     |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                      sender's octet count                     |
 *  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 *  |                                                               | 3) Reception
 *  /                                                               /    Report
 *
 *                    Reception Report 0..N (if any)
 *
 *  /                                                               /
 *  |                                                               |
 *  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 *  |                  profile-specific extensions                  | 4) Extensions
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * <h2>1) Packet Header</h2>
 * The first section, the header, is 8 octets long. The fields have the
 * following meaning:
 * 
 * <h3>version (V): 2 bits</h3>
 *   Identifies the version of RTP, which is the same in RTCP packets
 *   as in RTP data packets. The version defined by this
 *   specification is two (2).
 * 
 * <h3>padding (P): 1 bit</h3>
 *   If the padding bit is set, this RTCP packet contains some
 *   additional padding octets at the end which are not part of the
 *   control information. The last octet of the padding is a count of
 *   how many padding octets should be ignored. Padding may be needed
 *   by some encryption algorithms with fixed block sizes. In a
 *   compound RTCP packet, padding should only be required on the
 *   last individual packet because the compound packet is encrypted
 *   as a whole.
 * 
 * <h3>reception report count (RC): 5 bits</h3>
 *   The number of reception report blocks contained in this packet.
 *   A value of zero is valid.
 * 
 * <h3>packet type (PT): 8 bits</h3>
 *   Contains the constant 200 to identify this as an RTCP SR packet.
 * 
 * <h3>length: 16 bits</h3>
 *   The length of this RTCP packet in 32-bit words minus one,
 *   including the header and any padding. (The offset of one makes
 *   zero a valid length and avoids a possible infinite loop in
 *   scanning a compound RTCP packet, while counting 32-bit words
 *   avoids a validity check for a multiple of 4.)
 * 
 * <h3>SSRC: 32 bits</h3>
 *   The synchronization source identifier for the originator of this
 *   SR packet.
 * <p>
 * <h2>2) Sender Information</h2>
 * The second section, the sender information, is 20 octets long and is
 * present in every sender report packet. It summarizes the data
 * transmissions from this sender. The fields have the following
 * meaning:
 * <h3>NTP timestamp: 64 bits</h3>
 *   Indicates the wallclock time when this report was sent so that
 *   it may be used in combination with timestamps returned in
 *   reception reports from other receivers to measure round-trip
 *   propagation to those receivers. Receivers should expect that the
 *   measurement accuracy of the timestamp may be limited to far less
 *   than the resolution of the NTP timestamp. The measurement
 *   uncertainty of the timestamp is not indicated as it may not be
 *   known. A sender that can keep track of elapsed time but has no
 *   notion of wallclock time may use the elapsed time since joining
 *   the session instead. This is assumed to be less than 68 years,
 *   so the high bit will be zero. It is permissible to use the
 *   sampling clock to estimate elapsed wallclock time. A sender that
 *   has no notion of wallclock or elapsed time may set the NTP
 *   timestamp to zero.
 * 
 * <h3>RTP timestamp: 32 bits</h3>
 *   Corresponds to the same time as the NTP timestamp (above), but
 *   in the same units and with the same random offset as the RTP
 *   timestamps in data packets. This correspondence may be used for
 *   intra- and inter-media synchronization for sources whose NTP
 *   timestamps are synchronized, and may be used by media-
 *   independent receivers to estimate the nominal RTP clock
 *   frequency. Note that in most cases this timestamp will not be
 *   equal to the RTP timestamp in any adjacent data packet. Rather,
 *   it is calculated from the corresponding NTP timestamp using the
 *   relationship between the RTP timestamp counter and real time as
 *   maintained by periodically checking the wallclock time at a
 *   sampling instant.
 * 
 * <h3>sender's packet count: 32 bits</h3>
 *   The total number of RTP data packets transmitted by the sender
 *   since starting transmission up until the time this SR packet was
 *   generated.  The count is reset if the sender changes its SSRC
 *   identifier.
 * 
 * <h3>sender's octet count: 32 bits</h3>
 *   The total number of payload octets (i.e., not including header
 *   or padding) transmitted in RTP data packets by the sender since
 *   starting transmission up until the time this SR packet was
 *   generated. The count is reset if the sender changes its SSRC
 *   identifier. This field can be used to estimate the average
 *   payload data rate.
 *   
 * <h2>3) Reception Report Blocks</h2>
 *   The third section contains zero or more reception report blocks
 *   depending on the number of other sources heard by this sender since
 *   the last report. Each reception report block conveys statistics on
 *   the reception of RTP packets from a single synchronization source.
 *   See {@link RtcpReceptionReport} for a description of these blocks.
 *    
 * <h3>4) Extensions</h3>
 *   Ignored in this implementation.
 *  
 * <p>
 * 
 * @see {@link RtcpReceptionReport}
 *
 * @author Gregory Bumgardner
 */
public final class RtcpSenderReportPacket extends RtcpPacket {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser implements RtcpPacket.ParserType {

        @Override
        public RtcpPacket parse(ByteBuffer buffer) throws ParseException {
            return new RtcpSenderReportPacket(buffer);
        }

        @Override
        public Object getKey() {
            return PACKET_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    public final static ByteBitField ReceptionReportCount = new ByteBitField(0,0,5);
    public final static IntegerField SourceIdentifier = new IntegerField(4);
    public final static LongField NTPTimestamp = new LongField(8);
    public final static IntegerField RTPTimestamp = new IntegerField(16);
    public final static IntegerField SenderPacketCount = new IntegerField(20);
    public final static IntegerField SenderOctetCount = new IntegerField(24);

    public final static byte PACKET_TYPE = (byte)200;
    
    public final static int BASE_MESSAGE_LENGTH = 28;


    /*-- Static Functions ---------------------------------------------------*/

    public static RtcpSenderReportPacket.Parser constructParser() {
        return new RtcpSenderReportPacket.Parser();
    }

    
    /*-- Member Variables ---------------------------------------------------*/

    LinkedList<RtcpReceptionReport> receptionReports = new LinkedList<RtcpReceptionReport>();
    
    ByteBuffer extensions = null; 

    /*-- Member Functions ---------------------------------------------------*/

    public RtcpSenderReportPacket() {
        super(BASE_MESSAGE_LENGTH, PACKET_TYPE);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpSenderReportPacket.RtcpSenderReportPacket"));
            logState(logger);
        }
    }

    public RtcpSenderReportPacket(int ssrc,
                                  long ntpTimestamp,
                                  int rtpTimestamp,
                                  int senderPacketCount,
                                  int senderOctetCount) {
        super(BASE_MESSAGE_LENGTH, PACKET_TYPE);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpSenderReportPacket.RtcpSenderReportPacket"));
        }
        
        setSourceIdentifier(ssrc);
        setNTPTimestamp(ntpTimestamp);
        setRTPTimestamp(rtpTimestamp);
        setSenderPacketCount(senderPacketCount);
        setSenderOctetCount(senderOctetCount);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    public RtcpSenderReportPacket(ByteBuffer buffer) throws ParseException {
        super(consume(buffer,BASE_MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpSenderReportPacket.RtcpSenderReportPacket", buffer));
        }
        
        int count = ReceptionReportCount.get(getBufferInternal());
        
        for (int i = 0; i < count; i++) {
            this.receptionReports.add(new RtcpReceptionReport(buffer));
        }

        int currentLength = getTotalLength(); 
        int totalLength = (Length.get(getBufferInternal()) + 1) * 4;
        int remainingLength = totalLength - currentLength;
        if (remainingLength > 0) {
            this.extensions = consume(buffer,remainingLength);
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
        logger.info(ObjectId + " : reception-report-count=" + getReceptionReportCount());
        logger.info(ObjectId + " : source-identifier=" + String.format("%08X", getSourceIdentifier()));
        logger.info(ObjectId + " : NTP-timestamp=" + new NtpTimeStamp(getNTPTimestamp()).toDateString());
        logger.info(ObjectId + " : RTP-timestamp=" + getRTPTimestamp());
        logger.info(ObjectId + " : sender-packet-count=" + getSenderPacketCount());
        logger.info(ObjectId + " : sender-octet-count=" + getSenderOctetCount());
        logger.info(ObjectId + " : ----> Reception Reports");
        for (RtcpReceptionReport report : this.receptionReports) {
            logger.info(ObjectId + " : -- Reception Report --");
            report.log(logger);
        }
        logger.info(ObjectId + " : <---- Reception Reports");
    }

    @Override
    public void writeTo(ByteBuffer buffer) {

        super.writeTo(buffer);

        for (RtcpReceptionReport report : receptionReports) {
            report.writeTo(buffer);
        }

        if (this.extensions != null) {
            buffer.put(this.extensions);
            this.extensions.rewind();
        }
    }

    @Override
    public int getTotalLength() {
        return BASE_MESSAGE_LENGTH +
               this.receptionReports.size() * RtcpReceptionReport.REPORT_LENGTH +
               (this.extensions != null ? this.extensions.limit() : 0);
    }

    @Override
    public Byte getType() {
        return PACKET_TYPE;
    }

    /**
     * 
     * @return
     */
    public int getReceptionReportCount() {
        return ReceptionReportCount.get(getBufferInternal());
    }

    /**
     * 
     * @param count
     */
    protected void setReceptionReportCount(int count) {
        ReceptionReportCount.set(getBufferInternal(), (byte)count);
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
    public long getNTPTimestamp() {
        return NTPTimestamp.get(getBufferInternal());
    }

    /**
     * 
     * @param timestamp
     */
    public void setNTPTimestamp(long timestamp) {
        NTPTimestamp.set(getBufferInternal(), timestamp);
    }

    /**
     * 
     * @return
     */
    public int getRTPTimestamp() {
        return RTPTimestamp.get(getBufferInternal());
    }

    /**
     * 
     * @param timestamp
     */
    public void setRTPTimestamp(int timestamp) {
        RTPTimestamp.set(getBufferInternal(), timestamp);
    }

    /**
     * 
     * @return
     */
    public int getSenderPacketCount() {
        return SenderPacketCount.get(getBufferInternal());
    }

    /**
     * 
     * @param packetCount
     */
    public void setSenderPacketCount(int packetCount) {
        SenderPacketCount.set(getBufferInternal(), packetCount);
    }

    /**
     * 
     * @return
     */
    public int getSenderOctetCount() {
        return SenderOctetCount.get(getBufferInternal());
    }

    /**
     * 
     * @param octetCount
     */
    public void setSenderOctetCount(int octetCount) {
        SenderOctetCount.set(getBufferInternal(), octetCount);
    }
    
    /**
     * 
     * @param receptionReport
     */
    public void addReport(RtcpReceptionReport receptionReport) {
        this.receptionReports.add(receptionReport);
        setReceptionReportCount(this.receptionReports.size());
        setTotalLength(getTotalLength());
    }

    /**
     * 
     * @return
     */
    public Iterator<RtcpReceptionReport> getReportIterator() {
        return this.receptionReports.iterator();
    }
}
