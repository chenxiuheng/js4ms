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
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ApplicationMessage;
import com.larkwoodlabs.util.buffer.BufferBackedObject;
import com.larkwoodlabs.util.buffer.fields.BooleanField;
import com.larkwoodlabs.util.buffer.fields.ByteBitField;
import com.larkwoodlabs.util.buffer.fields.IntegerField;
import com.larkwoodlabs.util.buffer.fields.ShortField;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An RTP Message.
 * See <a href="http://www.ietf.org/rfc/rfc1889.txt">[RFC-1889]</a>
 * <pre>
 * The RTP header has the following format:
 * 
 *     0               1               2             3
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |V=2|P|X|  CC   |M|     PT      |       sequence number         |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                           timestamp                           |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |           synchronization source (SSRC) identifier            |
 *    +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 *    |            contributing source (CSRC) identifiers             |
 *    |                             ....                              |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 *    The first twelve octets are present in every RTP packet, while the
 *    list of CSRC identifiers is present only when inserted by a mixer.
 *    The fields have the following meaning:
 * 
 *    version (V): 2 bits
 *         This field identifies the version of RTP. The version defined by
 *         this specification is two (2). (The value 1 is used by the first
 *         draft version of RTP and the value 0 is used by the protocol
 *         initially implemented in the &quot;vat&quot; audio tool.)
 * 
 *    padding (P): 1 bit
 *         If the padding bit is set, the packet contains one or more
 *         additional padding octets at the end which are not part of the
 *         payload. The last octet of the padding contains a count of how
 *         many padding octets should be ignored. Padding may be needed by
 *         some encryption algorithms with fixed block sizes or for
 *         carrying several RTP packets in a lower-layer protocol data
 *         unit.
 * 
 *    extension (X): 1 bit
 *         If the extension bit is set, the fixed header is followed by
 *         exactly one header extension, with a format defined in Section
 *         5.3.1.
 * 
 *    CSRC count (CC): 4 bits
 *         The CSRC count contains the number of CSRC identifiers that
 *         follow the fixed header.
 * 
 *    marker (M): 1 bit
 *         The interpretation of the marker is defined by a profile. It is
 *         intended to allow significant events such as frame boundaries to
 *         be marked in the packet stream. A profile may define additional
 *         marker bits or specify that there is no marker bit by changing
 *         the number of bits in the payload type field (see Section 5.3).
 * 
 *    payload type (PT): 7 bits
 *         This field identifies the format of the RTP payload and
 *         determines its interpretation by the application. A profile
 *         specifies a default static mapping of payload type codes to
 *         payload formats. Additional payload type codes may be defined
 *         dynamically through non-RTP means (see Section 3). An initial
 *         set of default mappings for audio and video is specified in the
 *         companion profile Internet-Draft draft-ietf-avt-profile, and
 *         may be extended in future editions of the Assigned Numbers RFC
 *         [6].  An RTP sender emits a single RTP payload type at any given
 *         time; this field is not intended for multiplexing separate media
 *         streams (see Section 5.2).
 * 
 *    sequence number: 16 bits
 *         The sequence number increments by one for each RTP data packet
 *         sent, and may be used by the receiver to detect packet loss and
 *         to restore packet sequence. The initial value of the sequence
 *         number is random (unpredictable) to make known-plaintext attacks
 *         on encryption more difficult, even if the source itself does not
 *         encrypt, because the packets may flow through a translator that
 *         does. Techniques for choosing unpredictable numbers are
 *         discussed in [7].
 * 
 *    timestamp: 32 bits
 *         The timestamp reflects the sampling instant of the first octet
 *         in the RTP data packet. The sampling instant must be derived
 *         from a clock that increments monotonically and linearly in time
 *         to allow synchronization and jitter calculations (see Section
 *         6.3.1).  The resolution of the clock must be sufficient for the
 *         desired synchronization accuracy and for measuring packet
 *         arrival jitter (one tick per video frame is typically not
 *         sufficient).  The clock frequency is dependent on the format of
 *         data carried as payload and is specified statically in the
 *         profile or payload format specification that defines the format,
 *         or may be specified dynamically for payload formats defined
 *         through non-RTP means. If RTP packets are generated
 *         periodically, the nominal sampling instant as determined from
 *         the sampling clock is to be used, not a reading of the system
 *         clock. As an example, for fixed-rate audio the timestamp clock
 *         would likely increment by one for each sampling period.  If an
 *         audio application reads blocks covering 160 sampling periods
 *         from the input device, the timestamp would be increased by 160
 *         for each such block, regardless of whether the block is
 *         transmitted in a packet or dropped as silent.
 * 
 *    The initial value of the timestamp is random, as for the sequence
 *    number. Several consecutive RTP packets may have equal timestamps if
 *    they are (logically) generated at once, e.g., belong to the same
 *    video frame. Consecutive RTP packets may contain timestamps that are
 *    not monotonic if the data is not transmitted in the order it was
 *    sampled, as in the case of MPEG interpolated video frames. (The
 *    sequence numbers of the packets as transmitted will still be
 *    monotonic.)
 * 
 *    SSRC: 32 bits
 *         The SSRC field identifies the synchronization source. This
 *         identifier is chosen randomly, with the intent that no two
 *         synchronization sources within the same RTP session will have
 *         the same SSRC identifier. An example algorithm for generating a
 *         random identifier is presented in Appendix A.6. Although the
 *         probability of multiple sources choosing the same identifier is
 *         low, all RTP implementations must be prepared to detect and
 *         resolve collisions.  Section 8 describes the probability of
 *         collision along with a mechanism for resolving collisions and
 *         detecting RTP-level forwarding loops based on the uniqueness of
 *         the SSRC identifier. If a source changes its source transport
 *         address, it must also choose a new SSRC identifier to avoid
 *         being interpreted as a looped source.
 * 
 *    CSRC list: 0 to 15 items, 32 bits each
 *         The CSRC list identifies the contributing sources for the
 *         payload contained in this packet. The number of identifiers is
 *         given by the CC field. If there are more than 15 contributing
 *         sources, only 15 may be identified. CSRC identifiers are
 *         inserted by mixers, using the SSRC identifiers of contributing
 *         sources. For example, for audio packets the SSRC identifiers of
 *         all sources that were mixed together to create a packet are
 *         listed, allowing correct talker indication at the receiver.
 * </pre>
 * 
 * @author Gregory Bumgardner
 */
