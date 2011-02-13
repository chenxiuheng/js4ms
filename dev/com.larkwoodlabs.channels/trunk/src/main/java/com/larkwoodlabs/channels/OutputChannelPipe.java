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

package com.larkwoodlabs.channels;

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * A message output channel that sends messages to any object that implements
 * the {@link MessageOutput} interface.
 *
 * @param <MessageType> - The message object type.
 *
 * @author gbumgard@cisco.com
 */
public final class OutputChannelPipe<MessageType>
                   implements OutputChannel<MessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * The message pipe implementation used in this output channel.
     */
    private MessageOutput<MessageType> pipe;


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs an unconnected output channel.
     * Use {@link #connect(MessageOutput)} to connect the channel to a message pipe.
     */
    public OutputChannelPipe() {
        connect(null);
    }

    /**
     * Constructs an output channel that is connected to the specified message pipe.
     * @param pipe
     */
    public OutputChannelPipe(final MessageOutput<MessageType> pipe) {
        connect(pipe);
    }

    /**
     * Connects this output channel to the specified message pipe.
     * @param pipe
     */
    public final void connect(final MessageOutput<MessageType> pipe) {
        this.pipe = pipe;
    }

    @Override
    
    public final void close() {
        // NO-OP
    }

    @Override
    public final void send(final MessageType message, final int milliseconds) throws IOException,
                                                                                     InterruptedIOException,
                                                                                     InterruptedException {
        if (this.pipe == null) {
            throw new IOException("pipe not connected");
        }

        this.pipe.send(message, milliseconds);
    }

}
