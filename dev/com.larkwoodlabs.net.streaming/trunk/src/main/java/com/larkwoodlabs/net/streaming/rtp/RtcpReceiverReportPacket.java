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
import com.larkwoodlabs.util.logging.Logging;

/**
 * An RTCP Reception Report (RR) message. See <a
 * href="http://www.ietf.org/rfc/rfc1889.txt">[RFC-1889]</a>
 * <h2>6.3.2 RR: Receiver report RTCP packet</h2>
 * The reception report packet consists of three sections, possibly
 * followed by a fourth profile-specific extension section if defined.
 * <pre>
 *   0                   1                   2                   3
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |V=2|P|    RC   |   PT=RR=201   |             length            | 1) Header
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                     SSRC of packet sender                     |
 *  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 *  |                                                               | 2) Reception
 *  /                                                               /    Report
 *
 *                      Reception Report 1..N (if any)
 *
 *  /                                                               /
 *  |                                                               |
 *  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 *  |                  profile-specific extensions                  | 3) Extensions
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre> 
 *
 * <h2>1) Header</h2>
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
 *   An empty RR packet (RC = 0) is put at the head of a compound RTCP
 *   packet when there is no data transmission or reception to report.
 *
 * <h3>packet type (PT): 8 bits</h3>
 *   Contains the constant 201 to identify this as an RTCP RR packet.
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
 *   SR packet.<p>
 *   
 * <h2>2) Reception Report Blocks</h2>
 *   See {@link RtcpReceptionReport} for a description of these blocks.
 * 
 * <h3>3) Extensions</h3>
 *   Ignored in this implementation.
 * <p>
 * @see {@link RtcpReceptionReport}
 * 
 * @author Gregory Bumgardner
 */
public final class RtcpReceiverReportPacket extends RtcpPacket {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser implements RtcpPacket.ParserType {

        @Override
        public RtcpPacket parse(ByteBuffer buffer) throws ParseException {
            return new RtcpReceiverReportPacket(buffer);
        }

        @Override
        public Object getKey() {
            return PACKET_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    public final static ByteBitField ReceptionReportCount = new ByteBitField(0,0,5);
    public final static IntegerField SourceIdentifier = new IntegerField(4);
    
    public final static byte PACKET_TYPE = (byte)201;
    
    public final static int BASE_MESSAGE_LENGTH = 8;

    /*-- Static Functions ---------------------------------------------------*/

    public static RtcpReceiverReportPacket.Parser constructParser() {
        return new RtcpReceiverReportPacket.Parser();
    }

    
    /*-- Member Variables ---------------------------------------------------*/

    LinkedList<RtcpReceptionReport> receptionReports = new LinkedList<RtcpReceptionReport>();

    ByteBuffer extensions = null; 

    
    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param sourceIdentifier
     */
    public RtcpReceiverReportPacket(int sourceIdentifier) {
        super(BASE_MESSAGE_LENGTH, PACKET_TYPE);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpReceiverReportPacket.RtcpReceptionReportPacket"));
        }
        
        setSourceIdentifier(sourceIdentifier);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public RtcpReceiverReportPacket(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpReceiverReportPacket.RtcpReceptionReportPacket", buffer));
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
     * Logs value of variables declared or maintained by this class.
     * @param logger
     */
    private void logState(Logger logger) {
        logger.info(ObjectId + " : reception-report-count=" + getReceptionReportCount());
        logger.info(ObjectId + " : source-identifier=" + String.format("%08X", getSourceIdentifier()));
        logger.info(ObjectId + " : ----> Reception Reports");
        for (RtcpReceptionReport report : this.receptionReports) {
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
     * @param receptionReport
     */
    public void addReport(RtcpReceptionReport receptionReport) {
        this.receptionReports.add(receptionReport);
        setReceptionReportCount(this.receptionReports.size());
        setTotalLength(getTotalLength());
    }
    
    /**
     * 
     */
    public Iterator<RtcpReceptionReport> getReportIterator() {
        return this.receptionReports.iterator();
    }
}
