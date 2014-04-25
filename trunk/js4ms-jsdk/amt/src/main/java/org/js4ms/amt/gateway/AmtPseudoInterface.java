package org.js4ms.amt.gateway;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * AmtPseudoInterface.java [org.js4ms.jsdk:amt]
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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.io.channel.OutputChannel;
import org.js4ms.io.channel.OutputChannelTee;
import org.js4ms.ip.IPPacket;
import org.js4ms.ip.ipv4.IPv4Packet;



/**
 * An AMT pseudo-interface.
 * The {@link AmtPseudoInterfaceManager} constructs a separate AMT interface for each
 * unique
 * AMT relay acting as a remote AMT tunnel end-point.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class AmtPseudoInterface {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtPseudoInterface.class.getName());

    /*-- Member Variables ---------------------------------------------------*/

    protected final Log log = new Log(this);

    private AmtPseudoInterfaceManager manager = null;

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
     * @throws InterruptedException 
     */
    AmtPseudoInterface(final AmtPseudoInterfaceManager manager,
                       final InetAddress relayDiscoveryAddress) throws IOException, InterruptedException {
        this(relayDiscoveryAddress);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtPseudoInterface.AmtPseudoInterface", manager, Logging.address(relayDiscoveryAddress)));
        }

        // Reverse extra acquire() call made in public constructor
        this.release();
        
        this.manager = manager;

    }

    /**
     * Constructs an AMT pseudo interface.
     * The release method MUST be called if the AmtPseudoInterfaceManager was not used to
     * construct this object.
     * 
     * @param manager
     * @param relayDiscoveryAddress
     * @throws IOException
     */
    public AmtPseudoInterface(final InetAddress relayDiscoveryAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtPseudoInterface.AmtPseudoInterface", manager, Logging.address(relayDiscoveryAddress)));
        }

        this.acquire();

        this.relayDiscoveryAddress = relayDiscoveryAddress;

        this.dispatchChannel = new OutputChannelTee<IPPacket>();
    }

    /**
     * @param destinationChannel
     */
    public void addOutputChannel(final OutputChannel<IPPacket> destinationChannel) {
        this.dispatchChannel.add(destinationChannel);
    }

    /**
     * @param destinationChannel
     */
    public void removeOutputChannel(final OutputChannel<IPPacket> destinationChannel) {
        this.dispatchChannel.remove(destinationChannel);
    }

    /**
     * Gets the Relay Discovery Address associated with this interface.
     * 
     * @return An InetAddress object containing the Relay Discovery Address
     */
    public InetAddress getRelayDiscoveryAddress() {
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
    public synchronized void release() throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtPseudoInterface.release"));
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
    public void send(final IPPacket packet) throws IOException {

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
