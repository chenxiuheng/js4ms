package org.js4ms.io.channel;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * OutputChannelTee.java [org.js4ms.jsdk:io]
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
import java.util.LinkedHashSet;

import org.js4ms.common.exception.BoundException;
import org.js4ms.io.MultiIOException;



/**
 * An output channel that forwards messages to one or more attached output channels.
 * Messages are delivered to channels in the order the channels were added to the Tee.
 * The Tee does not allow an output channel to be added multiple times -
 * it will ignore any attempt to add the same output channel more than once.
 * A thread should not attempt to add or remove channels while executing in the
 * {@link #send(Object, int)} method as this may result in an exception.
 * 
 * @param <MessageType>
 * @author Greg Bumgardner (gbumgard)
 */
public final class OutputChannelTee<MessageType>
                implements OutputChannel<MessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * Collection of output channels that will receive messages sent to this output
     * channel.
     */
    private final LinkedHashSet<OutputChannel<MessageType>> channels = new LinkedHashSet<OutputChannel<MessageType>>();

    /**
     * Monitor object used for thread synchronization.
     */
    private final Object lock = new Object();

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs an Tee with no output channels.
     * Use {@link #add(OutputChannel)} to attach channels to the Tee.
     */
    public OutputChannelTee() {
    }

    /**
     * Constructs a Tee and attaches one or more output channels to the Tee.
     * 
     * @param channels
     */
    @SafeVarargs
	public OutputChannelTee(final OutputChannel<MessageType>... channels) {
        for (OutputChannel<MessageType> channel : channels) {
            this.channels.add(channel);
        }
    }

    /**
     * Adds the specified channel to the Tee.
     * 
     * @param channel
     */
    public final void add(OutputChannel<MessageType> channel) {
        synchronized (this.lock) {
            this.channels.add(channel);
        }
    }

    /**
     * Removes the specified channel from the Tee.
     * 
     * @param channel
     */
    public final void remove(OutputChannel<MessageType> channel) {
        synchronized (this.lock) {
            this.channels.remove(channel);
        }
    }

    /**
     * Indicates whether there are any output channel attached to the Tee.
     * 
     * @return A boolean value of <code>true</code> indicates that no
     *         output channels are currently attached to the tee and <code>false</code>
     *         indicates that one or more channels are currently attached.
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
                    me.add(new BoundException(channel, e));
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
                    me.add(new BoundException(channel, e));
                }
            }
            // Throws the multi-exception if an IOException was stored in it
            me.rethrow();
        }

    }

}
