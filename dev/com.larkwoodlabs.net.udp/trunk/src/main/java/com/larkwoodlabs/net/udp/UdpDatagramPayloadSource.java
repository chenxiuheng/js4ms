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

package com.larkwoodlabs.net.udp;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.larkwoodlabs.channels.ChannelPump;
import com.larkwoodlabs.channels.MessageSource;
import com.larkwoodlabs.channels.MessageTransform;
import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.channels.OutputChannelTransform;

/**
 * A {@link MessageSource} that receives UDP packets via a {@link UdpEndpoint} and 
 * sends the datagram payload to an {@link OutputChannel}.
 *
 * @author Gregory Bumgardner
 */
public class UdpDatagramPayloadSource extends MessageSource<ByteBuffer> {

    
    /*-- Inner Classes -------------------------------------------------------*/

    /**
     * Transform used to extract the datagram payload from a {@link UdpDatagram}.
     */
    final static class Transform implements MessageTransform<UdpDatagram, ByteBuffer> {

        public Transform() {
        }
        
        @Override
        public ByteBuffer transform(final UdpDatagram message) throws IOException {
            return message.getPayload();
        }
    }

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    private final ChannelPump<UdpDatagram> pump;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a packet source that will receive UDP datagrams from a UDP endpoint
     * and send them to an {@link OutputChannel}.
     */
    public UdpDatagramPayloadSource(final UdpEndpoint udpEndpoint,
                           final OutputChannel<ByteBuffer> outputChannel) throws IOException {
        super(outputChannel);

        this.pump = new ChannelPump<UdpDatagram>(new UdpInputChannel(udpEndpoint),
                                                 new OutputChannelTransform<UdpDatagram, ByteBuffer>(outputChannel,
                                                                                                     new Transform()));
    }

    @Override
    protected void doStart() throws IOException, InterruptedException {
        this.pump.start();
    }

    @Override
    protected void doStop() throws IOException, InterruptedException {
        this.pump.stop(Integer.MAX_VALUE);
    }

    @Override
    protected void doClose() throws IOException, InterruptedException {
        this.pump.close();
    }

}
