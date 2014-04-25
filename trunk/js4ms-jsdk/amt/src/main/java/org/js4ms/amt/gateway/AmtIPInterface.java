package org.js4ms.amt.gateway;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * AmtIPInterface.java [org.js4ms.jsdk:amt]
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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.amt.proxy.IPv4MembershipQueryTransform;
import org.js4ms.amt.proxy.IPv4MembershipReportTransform;
import org.js4ms.amt.proxy.IPv6MembershipQueryTransform;
import org.js4ms.amt.proxy.IPv6MembershipReportTransform;
import org.js4ms.amt.proxy.MembershipQuery;
import org.js4ms.amt.proxy.MembershipReport;
import org.js4ms.common.util.logging.Log;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.io.channel.MessageKeyExtractor;
import org.js4ms.io.channel.OutputChannel;
import org.js4ms.io.channel.OutputChannelMap;
import org.js4ms.io.channel.OutputChannelTee;
import org.js4ms.io.channel.OutputChannelTransform;
import org.js4ms.ip.IPMessage;
import org.js4ms.ip.IPPacket;
import org.js4ms.ip.Precondition;
import org.js4ms.ip.ipv4.IPv4Packet;
import org.js4ms.ip.ipv6.IPv6Packet;
import org.js4ms.ip.protocol.igmp.IGMPMessage;
import org.js4ms.ip.protocol.mld.MLDMessage;




