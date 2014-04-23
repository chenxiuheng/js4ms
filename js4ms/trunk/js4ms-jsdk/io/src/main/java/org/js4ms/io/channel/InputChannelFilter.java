package org.js4ms.io.channel;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * InputChannelFilter.java [org.js4ms.jsdk:io]
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

/**
 * An input channel adapter that uses a filter to determine
 * which messages received from an inner input channel can be
 * received from the adapter channel.
 * 
 * @param <MessageType>
 *            The message object type.
 * @author Greg Bumgardner (gbumgard)
 */
public final class InputChannelFilter<MessageType>
                extends InputChannelAdapter<MessageType, MessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * The message filter used to filter messages received from the inner input channel.
     */
    protected final MessageFilter<MessageType> filter;

    /**
     * Monitor object used for thread synchronization.
     */
    private final Object lock = new Object();

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs an input channel filter.
     * 
     * @param innerChannel
     *            The channel that will provide messages to the adapter.
     * @param filter
     *            The {@link MessageFilter} that will be used to filter messages.
     */
    public InputChannelFilter(final InputChannel<MessageType> innerChannel,
                              final MessageFilter<MessageType> filter) {
        super(innerChannel);
        this.filter = filter;
    }

    @Override
    public MessageType receive(int milliseconds) throws IOException, InterruptedIOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        int timeRemaining = milliseconds;
        while (timeRemaining > 0) {
            synchronized (this.lock) {
                MessageType message = this.innerChannel.receive(timeRemaining);
                if (this.filter.isMatch(message)) {
                    return message;
                }
                timeRemaining = milliseconds - (int) (System.currentTimeMillis() - startTime);
            }
        }
        throw new InterruptedIOException("receive operation timed-out");
    }

}
