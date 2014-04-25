package org.js4ms.rtsp.rtp;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * InterleavedPacketOutputChannel.java [org.js4ms.jsdk:rtsp]
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
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;
import org.js4ms.io.channel.OutputChannel;
import org.js4ms.server.Connection;




/**
 * An {@link OutputChannel} that can be used to send a byte array containing an
 * RTP/RTCP packet over an RTSP TCP control connection using the interleaved framing
 * method described in RFC-2326.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class InterleavedPacketOutputChannel implements OutputChannel<ByteBuffer> {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(InterleavedPacketOutputChannel.class.getName());
    

    protected final Log log = new Log(this);

    private final int channel;
    private final Connection connection;

    /**
     * Constructs an output channel that can be used to send packets via the specified RTSP TCP control connection.
     * @param channel - The number used to identify the target media stream for the packet. 
     * @param connection - The RTSP TCP control connection.
     */
    public InterleavedPacketOutputChannel(final int channel, final Connection connection) {
        this.channel = channel;
        this.connection = connection;
    }

    @Override
    public void send(final ByteBuffer packet, final int milliseconds) throws IOException, InterruptedIOException, InterruptedException {

        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(log.msg("sending packet on channel="+this.channel+" length="+packet.limit()));
        }

        OutputStream outputStream = this.connection.getOutputStream();

        // Must be synchronized to prevent simultaneous write while 
        // an RTSP message or another interleaved packet is being sent.
        synchronized (outputStream) {
            int count = packet.limit();
            outputStream.write('$');
            outputStream.write((byte)this.channel);
            outputStream.write((byte)((count >> 8) & 0xFF));
            outputStream.write((byte)(count & 0xFF));
            outputStream.write(packet.array(), packet.arrayOffset(), count);
        }
    }

    @Override
    public void close() throws IOException, InterruptedException {
        // Do nothing - the connection is managed elsewhere.
    }

}
