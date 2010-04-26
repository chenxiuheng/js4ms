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
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;


/**
 * A {@link PacketSink} used to send incoming media packets to an RTSP participant
 * using a UDP DatagramSocket.
 *
 * @author Gregory Bumgardner
 */
public final class UdpPacketSink extends PacketSink {

    private final DatagramSocket socket;
    
    /**
     * Constructs a packet sink that can be used to send media packets via UDP
     * to the specified destination address and port.
     * @param remoteAddress - The IP address and port of the receiving host.
     * @throws IOException If an I/O occurs during construction of the underlying DatagramSocket.
     */
    public UdpPacketSink(InetSocketAddress remoteAddress) throws IOException {
        this.socket = new DatagramSocket(0);
        this.socket.connect(remoteAddress);
    }

    @Override
    public void send(ByteBuffer packet, int milliseconds) throws IOException, InterruptedIOException, InterruptedException {
        DatagramPacket datagram = new DatagramPacket(packet.array(), packet.arrayOffset(), packet.limit());
        this.socket.send(datagram);
    }

    @Override
    public void close(boolean isCloseAll) throws IOException, InterruptedException {
        this.socket.close();
    }
    
}
