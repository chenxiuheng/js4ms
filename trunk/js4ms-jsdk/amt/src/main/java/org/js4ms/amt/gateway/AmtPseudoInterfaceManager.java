package org.js4ms.amt.gateway;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * AmtPseudoInterfaceManager.java [org.js4ms.jsdk:amt]
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
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;
import org.js4ms.common.util.logging.Logging;


/**
 * Constructs and manages a collection of {@link AmtPseudoInterface} objects.
 * The interface manager creates a new interface for each unique relay discovery address.
 * Objects wishing to acquire an AMT interface must use the {@link #getInstance()} method
 * to first retrieve the singleton interface manager and then call
 * {@link #getInterface(InetAddress)} to obtain an interface.
 * The AmtPseudoInterfaceManager and AmtPseudoInterface classes are not normally accessed directly -
 * applications should use the {@link AmtMulticastEndpoint)} class when AMT functionality
 * is required.
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class AmtPseudoInterfaceManager {

    /*-- Static Variables ---------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(AmtPseudoInterfaceManager.class.getName());

    /**
     * The singleton AmtTunnelTransport instance.
     */
    private static final AmtPseudoInterfaceManager instance = new AmtPseudoInterfaceManager();

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
    public static AmtPseudoInterfaceManager getInstance() {
        return AmtPseudoInterfaceManager.instance;
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

    private final Log log = new Log(this);

    /**
     * Map containing AMT IPv4 and IPv6 endpoints referenced by the
     * relay discovery address used to construct each instance.
     */
    private HashMap<InetAddress, AmtPseudoInterface> interfaces;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Private constructor used to construct singleton instance.
     * Use {@link #getInstance()} to retrieve the singleton instance.
     */
    private AmtPseudoInterfaceManager() {
        this.interfaces = new HashMap<InetAddress, AmtPseudoInterface>();
    }

    /**
     * @param relayDiscoveryAddress
     * @return
     * @throws IOException
     * @throws InterruptedException 
     */
    public synchronized AmtPseudoInterface getInterface(final InetAddress relayDiscoveryAddress) throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("AmtPseudoInterfaceManager.getInterface", Logging.address(relayDiscoveryAddress)));
        }

        AmtPseudoInterface endpoint = this.interfaces.get(relayDiscoveryAddress);

        if (endpoint == null) {

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(log.msg("constructing new AmtPseudoInterface"));
            }

            endpoint = new AmtPseudoInterface(this, relayDiscoveryAddress);
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
    synchronized void closeInterface(final AmtPseudoInterface amtInterface) throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("AmtPseudoInterfaceManager.closeInterface", amtInterface));
        }

        amtInterface.close();

        // Remove the endpoint from the endpoints map
        this.interfaces.remove(amtInterface.getRelayDiscoveryAddress());
    }

}
