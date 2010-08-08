/*
 * Copyright © 2009-2010 Larkwood Labs Software.
 *
 * Licensed under the Larkwood Labs Software Source Code License, Version 1.0.
 * You may not use this file except in compliance with this License.
 *
 * You may view the Source Code License at
 * http://www.larkwoodlabs.com/source-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the license.
 */

package com.larkwoodlabs.net.amt;

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
     * Map containing AMT tunnel endpoints referenced by the local host
     * and relay discovery address used to construct each instance.
     */
    private HashMap<InetAddress, AmtInterface> interfaces;
    
 
    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Private constructor used to construct singleton instance.
     * Use {@link #getInstance()} to retrieve the singleton instance.
     */
    private AmtGateway() {
        this.interfaces = new HashMap<InetAddress, AmtInterface>();
    }

    /**
     * 
     * @param relayDiscoveryAddress
     * @return
     * @throws IOException
     */
    public synchronized AmtInterface getInterface(final InetAddress relayDiscoveryAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtGateway.getEndpoint", Logging.address(relayDiscoveryAddress)));
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
     * 
     * @param endpoint
     * @throws InterruptedException
     * @throws IOException
     */
    synchronized void closeEndpoint(final AmtInterface endpoint) throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtGateway.closeEndpoint", endpoint));
        }

        endpoint.close();

        // Remove the endpoint from the endpoints map
        this.interfaces.remove(endpoint.getRelayDiscoveryAddress());
    }

}
