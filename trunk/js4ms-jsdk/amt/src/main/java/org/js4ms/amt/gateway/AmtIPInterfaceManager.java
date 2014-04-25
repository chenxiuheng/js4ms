package org.js4ms.amt.gateway;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * AmtIPInterfaceManager.java [org.js4ms.jsdk:amt]
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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;
import org.js4ms.common.util.logging.Logging;



public class AmtIPInterfaceManager {

    /*-- Static Variables ---------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(AmtIPInterfaceManager.class.getName());

    /**
     * The singleton AmtTunnelTransport instance.
     */
    private static final AmtIPInterfaceManager instance = new AmtIPInterfaceManager();

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return
     */
    public static AmtIPInterfaceManager getInstance() {
        return AmtIPInterfaceManager.instance;
    }

    /**
     * @return
     */
    public static InetAddress getDefaultRelayDiscoveryAddress() {
        return AmtPseudoInterfaceManager.getDefaultRelayDiscoveryAddress();
    }

    /*-- Member Variables ---------------------------------------------------*/

    private final Log log = new Log(this);

    /**
     * Map containing AMT IP interfaces mapped to relay discovery addresses.
     */
    private HashMap<InetAddress, AmtIPInterface> interfaces;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Private constructor used to construct singleton instance.
     * Use {@link #getInstance()} to retrieve the singleton instance.
     */
    private AmtIPInterfaceManager() {
        this.interfaces = new HashMap<InetAddress, AmtIPInterface>();
    }

    /**
     * @param relayDiscoveryAddress
     * @return
     * @throws IOException
     * @throws InterruptedException 
     */
    public synchronized AmtIPInterface getInterface(final InetAddress relayDiscoveryAddress) throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterfaceManager.getInterface", Logging.address(relayDiscoveryAddress)));
        }

        AmtIPInterface ipInterface = this.interfaces.get(relayDiscoveryAddress);

        if (ipInterface == null) {

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(this.log.msg("constructing new AmtIPInterface"));
            }

            ipInterface = new AmtIPInterface(this, relayDiscoveryAddress);
            this.interfaces.put(relayDiscoveryAddress, ipInterface);
        }

        ipInterface.acquire();

        return ipInterface;
    }

    /**
     * @param amtInterface
     * @throws InterruptedException
     * @throws IOException
     */
    synchronized void closeInterface(final AmtIPInterface ipInterface) throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtIPInterfaceManager.closeInterface", ipInterface));
        }

        ipInterface.close();

        // Remove the endpoint from the endpoints map
        this.interfaces.remove(ipInterface.getRelayDiscoveryAddress());
    }

}
