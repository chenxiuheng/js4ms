/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: AmtIPInterface.java (com.larkwoodlabs.net.amt.gateway)
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
import java.net.InetAddress;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.MessageTransform;
import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.channels.OutputChannelTransform;
import com.larkwoodlabs.net.amt.MembershipQuery;
import com.larkwoodlabs.net.amt.MembershipReport;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.udp.UdpDatagram;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Manages an AMT tunnel end-point.
 * The {@link AmtIPInterfaceManager} constructs a separate AMT interface for each unique
 * AMT relay acting as a remote AMT tunnel end-point.
 * An AmtIPInterface provides functions for joining, leaving and receiving packets
 * for any number of multicast groups. The AmtIPInterface tracks local group
 * membership state and handles the exchange of IGMP/MLD messages used
 * to query or update that state.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public abstract class AmtIPInterface {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtIPInterface.class.getName());

    /*-- Member Variables ---------------------------------------------------*/

    protected final String ObjectId = Logging.identify(this);

    private final InetAddress relayDiscoveryAddress;

    protected final AmtTunnelEndpoint tunnelEndpoint;

    protected final InterfaceMembershipManager interfaceManager;

    protected final ChannelMembershipManager channelManager;

    private final Timer taskTimer;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param manager
     * @param relayDiscoveryAddress
     * @throws IOException
     */
    protected AmtIPInterface(final InetAddress relayDiscoveryAddress,
                             final MessageTransform<IPPacket, MembershipQuery> queryTransform,
                             final MessageTransform<MembershipReport, IPPacket> reportTransform) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtIPInterface.AmtIPInterface", Logging.address(relayDiscoveryAddress)));
        }

        this.relayDiscoveryAddress = relayDiscoveryAddress;

        this.taskTimer = new Timer("AMT IP interface");

        this.tunnelEndpoint = new AmtTunnelEndpoint(this, relayDiscoveryAddress, this.taskTimer);
        this.interfaceManager = new InterfaceMembershipManager(this.taskTimer, this.tunnelEndpoint);

        // Connect a channel membership state manager to the 
        // interface membership state manager
        this.channelManager = new ChannelMembershipManager(this.interfaceManager);

        this.tunnelEndpoint.setIncomingDataChannel(
                        new OutputChannelTransform<IPPacket, UdpDatagram>(
                                                                          this.channelManager.getDispatchChannel(),
                                                                          new MulticastDataTransform()));

        // Connect report channel of interface membership manager to tunnel endpoint
        this.interfaceManager.setOutgoingReportChannel(
                        new OutputChannelTransform<MembershipReport, IPPacket>(
                                                                               this.tunnelEndpoint.getOutgoingUpdateChannel(),
                                                                               reportTransform));

        // Connect query channel of tunnel endpoint to interface membership manager
        this.tunnelEndpoint.setIncomingQueryChannel(
                        new OutputChannelTransform<IPPacket, MembershipQuery>(
                                                                              this.interfaceManager.getIncomingQueryChannel(),
                                                                              queryTransform));

        this.tunnelEndpoint.start();
    }

    /**
     * Gets the Relay Discovery Address associated with this interface.
     * @return An InetAddress object containing the Relay Discovery Address
     */
    public final InetAddress getRelayDiscoveryAddress() {
        return this.relayDiscoveryAddress;
    }

    /**
     * @throws InterruptedException
     * @throws IOException
     */
    public final void close() throws InterruptedException, IOException {
        this.tunnelEndpoint.stop();
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param port
     * @throws IOException
     * @throws InterruptedException
     */
    public final void join(final OutputChannel<UdpDatagram> pushChannel,
                           final InetAddress groupAddress,
                           int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtIPInterface.join", pushChannel, Logging.address(groupAddress), port));
        }

        this.channelManager.join(pushChannel, groupAddress, port);
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @param port
     * @throws IOException
     * @throws InterruptedException
     */
    public final void join(final OutputChannel<UdpDatagram> pushChannel,
                           final InetAddress groupAddress,
                           final InetAddress sourceAddress,
                           final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "AmtIPInterface.join",
                                          pushChannel,
                                          Logging.address(groupAddress),
                                          Logging.address(sourceAddress),
                                          port));
        }

        this.channelManager.join(pushChannel, groupAddress, sourceAddress, port);
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel,
                            final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtIPInterface.leave", pushChannel, Logging.address(groupAddress)));
        }

        this.channelManager.leave(pushChannel, groupAddress);
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param port
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel,
                            final InetAddress groupAddress,
                            final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtIPInterface.leave", pushChannel, Logging.address(groupAddress), port));
        }

        this.channelManager.leave(pushChannel, groupAddress, port);
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel,
                            final InetAddress groupAddress,
                            final InetAddress sourceAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "AmtIPInterface.leave",
                                          pushChannel,
                                          Logging.address(groupAddress),
                                          Logging.address(sourceAddress)));
        }

        this.channelManager.leave(pushChannel, groupAddress, sourceAddress);
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @param port
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel,
                            final InetAddress groupAddress,
                            final InetAddress sourceAddress,
                            final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "AmtIPInterface.leave",
                                          pushChannel,
                                          Logging.address(groupAddress),
                                          Logging.address(sourceAddress),
                                          port));
        }

        this.channelManager.leave(pushChannel, groupAddress, sourceAddress, port);

    }

    /**
     * @param pushChannel
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtIPInterface.leave", pushChannel));
        }

        this.channelManager.leave(pushChannel);

    }

    /**
     * @throws InterruptedException
     */
    final void shutdown() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtIPInterface.shutdown"));
        }

        this.channelManager.shutdown();
    }
}
