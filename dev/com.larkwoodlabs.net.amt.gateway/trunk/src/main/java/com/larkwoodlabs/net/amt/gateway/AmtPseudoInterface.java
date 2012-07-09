/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: AmtPseudoInterface.java (com.larkwoodlabs.net.amt.gateway)
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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.channels.OutputChannelTee;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.ipv4.IPv4Packet;
import com.larkwoodlabs.util.logging.Log;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An AMT pseudo-interface.
 * The {@link AmtPseudoInterfaceManager} constructs a separate AMT interface for each unique
 * AMT relay acting as a remote AMT tunnel end-point.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class AmtPseudoInterface {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtPseudoInterface.class.getName());

    /*-- Member Variables ---------------------------------------------------*/

    protected final Log log = new Log(this);

    private final AmtPseudoInterfaceManager manager;

    private final InetAddress relayDiscoveryAddress;

    private int referenceCount = 0;

    private final OutputChannelTee<IPPacket> dispatchChannel;

    private AmtTunnelEndpoint ipv4Endpoint = null;

    private AmtTunnelEndpoint ipv6Endpoint = null;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param manager
     * @param relayDiscoveryAddress
     * @throws IOException
     */
    AmtPseudoInterface(final AmtPseudoInterfaceManager manager,
                       final InetAddress relayDiscoveryAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtPseudoInterface.AmtPseudoInterface", manager, Logging.address(relayDiscoveryAddress)));
        }

        this.manager = manager;
        this.relayDiscoveryAddress = relayDiscoveryAddress;
        this.dispatchChannel = new OutputChannelTee<IPPacket>();

    }

    /**
     * 
     * @param destinationChannel
     */
    public final void addOutputChannel(final OutputChannel<IPPacket> destinationChannel) {
        this.dispatchChannel.add(destinationChannel);
    }

    /**
     * 
     * @param destinationChannel
     */
    public final void removeOutputChannel(final OutputChannel<IPPacket> destinationChannel) {
        this.dispatchChannel.remove(destinationChannel);
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

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtPseudoInterface.acquire"));
        }

        this.referenceCount++;
    }

    /**
     * @throws InterruptedException
     * @throws IOException
     */
    final synchronized void release() throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtPseudoInterface.release"));
        }

        if (--this.referenceCount == 0) {
            this.manager.closeInterface(this);
        }
    }

    /**
     * @throws InterruptedException
     * @throws IOException
     */
    public final void close() throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtPseudoInterface.close"));
        }

        if (this.ipv4Endpoint != null) {
            this.ipv4Endpoint.close();
            this.ipv4Endpoint = null;
        }
        else if (this.ipv6Endpoint != null) {
            this.ipv6Endpoint.close();
            this.ipv6Endpoint = null;
        }
    }

    /**
     * @param packet
     * @throws IOException
     */
    public final void send(final IPPacket packet) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtPseudoInterface.send", packet));
        }

        if (packet.getVersion() == IPv4Packet.INTERNET_PROTOCOL_VERSION) {
            if (this.ipv4Endpoint == null) {
                this.ipv4Endpoint = new AmtTunnelEndpoint(this.relayDiscoveryAddress,
                                                          this.dispatchChannel,
                                                          AmtTunnelEndpoint.Protocol.IPv4);
            }
            this.ipv4Endpoint.send(packet);
        }
        else {
            if (this.ipv6Endpoint == null) {
                this.ipv6Endpoint = new AmtTunnelEndpoint(this.relayDiscoveryAddress,
                                                          this.dispatchChannel,
                                                          AmtTunnelEndpoint.Protocol.IPv6);
            }
            this.ipv6Endpoint.send(packet);
        }
    }

}
