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
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.MessageQueue;
import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.channels.OutputChannelPipe;
import com.larkwoodlabs.net.udp.MulticastEndpoint;
import com.larkwoodlabs.net.udp.UdpDatagram;
import com.larkwoodlabs.util.logging.Logging;

public final class AmtMulticastEndpoint implements MulticastEndpoint {


    /*-- Static Variables ---------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtMulticastEndpoint.class.getName());


    /*-- Static Functions ---------------------------------------------------*/

    public static InetAddress getWildcardAddress() {
        try {
            return InetAddress.getByAddress(new byte[4]);
        } catch (UnknownHostException e) {
            throw new Error(e);
        }
    }

    /*-- Member Variables ---------------------------------------------------*/

    private final String ObjectId = Logging.identify(this);
    
    private AmtInterface amtIpv4Interface = null;
    private AmtInterface amtIpv6Interface = null;

    private final int port;

    private final InetAddress relayDiscoveryAddress;
    
    /**
     * This is the channel that the AMT interface will push datagrams into.
     * This channel may be constructed externally or internally.
     * If the channel is constructed externally and supplied to a constructor,
     * then the {@link #receive(int)} method will throw an IOException since
     * datagrams are being delivered directly to the external channel.
     * If an external channel is not used, this class will construct an internal
     * buffer that can be read using the {@link #receive(int)} method.
     * The size of the internal buffer is set in the constructor.
     */
    private OutputChannel<UdpDatagram> pushChannel;
    
    private MessageQueue<UdpDatagram> datagramQueue = null;
    

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param port
     * @param bufferCapacity
     * @throws IOException
     */
    public AmtMulticastEndpoint(final int port,
                                final int bufferCapacity) throws IOException {
        this(port, AmtGateway.getDefaultRelayDiscoveryAddress(), bufferCapacity);
    }
    
    /**
     * 
     * @param port
     * @param pushChannel
     * @throws IOException
     */
    public AmtMulticastEndpoint(final int port,
                                final OutputChannel<UdpDatagram> pushChannel) throws IOException {
        this(port, AmtGateway.getDefaultRelayDiscoveryAddress(), pushChannel);
    }
    
    /**
     * 
     * @param port
     * @param relayDiscoveryAddress
     * @param bufferCapacity
     * @throws IOException
     */
    public AmtMulticastEndpoint(final int port,
                                final InetAddress relayDiscoveryAddress,
                                final int bufferCapacity) throws IOException {
        this(port, relayDiscoveryAddress);
        this.datagramQueue = new MessageQueue<UdpDatagram>(bufferCapacity);
        this.pushChannel = new OutputChannelPipe<UdpDatagram>(this.datagramQueue);
    }
    
    /**
     * 
     * @param port
     * @param relayDiscoveryAddress
     * @param pushChannel
     * @throws IOException
     */
    public AmtMulticastEndpoint(final int port,
                                final InetAddress relayDiscoveryAddress,
                                final OutputChannel<UdpDatagram> pushChannel) throws IOException {
        this(port, relayDiscoveryAddress);
        this.pushChannel = pushChannel; // TODO wrap channel to intercept exceptions so channel can leave AMT interface
    }

