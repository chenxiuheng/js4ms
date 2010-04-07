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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.Transport;
import com.larkwoodlabs.net.udp.MulticastEndpoint;
import com.larkwoodlabs.util.logging.LoggableBase;
import com.larkwoodlabs.util.logging.Logging;


/**
 * @author Gregory Bumgardner
 *
 */
public final class AmtMulticastTransport
                   extends LoggableBase
                   implements Transport<Integer, MulticastEndpoint> {

    /*-- Static Variables ---------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtMulticastTransport.class.getName());

    /**
     * The singleton AmtMulticastTransport instance.
     */
    private static final AmtMulticastTransport instance = new AmtMulticastTransport();
    

    /*-- Static Functions ---------------------------------------------------*/

    public final static AmtMulticastTransport getInstance() {
        return AmtMulticastTransport.instance;
    }

    /*-- Member Functions ---------------------------------------------------*/
    
    /**
     * Private constructor used to construct singleton instance.
     */
    private AmtMulticastTransport() {
        
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public AmtMulticastEndpoint getEndpoint(Integer port, Object... options) throws IOException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMulticastTransport.getEndpoint", port, Logging.args(options)));
        }

        InetAddress relayDiscoveryAddress;
        
        if (options.length > 0 && (options[0] instanceof InetAddress)) {
            relayDiscoveryAddress = (InetAddress)options[0];
        }
        else {
            relayDiscoveryAddress = InetAddress.getByAddress(AmtGateway.DEFAULT_RELAY_DISCOVERY_ADDRESS);
        }

        return new AmtMulticastEndpoint(port, relayDiscoveryAddress);
        
    }


}
