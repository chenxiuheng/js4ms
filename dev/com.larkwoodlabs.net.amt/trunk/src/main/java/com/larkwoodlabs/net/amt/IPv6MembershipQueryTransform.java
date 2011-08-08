/**
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.util.HashSet;
import java.util.Iterator;

import com.larkwoodlabs.channels.MessageTransform;
import com.larkwoodlabs.net.ip.IPMessage;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.ipv6.IPv6Packet;
import com.larkwoodlabs.net.ip.mld.MLDMessage;
import com.larkwoodlabs.net.ip.mld.MLDQueryMessage;
import com.larkwoodlabs.net.ip.mld.MLDv2QueryMessage;

final class IPv6MembershipQueryTransform implements MessageTransform<IPPacket, MembershipQuery> {

    private final AmtTunnelEndpoint endpoint;

    public IPv6MembershipQueryTransform(final AmtTunnelEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public MembershipQuery transform(final IPPacket packet) throws IOException {
        
        MembershipQuery membershipQuery = null;
        
        if (packet.getVersion() == IPv6Packet.INTERNET_PROTOCOL_VERSION) {
        
            IPMessage ipMessage = packet.getProtocolMessage(MLDMessage.IP_PROTOCOL_NUMBER);
            
            if (ipMessage == null || !(ipMessage instanceof MLDQueryMessage)) {
                throw new ProtocolException("AMT Membership Query Message does not contain an MLD Membership Query Message");
            }
    
            MLDQueryMessage queryMessage = (MLDQueryMessage)ipMessage;
    
            InetAddress groupAddress = InetAddress.getByAddress(queryMessage.getGroupAddress());
        
            int maximumResponseTime = queryMessage.getMaximumResponseDelay();
            int robustnessVariable = 2;
            int queryInterval = 125000; // Default query interval
            HashSet<InetAddress> sourceSet = null;
            if (queryMessage instanceof MLDv2QueryMessage) {
                MLDv2QueryMessage v2QueryMessage = (MLDv2QueryMessage)queryMessage;
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
            throw new ProtocolException("AMT Membership Query Message does not contain an MLD Membership Query Message");
        }

        // Use query interval received in query message to (re)start periodic request generation task.

        this.endpoint.startRequestTask(membershipQuery.getQueryInterval());

        return membershipQuery;

    }
}
