package org.js4ms.io.channel;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * OutputChannelFilter.java [org.js4ms.jsdk:io]
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
 * An output channel adapter that uses a filter to determine
 * which messages sent to the adapter channel should be sent
 * to the inner output channel.
 * 
 * @param <MessageType>
 *            The message object type.
 * @author Greg Bumgardner (gbumgard)
 */
public final class OutputChannelFilter<MessageType>
                extends OutputChannelAdapter<MessageType, MessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * The message filter.
     */
    protected final MessageFilter<MessageType> filter;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs an output channel filter.
     * 
     * @param innerChannel
     *            The channel that will receive messages from the adapter.
     * @param filter
     *            The {@link MessageFilter} that will be used to filter messages.
     */
    public OutputChannelFilter(final OutputChannel<MessageType> innerChannel,
                               final MessageFilter<MessageType> filter) {
        super(innerChannel);
        this.filter = filter;
    }

    @Override
    public final void send(final MessageType message, final int milliseconds) throws IOException,
                                                                             InterruptedIOException,
                                                                             InterruptedException {
        if (this.filter.isMatch(message)) {
            this.innerChannel.send(message, milliseconds);
        }
    }

}
