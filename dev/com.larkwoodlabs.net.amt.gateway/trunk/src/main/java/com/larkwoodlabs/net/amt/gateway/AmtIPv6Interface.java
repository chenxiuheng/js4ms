/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: AmtIPv6Interface.java (com.larkwoodlabs.net.amt.gateway)
 * 
 * Copyright � 2009-2012 Cisco Systems, Inc.
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
import java.util.logging.Logger;

import com.larkwoodlabs.channels.OutputChannelTransform;
import com.larkwoodlabs.net.amt.IPv6MembershipReportTransform;
import com.larkwoodlabs.net.amt.IPv6MembershipQueryTransform;
import com.larkwoodlabs.net.amt.MembershipQuery;
import com.larkwoodlabs.net.amt.MembershipReport;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Manages an IPv6 AMT tunnel end-point.
 * The {@link AmtInterfaceManager} constructs a separate AmtIPv6Interface for each unique
 * AMT relay or AMT gateway peer acting as a remote IPv6 AMT tunnel end-point.
 * An AmtIPv6Interface provides functions for joining, leaving and receiving packets
 * for any number of IPv6 multicast groups. The AmtIPv6Interface tracks local group
 * membership state and handles the exchange of MLD messages used
 * to query or update that state.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class AmtIPv6Interface
                extends AmtInterface {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(AmtIPv6Interface.class.getName());

    /*-- Member Variables ---------------------------------------------------*/

    private final String ObjectId = Logging.identify(this);

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param manager
     * @param relayDiscoveryAddress
     * @throws IOException
     */
    public AmtIPv6Interface(final AmtInterfaceManager manager, final InetAddress relayDiscoveryAddress) throws IOException {
        super(manager, relayDiscoveryAddress);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtIPv6Interface.AmtIPv6Interface", manager,
                                          Logging.address(relayDiscoveryAddress)));
        }

        // Connect report channel of interface membership manager to tunnel endpoint
        this.interfaceManager.setOutgoingReportChannel(
                        new OutputChannelTransform<MembershipReport, IPPacket>(
                                                                               this.tunnelEndpoint.getOutgoingUpdateChannel(),
                                                                               new IPv6MembershipReportTransform()));

        // Connect query channel of tunnel endpoint to interface membership manager
        this.tunnelEndpoint.setIncomingQueryChannel(
                        new OutputChannelTransform<IPPacket, MembershipQuery>(
                                                                              this.interfaceManager.getIncomingQueryChannel(),
                                                                              new IPv6MembershipQueryTransform()));

        this.tunnelEndpoint.start();
    }

}
