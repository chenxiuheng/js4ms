package org.js4ms.io.channel;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MessageOutputMap.java [org.js4ms.jsdk:io]
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
import java.util.Set;

/**
 * An message channel that routes an incoming message to one in a set of message channels.
 * The message output channel is chosen using a key value extracted from the message.
 * The message channel map only allows one channel per key value. A channel added
 * previously
 * can be replaced by adding a different channel using the same key value.
 * A thread should not attempt to add or remove channels while executing in the
 * {@link #send(Object, int)} method as this may result
 * in an exception.
 * 
 * @param <MessageType>
 *            The message object type.
 * @see MessageKeyExtractor
 * @author Greg Bumgardner (gbumgard)
 */
public final class MessageOutputMap<MessageType>
                implements MessageOutput<MessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * A hash map that maps message keys to message output channels.
     */
    private final HashMap<Object, MessageOutput<MessageType>> channelMap = new HashMap<Object, MessageOutput<MessageType>>();

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
     * Constructs an message channel map that uses the specified
     * {@link MessageKeyExtractor} to retrieve the key value from each
     * message that will be used to select which output
     * channel will receive the message.
     */
    public MessageOutputMap(final MessageKeyExtractor<MessageType> keyExtractor) {
        this.keyExtractor = keyExtractor;
    }

    /**
     * Adds or replaces a message channel in the map.
     * 
     * @param key
     *            The key value that will be used to select the channel.
     * @param channel
     *            The output channel that will be selected for the specified key.
     */
    public final void put(final Object key, final MessageOutput<MessageType> channel) {
        synchronized (this.lock) {
            this.channelMap.put(key, channel);
        }
    }

    /**
     * Returns the message channel, if any, added using the specified key.
     * 
     * @param key
     *            The key value used to add a channel.
     */
    public final MessageOutput<MessageType> get(final Object key) {
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
     * Removes the specified message channel from the map.
     * 
     * @param channel
     *            The channel to remove.
     */
    public final void remove(final MessageOutput<MessageType> channel) {
        synchronized (this.lock) {
            Iterator<MessageOutput<MessageType>> iter = this.channelMap.values().iterator();
            while (iter.hasNext()) {
                MessageOutput<MessageType> entry = iter.next();
                if (entry == channel) {
                    iter.remove();
                    return;
                }
            }
        }
    }

    @Override
    public final void send(final MessageType message, final int milliseconds) throws IOException,
                                                                             InterruptedIOException,
                                                                             InterruptedException {
        synchronized (this.lock) {
            MessageOutput<MessageType> channel = this.channelMap.get(this.keyExtractor.getKey(message));
            if (channel != null) {
                channel.send(message, milliseconds);
            }
        }

    }

}
