package org.js4ms.amt.proxy;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPv4MembershipReportTransform.java [org.js4ms.jsdk:amt]
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

import org.js4ms.amt.message.GroupMembershipRecord;
import org.js4ms.io.channel.MessageTransform;
import org.js4ms.ip.IPPacket;
import org.js4ms.ip.protocol.igmp.IGMPGroupRecord;
import org.js4ms.ip.protocol.igmp.IGMPMessage;
import org.js4ms.ip.protocol.igmp.IGMPv3ReportMessage;



/**
 * Transforms a protocol-independent MembershipReport object into an IPPacket object
 * containing an IGMPv3 report message.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class IPv4MembershipReportTransform
                implements MessageTransform<MembershipReport, IPPacket> {

    /**
     * An IANA-assigned link-local address used by AMT gateways as an IGMP packet
     * source address.
     */
    private static final byte[] ASSIGNED_IGMP_SOURCE_ADDRESS = {
                    (byte) 154, (byte) 7, (byte) 1, (byte) 2
    };

    // private byte[] ipv4SourceAddress = null;

    /**
     * Constructs a transform instance.
     * This constructor iterates over the set of network interfaces
     * to acquire an IPv4 address to use as the source address in the
     * packets
     */
    public IPv4MembershipReportTransform() {

        /*
         * NOTE: A host IPv4 address is no longer used as the source address in the IGMP
         * packet.
         * 
         * InetAddress localHostAddress;
         * try {
         * localHostAddress = InetAddress.getLocalHost();
         * }
         * catch (UnknownHostException e) {
         * throw new Error(e);
         * }
         * 
         * NetworkInterface networkInterface = null;
         * 
         * try {
         * networkInterface = NetworkInterface.getByInetAddress(localHostAddress);
         * }
         * catch (SocketException e) {
         * //throw new UnknownHostException(
         * "attempt to identify network interface for local host address " +
         * // localHostAddress.getHostAddress() +
         * // " failed - " + e.getMessage());
         * }
         * 
         * if (networkInterface != null) {
         * Enumeration<InetAddress> iter = networkInterface.getInetAddresses();
         * while (iter.hasMoreElements()) {
         * byte[] address = iter.nextElement().getAddress();
         * if (address.length == 4) {
         * this.ipv4SourceAddress = address;
         * }
         * }
         * }
         * 
         * if (this.ipv4SourceAddress == null) {
         * this.ipv4SourceAddress = new byte[4];
         * }
         */
    }

    @Override
    public IPPacket transform(final MembershipReport message) throws IOException {

        IPPacket reportPacket = null;

        IGMPv3ReportMessage reportMessage = new IGMPv3ReportMessage();

        for (GroupMembershipRecord record : message.getRecords()) {
            IGMPGroupRecord groupRecord = new IGMPGroupRecord((byte) record.getRecordType().getValue(), record.getGroup()
                            .getAddress());
            for (InetAddress sourceAddress : record.getSources()) {
                groupRecord.addSource(sourceAddress);
            }
            reportMessage.addGroupRecord(groupRecord);
        }

        reportPacket = IGMPMessage.constructIPv4Packet(ASSIGNED_IGMP_SOURCE_ADDRESS,
                                                       IGMPMessage.IPv4ReportDestinationAddress,
                                                       reportMessage);

        return reportPacket;
    }
}
