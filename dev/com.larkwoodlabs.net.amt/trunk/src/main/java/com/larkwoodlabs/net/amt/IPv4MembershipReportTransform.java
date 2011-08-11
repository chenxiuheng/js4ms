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


import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import com.larkwoodlabs.channels.MessageTransform;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.igmp.IGMPGroupRecord;
import com.larkwoodlabs.net.ip.igmp.IGMPMessage;
import com.larkwoodlabs.net.ip.igmp.IGMPv3ReportMessage;

public final class IPv4MembershipReportTransform implements MessageTransform<MembershipReport, IPPacket> {

    private byte[] ipv4SourceAddress = null;
    
    public IPv4MembershipReportTransform() {

        InetAddress localHostAddress;
        try {
            localHostAddress = InetAddress.getLocalHost();
        }
        catch (UnknownHostException e) {
            throw new Error(e);
        }

        NetworkInterface networkInterface = null;

        try {
            networkInterface = NetworkInterface.getByInetAddress(localHostAddress);
        }
        catch (SocketException e) {
            /*
            throw new UnknownHostException("attempt to identify network interface for local host address " +
                                           localHostAddress.getHostAddress() +
                                           " failed - " + e.getMessage());
            */
        }

        if (networkInterface != null) {
            Enumeration<InetAddress> iter = networkInterface.getInetAddresses();
            while (iter.hasMoreElements()) {
                byte[] address = iter.nextElement().getAddress();
                if (address.length == 4) {
                    this.ipv4SourceAddress = address;
                }
            }
        }
        
        if (this.ipv4SourceAddress == null) {
            this.ipv4SourceAddress = new byte[4];
        }

    }
    
    @Override
    public IPPacket transform(final MembershipReport message) throws IOException {

        IPPacket reportPacket = null;

        IGMPv3ReportMessage reportMessage = new IGMPv3ReportMessage();

        for (GroupMembershipRecord record : message.getRecords()) {
            IGMPGroupRecord groupRecord = new IGMPGroupRecord((byte)record.getRecordType().getValue(), record.getGroup().getAddress());
            for (InetAddress sourceAddress : record.getSources()) {
                groupRecord.addSource(sourceAddress);
            }
            reportMessage.addGroupRecord(groupRecord);
        }

        reportPacket = IGMPMessage.constructIPv4Packet(this.ipv4SourceAddress,
                                                       IGMPMessage.IPv4ReportDestinationAddress,
                                                       reportMessage);
        
        return reportPacket;
    }
}
