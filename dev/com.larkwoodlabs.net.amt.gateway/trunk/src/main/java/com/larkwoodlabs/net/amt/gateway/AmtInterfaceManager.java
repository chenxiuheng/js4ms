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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Logging;

/**
 * Constructs and manages a collection of {@link AmtInterface} objects.
 * The interface manager creates a new interface for each unique relay discovery address.
 * Objects wishing to acquire an AMT interface must use the {@link #getInstance()} method
 * to first retrieve the singleton interface manager and then call
 * {@link #getInterface(InetAddress)} to obtain an interface.
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
     * @return
     */
    public static AmtInterfaceManager getInstance() {
        return AmtInterfaceManager.instance;
    }

    /**
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
     * Map containing AMT IPv4 and IPv6 endpoints referenced by the
     * relay discovery address used to construct each instance.
     */
    private HashMap<InetAddress, AmtInterface> interfaces;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Private constructor used to construct singleton instance.
     * Use {@link #getInstance()} to retrieve the singleton instance.
     */
    private AmtInterfaceManager() {
        this.interfaces = new HashMap<InetAddress, AmtInterface>();
    }

    /**
     * @param relayDiscoveryAddress
     * @return
     * @throws IOException
     */
    public synchronized AmtInterface getInterface(final InetAddress relayDiscoveryAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtInterfaceManager.getInterface", Logging.address(relayDiscoveryAddress)));
        }

        AmtInterface endpoint = this.interfaces.get(relayDiscoveryAddress);

        if (endpoint == null) {

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " constructing new AmtInterface");
            }

            endpoint = new AmtInterface(this, relayDiscoveryAddress);
            this.interfaces.put(relayDiscoveryAddress, endpoint);
        }

        endpoint.acquire();
        return endpoint;
    }

    /**
     * @param amtInterface
     * @throws InterruptedException
     * @throws IOException
     */
    synchronized void closeInterface(final AmtInterface amtInterface) throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtInterfaceManager.closeInterface", amtInterface));
        }

        amtInterface.close();

        // Remove the endpoint from the endpoints map
        this.interfaces.remove(amtInterface.getRelayDiscoveryAddress());
    }

}
