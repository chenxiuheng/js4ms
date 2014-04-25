package org.js4ms.rtsp.rtp;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * InterleavedPacketReader.java [org.js4ms.jsdk:rtsp]
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

import java.io.EOFException;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;
import org.js4ms.io.channel.OutputChannel;
import org.js4ms.server.Connection;





public class InterleavedPacketReader {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(InterleavedPacketReader.class.getName());

    private final Log log = new Log(this);

    private final Vector<OutputChannel<ByteBuffer>> channels = new Vector<OutputChannel<ByteBuffer>>();

    public InterleavedPacketReader() {
    }

    public void set(final int channelIndex, final OutputChannel<ByteBuffer> outputChannel) {
        if (channelIndex > this.channels.size() - 1) {
            this.channels.add(channelIndex, outputChannel);
        }
        else {
            this.channels.set(channelIndex, outputChannel);
        }
    }

    public void close(final int channelIndex) throws IOException, InterruptedException {
        if (channelIndex < this.channels.size()) {
            OutputChannel<ByteBuffer> channel = this.channels.get(channelIndex);
            if (channel != null) {
                channel.close();
            }
        }
    }

    public void close() throws IOException, InterruptedException {
        for (OutputChannel<ByteBuffer> channel : this.channels) {
            channel.close();
        }
        this.channels.clear();
    }

    public boolean hasActiveChannels() {
        for (int i = 0; i < this.channels.size(); i++) {
            if (this.channels.get(i) != null) return true;
        }
        return false;
    }

    public int channelCount() {
        return channels.size();
    }

    /**
     * Reads sequence of packets from the connection and sends them to registered output channels.
     * This method does not exit until a possible control message is received (the first byte read
     * following a packet is not an '$' character) or the calling thread is interrupted.
     * @param connection - the connection from which to read interleaved packets.
     * @throws EOFException - If the input stream reaches EOF or is closed.
     * @throws IOException - An I/O error occurred
     */
    public void readPackets(final Connection connection) throws IOException {

        final PushbackInputStream inputStream = connection.getInputStream();

        while (!Thread.currentThread().isInterrupted()) {
            // Get first character in message 
            // Throws SocketException if the socket is closed by 
            // another thread while waiting in this call
            int c = inputStream.read();
    
            if (c == -1) {
                // Peer stopped sending data or input was shutdown
                throw new EOFException("connection stream returned EOF");
            }
    
            if (c == '$') {
                // receiving an interleaved RTP/RTCP packet
                int channel = inputStream.read();
                if (channel != -1) {
                    int msb = inputStream.read();
                    if (msb != -1) {
                        int lsb = inputStream.read();
                        if (lsb != -1) {
                            int count = msb << 8 + lsb;
                            byte[] packet = new byte[count];
                            int actual = inputStream.read(packet);
                            if (actual == count) {
                                OutputChannel<ByteBuffer> outputChannel = this.channels.get(channel);
                                if (outputChannel != null) {
                                    try {
                                        if (logger.isLoggable(Level.FINER)) {
                                            logger.finer(log.msg("received packet on channel="+channel+" length="+count));
                                        }
                                        outputChannel.send(ByteBuffer.wrap(packet), Integer.MAX_VALUE);
                                    }
                                    catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        return;
                                    }
                                }
                                continue;
                            } 
                        }
                    }
                }
                // Peer stopped sending data or input was shutdown
                throw new EOFException("unexpected EOF occurred while reading interleaved packet");
            }
            else {
                // The next byte is the the first byte in a control message
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("interleaved packet listener exiting - new message received '"+(char)c+"'"));
                }
                inputStream.unread(c);
                return;
            }
        }
    }

}
