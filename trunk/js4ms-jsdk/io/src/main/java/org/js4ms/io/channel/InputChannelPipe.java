package org.js4ms.io.channel;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * InputChannelPipe.java [org.js4ms.jsdk:io]
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
 * A message input channel that receives messages from any object that
 * implements the {@link MessageInput} interface.
 * 
 * @param <MessageType>
 *            The message object type.
 * @author Greg Bumgardner (gbumgard)
 */
public final class InputChannelPipe<MessageType>
                implements InputChannel<MessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * The message pipe implementation used in this input channel.
     */
    private MessageInput<MessageType> pipe;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs an unconnected input channel.
     * Use {@link #connect(MessageInput)} to connect the channel to a message pipe.
     */
    public InputChannelPipe() {
        connect(null);
    }

    /**
     * Constructs an input channel that is connected to the specified message pipe.
     * 
     * @param pipe
     */
    public InputChannelPipe(final MessageInput<MessageType> pipe) {
        connect(pipe);
    }

    /**
     * Connects this input channel to the specified message pipe.
     * 
     * @param pipe
     */
    public final void connect(final MessageInput<MessageType> pipe) {
        this.pipe = pipe;
    }

    @Override
    public void close() {
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
