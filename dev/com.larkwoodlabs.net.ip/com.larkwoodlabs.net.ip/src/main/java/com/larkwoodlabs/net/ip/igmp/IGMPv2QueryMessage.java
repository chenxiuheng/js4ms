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

import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.ipv4.IPv4Packet;
import com.larkwoodlabs.util.logging.Logging;

public final class IGMPv2QueryMessage extends IGMPQueryMessage {

    /*-- Inner Classes ------------------------------------------------------*/
    
    public static final class Parser implements IGMPMessage.ParserType {

        @Override
        public IGMPMessage parse(ByteBuffer buffer) throws ParseException {
            return new IGMPv2QueryMessage(buffer);
        }

        public boolean verifyChecksum(ByteBuffer buffer) {
            return IGMPv2QueryMessage.verifyChecksum(buffer);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }


    /*-- Static Variables ---------------------------------------------------*/
    
    public static final int BASE_MESSAGE_LENGTH = 8;


    /*-- Static Functions ---------------------------------------------------*/

    public static IGMPMessage.Parser getIGMPMessageParser() {
        return getIGMPMessageParser(new IGMPv2QueryMessage.Parser());
    }

    public static IPMessage.Parser getIPMessageParser() {
        return getIPMessageParser(new IGMPv2QueryMessage.Parser());
    }

    public static IPv4Packet.Parser getIPv4PacketParser() {
        return getIPv4PacketParser(new IGMPv2QueryMessage.Parser());
    }

    public static IPPacket.Parser getIPPacketParser() {
        return getIPPacketParser(new IGMPv2QueryMessage.Parser());
    }

    
    /**
     * Verifies the IGMP message checksum. Called by the parser prior to constructing the packet.
     * @param segment - the buffer segment containing the IGMP message.
     */
    public static boolean verifyChecksum(ByteBuffer buffer) {
        return Checksum.get(buffer) == IGMPMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH);
    }

    /**
     * Writes the IGMP message checksum into a buffer containing an IGMP message.
     */
    public static void setChecksum(ByteBuffer buffer) {
        Checksum.set(buffer, IGMPMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH));
    }


    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param maximumResponseTime
     * @param groupAddress
     */
    public IGMPv2QueryMessage(short maximumResponseTime, byte[] groupAddress) {
        super(BASE_MESSAGE_LENGTH, maximumResponseTime, groupAddress);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv2QueryMessage.IGMPv2QueryMessage", maximumResponseTime, Logging.address(groupAddress)));
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public IGMPv2QueryMessage(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv2QueryMessage.IGMPv2QueryMessage", buffer));
        }
    }

    @Override
    public void writeChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        IGMPv2QueryMessage.setChecksum(buffer);
    }

    @Override
    public int getMessageLength() {
        return BASE_MESSAGE_LENGTH;
    }

    /**
     * Returns the "<code>Max Resp Time</code>" value in milliseconds.
     * This value is calculated from {@linkplain #MaxRespCode Max Resp Code}.
     * @return
     */
    public int getMaximumResponseTime() {
        return getMaxRespCode() * 100;
    }

    /**
     * Sets the "<code>Max Resp Time</code>" value in milliseconds.
     * This value is converted into a {@linkplain #MaxRespCode Max Resp Code} value.
     * @return
     */
    public void setMaximumResponseTime(int milliseconds) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "IGMPv2QueryMessage.setMaximumResponseTime", milliseconds));
        }
        
        short tenths = (short)(milliseconds / 100);
        setMaxRespCode((byte)tenths);
    }
}
