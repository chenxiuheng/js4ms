/*
 * Copyright � 2009-2010 Larkwood Labs Software.
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

package com.larkwoodlabs.net.streaming.rtsp;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.MessageTransform;
import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.channels.OutputChannelTransform;
import com.larkwoodlabs.net.amt.AmtMulticastEndpoint;
import com.larkwoodlabs.net.amt.SourceFilter;
import com.larkwoodlabs.net.udp.UdpDatagram;
import com.larkwoodlabs.util.logging.Logging;

/**
 * A packet source for streams sent to a multicast address.
 * This class uses the Automatic Multicast Tunneling (AMT) protocol to join and
 * receive the multicast packet stream (via an {@link AmtMulticastEndpoint} object).
 *
 * @author Gregory Bumgardner
 */
public final class MulticastPacketSource extends PacketSource {

    /*-- Inner Classes -------------------------------------------------------*/

    /**
     * Simple transform that returns the payload of a {@link UdpDatagram} as a result.
     */
    final static class Transform implements MessageTransform<UdpDatagram, ByteBuffer> {

        public Transform() {
        }
        
        @Override
        public ByteBuffer transform(final UdpDatagram message) throws IOException {
            return message.getPayload();
        }
    }

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(MulticastPacketSource.class.getName());


    /*-- Member Variables ----------------------------------------------------*/

    final String ObjectId = Logging.identify(this);
    
    final SourceFilter filter;
    
    AmtMulticastEndpoint amtEndpoint;

    
    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a packet source for a stream sent to a multicast address.
     * @param sourcePort - The destination port of the packet stream.
     * @param filter - A source filter that identifies the any-source multicast (ASM) destination address 
     *                 or source-specific multicast (SSM) destination address and source host address(es)
     *                 of the packet stream.
     * @param outputChannel - The channel that will receive packets as they arrive.
     * @throws IOException - If an I/O error occurred while constructing the multicast endpoint.
     */
    protected MulticastPacketSource(final SourceFilter filter) throws IOException {
        super();
        
        this.filter = filter;
        
    }

    /**
     * Constructs a packet source for a stream sent to a multicast address.
     * @param sourcePort - The destination port of the packet stream.
     * @param filter - A source filter that identifies the any-source multicast (ASM) destination address 
     *                 or source-specific multicast (SSM) destination address and source host address(es)
     *                 of the packet stream.
     * @param outputChannel - The channel that will receive packets as they arrive.
     * @throws IOException - If an I/O error occurred while constructing the multicast endpoint.
     */
    public MulticastPacketSource(final int sourcePort,
                                 final SourceFilter filter,
                                 final OutputChannel<ByteBuffer> outputChannel) throws IOException {
        this(filter);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MulticastPacketSource.MulticastPacketSource", filter, sourcePort, outputChannel));
        }

        this.amtEndpoint = new AmtMulticastEndpoint(sourcePort,
                                                    new OutputChannelTransform<UdpDatagram, ByteBuffer>(outputChannel,
                                                                                                        new Transform()));
    }


    /**
     * Constructs a packet source for a stream sent to a multicast address.
     * @param sourcePort - The destination port of the packet stream.
     * @param relayDiscoveryAddress - The anycast or unicast address used to access an AMT relay.
     * @param filter - A source filter that identifies the any-source multicast (ASM) destination address 
     *                 or source-specific multicast (SSM) destination address and source host address(es)
     *                 of the packet stream.
     * @param outputChannel - The channel that will receive packets as they arrive.
     * @throws IOException - If an I/O error occurred while constructing the multicast endpoint.
     */
    public MulticastPacketSource(final int sourcePort,
                                 final InetAddress relayDiscoveryAddress,
                                 final SourceFilter filter,
                                 final OutputChannel<ByteBuffer> outputChannel) throws IOException {
        this(filter);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MulticastPacketSource.MulticastPacketSource", filter, sourcePort, outputChannel));
        }
        
        this.amtEndpoint = new AmtMulticastEndpoint(sourcePort,
                                                    relayDiscoveryAddress,
                                                    new OutputChannelTransform<UdpDatagram, ByteBuffer>(outputChannel,
                                                                                                        new Transform()));
    }

    @Override
    public void doStart() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MulticastPacketSource.doStart"));
        }

        InetAddress groupAddress = this.filter.getGroupAddress();
        HashSet<InetAddress> sourceAddresses = this.filter.getSourceSet();
        if (sourceAddresses.size() > 0) {
            for (InetAddress sourceAddress : sourceAddresses) {
                this.amtEndpoint.join(groupAddress, sourceAddress);
            }
        }
        else {
            this.amtEndpoint.join(groupAddress);
        }
    }


    @Override
    public void doStop() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MulticastPacketSource.doStop"));
        }
        
        this.amtEndpoint.leave();
    }

    @Override
    public void doClose() throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MulticastPacketSource.doClose"));
        }
        
        this.amtEndpoint.close();
    }

}