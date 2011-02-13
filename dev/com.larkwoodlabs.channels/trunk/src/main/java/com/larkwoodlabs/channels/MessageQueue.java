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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A message pipe that buffers messages in a fixed-size, blocking queue.
 * Typically used to connect an {@link InputChannelPipe} to an
 * {@link OutputChannelPipe} to provide a means for passing messages
 * from one thread to another thread.
 *
 * @param <MessageType> - The message object type.
 *
 * @author gbumgard@cisco.com
 */
public final class MessageQueue<MessageType>
                   implements MessagePipe<MessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    private final LinkedBlockingQueue<MessageType> queue;


    /*-- Member Variables ----------------------------------------------------*/
    
    /**
     * Constructs a message queue with the specified maximum capacity.
     * When the queue reaches maximum capacity, the {@link #send(Object, int)}
     * method will block until a message is removed from the queue, or the
     * send timeout is reached.
     * 
     * @param capacity - The maximum number of messages that can be held in the queue.
     */
    public MessageQueue(int capacity) {
        this.queue = new LinkedBlockingQueue<MessageType>(capacity);
    }

    @Override
    public final MessageType receive(final int milliseconds) throws IOException,
                                                                    InterruptedIOException,
                                                                    InterruptedException {

        MessageType message = this.queue.poll((long)milliseconds, TimeUnit.MILLISECONDS);

        if (message == null) {
            throw new InterruptedIOException("receive operation timed out");
        }
        else {
            return message;
        }
    }

    @Override
    public final void send(final MessageType message, final int milliseconds) throws IOException,
                                                                                     InterruptedIOException,
                                                                                     InterruptedException {

        if (!this.queue.offer(message, (long)milliseconds, TimeUnit.MILLISECONDS)) {
            throw new InterruptedIOException("send operation timed out");
        }

    }

}
