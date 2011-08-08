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
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.channels.OutputChannelTransform;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.udp.UdpDatagram;
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
public abstract class AmtInterface {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtInterface.class.getName());


    /*-- Member Variables ---------------------------------------------------*/

    final String ObjectId = Logging.identify(this);
    
    final AmtGateway gateway;
    final InetAddress relayDiscoveryAddress;

    int referenceCount = 0;

    final AmtTunnelEndpoint tunnelEndpoint;
    final InterfaceMembershipManager interfaceManager;
    final ChannelMembershipManager channelManager;
    
    final Timer taskTimer;


    /*-- Member Functions ---------------------------------------------------*/
    
    /**
     * 
     * @param gateway
     * @param relayDiscoveryAddress
     * @throws IOException
     */
    protected AmtInterface(final AmtGateway gateway, final InetAddress relayDiscoveryAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtInterface.AmtInterface", gateway, Logging.address(relayDiscoveryAddress)));
        }

        this.gateway = gateway;
        this.relayDiscoveryAddress = relayDiscoveryAddress;

        this.taskTimer = new Timer("AMT interface");

        this.tunnelEndpoint = new AmtTunnelEndpoint(this, relayDiscoveryAddress, this.taskTimer);
        this.interfaceManager = new InterfaceMembershipManager(this.taskTimer, this.tunnelEndpoint);

        // Connect a channel membership state manager to the interface membership state manager
        this.channelManager = new ChannelMembershipManager(this.interfaceManager);

        this.tunnelEndpoint.setIncomingDataChannel(
                new OutputChannelTransform<IPPacket, UdpDatagram>(
                        this.channelManager.getDispatchChannel(),
                        new MulticastDataTransform()));

        this.tunnelEndpoint.start();
    }

    /**
     * 
     * @return
     */
    public final InetAddress getRelayDiscoveryAddress() {
        return this.relayDiscoveryAddress;
    }
    
    /**
     * 
     */
    public final synchronized void acquire() {
        this.referenceCount++;
    }

    /**
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    public final synchronized void release() throws InterruptedException, IOException {
        if (--this.referenceCount == 0) {
            this.taskTimer.cancel();
            this.gateway.closeIpv4Endpoint(this);
        }
    }

    /**
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    public final void close() throws InterruptedException, IOException {
        this.tunnelEndpoint.stop();
    }

    /**
     * 
     * @param pushChannel
     * @param groupAddress
     * @param port
     * @throws IOException
     * @throws InterruptedException 
     */
    public final void join(final OutputChannel<UdpDatagram> pushChannel,
                           final InetAddress groupAddress,
                           int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtInterface.join", pushChannel, Logging.address(groupAddress), port));
        }
        
        this.channelManager.join(pushChannel, groupAddress, port);
    }

    /**
     * 
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @param port
     * @throws IOException
     * @throws InterruptedException 
     */
    public final void join(final OutputChannel<UdpDatagram> pushChannel,
                           final InetAddress groupAddress,
                           final InetAddress sourceAddress,
                           final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "AmtInterface.join",
                                          pushChannel,
                                          Logging.address(groupAddress),
                                          Logging.address(sourceAddress),
                                          port));
        }

        this.channelManager.join(pushChannel, groupAddress, sourceAddress, port);
    }

    /**
     * 
     * @param pushChannel
     * @param groupAddress
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel,
                            final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtInterface.leave", pushChannel, Logging.address(groupAddress)));
        }

        this.channelManager.leave(pushChannel, groupAddress);
    }

    /**
     * 
     * @param pushChannel
     * @param groupAddress
     * @param port
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel,
                            final InetAddress groupAddress,
                            final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtInterface.leave", pushChannel, Logging.address(groupAddress), port));
        }

        this.channelManager.leave(pushChannel, groupAddress, port);
    }

    /**
     * 
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel,
                            final InetAddress groupAddress,
                            final InetAddress sourceAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, 
                                          "AmtInterface.leave",
                                          pushChannel,
                                          Logging.address(groupAddress),
                                          Logging.address(sourceAddress)));
        }

        this.channelManager.leave(pushChannel, groupAddress, sourceAddress);
    }

    /**
     * 
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @param port
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel,
                            final InetAddress groupAddress,
                            final InetAddress sourceAddress,
                            final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "AmtInterface.leave",
                                          pushChannel,
                                          Logging.address(groupAddress),
                                          Logging.address(sourceAddress),
                                          port));
        }

        this.channelManager.leave(pushChannel, groupAddress, sourceAddress, port);

    }

    /**
     * 
     * @param pushChannel
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtInterface.leave", pushChannel));
        }

        this.channelManager.leave(pushChannel);
        
    }
    
    /**
     * 
     * @throws InterruptedException
     */
    final void shutdown() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtInterface.shutdown"));
        }

        this.channelManager.shutdown();
    }
}
