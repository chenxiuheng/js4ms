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
import java.nio.ByteBuffer;

import com.larkwoodlabs.channels.ChannelPump;
import com.larkwoodlabs.channels.MessageTransform;
import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.channels.OutputChannelTransform;
import com.larkwoodlabs.net.udp.UdpDatagram;
import com.larkwoodlabs.net.udp.UdpEndpoint;
import com.larkwoodlabs.net.udp.UdpInputChannel;

public class UdpPacketSource extends PacketSource {

    
    /*-- Inner Classes -------------------------------------------------------*/

    final static class Transform implements MessageTransform<UdpDatagram, ByteBuffer> {

        public Transform() {
        }
        
        @Override
        public ByteBuffer transform(final UdpDatagram message) throws IOException {
            return message.getPayload();
        }
    }

    /*-- Member Variables ----------------------------------------------------*/

    private final UdpEndpoint udpEndpoint;
    
    private final ChannelPump<UdpDatagram> pump;
    
    /*-- Member Functions ----------------------------------------------------*/

    protected UdpPacketSource(final UdpEndpoint udpEndpoint,
                              final OutputChannel<ByteBuffer> outputChannel) throws IOException {
        super();

        this.udpEndpoint = udpEndpoint;

        this.pump = new ChannelPump<UdpDatagram>(new UdpInputChannel(udpEndpoint),
                                                 new OutputChannelTransform<UdpDatagram, ByteBuffer>(outputChannel,
                                                                                                     new Transform()));
        
        this.pump.start();
    }

    @Override
    public void doStart() {
    }

    @Override
    public void doStop() {
    }

    @Override
    public void doClose() throws IOException {
        this.udpEndpoint.close(true);
    }

}