public class RtpPacket extends BufferBackedObject implements ApplicationMessage {

    public static class Parser implements ApplicationMessage.Parser {

        @Override
        public RtpPacket parse(ByteBuffer buffer) throws ParseException, MissingParserException {
            return new RtpPacket(buffer);
        }
        
    }

    /*-- Static Variables ---------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(RtpPacket.class.getName());

    protected static final int BASE_HEADER_LENGTH = 12;
    
    public static final ByteBitField    Version = new ByteBitField(0,6,2);
    public static final BooleanField    Padding = new BooleanField(0,5);
    public static final BooleanField    Extension = new BooleanField(0,4);
    public static final ByteBitField    CSRCCount = new ByteBitField(0,0,4);
    public static final BooleanField    Marker = new BooleanField(1,7);
    public static final ByteBitField    PayloadType = new ByteBitField(1,0,7);
    public static final ShortField      SequenceNumber = new ShortField(2);
    public static final IntegerField    Timestamp = new IntegerField(4);
    public static final IntegerField    SSRC = new IntegerField(8);
    public static final IntegerField    CSRCEntry = new IntegerField(0);

    /*-- Static Functions ---------------------------------------------------*/
    
    public static RtpPacket.Parser constructRtpPacketParser() {
        return new RtpPacket.Parser();
    }

    /*-- Member Variables ---------------------------------------------------*/

    HashSet<Integer> csrcs = null;
    
    ByteBuffer payload = null;
    
    /*-- Member Functions ---------------------------------------------------*/

