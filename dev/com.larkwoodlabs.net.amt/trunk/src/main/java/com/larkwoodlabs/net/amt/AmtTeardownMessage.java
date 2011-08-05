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

package com.larkwoodlabs.net.amt;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.util.buffer.fields.ByteArrayField;
import com.larkwoodlabs.util.buffer.fields.ByteField;
import com.larkwoodlabs.util.buffer.fields.IntegerField;
import com.larkwoodlabs.util.buffer.fields.ShortField;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An AMT Teardown Message.
 * 
 * <pre>
 * 6.8. AMT Teardown
 * 
 *    An AMT Teardown is sent by a Gateway after a valid Response MAC has
 *    been received and after the source address that was used to generate
 *    the Response MAC is no longer available for sending packets.
 * 
 *    It is sent to the source address received in the original Query which
 *    should be the same as the original Request.
 * 
 *    The UDP destination port number should be the same one sent in the
 *    original Request.
 * 
 *    An AMT Teardown from the original source address and source port is
 *    NOT valid and should be discarded if received.  Use an AMT Membership
 *    Update instead.
 * 
 *    In order for the Relay to verify the Teardown message, this message
 *    must contain the original source address and source port in addition
 *    to the Original Request Nonce and Original Response MAC.  In
 *    situations where NAT is used, this information can be known by the
 *    Gateway thanks to the optional Gateway information fields in the
 *    Query message (Section 6.5.6).  Hence, a Relay supporting the
 *    Teardown mechanism SHOULD include the Gateway information fields in
 *    the Query messages it sends.
 * 
 *    On reception of a valid Teardown message, a Relay should remove all
 *    state corresponding to the gateway identified by the (original source
 *    address, original source port) tuple, and stop forwarding all traffic
 *    to this destination.
 * 
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |     Type=0x7  |    Reserved   |    Original Response MAC      |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |            Original Response MAC (continued)                  |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |            Original Request Nonce                             |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |     Original Source Port      |  Original Source Address ...  |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                  Original Source Address (ctd) ...            |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |             ...  Original Source Address (ctd) ...            |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |             ...  Original Source Address (ctd) ...            |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    | ... Original Src Addr. (ctd)  |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * 
 * 
 * 6.8.1. Type
 * 
 *    The type of the message.
 * 
 * 6.8.2. Reserved
 * 
 *    A 8-bit reserved field.  Sent as 0, ignored on receipt.
 * 
 * 6.8.3. Original Response MAC
 * 
 *    The 48-bit MAC received in the Membership Query.
 * 
 * 6.8.4. Original Request Nonce
 * 
 *    A 32-bit identifier corresponding to the original Request.
 * 
 * 6.8.5. Original Source Port
 * 
 *    The 16-bit port number used in the original AMT Request message that
 *    was used to generate the Original Response MAC.
 * 
 * 6.8.6. Original Source Address
 * 
 *    A 16-byte field containing the IP source address used in the original
 *    AMT Request message that was used to generate the Original Response
 *    MAC of the Request message that triggered this Query message.  The
 *    field contains an IPv4-compatible IPv6 address ([RFC4291], section
 *    2.5.5.1) if the address is an IPv4 address (i.e. the IPv4 address
 *    prefixed with 96 bits set to zero), or an IPv6 address.
 * </pre>
 * 
 * @author Gregory Bumgardner
 */
final class AmtTeardownMessage extends AmtMessage {

    /*-- Inner Classes ------------------------------------------------------*/

    public static class Parser implements AmtMessage.ParserType {

        @Override
        public AmtTeardownMessage parse(ByteBuffer buffer) throws ParseException {
            return new AmtTeardownMessage(buffer);
        }

        @Override
        public Object getKey() {
            return MESSAGE_TYPE;
        }

    }


    /*-- Static Variables ---------------------------------------------------*/

    public static final byte MESSAGE_TYPE = 0x7;
    public static final int MESSAGE_LENGTH = 30;

