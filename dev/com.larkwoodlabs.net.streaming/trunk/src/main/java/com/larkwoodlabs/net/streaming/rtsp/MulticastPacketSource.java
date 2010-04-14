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
 * An packet source that uses an {@link AmtMulticastEndpoint} to join and
 * receive multicast packet streams.
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
    
    final AmtMulticastEndpoint amtEndpoint;

    
    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     * @param outputChannel
     * @throws IOException 
     */
    public MulticastPacketSource(final int sourcePort,
                                 final SourceFilter filter,
                                 final OutputChannel<ByteBuffer> outputChannel) throws IOException {
        super();
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MulticastPacketSource.MulticastPacketSource", filter, sourcePort, outputChannel));
        }

        this.filter = filter;
        
        this.amtEndpoint = new AmtMulticastEndpoint(sourcePort,
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
