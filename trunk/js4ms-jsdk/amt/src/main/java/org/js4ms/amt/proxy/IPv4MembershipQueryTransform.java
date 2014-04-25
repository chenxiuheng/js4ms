package org.js4ms.amt.proxy;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPv4MembershipQueryTransform.java [org.js4ms.jsdk:amt]
 * %%
 * Copyright (C) 2009 - 2014 Cisco Systems, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.io.IOException;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.util.HashSet;
import java.util.Iterator;

import org.js4ms.io.channel.MessageTransform;
import org.js4ms.ip.IPMessage;
import org.js4ms.ip.IPPacket;
import org.js4ms.ip.ipv4.IPv4Packet;
import org.js4ms.ip.protocol.igmp.IGMPMessage;
import org.js4ms.ip.protocol.igmp.IGMPQueryMessage;
import org.js4ms.ip.protocol.igmp.IGMPv3QueryMessage;



/**
 * Transforms an IPPacket object containing an IGMPv3 query message into a 
 * protocol-independent MembershipQuery object.
 *
 * @author Greg Bumgardner (gbumgard)
 */
public final class IPv4MembershipQueryTransform implements MessageTransform<IPPacket, MembershipQuery> {

    /**
     * Default constructor.
     */
    public IPv4MembershipQueryTransform() {
    }

    @Override
    public MembershipQuery transform(final IPPacket packet) throws IOException {
        
        MembershipQuery membershipQuery = null;
        
        if (packet.getVersion() == IPv4Packet.INTERNET_PROTOCOL_VERSION) {
        
            IPMessage ipMessage = packet.getProtocolMessage(IGMPMessage.IP_PROTOCOL_NUMBER);
    
            if (ipMessage == null || !(ipMessage instanceof IGMPQueryMessage)) {
                throw new ProtocolException("IP packet does not contain an IGMP Membership Query Message");
            }
    
            IGMPQueryMessage queryMessage = (IGMPQueryMessage)ipMessage;
    
            InetAddress groupAddress = InetAddress.getByAddress(queryMessage.getGroupAddress());
        
            int maximumResponseTime = queryMessage.getMaximumResponseTime();
            int robustnessVariable = 2;
            int queryInterval = 125000; // Default query interval
            HashSet<InetAddress> sourceSet = null;
            if (queryMessage instanceof IGMPv3QueryMessage) {
                IGMPv3QueryMessage v3QueryMessage = (IGMPv3QueryMessage)queryMessage;
                robustnessVariable = v3QueryMessage.getQuerierRobustnessVariable();
                queryInterval = v3QueryMessage.getQueryIntervalTime() * 1000;
                if (v3QueryMessage.getNumberOfSources() > 0) {
                    sourceSet = new HashSet<InetAddress>();
                    Iterator<byte[]> iter = v3QueryMessage.getSourceIterator();
                    InetAddress sourceAddress = InetAddress.getByAddress(iter.next());
                    while (iter.hasNext()) {
                        sourceSet.add(sourceAddress);
                    }
                }
            }

            membershipQuery = new MembershipQuery(groupAddress,
                                                  sourceSet,
                                                  maximumResponseTime,
                                                  robustnessVariable,
                                                  queryInterval);
        }
        else {
            throw new ProtocolException("IP packet does not contain an IGMP Membership Query Message");
        }

        return membershipQuery;

    }
}
