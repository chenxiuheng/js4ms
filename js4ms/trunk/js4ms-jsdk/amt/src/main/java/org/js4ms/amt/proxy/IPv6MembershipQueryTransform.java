package org.js4ms.amt.proxy;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPv6MembershipQueryTransform.java [org.js4ms.jsdk:amt]
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
import org.js4ms.ip.ipv6.IPv6Packet;
import org.js4ms.ip.protocol.mld.MLDMessage;
import org.js4ms.ip.protocol.mld.MLDQueryMessage;
import org.js4ms.ip.protocol.mld.MLDv2QueryMessage;



/**
 * Transforms an IPPacket object containing an MLDv2 query message into a
 * protocol-independent MembershipQuery object.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class IPv6MembershipQueryTransform
                implements MessageTransform<IPPacket, MembershipQuery> {

    /**
     * Default constructor.
     */
    public IPv6MembershipQueryTransform() {
    }

    @Override
    public MembershipQuery transform(final IPPacket packet) throws IOException {

        MembershipQuery membershipQuery = null;

        if (packet.getVersion() == IPv6Packet.INTERNET_PROTOCOL_VERSION) {

            IPMessage ipMessage = packet.getProtocolMessage(MLDMessage.IP_PROTOCOL_NUMBER);

            if (ipMessage == null || !(ipMessage instanceof MLDQueryMessage)) {
                throw new ProtocolException("IP packet does not contain an MLD Membership Query Message");
            }

            MLDQueryMessage queryMessage = (MLDQueryMessage) ipMessage;

            InetAddress groupAddress = InetAddress.getByAddress(queryMessage.getGroupAddress());

            int maximumResponseTime = queryMessage.getMaximumResponseDelay();
            int robustnessVariable = 2;
            int queryInterval = 125000; // Default query interval
            HashSet<InetAddress> sourceSet = null;
            if (queryMessage instanceof MLDv2QueryMessage) {
                MLDv2QueryMessage v2QueryMessage = (MLDv2QueryMessage) queryMessage;
                robustnessVariable = v2QueryMessage.getQuerierRobustnessVariable();
                queryInterval = v2QueryMessage.getQueryIntervalTime() * 1000;
                if (v2QueryMessage.getNumberOfSources() > 0) {
                    sourceSet = new HashSet<InetAddress>();
                    Iterator<byte[]> iter = v2QueryMessage.getSourceIterator();
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
            throw new ProtocolException("IP packet does not contain an MLD Membership Query Message");
        }

        return membershipQuery;

    }
}
