package org.js4ms.amt.gateway;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * AmtUDPInterface.java [org.js4ms.jsdk:amt]
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

import org.js4ms.common.util.logging.Log;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.io.channel.MessageKeyExtractor;
import org.js4ms.io.channel.OutputChannel;
import org.js4ms.io.channel.OutputChannelMap;
import org.js4ms.io.channel.OutputChannelTransform;
import org.js4ms.io.net.UdpDatagram;
import org.js4ms.ip.IPPacket;
import org.js4ms.ip.Precondition;
import org.js4ms.ip.ipv4.IPv4Packet;
import org.js4ms.ip.ipv6.IPv6Packet;




public final class AmtUDPInterface {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtUDPInterface.class.getName());

    static final int MAX_REASSEMBLY_CACHE_SIZE = 100;

    /*-- Member Variables ---------------------------------------------------*/

    protected final Log log = new Log(this);

    private AmtUDPInterfaceManager manager = null;

    private final AmtIPInterface amtIPInterface;

    private int referenceCount = 0;

    private ChannelMembershipManager ipv4MembershipManager = null;

    private ChannelMembershipManager ipv6MembershipManager = null;

    OutputChannelMap<IPPacket> outputChannelMap;

    /**
     * @param manager
     * @param relayDiscoveryAddress
     * @throws IOException
     * @throws InterruptedException
     */
    AmtUDPInterface(final AmtUDPInterfaceManager manager,
                    final InetAddress relayDiscoveryAddress) throws IOException, InterruptedException {
        this(AmtIPInterfaceManager.getInstance().getInterface(relayDiscoveryAddress));

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.AmtUDPInterface", manager, Logging.address(relayDiscoveryAddress)));
        }

        this.manager = manager;

        // Reverse acquire() call made in public constructor.
        this.amtIPInterface.release();

    }

    /**
     * @param amtIPInterface
     * @throws IOException
     */
    public AmtUDPInterface(final AmtIPInterface amtIPInterface) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.AmtUDPInterface", amtIPInterface));
        }

        amtIPInterface.acquire();

        this.amtIPInterface = amtIPInterface;

        // Create an extractor to differentiate between IGMP, MLD and other message types.
        MessageKeyExtractor<IPPacket> protocolExtractor = new MessageKeyExtractor<IPPacket>() {

            @Override
            public Byte getKey(IPPacket packet) {
                return packet.getVersion();
            }
        };

        // Create the output channel map that will be used to route IP packets to the
        // appropriate recipients.
        this.outputChannelMap = new OutputChannelMap<IPPacket>(protocolExtractor);

        // Create packet assembler that will reassemble packets from the IP interface and
        // forward them to the output channel map
        PacketAssembler assembler = new PacketAssembler(outputChannelMap,
                                                        MAX_REASSEMBLY_CACHE_SIZE,
                                                        new Timer("AMT UDP Interface"));

        this.amtIPInterface.addOutputChannel(assembler);

    }

    /**
     * Gets the Relay Discovery Address associated with this interface.
     * 
     * @return An InetAddress object containing the Relay Discovery Address
     */
    public InetAddress getRelayDiscoveryAddress() {
        return this.amtIPInterface.getRelayDiscoveryAddress();
    }

    /**
     * 
     */
    synchronized void acquire() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.acquire"));
        }

        this.referenceCount++;
    }

    /**
     * @throws InterruptedException
     * @throws IOException
     */
    public synchronized void release() throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.release"));
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
        this.amtIPInterface.release();
    }

    private void constructIPv4MembershipManager() {

        this.ipv4MembershipManager = new ChannelMembershipManager(this.amtIPInterface);
        // Create channels that transform route UDP packets to the appropriate channel
        // membership manager channels.
        this.outputChannelMap
                        .put(IPv4Packet.INTERNET_PROTOCOL_VERSION,
                             new OutputChannelTransform<IPPacket, UdpDatagram>(
                                                                               this.ipv4MembershipManager.getDispatchChannel(),
                                                                               new MulticastDataTransform()));
    }

    private void constructIPv6MembershipManager() {

        this.ipv6MembershipManager = new ChannelMembershipManager(this.amtIPInterface);
        // Create channels that transform route UDP packets to the appropriate channel
        // membership manager channels.
        this.outputChannelMap
                        .put(IPv6Packet.INTERNET_PROTOCOL_VERSION,
                             new OutputChannelTransform<IPPacket, UdpDatagram>(
                                                                               this.ipv6MembershipManager.getDispatchChannel(),
                                                                               new MulticastDataTransform()));
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param port
     * @throws IOException
     * @throws InterruptedException
     */
    public void join(final OutputChannel<UdpDatagram> pushChannel,
                     final InetAddress groupAddress,
                     int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.join", pushChannel, Logging.address(groupAddress), port));
        }

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4MembershipManager == null) {
                constructIPv4MembershipManager();
            }
            this.ipv4MembershipManager.join(pushChannel, groupAddress, port);
        }
        else {
            if (this.ipv6MembershipManager == null) {
                constructIPv6MembershipManager();
            }
            this.ipv6MembershipManager.join(pushChannel, groupAddress, port);
        }
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @param port
     * @throws IOException
     * @throws InterruptedException
     */
    public void join(final OutputChannel<UdpDatagram> pushChannel,
                     final InetAddress groupAddress,
                     final InetAddress sourceAddress,
                     final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.join",
                                        pushChannel,
                                        Logging.address(groupAddress),
                                        Logging.address(sourceAddress),
                                        port));
        }

        Precondition.checkAddresses(groupAddress, sourceAddress);

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4MembershipManager == null) {
                constructIPv4MembershipManager();
            }
            this.ipv4MembershipManager.join(pushChannel, groupAddress, sourceAddress, port);
        }
        else {
            if (this.ipv6MembershipManager == null) {
                constructIPv6MembershipManager();
            }
            this.ipv6MembershipManager.join(pushChannel, groupAddress, sourceAddress, port);
        }
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @throws IOException
     */
    public void leave(final OutputChannel<UdpDatagram> pushChannel,
                      final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.leave", pushChannel, Logging.address(groupAddress)));
        }

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4MembershipManager == null) {
                constructIPv4MembershipManager();
            }
            this.ipv4MembershipManager.leave(pushChannel, groupAddress);
        }
        else {
            if (this.ipv6MembershipManager == null) {
                constructIPv6MembershipManager();
            }
            this.ipv6MembershipManager.leave(pushChannel, groupAddress);
        }
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param port
     * @throws IOException
     */
    public void leave(final OutputChannel<UdpDatagram> pushChannel,
                      final InetAddress groupAddress,
                      final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.leave", pushChannel, Logging.address(groupAddress), port));
        }

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4MembershipManager == null) {
                constructIPv4MembershipManager();
            }
            this.ipv4MembershipManager.leave(pushChannel, groupAddress, port);
        }
        else {
            if (this.ipv6MembershipManager == null) {
                constructIPv6MembershipManager();
            }
            this.ipv6MembershipManager.leave(pushChannel, groupAddress, port);
        }
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @throws IOException
     */
    public void leave(final OutputChannel<UdpDatagram> pushChannel,
                      final InetAddress groupAddress,
                      final InetAddress sourceAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.leave",
                                        pushChannel,
                                        Logging.address(groupAddress),
                                        Logging.address(sourceAddress)));
        }

        Precondition.checkAddresses(groupAddress, sourceAddress);

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4MembershipManager == null) {
                constructIPv4MembershipManager();
            }
            this.ipv4MembershipManager.leave(pushChannel, groupAddress, sourceAddress);
        }
        else {
            if (this.ipv6MembershipManager == null) {
                constructIPv6MembershipManager();
            }
            this.ipv6MembershipManager.leave(pushChannel, groupAddress, sourceAddress);
        }
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @param port
     * @throws IOException
     */
    public void leave(final OutputChannel<UdpDatagram> pushChannel,
                      final InetAddress groupAddress,
                      final InetAddress sourceAddress,
                      final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.leave",
                                        pushChannel,
                                        Logging.address(groupAddress),
                                        Logging.address(sourceAddress),
                                        port));
        }

        Precondition.checkAddresses(groupAddress, sourceAddress);

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4MembershipManager == null) {
                constructIPv4MembershipManager();
            }
            this.ipv4MembershipManager.leave(pushChannel, groupAddress, sourceAddress, port);
        }
        else {
            if (this.ipv6MembershipManager == null) {
                constructIPv6MembershipManager();
            }
            this.ipv6MembershipManager.leave(pushChannel, groupAddress, sourceAddress, port);
        }

    }

    /**
     * @param pushChannel
     * @throws IOException
     */
    public void leave(final OutputChannel<UdpDatagram> pushChannel) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.leave", pushChannel));
        }

        if (this.ipv4MembershipManager != null) {
            this.ipv4MembershipManager.leave(pushChannel);
        }
        else if (this.ipv6MembershipManager != null) {
            this.ipv6MembershipManager.leave(pushChannel);
        }

    }
}
