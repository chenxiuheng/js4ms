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

public abstract class RtpSourceChannel {

    OutputChannel<ByteBuffer> rtpChannel;
    OutputChannel<ByteBuffer> rtcpChannel;

    public RtpSourceChannel(OutputChannel<ByteBuffer> rtpChannel,
                            OutputChannel<ByteBuffer> rtcpChannel) {
        this.rtpChannel = rtpChannel;
        this.rtcpChannel = rtcpChannel;
    }
    
    /**
     * Returns the port number or interleaved channel number
     * assigned to the RTP channel.
     * @return
     */
    public abstract int getRtpChannelNumber();
    
    public OutputChannel<ByteBuffer> getRtpChannel() {
        return this.rtpChannel;
    }

    /**
     * Returns the port number or interleaved channel number
     * assigned to the RTCP channel.
     * @return
     */
    public abstract int getRtcpChannelNumber();
    
    public OutputChannel<ByteBuffer> getRtcpChannel() {
        return this.rtpChannel;
    }
}
