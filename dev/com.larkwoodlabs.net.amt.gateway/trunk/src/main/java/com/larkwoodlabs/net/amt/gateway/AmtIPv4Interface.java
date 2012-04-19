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

package com.larkwoodlabs.net.amt.gateway;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.OutputChannelTransform;
import com.larkwoodlabs.net.amt.IPv4MembershipQueryTransform;
import com.larkwoodlabs.net.amt.IPv4MembershipReportTransform;
import com.larkwoodlabs.net.amt.MembershipQuery;
import com.larkwoodlabs.net.amt.MembershipReport;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Manages an AMT tunnel end-point.
 * The {@link AmtGateway} constructs a separate AmtInterface for each unique 
 * AMT relay or AMT gateway peer acting as a remote AMT tunnel end-point.
 * An AmtInterface provides functions for joining, leaving and receiving packets
 * for any number of multicast groups. The AmtInterface tracks local group
 * membership state and handles the exchange of IGMP/MLD messages used
 * to query or update that state.
 *
 * @author gbumgard
 */
public final class AmtIPv4Interface extends AmtInterface {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtIPv4Interface.class.getName());


    /*-- Member Variables ---------------------------------------------------*/

    final String ObjectId = Logging.identify(this);


    /*-- Member Functions ---------------------------------------------------*/
    
    /**
     * 
     * @param gateway
     * @param relayDiscoveryAddress
     * @throws IOException
     */
    public AmtIPv4Interface(final AmtGateway gateway, final InetAddress relayDiscoveryAddress) throws IOException {
        super(gateway, relayDiscoveryAddress);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtIPv4Interface.AmtIPv4Interface", gateway, Logging.address(relayDiscoveryAddress)));
        }

        // Connect report channel of interface membership manager to tunnel endpoint
        this.interfaceManager.setOutgoingReportChannel(
                new OutputChannelTransform<MembershipReport,IPPacket>(
                        this.tunnelEndpoint.getOutgoingUpdateChannel(),
                        new IPv4MembershipReportTransform()));
        
        // Connect query channel of tunnel endpoint to interface membership manager
        this.tunnelEndpoint.setIncomingQueryChannel(
                new OutputChannelTransform<IPPacket, MembershipQuery>(
                        this.interfaceManager.getIncomingQueryChannel(),
                        new IPv4MembershipQueryTransform()));
        

        this.tunnelEndpoint.start();
    }

}