    public RtpPacket(byte version,
                     boolean padding,
                     boolean extension,
                     boolean marker,
                     byte payloadType,
                     short sequenceNumber,
                     int timestamp,
                     int ssrc) {
        super(BASE_HEADER_LENGTH);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "RtpPacket.RtpPacket",
                                          version,
                                          padding,
                                          extension,
                                          marker,
                                          payloadType,
                                          sequenceNumber,
                                          timestamp,
                                          ssrc));
            logState(logger);
        }
    }

    public RtpPacket(ByteBuffer buffer) {
        super(consume(buffer,BASE_HEADER_LENGTH));
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtpPacket.RtpPacket", buffer));
        }
        
        int csrcCount = getCSRCCount();
        if (csrcCount > 0) {
            this.csrcs = new HashSet<Integer>();
            for (int i = 0; i < csrcCount; i++) {
                this.csrcs.add(buffer.getInt());
            }
            setCSRCCount((byte)this.csrcs.size());
        }

        this.payload = consume(buffer, buffer.remaining());
            
        if (logger.isLoggable(Level.FINER)) {
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
        logger.info(ObjectId + " : version="+getVersion());
        logger.info(ObjectId + " : padding="+getPadding());
        logger.info(ObjectId + " : extension="+getExtension());
        logger.info(ObjectId + " : CSRC-count="+getCSRCCount());
        logger.info(ObjectId + " : marker="+getMarker());
        logger.info(ObjectId + " : payload-type="+getPayloadType());
        logger.info(ObjectId + " : sequence-number="+(getSequenceNumber() & 0xFFFF));
        logger.info(ObjectId + " : SSRC="+String.format("%08X", getSSRC()));
        if (this.csrcs != null) {
            logger.info(ObjectId + " ----> CSRC list");
            for (Integer csrc : this.csrcs) {
                logger.info(ObjectId + " : " + String.format("%08X", csrc));
            }
            logger.info(ObjectId + " <---- CSRC list");
        }
        logger.info(ObjectId +
                " : buffer array-offset=" + this.payload.arrayOffset() +
                ", position=" + this.payload.position() +
                ", remaining=" + this.payload.remaining() +
                ", limit=" + this.payload.limit() +
                ", capacity=" + this.payload.capacity());
    }

    @Override
    public int getHeaderLength() {
        return BASE_HEADER_LENGTH + this.csrcs.size() * 4;
    }

    @Override
    public int getTotalLength() {
        return getHeaderLength() + (this.payload.limit());
    }

    public byte getVersion() {
        return Version.get(getBufferInternal());
    }
    
    public void setVersion(byte version) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtpPacket.setVersion", version));
        }
        
        Version.set(getBufferInternal(),version);
    }

    public boolean getPadding() {
        return Padding.get(getBufferInternal());
    }

    public void setPadding(boolean padding) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtpPacket.setPadding", padding));
        }
        
        Padding.set(getBufferInternal(),padding);
    }

    public boolean getExtension() {
        return Extension.get(getBufferInternal());
    }
    
    public void setExtension(boolean extension) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtpPacket.setExtension", extension));
        }
        
        Extension.set(getBufferInternal(),extension);
    }

    public byte getCSRCCount() {
        return CSRCCount.get(getBufferInternal());
    }
    
    protected void setCSRCCount(byte csrcCount) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtpPacket.setCSRCCount", csrcCount));
        }
        
        CSRCCount.set(getBufferInternal(),csrcCount);
    }

    public boolean getMarker() {
        return Marker.get(getBufferInternal());
    }

    public void setMarker(boolean marker) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtpPacket.setMarker", marker));
        }
        
        Marker.set(getBufferInternal(),marker);
    }

    public byte getPayloadType() {
        return PayloadType.get(getBufferInternal());
    }

    public void setPayloadType(byte payloadType) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtpPacket.setPayloadType", payloadType));
        }
        
        PayloadType.set(getBufferInternal(), payloadType);
    }

    public short getSequenceNumber() {
        return SequenceNumber.get(getBufferInternal());
    }
    
    public void setSequenceNumber(short sequenceNumber) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtpPacket.setSequenceNumber", sequenceNumber));
        }
        
        SequenceNumber.set(getBufferInternal(),sequenceNumber);
    }

    public int getSSRC() {
        return SSRC.get(getBufferInternal());
    }
    
    public void setSSRC(int ssrc) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtpPacket.setSSRC", ssrc));
        }

        SSRC.set(getBufferInternal(),ssrc);
    }

    public void addCSRC(int csrc) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtpPacket.addCSRC", csrc));
        }

        if (this.csrcs == null) {
            this.csrcs = new HashSet<Integer>();
        }
        this.csrcs.add(csrc);

        setCSRCCount((byte)this.csrcs.size());
    }
    
    public void removeCSRC(int csrc) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtpPacket.removeCSRC", csrc));
        }
        
        if (this.csrcs != null) {
            this.csrcs.remove(csrc);
            setCSRCCount((byte)this.csrcs.size());
        }
    }
}

