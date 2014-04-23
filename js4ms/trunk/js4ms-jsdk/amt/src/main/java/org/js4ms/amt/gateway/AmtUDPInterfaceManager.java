package org.js4ms.amt.gateway;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * AmtUDPInterfaceManager.java [org.js4ms.jsdk:amt]
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



public class AmtUDPInterfaceManager {

    /*-- Static Variables ---------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(AmtUDPInterfaceManager.class.getName());

    /**
     * The singleton AmtTunnelTransport instance.
     */
    private static final AmtUDPInterfaceManager instance = new AmtUDPInterfaceManager();

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return
     */
    public static AmtUDPInterfaceManager getInstance() {
        return AmtUDPInterfaceManager.instance;
    }

    /**
     * @return
     */
    public static InetAddress getDefaultRelayDiscoveryAddress() {
        return AmtIPInterfaceManager.getDefaultRelayDiscoveryAddress();
    }

    /*-- Member Variables ---------------------------------------------------*/

    private final Log log = new Log(this);

    /**
     * Map containing AMT UDP interfaces mapped to relay discovery addresses.
     */
    private HashMap<InetAddress, AmtUDPInterface> interfaces;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Private constructor used to construct singleton instance.
     * Use {@link #getInstance()} to retrieve the singleton instance.
     */
    private AmtUDPInterfaceManager() {
        this.interfaces = new HashMap<InetAddress, AmtUDPInterface>();
    }

    /**
     * @param relayDiscoveryAddress
     * @return
     * @throws IOException
     * @throws InterruptedException 
     */
    public synchronized AmtUDPInterface getInterface(final InetAddress relayDiscoveryAddress) throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterfaceManager.getInterface", Logging.address(relayDiscoveryAddress)));
        }

        AmtUDPInterface udpInterface = this.interfaces.get(relayDiscoveryAddress);

        if (udpInterface == null) {

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(this.log.msg("constructing new AmtUDPInterface"));
            }

            udpInterface = new AmtUDPInterface(this, relayDiscoveryAddress);
            this.interfaces.put(relayDiscoveryAddress, udpInterface);
        }

        udpInterface.acquire();

        return udpInterface;
    }

    /**
     * @param udpInterface
     * @throws InterruptedException
     * @throws IOException
     */
    synchronized void closeInterface(final AmtUDPInterface udpInterface) throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterfaceManager.closeInterface", udpInterface));
        }

        udpInterface.close();

        // Remove the endpoint from the endpoints map
        this.interfaces.remove(udpInterface.getRelayDiscoveryAddress());
    }


}
