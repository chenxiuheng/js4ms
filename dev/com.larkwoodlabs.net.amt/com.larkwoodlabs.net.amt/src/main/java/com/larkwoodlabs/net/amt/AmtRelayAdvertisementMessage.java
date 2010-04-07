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

package com.larkwoodlabs.net.amt;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.fields.ByteArrayField;
import com.larkwoodlabs.util.buffer.fields.IntegerField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An AMT Relay Advertisement Message.
 * <pre>
 * 6.2 AMT Relay Advertisement.
 * 
 *   The AMT Relay Advertisement message is a UDP packet sent from the AMT relay
 *   anycast address to the source of the discovery message.
 * 
 *   The UDP source port is the IANA reserved AMT port number and the UDP
 *   destination port is the source port received in the Discovery message. The
 *   UDP checksum MUST be valid in AMT control messages.
 * 
 *   The payload of the UDP packet contains the following fields:
 * 
 *    0                   1                   2                   3
 *    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *   |     Type=0x2  |     Reserved                                  |
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *   |            Discovery Nonce                                    |
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *   |            Relay Address                                      |
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 * 6.2.1. Type
 * 
 *   The type of the message.
 * 
 * 6.2.2. Reserved
 * 
 *   A 24-bit reserved field. Sent as 0, ignored on receipt.
 * 
 * 6.2.3. Discovery Nonce
 * 
 *   A 32-bit random value generated by the gateway and replayed by the relay.
 * 
 * 6.2.4. Relay Address
 * 
 *   The unicast IPv4 or IPv6 address of the AMT relay. The family can be
 *   determined by the length of the Advertisement.
 * </pre>
 */
final class AmtRelayAdvertisementMessage extends AmtMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser implements AmtMessage.ParserType {

        @Override
        public AmtMessage parse(ByteBuffer buffer) throws ParseException {
            return new AmtRelayAdvertisementMessage(buffer);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }


    /*-- Static Variables ---------------------------------------------------*/

    public static final byte MESSAGE_TYPE = 0x2;
    public static final int BASE_MESSAGE_LENGTH = 12;
    public static final int MIN_MESSAGE_LENGTH = 12;
    public static final int MAX_MESSAGE_LENGTH = 24;
    
    public static final IntegerField   DiscoveryNonce = new IntegerField(4);
    public static final ByteArrayField IPv4RelayAddress = new ByteArrayField(8,4);
    public static final ByteArrayField IPv6RelayAddress = new ByteArrayField(8,16);


    /*-- Static Functions ---------------------------------------------------*/

    public static AmtRelayAdvertisementMessage.Parser constructParser() {
        return new AmtRelayAdvertisementMessage.Parser();
    }


    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param discoveryNonce
     * @param relayAddress
     */
    public AmtRelayAdvertisementMessage(int discoveryNonce, byte[] relayAddress) {
        super(BASE_MESSAGE_LENGTH + relayAddress.length,MESSAGE_TYPE);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                        "AmtRelayAdvertisementMessage.AmtRelayAdvertisementMessage",
                                        discoveryNonce,
                                        Logging.address(relayAddress)));
        }
        
        setDiscoveryNonce(discoveryNonce);
        setRelayAddress(relayAddress);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public AmtRelayAdvertisementMessage(ByteBuffer buffer) throws ParseException {
        super(consume(buffer, buffer.limit() > MIN_MESSAGE_LENGTH ? MAX_MESSAGE_LENGTH : MIN_MESSAGE_LENGTH));
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtRelayAdvertisementMessage.AmtRelayAdvertisementMessage", buffer));
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
        logger.info(ObjectId + " : discovery-nonce="+getDiscoveryNonce());
        logger.info(ObjectId + " : relay-address="+Logging.address(getRelayAddress()));
    }

    @Override
    public Byte getType() {
        return MESSAGE_TYPE;
    }

    @Override
    public int getTotalLength() {
        return getBufferInternal().limit();
    }

    /**
     * Gets the current value of the Discovery Nonce field.
     */
    public int getDiscoveryNonce() {
        return DiscoveryNonce.get(getBufferInternal());
    }

    /**
     * Sets the value of the Discovery Nonce field.
     */
    public void setDiscoveryNonce(int discoveryNonce) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtRelayAdvertisementMessage.setDiscoveryNonce", discoveryNonce));
        }
        
        DiscoveryNonce.set(getBufferInternal(), discoveryNonce);
    }

    /**
     * Gets the current value of the Relay Address field.
     * May be IPv4 (4 byte) or IPv6 (16 byte) address.
     */
    public byte[] getRelayAddress() {
        if (getBufferInternal().limit() > MIN_MESSAGE_LENGTH) {
            return IPv6RelayAddress.get(getBufferInternal());
        }
        else {
            return IPv4RelayAddress.get(getBufferInternal());
        }
    }

    /**
     * Sets the Relay Address field value.
     * See {@link #getRelayAddress()}.
     * 
     * @param relayAddress
     *            - an IPv4 address.
     */
    public void setRelayAddress(InetAddress relayAddress) {
        byte[] address = relayAddress.getAddress();
        setRelayAddress(address);
    }

    /**
     * Sets the Relay Address field value.
     * See {@link #getRelayAddress()}.
     * 
     * @param relayAddress
     *            - an IPv4 address.
     */
    public void setRelayAddress(byte[] relayAddress) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtRelayAdvertisementMessage.setRelayAddress", Logging.address(relayAddress)));
        }
        
        //Precondition.checkAddress(relayAddress);
        if (relayAddress.length == 4) {
            IPv4RelayAddress.set(getBufferInternal(), relayAddress);
        }
        else {
            IPv6RelayAddress.set(getBufferInternal(), relayAddress);
        }
    }

}
