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

package com.larkwoodlabs.net.udp;

import java.io.IOException;

import com.larkwoodlabs.channels.ChannelPump;
import com.larkwoodlabs.channels.MessageSource;
import com.larkwoodlabs.channels.OutputChannel;

/**
 * A {@link MessageSource} that receives UDP datagrams via a {@link UdpEndpoint} and 
 * sends the datagram to an {@link OutputChannel}.
 *
 * @author Gregory Bumgardner
 */
public class UdpDatagramSource extends MessageSource<UdpDatagram> {

    
    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    private final ChannelPump<UdpDatagram> pump;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a UDP datagram source that will receive datagrams from a UDP endpoint
     * and send them to an {@link OutputChannel}.
     */
    public UdpDatagramSource(final UdpEndpoint udpEndpoint,
                             final OutputChannel<UdpDatagram> outputChannel) throws IOException {
        super(outputChannel);

        this.pump = new ChannelPump<UdpDatagram>(new UdpInputChannel(udpEndpoint), outputChannel);
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