    public static final ByteField       Reserved = new ByteField(1);
    public static final ByteArrayField  ResponseMac = new ByteArrayField(2,6);
    public static final IntegerField    RequestNonce = new IntegerField(8);
    public static final ShortField      GatewayPort = new ShortField(12);
    public static final ByteArrayField  GatewayAddress = new ByteArrayField(14,16);
 
    
    /*-- Static Functions ---------------------------------------------------*/
    
     public static AmtTeardownMessage.Parser constructParser() {
        AmtTeardownMessage.Parser parser = new AmtTeardownMessage.Parser();
        return parser;
    }

    /*-- Member Functions---------------------------------------------------*/
    
    /**
     * 
     * @param responseMac
     * @param requestNonce
     * @param updatePacket
     */
    public AmtTeardownMessage(final byte[] responseMac, final int requestNonce, final InetSocketAddress gatewayAddress) {
        super(MESSAGE_LENGTH, MESSAGE_TYPE);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                        "AmtTeardownMessage.AmtTeardownMessage",
                                        Logging.mac(responseMac),
                                        requestNonce,
                                        Logging.address(gatewayAddress)));
        }

        setResponseMac(responseMac);
        setRequestNonce(requestNonce);
        setGatewayAddress(gatewayAddress);

        if (logger.isLoggable(Level.FINER)) {
            logState(logger);
        }
    }

    /**
     * 
     * @param buffer
     * @throws ParseException
     */
    public AmtTeardownMessage(final ByteBuffer buffer) throws ParseException {
        super(consume(buffer, MESSAGE_LENGTH));
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTeardownMessage.AmtTeardownMessage", buffer));
            logState(logger);
        }
    }

    @Override
    public void log(final Logger logger) {
        super.log(logger);
        logState(logger);
    }

    /**
     * Logs value of member variables declared or maintained by this class.
     * @param logger
     */
    private void logState(final Logger logger) {
        logger.info(ObjectId + " : response-MAC="+Logging.mac(getResponseMac()));
        logger.info(ObjectId + " : request-nonce="+getRequestNonce());
        logger.info(ObjectId + " : gateway-address="+Logging.address(getGatewayAddress()));
    }

    @Override
    public Byte getType() {
        return MESSAGE_TYPE;
    }

    /**
     * 
     * @return
     */
    public byte[] getResponseMac() {
        return ResponseMac.get(getBufferInternal());
    }
    
    /**
     * 
     * @param responseMac
     */
    public void setResponseMac(final byte[] responseMac) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,"AmtTeardownMessage.setResponseMac", Logging.mac(responseMac)));
        }
        
        ResponseMac.set(getBufferInternal(),responseMac);
    }
    
    /**
     * 
     * @return
     */
    public int getRequestNonce() {
        return RequestNonce.get(getBufferInternal());
    }
    
    /**
     * 
     * @param requestNonce
     */
    public void setRequestNonce(final int requestNonce) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTeardownMessage.setRequestNonce", requestNonce));
        }
        
        RequestNonce.set(getBufferInternal(),requestNonce);
    }

    /**
     * 
     * @return
     */
    public InetSocketAddress getGatewayAddress() {
        try {
            return new InetSocketAddress(InetAddress.getByAddress(GatewayAddress.get(getBufferInternal())),GatewayPort.get(getBufferInternal()));
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 
     * @param gatewayAddress
     */
    public void setGatewayAddress(final InetSocketAddress gatewayAddress) {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTeardownMessage.setGatewayAddress", gatewayAddress));
        }

        short port = (short)gatewayAddress.getPort();
        GatewayPort.set(getBufferInternal(), port);

        byte[] address = gatewayAddress.getAddress().getAddress();
        if (address.length == 4) {
            byte[] ipv6Address = new byte[16];
            for (int i=0;i<4;i++) {
                ipv6Address[i+12] = address[i];
            }
            address = ipv6Address;
        }
        GatewayAddress.set(getBufferInternal(),address);
    }
}
