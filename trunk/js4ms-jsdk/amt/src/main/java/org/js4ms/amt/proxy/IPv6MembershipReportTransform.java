package org.js4ms.amt.proxy;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * IPv6MembershipReportTransform.java [org.js4ms.jsdk:amt]
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
import org.js4ms.ip.protocol.mld.MLDGroupRecord;
import org.js4ms.ip.protocol.mld.MLDMessage;
import org.js4ms.ip.protocol.mld.MLDv2ReportMessage;



/**
 * Transforms a protocol-independent MembershipReport object into an IPPacket object
 * containing an MLDv2 report message.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class IPv6MembershipReportTransform
                implements MessageTransform<MembershipReport, IPPacket> {

    /**
     * The link-local address used by AMT gateways as an MLD packet source address.
     */
    private static final byte[] ASSIGNED_MLD_SOURCE_ADDRESS = {
                    (byte) 0xFE, (byte) 0x80, (byte) 0, (byte) 0,
                    (byte) 0, (byte) 0, (byte) 0, (byte) 0,
                    (byte) 0, (byte) 0, (byte) 0, (byte) 0,
                    (byte) 0, (byte) 0, (byte) 0, (byte) 3
    };

    // private byte[] ipv6SourceAddress = null;

    public IPv6MembershipReportTransform() {

        /*
         * NOTE: A host IPv6 address is no longer used as the source address in the MLD
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
         * // throw new UnknownHostException(
         * "attempt to identify network interface for local host address " +
         * // localHostAddress.getHostAddress() +
         * // " failed - " + e.getMessage());
         * }
         * 
         * if (networkInterface != null) {
         * Enumeration<InetAddress> iter = networkInterface.getInetAddresses();
         * while (iter.hasMoreElements()) {
         * byte[] address = iter.nextElement().getAddress();
         * if (address.length == 6) {
         * this.ipv6SourceAddress = address;
         * }
         * }
         * }
         * 
         * if (this.ipv6SourceAddress == null) {
         * this.ipv6SourceAddress = new byte[16];
         * }
         */
    }

    @Override
    public IPPacket transform(final MembershipReport message) throws IOException {

        IPPacket reportPacket = null;
        MLDv2ReportMessage reportMessage = new MLDv2ReportMessage();

        for (GroupMembershipRecord record : message.getRecords()) {
            MLDGroupRecord groupRecord = new MLDGroupRecord((byte) record.getRecordType().getValue(), record.getGroup()
                            .getAddress());
            for (InetAddress sourceAddress : record.getSources()) {
                groupRecord.addSource(sourceAddress);
            }
            reportMessage.addGroupRecord(groupRecord);
        }

        reportPacket = MLDMessage.constructIPv6Packet(ASSIGNED_MLD_SOURCE_ADDRESS,
                                                      MLDMessage.IPv6ReportDestinationAddress,
                                                      reportMessage);

        return reportPacket;
    }
}
