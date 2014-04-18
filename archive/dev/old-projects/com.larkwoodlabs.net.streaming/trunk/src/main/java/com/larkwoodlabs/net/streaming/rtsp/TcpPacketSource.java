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

import java.nio.ByteBuffer;

import com.larkwoodlabs.channels.OutputChannel;

/**
 * A {@link PacketSource} that receives media packets from an RTSP participant
 * via the TCP {@link Connection} managed by a {@link ConnectionHandler} and delivers
 * those packets to an {@link OutputChannel}.
 * The packets are received over an RTSP control connection using the mechanism described
 * in [<a href="http://tools.ietf.org/html/rfc2326#page-40">RFC-2326, Section 10.12</a>].
 *
 * @author Gregory Bumgardner
 */
public final class TcpPacketSource extends PacketSource {

    private final int channelNumber;
    private final ConnectionHandler handler;
    
    /**
     * Constructs a packet source that will receive incoming interleaved packets sent to
     * with the specified channel number and send them to an {@link OutputChannel}.
     * @param channelNumber - The channel number associated with the incoming packet stream.
     * @param outputChannel - An {@link OutputChannel} to which the incoming packets will be sent.
     * @param handler - The connection handler that will receive and forward the interleaved packets.
     */
    public TcpPacketSource(final int channelNumber,
                           final OutputChannel<ByteBuffer> outputChannel,
                           final ConnectionHandler handler) {
        this.channelNumber = channelNumber;
        this.handler = handler;
        handler.addPacketChannel(channelNumber, outputChannel);
    }

    @Override
    public void doStart() {
    }

    @Override
    public void doStop() {
    }

    @Override
    public void doClose() {
        this.handler.removePacketChannel(this.channelNumber);
    }

}