public class AmtIPInterface {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtIPInterface.class.getName());

    /*-- Member Variables ---------------------------------------------------*/

    protected final Log log = new Log(this);

    private AmtIPInterfaceManager manager = null;

    private final AmtPseudoInterface amtPseudoInterface;

    private final OutputChannelTee<IPPacket> dispatchChannel;

    private int referenceCount = 0;

    private final Timer taskTimer;

    private final InterfaceMembershipManager ipv4MembershipManager;

    private final InterfaceMembershipManager ipv6MembershipManager;

    /**
     * @param manager
     * @param relayDiscoveryAddress
     * @throws IOException
     * @throws InterruptedException
     */
    AmtIPInterface(final AmtIPInterfaceManager manager,
                   final InetAddress relayDiscoveryAddress) throws IOException, InterruptedException {
        this(AmtPseudoInterfaceManager.getInstance().getInterface(relayDiscoveryAddress));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.AmtIPInterface", manager, Logging.address(relayDiscoveryAddress)));
        }

        this.manager = manager;

        // Reverse extra acquire() call made in public constructor
        this.amtPseudoInterface.release();
    }

    /**
     * Constructs an AMT IP interface.
     * The release method MUST be called if the AmtIPInterfaceManager was not used to
     * construct this object.
     * 
     * @param amtPseudoInterface
     * @throws IOException
     */
    public AmtIPInterface(final AmtPseudoInterface amtPseudoInterface) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.AmtIPInterface", amtPseudoInterface));
        }

        amtPseudoInterface.acquire();

        this.amtPseudoInterface = amtPseudoInterface;
        this.dispatchChannel = new OutputChannelTee<IPPacket>();

        this.taskTimer = new Timer("AMT IP Interface");

        // Create channel that sends packets over the pseudo-interface
        OutputChannel<IPPacket> reportChannel = new OutputChannel<IPPacket>() {

            @Override
            public void send(IPPacket packet, int milliseconds) throws IOException, InterruptedException {
                AmtIPInterface.this.amtPseudoInterface.send(packet);
            }

            @Override
            public void close() {
            }
        };

        // Create separate membership managers for IPv4/IGMP and IPv6/MLD group
        // subscriptions
        this.ipv4MembershipManager = new InterfaceMembershipManager(this.taskTimer);
        this.ipv6MembershipManager = new InterfaceMembershipManager(this.taskTimer);

        // Connect report channel of interface membership managers to pseudo-interface.
        this.ipv4MembershipManager.setOutgoingReportChannel(
                        new OutputChannelTransform<MembershipReport, IPPacket>(reportChannel,
                                                                               new IPv4MembershipReportTransform()));
        this.ipv6MembershipManager.setOutgoingReportChannel(
                        new OutputChannelTransform<MembershipReport, IPPacket>(reportChannel,
                                                                               new IPv6MembershipReportTransform()));

        // Create transforms for interface membership manager to query channels
        OutputChannelTransform<IPPacket, MembershipQuery> ipv4TransformChannel = new OutputChannelTransform<IPPacket, MembershipQuery>(
                                                                                                                                       this.ipv4MembershipManager
                                                                                                                                                       .getIncomingQueryChannel(),
                                                                                                                                       new IPv4MembershipQueryTransform());
        OutputChannelTransform<IPPacket, MembershipQuery> ipv6TransformChannel = new OutputChannelTransform<IPPacket, MembershipQuery>(
                                                                                                                                       this.ipv6MembershipManager
                                                                                                                                                       .getIncomingQueryChannel(),
                                                                                                                                       new IPv6MembershipQueryTransform());

        // Create an extractor to differentiate between IGMP, MLD and other message types.
        MessageKeyExtractor<IPPacket> protocolExtractor = new MessageKeyExtractor<IPPacket>() {

            @Override
            public Byte getKey(IPPacket packet) {
                if (packet.getVersion() == IPv4Packet.INTERNET_PROTOCOL_VERSION) {
                    IPMessage ipMessage = packet.getProtocolMessage(IGMPMessage.IP_PROTOCOL_NUMBER);
                    if (ipMessage == null) {
                        return null;
                    }
                    return IGMPMessage.IP_PROTOCOL_NUMBER;
                }
                else if (packet.getVersion() == IPv6Packet.INTERNET_PROTOCOL_VERSION) {
                    IPMessage ipMessage = packet.getProtocolMessage(MLDMessage.IP_PROTOCOL_NUMBER);
                    if (ipMessage == null) {
                        return null;
                    }
                    return MLDMessage.IP_PROTOCOL_NUMBER;
                }
                else {
                    return null;
                }
            }
        };

        // Create the output channel map that will be used to route packets to the
        // appropriate recipients.
        OutputChannelMap<IPPacket> outputChannelMap = new OutputChannelMap<IPPacket>(protocolExtractor);

        // Create channels that route IGMP or MLD messages to appropriate transform
        // channels.
        outputChannelMap.put(IGMPMessage.IP_PROTOCOL_NUMBER, ipv4TransformChannel);
        outputChannelMap.put(MLDMessage.IP_PROTOCOL_NUMBER, ipv6TransformChannel);

        // Add channel that will receive any packets containing something other than an
        // IGMP or MLD messages.
        outputChannelMap.put(null, this.dispatchChannel);

        // Connect the channel map to the pseudo-interface output channel
        this.amtPseudoInterface.addOutputChannel(outputChannelMap);

    }

    /**
     * Adds a packet destination channel.
     * 
     * @param destinationChannel
     */
    public void addOutputChannel(final OutputChannel<IPPacket> destinationChannel) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.addOutputChannel", destinationChannel));
        }

        this.dispatchChannel.add(destinationChannel);
    }

    /**
     * Removes a packet destination channel.
     * 
     * @param destinationChannel
     */
    public void removeOutputChannel(final OutputChannel<IPPacket> destinationChannel) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.removeOutputChannel", destinationChannel));
        }

        this.dispatchChannel.remove(destinationChannel);
    }

    /**
     * Gets the Relay Discovery Address associated with this interface.
     * 
     * @return An InetAddress object containing the Relay Discovery Address
     */
    public InetAddress getRelayDiscoveryAddress() {
        return this.amtPseudoInterface.getRelayDiscoveryAddress();
    }

    /**
     * 
     */
    synchronized void acquire() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.acquire"));
        }

        this.referenceCount++;
    }

    /**
     * @throws InterruptedException
     * @throws IOException
     */
    public synchronized void release() throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.release"));
        }

        if (--this.referenceCount == 0) {
            if (this.manager != null) {
                this.manager.closeInterface(this);
            }
            else {
                close();
            }
        }
    }

    /**
     * @throws InterruptedException
     * @throws IOException
     */
    public void close() throws InterruptedException, IOException {
        this.amtPseudoInterface.release();
    }

    /**
     * @param groupAddress
     * @throws IOException
     * @throws InterruptedException
     */
    public void join(final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.join", Logging.address(groupAddress)));
        }

        if (groupAddress instanceof Inet4Address) {
            this.ipv4MembershipManager.join(groupAddress);
        }
        else {
            this.ipv6MembershipManager.join(groupAddress);
        }
    }

    /**
     * @param groupAddress
     * @param sourceAddress
     * @throws IOException
     * @throws InterruptedException
     */
    public void join(final InetAddress groupAddress,
                     final InetAddress sourceAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.join",
                                        Logging.address(groupAddress),
                                        Logging.address(sourceAddress)));
        }

        Precondition.checkAddresses(groupAddress, sourceAddress);

        if (groupAddress instanceof Inet4Address) {
            this.ipv4MembershipManager.join(groupAddress, sourceAddress);
        }
        else {
            this.ipv6MembershipManager.join(groupAddress, sourceAddress);
        }
    }

    /**
     * @param groupAddress
     * @throws IOException
     */
    public void leave(final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.leave", Logging.address(groupAddress)));
        }

        if (groupAddress instanceof Inet4Address) {
            this.ipv4MembershipManager.leave(groupAddress);
        }
        else {
            this.ipv6MembershipManager.leave(groupAddress);
        }
    }

    /**
     * @param groupAddress
     * @param sourceAddress
     * @throws IOException
     */
    public void leave(final InetAddress groupAddress,
                      final InetAddress sourceAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.leave",
                                        Logging.address(groupAddress),
                                        Logging.address(sourceAddress)));
        }

        Precondition.checkAddresses(groupAddress, sourceAddress);

        if (groupAddress instanceof Inet4Address) {
            this.ipv4MembershipManager.leave(groupAddress, sourceAddress);
        }
        else {
            this.ipv6MembershipManager.leave(groupAddress, sourceAddress);
        }
    }

    /**
     * @throws IOException
     */
    public void leave() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterface.leave"));
        }

        this.ipv4MembershipManager.leave();
        this.ipv6MembershipManager.leave();
    }
}
