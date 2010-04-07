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
 * A message input channel that receives messages from any object that
 * implements the {@link MessageInput} interface.
 *
 * @param <MessageType> - The message object type.
 *
 * @author Gregory Bumgardner
 */
public final class InputChannelPipe<MessageType>
                   extends ChannelBase
                   implements InputChannel<MessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    private  MessageInput<MessageType> pipe;


    /*-- Member Functions ----------------------------------------------------*/

    public InputChannelPipe() {
        connect(null);
    }

    public InputChannelPipe(final MessageInput<MessageType> pipe) {
        connect(pipe);
    }
    
    public final void connect(final MessageInput<MessageType> pipe) {
        this.pipe = pipe;
    }

    @Override
    public void close(boolean isCloseAll) {
        // NO-OP
    }

    @Override
    public final MessageType receive(final int milliseconds) throws IOException,
                                                                    InterruptedIOException,
                                                                    InterruptedException {
        if (this.pipe == null) {
            throw new IOException("pipe not connected");
        }

        return this.pipe.receive(milliseconds);
    }

}
