/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: MulticastDataTransform.java (com.larkwoodlabs.net.amt.gateway)
 * 
 * Copyright © 2009-2012 Cisco Systems, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.larkwoodlabs.net.amt.gateway;

import java.io.IOException;

import com.larkwoodlabs.channels.MessageTransform;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.udp.UdpPacket;
import com.larkwoodlabs.net.udp.UdpDatagram;

/**
 *
 */
final class MulticastDataTransform implements MessageTransform<IPPacket, UdpDatagram> {


    private final UdpPacket.Parser parser;
    
    MulticastDataTransform() {
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