    /**
     * 
     * @param port
     * @param relayDiscoveryAddress
     * @throws IOException
     */
    protected AmtMulticastEndpoint(final int port,
                                   final InetAddress relayDiscoveryAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "AmtMulticastEndpoint.AmtMulticastEndpoint",
                                          port,
                                          Logging.address(relayDiscoveryAddress)));
        }

        this.port = port;
        this.relayDiscoveryAddress = relayDiscoveryAddress;
    }

    /**
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    public final void close() throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMulticastEndpoint.onClose"));
        }

        if (this.amtIpv4Interface != null) {
            this.amtIpv4Interface.leave(this.pushChannel);
            this.amtIpv4Interface.release();
        }

        if (this.amtIpv6Interface != null) {
            this.amtIpv6Interface.leave(this.pushChannel);
            this.amtIpv6Interface.release();
        }
    }

    /**
     * 
     * @return
     */
    public final int getPort() {
        return this.port;
    }

    /**
     * 
     * @return
     */
    public final InetAddress getRelayDiscoveryAddress() {
        return this.relayDiscoveryAddress;
    }

    @Override
    public final void join(final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMulticastEndpoint.join", Logging.address(groupAddress)));
        }

        join(groupAddress, this.port);

    }

    @Override
    public final void join(final InetAddress groupAddress, final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMulticastEndpoint.join", Logging.address(groupAddress), port));
        }

        if (groupAddress instanceof Inet4Address) {
            if (this.amtIpv4Interface == null) {
                this.amtIpv4Interface = AmtGateway.getInstance().getIPv4Interface(this.relayDiscoveryAddress);
            }
            this.amtIpv4Interface.join(this.pushChannel, groupAddress, port);
        }
        else {
            if (this.amtIpv6Interface == null) {
                this.amtIpv6Interface = AmtGateway.getInstance().getIPv6Interface(this.relayDiscoveryAddress);
            }
            this.amtIpv6Interface.join(this.pushChannel, groupAddress, port);
        }
    }

    @Override
    public final void join(final InetAddress groupAddress, final InetAddress sourceAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMulticastEndpoint.join", Logging.address(groupAddress), Logging.address(sourceAddress)));
        }

        join(groupAddress, sourceAddress, this.port);
    }


    @Override
    public final void join(final InetAddress groupAddress, final InetAddress sourceAddress, final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMulticastEndpoint.join", Logging.address(groupAddress), Logging.address(sourceAddress), port));
        }

        if (groupAddress instanceof Inet4Address) {
            if (this.amtIpv4Interface == null) {
                this.amtIpv4Interface = AmtGateway.getInstance().getIPv4Interface(this.relayDiscoveryAddress);
            }
            this.amtIpv4Interface.join(this.pushChannel, groupAddress, sourceAddress, port);
        }
        else {
            if (this.amtIpv6Interface == null) {
                this.amtIpv6Interface = AmtGateway.getInstance().getIPv6Interface(this.relayDiscoveryAddress);
            }
            this.amtIpv6Interface.join(this.pushChannel, groupAddress, sourceAddress, port);
        }
    }

    @Override
    public final void leave(final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMulticastEndpoint.leave", Logging.address(groupAddress)));
        }

        if (groupAddress instanceof Inet4Address) {
            if (this.amtIpv4Interface != null) {
                this.amtIpv4Interface.leave(this.pushChannel, groupAddress);
            }
        }
        else {
            if (this.amtIpv6Interface != null) {
                this.amtIpv6Interface.leave(this.pushChannel, groupAddress);
            }
        }
    }

    @Override
    public final void leave(final InetAddress groupAddress, final InetAddress sourceAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMulticastEndpoint.leave", Logging.address(groupAddress), Logging.address(sourceAddress)));
        }

        leave(groupAddress, sourceAddress, this.port);
    }

    @Override
    public final void leave(final InetAddress groupAddress, final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMulticastEndpoint.leave", Logging.address(groupAddress), port));
        }

        if (groupAddress instanceof Inet4Address) {
            if (this.amtIpv4Interface != null) {
                this.amtIpv4Interface.leave(this.pushChannel, groupAddress, port);
            }
        }
        else {
            if (this.amtIpv6Interface != null) {
                this.amtIpv6Interface.leave(this.pushChannel, groupAddress, port);
            }
        }
    }

    @Override
    public final void leave(final InetAddress groupAddress, final InetAddress sourceAddress, final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMulticastEndpoint.join", Logging.address(groupAddress), Logging.address(sourceAddress), port));
        }

        if (groupAddress instanceof Inet4Address) {
            if (this.amtIpv4Interface != null) {
                this.amtIpv4Interface.leave(this.pushChannel, groupAddress, sourceAddress, port);
            }
        }
        else {
            if (this.amtIpv6Interface != null) {
                this.amtIpv6Interface.leave(this.pushChannel, groupAddress, sourceAddress, port);
            }
        }
    }

    @Override
    public final void leave() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMulticastEndpoint.leave"));
        }

        if (this.amtIpv4Interface != null) {
            this.amtIpv4Interface.leave(this.pushChannel);
        }
        if (this.amtIpv6Interface != null) {
            this.amtIpv6Interface.leave(this.pushChannel);
        }
    }

    @Override
    public final UdpDatagram receive(final int milliseconds) throws IOException, InterruptedIOException, InterruptedException {
        return this.datagramQueue.receive(milliseconds);
    }

}
