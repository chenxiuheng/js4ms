/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: AmtInterface.java (com.larkwoodlabs.net.amt.gateway)
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
import java.net.Inet4Address;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.net.Precondition;
import com.larkwoodlabs.net.udp.UdpDatagram;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An AMT pseudo-interface.
 * The {@link AmtInterfaceManager} constructs a separate AMT interface for each unique
 * AMT relay acting as a remote AMT tunnel end-point.
 * An AmtInterface provides functions for registering {@link OutputChannel} objects
 * to receive specific SSM or ASM traffic.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class AmtInterface {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtInterface.class.getName());

    /*-- Member Variables ---------------------------------------------------*/

    private final String ObjectId = Logging.identify(this);

    private final AmtInterfaceManager manager;

    private final InetAddress relayDiscoveryAddress;

    private int referenceCount = 0;

    private AmtIPv4TunnelEndpoint ipv4Interface = null;

    private AmtIPv6TunnelEndpoint ipv6Interface = null;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param manager
     * @param relayDiscoveryAddress
     * @throws IOException
     */
    AmtInterface(final AmtInterfaceManager manager, final InetAddress relayDiscoveryAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtInterface.AmtInterface", manager, Logging.address(relayDiscoveryAddress)));
        }

        this.manager = manager;
        this.relayDiscoveryAddress = relayDiscoveryAddress;

        if (relayDiscoveryAddress instanceof Inet4Address) {
            this.ipv4Interface = new AmtIPv4TunnelEndpoint(this.relayDiscoveryAddress);
        }
        else {
            this.ipv6Interface = new AmtIPv6TunnelEndpoint(this.relayDiscoveryAddress);
        }

    }

    /**
     * Gets the Relay Discovery Address associated with this interface.
     * 
     * @return An InetAddress object containing the Relay Discovery Address
     */
    public final InetAddress getRelayDiscoveryAddress() {
        return this.relayDiscoveryAddress;
    }

    /**
     * 
     */
    final synchronized void acquire() {
        this.referenceCount++;
    }

    /**
     * @throws InterruptedException
     * @throws IOException
     */
    final synchronized void release() throws InterruptedException, IOException {
        if (--this.referenceCount == 0) {
            this.manager.closeInterface(this);
        }
    }

    /**
     * @throws InterruptedException
     * @throws IOException
     */
    public final void close() throws InterruptedException, IOException {
        if (this.ipv4Interface != null) {
            this.ipv4Interface.close();
            this.ipv4Interface = null;
        }
        else if (this.ipv6Interface != null) {
            this.ipv6Interface.close();
            this.ipv6Interface = null;
        }
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
            logger.finer(Logging.entering(ObjectId, "AmtInterface.join", pushChannel, Logging.address(groupAddress), port));
        }

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4Interface == null) {
                this.ipv4Interface = new AmtIPv4TunnelEndpoint(this.relayDiscoveryAddress);
            }
            this.ipv4Interface.join(pushChannel, groupAddress, port);
        }
        else {
            if (this.ipv6Interface == null) {
                this.ipv6Interface = new AmtIPv6TunnelEndpoint(this.relayDiscoveryAddress);
            }
            this.ipv6Interface.join(pushChannel, groupAddress, port);
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
    public final void join(final OutputChannel<UdpDatagram> pushChannel,
                           final InetAddress groupAddress,
                           final InetAddress sourceAddress,
                           final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "AmtInterface.join",
                                          pushChannel,
                                          Logging.address(groupAddress),
                                          Logging.address(sourceAddress),
                                          port));
        }

        Precondition.checkAddresses(groupAddress, sourceAddress);

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4Interface == null) {
                this.ipv4Interface = new AmtIPv4TunnelEndpoint(this.relayDiscoveryAddress);
            }
            this.ipv4Interface.join(pushChannel, groupAddress, sourceAddress, port);
        }
        else {
            if (this.ipv6Interface == null) {
                this.ipv6Interface = new AmtIPv6TunnelEndpoint(this.relayDiscoveryAddress);
            }
            this.ipv6Interface.join(pushChannel, groupAddress, sourceAddress, port);
        }
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel,
                            final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtInterface.leave", pushChannel, Logging.address(groupAddress)));
        }

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4Interface == null) {
                this.ipv4Interface = new AmtIPv4TunnelEndpoint(this.relayDiscoveryAddress);
            }
            this.ipv4Interface.leave(pushChannel, groupAddress);
        }
        else {
            if (this.ipv6Interface == null) {
                this.ipv6Interface = new AmtIPv6TunnelEndpoint(this.relayDiscoveryAddress);
            }
            this.ipv6Interface.leave(pushChannel, groupAddress);
        }
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
            logger.finer(Logging.entering(ObjectId, "AmtInterface.leave", pushChannel, Logging.address(groupAddress), port));
        }

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4Interface == null) {
                this.ipv4Interface = new AmtIPv4TunnelEndpoint(this.relayDiscoveryAddress);
            }
            this.ipv4Interface.leave(pushChannel, groupAddress, port);
        }
        else {
            if (this.ipv6Interface == null) {
                this.ipv6Interface = new AmtIPv6TunnelEndpoint(this.relayDiscoveryAddress);
            }
            this.ipv6Interface.leave(pushChannel, groupAddress, port);
        }
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
                                          "AmtInterface.leave",
                                          pushChannel,
                                          Logging.address(groupAddress),
                                          Logging.address(sourceAddress)));
        }

        Precondition.checkAddresses(groupAddress, sourceAddress);

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4Interface == null) {
                this.ipv4Interface = new AmtIPv4TunnelEndpoint(this.relayDiscoveryAddress);
            }
            this.ipv4Interface.leave(pushChannel, groupAddress, sourceAddress);
        }
        else {
            if (this.ipv6Interface == null) {
                this.ipv6Interface = new AmtIPv6TunnelEndpoint(this.relayDiscoveryAddress);
            }
            this.ipv6Interface.leave(pushChannel, groupAddress, sourceAddress);
        }
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
                                          "AmtInterface.leave",
                                          pushChannel,
                                          Logging.address(groupAddress),
                                          Logging.address(sourceAddress),
                                          port));
        }

        Precondition.checkAddresses(groupAddress, sourceAddress);

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4Interface == null) {
                this.ipv4Interface = new AmtIPv4TunnelEndpoint(this.relayDiscoveryAddress);
            }
            this.ipv4Interface.leave(pushChannel, groupAddress, sourceAddress, port);
        }
        else {
            if (this.ipv6Interface == null) {
                this.ipv6Interface = new AmtIPv6TunnelEndpoint(this.relayDiscoveryAddress);
            }
            this.ipv6Interface.leave(pushChannel, groupAddress, sourceAddress, port);
        }

    }

    /**
     * @param pushChannel
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtInterface.leave", pushChannel));
        }

        if (this.ipv4Interface != null) {
            this.ipv4Interface.leave(pushChannel);
        }
        else if (this.ipv6Interface != null) {
            this.ipv6Interface.leave(pushChannel);
        }

    }

}
