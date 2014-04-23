package org.js4ms.io.channel;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * OutputChannelMap.java [org.js4ms.jsdk:io]
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
 * @param <MessageType>
 *            The message object type.
 * @see MessageKeyExtractor
 * @author Greg Bumgardner (gbumgard)
 */
public final class OutputChannelMap<MessageType>
                implements OutputChannel<MessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * A hash map that maps message keys to output channels.
     */
    private final HashMap<Object, OutputChannel<MessageType>> channelMap = new HashMap<Object, OutputChannel<MessageType>>();

    /**
     * A message key extractor that will be used to extract a key from each message
     * that is used to retrieve an output channel from the channel map.
     */
    private final MessageKeyExtractor<MessageType> keyExtractor;

    /**
     * Monitor object used for thread synchronization.
     */
    private final Object lock = new Object();

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs an output channel map that uses the specified
     * {@link MessageKeyExtractor} to retrieve the key value from each message that will
     * be used to select which output
     * channel will receive the message.
     * 
     * @param keyExtractor
     */
    public OutputChannelMap(final MessageKeyExtractor<MessageType> keyExtractor) {
        this.keyExtractor = keyExtractor;
    }

    /**
     * Adds or replaces a channel in the map.
     * 
     * @param key
     *            The key value that will be used to select the channel.
     * @param channel
     *            The output channel that will be selected for the specified key.
     */
    public final void put(final Object key, final OutputChannel<MessageType> channel) {
        synchronized (this.lock) {
            this.channelMap.put(key, channel);
        }
    }

    /**
     * Returns the channel, if any, added using the specified key.
     * 
     * @param key
     *            The key value used to add a channel.
     */
    public final OutputChannel<MessageType> get(final Object key) {
        synchronized (this.lock) {
            return this.channelMap.get(key);
        }
    }

    /**
     * Indicates whether the map contains any channels.
     * 
     * @return A boolean value of <code>true</code> indicates that no
     *         output channels are currently mapped and <code>false</code> indicates that
     *         one or more channels are currently mapped.
     */
    public final boolean isEmpty() {
        synchronized (this.lock) {
            return this.channelMap.isEmpty();
        }
    }

    /**
     * Returns the current key set.
     * 
     * @return The current output channel key set.
     */
    public final Set<Object> getKeys() {
        synchronized (this.lock) {
            return this.channelMap.keySet();
        }
    }

    /**
     * Removes any channel added using the specified key from the map.
     * 
     * @param key
     *            The key value used to add a channel.
     */
    public final void remove(final Object key) {
        synchronized (this.lock) {
            this.channelMap.remove(key);
        }
    }

    /**
     * Removes the specified channel from the map.
     * 
     * @param channel
     *            The channel to remove.
     */
    public final void remove(final OutputChannel<MessageType> channel) {
        synchronized (this.lock) {
            Iterator<Map.Entry<Object, OutputChannel<MessageType>>> iter = this.channelMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Object, OutputChannel<MessageType>> entry = iter.next();
                if (entry.getValue() == channel) {
                    iter.remove();
                    return;
                }
            }
        }
    }

    @Override
    public final void close() throws IOException, InterruptedException {
        synchronized (this.lock) {
            for (Map.Entry<Object, OutputChannel<MessageType>> entry : this.channelMap.entrySet()) {
                entry.getValue().close();
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
