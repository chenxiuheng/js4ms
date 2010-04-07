/*
 * Copyright � 2009-2010 Larkwood Labs Software.
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

import java.nio.ByteBuffer;
import java.util.logging.Level;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.ipv6.IPv6Packet;
import com.larkwoodlabs.util.buffer.parser.MissingParserException;
import com.larkwoodlabs.util.logging.Logging;

public final class MLDv1QueryMessage extends MLDQueryMessage {

    public static class Parser implements MLDMessage.ParserType {

        @Override
        public MLDMessage parse(ByteBuffer buffer) throws ParseException {
            return new MLDv1QueryMessage(buffer);
        }

        @Override
        public boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) throws MissingParserException, ParseException {
            return MLDv1QueryMessage.verifyChecksum(buffer, sourceAddress, destinationAddress);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }

    /*-- Static Variables ---------------------------------------------------*/
    
    public static final int BASE_MESSAGE_LENGTH = 24;
    public static final short QUERY_RESPONSE_INTERVAL = 10*1000; // 10 secs as ms


    /*-- Static Functions ---------------------------------------------------*/
    
    public static MLDMessage.Parser getMLDMessageParser() {
        return getMLDMessageParser(new MLDv1QueryMessage.Parser());
    }

    public static IPMessage.Parser getIPMessageParser() {
        return getIPMessageParser(new MLDv1QueryMessage.Parser());
    }

    public static IPv6Packet.Parser getIPv6PacketParser() {
        return getIPv6PacketParser(new MLDv1QueryMessage.Parser());
    }

    public static IPPacket.Parser getIPPacketParser() {
        return getIPPacketParser(new MLDv1QueryMessage.Parser());
    }

    public static IPv6Packet constructGeneralQueryPacket(byte[] sourceAddress) {
        return constructGroupQueryPacket(sourceAddress,IPv6GeneralQueryGroupAddress);
    }

    public static IPv6Packet constructGroupQueryPacket(byte[] sourceAddress, byte[] groupAddress) {
        MLDv1QueryMessage message = new MLDv1QueryMessage(groupAddress);
        message.setMaximumResponseDelay(QUERY_RESPONSE_INTERVAL);
        return constructIPv6Packet(sourceAddress, groupAddress, message);
    }

    /**
     * Verifies the MLD message checksum. Called by the parser prior to constructing the packet.
     * @param segment - the buffer segment containing the MLD message.
     * @param sourceAddress An IPv6 (16-byte) address..
     * @param destinationAddress An IPv6 (16-byte) address.
     */
    public static boolean verifyChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        return Checksum.get(buffer) == MLDMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH, sourceAddress, destinationAddress);
    }

    /**
     * Writes the MLD message checksum into a buffer containing an MLD message.
     * @param buffer - a byte array.
     * @param offset - the offset within the array at which to write the message.
     * @param sourceAddress An IPv6 (16-byte) address..
     * @param destinationAddress An IPv6 (16-byte) address.
     */
    public static void setChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        Checksum.set(buffer, MLDMessage.calculateChecksum(buffer, BASE_MESSAGE_LENGTH, sourceAddress, destinationAddress));
    }

    
    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param groupAddress
     */
    public MLDv1QueryMessage(byte[] groupAddress) {
        super(BASE_MESSAGE_LENGTH,groupAddress);
        
        if (logger.isLoggable(Level.FINER)){
            logger.finer(Logging.entering(ObjectId, "MLDv1QueryMessage.MLDv1QueryMessage", Logging.address(groupAddress)));
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public MLDv1QueryMessage(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, BASE_MESSAGE_LENGTH));
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MLDv1QueryMessage.MLDv1QueryMessage", buffer));
        }
    }

    @Override
    public void writeChecksum(ByteBuffer buffer, byte[] sourceAddress, byte[] destinationAddress) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "MLDv1QueryMessage.writeChecksum",
                                          buffer,
                                          Logging.address(sourceAddress),
                                          Logging.address(destinationAddress)));
        }
        
        MLDv1QueryMessage.setChecksum(buffer, sourceAddress, destinationAddress);
    }
    
    @Override
    public int getMessageLength() {
        return BASE_MESSAGE_LENGTH;
    }

}
