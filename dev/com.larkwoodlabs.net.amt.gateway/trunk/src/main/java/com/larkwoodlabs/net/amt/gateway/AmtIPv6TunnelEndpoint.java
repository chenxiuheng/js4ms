/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: AmtIPv6TunnelEndpoint.java (com.larkwoodlabs.net.amt.gateway)
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

import com.larkwoodlabs.net.amt.IPv6MembershipReportTransform;
import com.larkwoodlabs.net.amt.IPv6MembershipQueryTransform;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An {@link AmtTunnelEndpoint} that uses MLD to communicate IPv6 group membership
 * changes to an AMT relay.
 * An AmtIPv6TunnelEndpoint provides functions for joining, leaving and receiving packets
 * for any number of IPv6 multicast groups. The AmtIPv6TunnelEndpoint tracks local group
 * membership state and handles the exchange of MLD messages used
 * to query or update that state.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class AmtIPv6TunnelEndpoint
                extends AmtTunnelEndpoint {

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param manager
     * @param relayDiscoveryAddress
     * @throws IOException
     */
    public AmtIPv6TunnelEndpoint(final InetAddress relayDiscoveryAddress) throws IOException {
        super(relayDiscoveryAddress,
              new IPv6MembershipQueryTransform(),
              new IPv6MembershipReportTransform());

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtIPv6TunnelEndpoint.AmtIPv6Interface",
                                          Logging.address(relayDiscoveryAddress)));
        }
    }

}
