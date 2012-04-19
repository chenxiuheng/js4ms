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

package com.larkwoodlabs.net.amt.client;

import java.io.IOException;

import com.larkwoodlabs.channels.MessageTransform;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.udp.UdpPacket;
import com.larkwoodlabs.net.udp.UdpDatagram;

/**
 *
 */
final class MulticastDataTransform implements MessageTransform<IPPacket, UdpDatagram> {


    final UdpPacket.Parser parser;
    
    public MulticastDataTransform() {
        this.parser = new UdpPacket.Parser();
    }

    @Override
    public UdpDatagram transform(final IPPacket ipPacket) throws IOException {

        /*
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MulticastDataTransform.translate", message));
        }
        */

        /*
        IPMessage ipMessage = ipPacket.getProtocolMessage(UdpPacket.IP_PROTOCOL_NUMBER);
        if (ipMessage == null) {
            // logger.warning(ObjectId + " AMT Multicast Data Message does not contain a valid UDP packet");
            throw new ProtocolException("AMT Multicast Data Message does not contain a valid UDP packet");
        }
        */
        
        UdpPacket udpPacket;
        try {
            udpPacket = (UdpPacket)this.parser.parse(ipPacket.getUnparsedPayload());
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.getClass().getName()+":"+e.getMessage());
        }
        
        // Construct a UdpDatagram to be delivered to all input channels
        UdpDatagram datagram = new UdpDatagram(ipPacket.getSourceAddress(),
                                               udpPacket.getSourcePort(),
                                               ipPacket.getDestinationAddress(),
                                               udpPacket.getDestinationPort(),
                                               udpPacket.getPayload());

        return datagram;
    }
}