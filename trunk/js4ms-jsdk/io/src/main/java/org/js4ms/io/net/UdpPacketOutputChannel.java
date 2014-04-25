package org.js4ms.io.net;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * UdpPacketOutputChannel.java [org.js4ms.jsdk:io]
 * %%
 * Copyright (C) 2009 - 2014 Cisco Systems, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;
import org.js4ms.io.channel.OutputChannel;



/**
 * An {@link OutputChannel} that can be used to send a byte array containing an
 * RTP/RTCP packet to a specific address and port via UDP.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public class UdpPacketOutputChannel
                implements OutputChannel<ByteBuffer> {

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(UdpPacketOutputChannel.class.getName());

    private final Log log = new Log(this);

    private final DatagramSocket socket;

    /**
     * Constructs an output channel that can be used to send packets via UDP
     * to the specified destination address and port.
     * 
     * @param socket
     *            - The datagram socket to use when sending packets.
     * @throws IOException
     *             If an I/O occurs during construction of the underlying DatagramSocket.
     */
    public UdpPacketOutputChannel(final DatagramSocket socket) {
        this.socket = socket;
    }

    /**
     * Constructs an output channel that can be used to send packets via UDP
     * to the specified destination address and port.
     * 
     * @param remoteAddress
     *            - The IP address and port of the receiving host.
     * @throws IOException
     *             If an I/O occurs during construction of the underlying DatagramSocket.
     */
    public UdpPacketOutputChannel(InetSocketAddress remoteAddress) throws IOException {
        this.socket = new DatagramSocket(0);
        this.socket.connect(remoteAddress);
    }

    @Override
    public void send(final ByteBuffer packet, final int milliseconds) throws IOException, InterruptedIOException, InterruptedException {

        try {
            this.socket.send(new DatagramPacket(packet.array(),
                                                packet.arrayOffset(),
                                                packet.limit()));
        }
        catch (IOException e) {
            logger.finer(log.msg("attempt to send packet failed with exception: " + e.getClass().getSimpleName() + ": "
                                 + e.getMessage()));
            throw e;
        }
    }

    @Override
    public void close() throws IOException, InterruptedException {
        this.socket.close();
    }

}
