/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: AmtGateway.java (com.larkwoodlabs.net.amt.gateway)
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
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Logging;

/**
 * This class plays the role of an AMT gateway. Looks up existing AMT pseudo interface
 * for use with a multicast endpoint.
 * 
 * @author Gregory Bumgardner
 */
public final class AmtGateway {

    /*-- Static Variables ---------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtGateway.class.getName());

    /**
     * The singleton AmtTunnelTransport instance.
     */
    private static final AmtGateway instance = new AmtGateway();
    
    /**
     * The any-cast IP address used to locate an AMT relay.
     */
    public static final byte[] DEFAULT_RELAY_DISCOVERY_ADDRESS = {(byte)154, (byte)17, (byte)0, (byte)1};


    /*-- Static Functions ---------------------------------------------------*/

    static AmtGateway getInstance() {
        return AmtGateway.instance;
    }
    
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
    private AmtGateway() {
        this.ipv4Interfaces = new HashMap<InetAddress, AmtInterface>();
        this.ipv6Interfaces = new HashMap<InetAddress, AmtInterface>();
    }

    /**
     * 
     * @param relayDiscoveryAddress
     * @return
     * @throws IOException
     */
    public synchronized AmtInterface getIPv4Interface(final InetAddress relayDiscoveryAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtGateway.getIpv4Interface", Logging.address(relayDiscoveryAddress)));
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
     * 
     * @param relayDiscoveryAddress
     * @return
     * @throws IOException
     */
    public synchronized AmtInterface getIPv6Interface(final InetAddress relayDiscoveryAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtGateway.getIpv6Interface", Logging.address(relayDiscoveryAddress)));
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
     * 
     * @param endpoint
     * @throws InterruptedException
     * @throws IOException
     */
    synchronized void closeIpv4Endpoint(final AmtInterface endpoint) throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtGateway.closeIpv4Endpoint", endpoint));
        }

        endpoint.close();

        // Remove the endpoint from the endpoints map
        this.ipv4Interfaces.remove(endpoint.getRelayDiscoveryAddress());
    }

    /**
     * 
     * @param endpoint
     * @throws InterruptedException
     * @throws IOException
     */
    synchronized void closeIpv6Endpoint(final AmtInterface endpoint) throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtGateway.closeIpv6Endpoint", endpoint));
        }

        endpoint.close();

        // Remove the endpoint from the endpoints map
        this.ipv6Interfaces.remove(endpoint.getRelayDiscoveryAddress());
    }
}
