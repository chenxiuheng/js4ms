package org.js4ms.rtsp.rtp;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * TcpPacketOutputChannel.java [org.js4ms.jsdk:rtsp]
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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.js4ms.io.channel.OutputChannel;



/**
 * An {@link OutputChannel} that can be used to send a byte array containing an
 * RTP/RTCP packet over a TCP connection using the framing method described in RFC-4571.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public class TcpPacketOutputChannel implements OutputChannel<ByteBuffer> {

    private final Socket socket;

    private final Object lock = new Object();

    /**
     * Constructs an output channel that can be used to send packets over the specified connection.
     * @param connection - The TCP connection over which the packets will be sent.
     * @throws IOException 
     */
    public TcpPacketOutputChannel(final InetSocketAddress remoteAddress) throws IOException {
        this.socket = new Socket();
        this.socket.connect(remoteAddress);
    }

    @Override
    public void send(final ByteBuffer packet, final int milliseconds) throws IOException, InterruptedIOException, InterruptedException {

        OutputStream outputStream = this.socket.getOutputStream();

        synchronized (this.lock) {
            int count = packet.limit();
            outputStream.write((byte)((count >> 8) & 0xFF));
            outputStream.write((byte)(count & 0xFF));
            outputStream.write(packet.array(), packet.arrayOffset(), count);
        }
    }

    @Override
    public void close() throws IOException, InterruptedException {
        this.socket.close();
    }
}
