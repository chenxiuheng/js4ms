/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: AmtInterfaceManager.java (com.larkwoodlabs.net.amt.gateway)
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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Logging;

/**
 * Constructs and manages {@link AmtInterface} objects.
 * The interface manager creates a new interface for each unique relay discovery address.
 * Objects wishing to acquire an AMT interface must use the {@link #getInstance()} method
 * to first retrieve the singleton interface manager and then call
 * {@link #getInterface(InetAddress)} to obtain an interface.
 * The interface manager maintains separate collections for IPv4 and IPv6 interfaces.
 * The AmtInterfaceManager and AmtInterface classes are not normally accessed directly -
 * applications should use the {@link AmtMulticastEndpoint)} class when AMT functionality
 * is required.
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class AmtInterfaceManager {

    /*-- Static Variables ---------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(AmtInterfaceManager.class.getName());

    /**
     * The singleton AmtTunnelTransport instance.
     */
    private static final AmtInterfaceManager instance = new AmtInterfaceManager();

    /**
     * The any-cast IP address used to locate an AMT relay.
     */
    public static final byte[] DEFAULT_RELAY_DISCOVERY_ADDRESS = {
                    (byte) 154, (byte) 17, (byte) 0, (byte) 1
    };

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * 
     * @return
     */
    public static AmtInterfaceManager getInstance() {
        return AmtInterfaceManager.instance;
    }

    /**
     * 
     * @return
     */
    public static InetAddress getDefaultRelayDiscoveryAddress() {
        try {
            return InetAddress.getByAddress(DEFAULT_RELAY_DISCOVERY_ADDRESS);
        }
        catch (UnknownHostException e) {
            throw new Error(e);
        }
    }

    /*-- Member Variables ---------------------------------------------------*/

    private final String ObjectId = Logging.identify(this);

    /**
     * Map containing IPv4 AMT tunnel endpoints referenced by the local host
     * and relay discovery address used to construct each instance.
     */
    private HashMap<InetAddress, AmtInterface> ipv4Interfaces;

    /**
     * Map containing IPv6 AMT tunnel endpoints referenced by the local host
     * and relay discovery address used to construct each instance.
     */
    private HashMap<InetAddress, AmtInterface> ipv6Interfaces;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Private constructor used to construct singleton instance.
     * Use {@link #getInstance()} to retrieve the singleton instance.
     */
    private AmtInterfaceManager() {
        this.ipv4Interfaces = new HashMap<InetAddress, AmtInterface>();
        this.ipv6Interfaces = new HashMap<InetAddress, AmtInterface>();
    }

    /**
     * 
     * @param relayDiscoveryAddress
     * @return
     * @throws IOException
     */
    public AmtInterface getInterface(final InetAddress relayDiscoveryAddress) throws IOException {
        return relayDiscoveryAddress instanceof Inet4Address
                        ? getIPv4Interface(relayDiscoveryAddress)
                        : getIPv6Interface(relayDiscoveryAddress);

    }

    /**
     * @param relayDiscoveryAddress
     * @return
     * @throws IOException
     */
    public synchronized AmtInterface getIPv4Interface(final InetAddress relayDiscoveryAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtInterfaceManager.getIpv4Interface", Logging.address(relayDiscoveryAddress)));
        }

        AmtInterface endpoint = this.ipv4Interfaces.get(relayDiscoveryAddress);

        if (endpoint == null) {

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " constructing new IPv4 AmtInterface");
            }

            endpoint = new AmtIPv4Interface(this, relayDiscoveryAddress);
            this.ipv4Interfaces.put(relayDiscoveryAddress, endpoint);
        }

        endpoint.acquire();
        return endpoint;
    }

    /**
     * @param relayDiscoveryAddress
     * @return
     * @throws IOException
     */
    public synchronized AmtInterface getIPv6Interface(final InetAddress relayDiscoveryAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtInterfaceManager.getIpv6Interface", Logging.address(relayDiscoveryAddress)));
        }

        AmtInterface endpoint = this.ipv6Interfaces.get(relayDiscoveryAddress);

        if (endpoint == null) {

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " constructing new IPv6 AmtInterface");
            }

            endpoint = new AmtIPv6Interface(this, relayDiscoveryAddress);
            this.ipv6Interfaces.put(relayDiscoveryAddress, endpoint);
        }

        endpoint.acquire();
        return endpoint;
    }

    /**
     * @param endpoint
     * @throws InterruptedException
     * @throws IOException
     */
    synchronized void closeIpv4Interface(final AmtInterface amtInterface) throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtInterfaceManager.closeIpv4Interface", amtInterface));
        }

        amtInterface.close();

        // Remove the endpoint from the endpoints map
        this.ipv4Interfaces.remove(amtInterface.getRelayDiscoveryAddress());
    }

    /**
     * @param endpoint
     * @throws InterruptedException
     * @throws IOException
     */
    synchronized void closeIpv6Interface(final AmtInterface amtInterface) throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtInterfaceManager.closeIpv6Interface", amtInterface));
        }

        amtInterface.close();

        // Remove the endpoint from the endpoints map
        this.ipv6Interfaces.remove(amtInterface.getRelayDiscoveryAddress());
    }
}
