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
import java.util.LinkedHashSet;

import com.larkwoodlabs.common.exceptions.BoundException;
import com.larkwoodlabs.common.exceptions.MultiIOException;

/**
 * An output channel that forwards messages to one or more attached output channels.
 * Messages are delivered to channels in the order the channels were added to the Tee.
 * The Tee does not allow an output channel to be added multiple times -
 * it will ignore any attempt to add the same output channel more than once.
 * A thread should not attempt to add or remove channels while executing in the 
 * {@link #send(Object, int)} method as this may result in an exception.
 *
 * @param <MessageType>
 *
 * @author Gregory Bumgardner
 */
public final class OutputChannelTee<MessageType>
                   extends ChannelBase
                   implements OutputChannel<MessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    private final LinkedHashSet<OutputChannel<MessageType>> channels = new LinkedHashSet<OutputChannel<MessageType>>();
    

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs an Tee with no output channels.
     * Use {@link #add(OutputChannel)} to attach channels to the Tee.
     */
    public OutputChannelTee() {    
    }

    /**
     * Constructs a Tee and attaches one or more output channels to the Tee.
     * @param channels
     */
    public OutputChannelTee(final OutputChannel<MessageType> ... channels) {
        for (OutputChannel<MessageType> channel : channels) {
            this.channels.add(channel);
        }
    }

    /**
     * Adds the specified channel to the Tee.
     * @param channel
     */
    public final void add(OutputChannel<MessageType> channel) {
        synchronized (this.lock) {
            this.channels.add(channel);
        }
    }

    /**
     * Removes the specified channel from the Tee.
     * @param channel
     */
    public final void remove(OutputChannel<MessageType> channel) {
        synchronized (this.lock) {
            this.channels.remove(channel);
        }
    }

    /**
     * Indicates whether there are any output channel attached to the Tee.
     * @return
     */
    public final boolean isEmpty() {
        synchronized (this.lock) {
            return this.channels.isEmpty();
        }
    }

    @Override
    public final void close() throws IOException, InterruptedException {
        synchronized (this.lock) {
            MultiIOException me = new MultiIOException();
            for (OutputChannel<MessageType> channel : this.channels) {
                try {
                    channel.close();
                }
                catch (IOException e) {
                    me.add(new BoundException(channel,e));
                }
            }
            // Throws the multi-exception if an IOException was stored in it
            me.rethrow();
            this.channels.clear();
        }
    }

    @Override
    public final void send(final MessageType message, final int milliseconds) throws IOException,
                                                                                     InterruptedIOException,
                                                                                     InterruptedException {
        synchronized (this.lock) {
            MultiIOException me = new MultiIOException();
            for (OutputChannel<MessageType> channel : this.channels) {
                try {
                    channel.send(message, milliseconds);
                }
                catch (IOException e) {
                    me.add(new BoundException(channel,e));
                }
            }
            // Throws the multi-exception if an IOException was stored in it
            me.rethrow();
        }

    }

}
