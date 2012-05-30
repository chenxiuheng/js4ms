/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: AmtIPv4TunnelEndpoint.java (com.larkwoodlabs.net.amt.gateway)
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
import java.util.logging.Level;

import com.larkwoodlabs.net.amt.IPv4MembershipQueryTransform;
import com.larkwoodlabs.net.amt.IPv4MembershipReportTransform;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An {@link AmtTunnelEndpoint} that uses IGMP to communicate IPv4 group membership
 * changes to an AMT relay.
 * An AmtIPv4TunnelEndpoint provides functions for joining, leaving and receiving packets
 * for any number of IPv4 multicast groups. The AmtIPv4TunnelEndpoint tracks local group
 * membership state and handles the exchange of IGMP messages used
 * to query or update that state.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class AmtIPv4TunnelEndpoint
                extends AmtTunnelEndpoint {

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param manager
     * @param relayDiscoveryAddress
     * @throws IOException
     */
    public AmtIPv4TunnelEndpoint(final InetAddress relayDiscoveryAddress) throws IOException {
        super(relayDiscoveryAddress,
              new IPv4MembershipQueryTransform(),
              new IPv4MembershipReportTransform());

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtIPv4TunnelEndpoint.AmtIPv4Interface",
                                          Logging.address(relayDiscoveryAddress)));
        }

    }

}
