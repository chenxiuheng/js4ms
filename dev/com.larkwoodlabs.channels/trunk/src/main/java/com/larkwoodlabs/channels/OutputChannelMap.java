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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * An output channel that forwards a message to one output channel out of a set
 * of one or more output channels based on a key value extracted from the message.
 * The channel map only allows one channel per key value. A channel added previously
 * can be replaced by adding a different channel using the same key value.
 * A thread should not attempt to add or remove channels while executing in the 
 * {@link #send(Object, int)} method as this may result in an exception.
 *
 * @param <MessageType> - The message object type.
 * @see {@link MessageKeyExtractor}
 *
 * @author Gregory Bumgardner
 */
public final class OutputChannelMap<MessageType>
                   extends ChannelBase
                   implements OutputChannel<MessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    private final HashMap<Object, OutputChannel<MessageType>> channelMap = new HashMap<Object, OutputChannel<MessageType>>();

    private final MessageKeyExtractor<MessageType> keyExtractor;


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs an output channel map that uses the specified {@link MessageKeyExtractor}
     * to retrieve the key value from each message that will be used to select which output
     * channel will receive the message.
     */
    public OutputChannelMap(final MessageKeyExtractor<MessageType> keyExtractor) {
        this.keyExtractor = keyExtractor;
    }

    /**
     * Adds or replaces a channel in the map.
     * @param key - The key value that will be used to select the channel.
     * @param channel - The output channel that will be selected for the specified key.
     */
    public final void put(final Object key, final OutputChannel<MessageType> channel) {
        synchronized (this.lock) {
            this.channelMap.put(key,channel);
        }
    }

    /**
     * Returns the channel, if any, added using the specified key.
     * @param key - The key value used to add a channel.
     */
    public final OutputChannel<MessageType> get(final Object key) {
        synchronized (this.lock) {
            return this.channelMap.get(key);
        }
    }

    /**
     * Indicates whether the map contains any channels.
     */
    public final boolean isEmpty() {
        synchronized (this.lock) {
            return this.channelMap.isEmpty();
        }
    }

    /**
     * Returns the current key set.
     * @param key - The key value used to add a channel.
     */
    public final Set<Object> getKeys() {
        synchronized (this.lock) {
            return this.channelMap.keySet();
        }
    }

    /**
     * Removes any channel added using the specified key from the map.
     * @param key - The key value used to add a channel.
     */
    public final void remove(final Object key) {
        synchronized (this.lock) {
            this.channelMap.remove(key);
        }
    }

    /**
     * Removes the specified channel from the map.
     * @param channel - The channel to remove.
     */
    public final void remove(final OutputChannel<MessageType> channel) {
        synchronized (this.lock) {
            Iterator<Map.Entry<Object,OutputChannel<MessageType>>> iter = this.channelMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Object,OutputChannel<MessageType>> entry = iter.next();
                if (entry.getValue() == channel) {
                    iter.remove();
                    return;
                }
            }
        }
    }

    @Override
    public final void close(final boolean isCloseAll) throws IOException, InterruptedException {
        synchronized (this.lock) {
            if (isCloseAll) {
                for (Map.Entry<Object,OutputChannel<MessageType>> entry : this.channelMap.entrySet()) {
                    entry.getValue().close(true);
                }
            }
            this.channelMap.clear();
        }
    }

    @Override
    public final void send(final MessageType message, final int milliseconds) throws IOException,
                                                                                     InterruptedIOException,
                                                                                     InterruptedException {
        synchronized (this.lock) {
            OutputChannel<MessageType> channel = this.channelMap.get(this.keyExtractor.getKey(message));
            if (channel != null) {
                channel.send(message, milliseconds);
            }
        }

    }
    
}
