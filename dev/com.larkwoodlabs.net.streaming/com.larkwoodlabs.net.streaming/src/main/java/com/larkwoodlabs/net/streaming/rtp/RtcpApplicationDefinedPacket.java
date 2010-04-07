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

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.fields.ByteArrayField;
import com.larkwoodlabs.util.buffer.fields.ByteBitField;
import com.larkwoodlabs.util.buffer.fields.IntegerField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An RTCP Application-Defined (APP) message.
 * See <a href="http://www.ietf.org/rfc/rfc1889.txt">[RFC-1889]</a>
 * <h2>6.6 APP: Application-defined RTCP packet</h2>
 * The APP packet is intended for experimental use as new applications and
 * new features are developed, without requiring packet type value registration.
 * APP packets with unrecognized names should be ignored. After testing and if
 * wider use is justified, it is recommended that each APP packet be redefined
 * without the subtype and name fields and registered with the Internet Assigned
 * Numbers Authority using an RTCP packet type.<p>
 * The APP packet has the following format:
 * <pre>
 *   0                   1                   2                   3
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |V=2|P| subtype |   PT=APP=204  |             length            |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                           SSRC/CSRC                           |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                          name (ASCII)                         |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                   application-dependent data              ...  
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 * 
 * <dl>
 * <dt>version (V): 2 bits<dd>
 *   Identifies the version of RTP, which is the same in RTCP packets
 *   as in RTP data packets. The version defined by this
 *   specification is two (2).<p>
 * 
 * <dt>padding (P): 1 bit<dd>
 *   If the padding bit is set, this RTCP packet contains some
 *   additional padding octets at the end which are not part of the
 *   control information. The last octet of the padding is a count of
 *   how many padding octets should be ignored. Padding may be needed
 *   by some encryption algorithms with fixed block sizes. In a
 *   compound RTCP packet, padding should only be required on the
 *   last individual packet because the compound packet is encrypted
 *   as a whole.<p>
 * 
 * <dt>subtype: 5 bits<dd>
 *   May be used as a subtype to allow a set of APP packets to be
 *   defined under one unique name, or for any application-dependent data.<p>
 * 
 * <dt>packet type (PT): 8 bits<dd>
 *   Contains the constant 204 to identify this as an RTCP APP packet.<p>
 * 
 * <dt>length: 16 bits<dd>
 *   The length of this RTCP packet in 32-bit words minus one,
 *   including the header and any padding. (The offset of one makes
 *   zero a valid length and avoids a possible infinite loop in
 *   scanning a compound RTCP packet, while counting 32-bit words
 *   avoids a validity check for a multiple of 4.)<p>
 *   
 * <dt>SSRC/CSRC: 32 bits<dd>
 *   The synchronization source identifier for the originator of this packet.<p>
 *   
 * <dt>name: 4 bytes<dd>
 *   A name chosen by the person defining the set of APP packets to
 *   be unique with respect to other APP packets this application might receive.
 *   The application creator might choose to use the application name, and then
 *   coordinate the allocation of subtype values to others who want to define new
 *   packet types for the application. Alternatively, it is recommended that
 *   others choose a name based on the entity they represent, then coordinate the
 *   use of the name within that entity. The name is interpreted as a sequence of
 *   four ASCII characters, with uppercase and lowercase characters treated as
 *   distinct.<p>
 *
 * <dt>application-dependent data: variable length<dd>
 *   Application-dependent data may or may not appear in an APP packet.
 *   It is interpreted by the application and not RTP itself. It must be 
 *   a multiple of 32 bits long.<p>
 * </dl>
 * @author Gregory Bumgardner
 */
public class RtcpApplicationDefinedPacket extends RtcpPacket {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser implements RtcpPacket.ParserType {

        @Override
        public RtcpPacket parse(ByteBuffer buffer) throws ParseException {
            return new RtcpApplicationDefinedPacket(buffer);
        }

        @Override
        public Object getKey() {
            return PACKET_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/

    public final static ByteBitField Subtype = new ByteBitField(0,0,5);
    public final static IntegerField SourceIdentifier = new IntegerField(4);
    public final static ByteArrayField Name = new ByteArrayField(8,4);

    public final static byte PACKET_TYPE = (byte)204;
    
    public final static int BASE_MESSAGE_LENGTH = 12;

    /*-- Static Functions ---------------------------------------------------*/

    public static RtcpApplicationDefinedPacket.Parser constructParser() {
        return new RtcpApplicationDefinedPacket.Parser();
    }

    
    /*-- Member Variables ---------------------------------------------------*/

    private ByteBuffer data;

    /*-- Member Functions ---------------------------------------------------*/

    public RtcpApplicationDefinedPacket() {
        super(BASE_MESSAGE_LENGTH, PACKET_TYPE);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpApplicationDefinedPacket.RtcpApplicationDefinedPacket"));
        }
    }

    public RtcpApplicationDefinedPacket(byte subtype,
                                        int sourceIdentifier,
                                        byte[] name,
                                        ByteBuffer data) {
        this();
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpApplicationDefinedPacket.RtcpApplicationDefinedPacket"));
        }

        setSubtype(subtype);
        setSourceIdentifier(sourceIdentifier);
        setName(name);
        setData(data);
        
        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    public RtcpApplicationDefinedPacket(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtcpApplicationDefinedPacket.RtcpApplicationDefinedPacket", buffer));
        }
        
        int remaining = (Length.get(getBufferInternal()) - 2) * 4;
        if (remaining > 0) {
            this.data = buffer.slice();
            this.data.limit(remaining);
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
        logger.info(ObjectId + " : xxx=");
    }

    @Override
    public void writeTo(ByteBuffer buffer) {
        super.writeTo(buffer);
        if (this.data != null) {
            buffer.put(this.data);
            this.data.rewind();
        }
    }

    @Override
    public int getTotalLength() {
        return BASE_MESSAGE_LENGTH + (this.data != null ? this.data.limit() : 0);
    }

    @Override
    public Byte getType() {
        return PACKET_TYPE;
    }

    /**
     * 
     * @return
     */
    public byte getSubtype() {
        return Subtype.get(getBufferInternal());
    }
    
    /**
     * 
     * @param subtype
     */
    public void setSubtype(byte subtype) {
        Subtype.set(getBufferInternal(), subtype);
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
    public byte[] getName() {
        return Name.get(getBufferInternal());
    }
    
    /**
     * 
     * @param name
     */
    public void setName(byte[] name) {
        Name.set(getBufferInternal(), name);
    }
    
    /**
     * 
     * @return
     */
    public ByteBuffer getData() {
        if (this.data != null) {
            return this.data.slice();
        }
        else {
            return null;
        }
    }

    /**
     * 
     * @param buffer
     */
    public void setData(ByteBuffer buffer) {
        this.data = buffer;
        setTotalLength(getTotalLength());
    }
}
