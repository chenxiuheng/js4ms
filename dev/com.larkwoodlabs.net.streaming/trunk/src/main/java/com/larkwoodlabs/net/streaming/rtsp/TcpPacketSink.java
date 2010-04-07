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
import java.nio.ByteBuffer;

public final class TcpPacketSink extends PacketSink {

    private final int channelNumber;
    private final ConnectionHandler handler;
    
    public TcpPacketSink(final int channelNumber, final ConnectionHandler handler) {
        this.channelNumber = channelNumber;
        this.handler = handler;
    }

    @Override
    public void close(boolean isCloseAll) throws IOException, InterruptedException {
        // Close nothing here since the output stream is the RTSP control connection
    }

    @Override
    public void send(ByteBuffer packet, int milliseconds) throws IOException, InterruptedIOException, InterruptedException {
        this.handler.sendPacket(channelNumber, packet);
    }

}
