package org.js4ms.io.channel;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MessageInput.java [org.js4ms.jsdk:io]
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
 * Interface exposed by objects that can be used to receive a message from a message
 * source.
 * 
 * @param <MessageType>
 *            The message object type.
 * @author Greg Bumgardner (gbumgard)
 */
public interface MessageInput<MessageType> {

    /**
     * Special message object used to indicate that the
     * source has stopped producing messages.
     */
    public static final Object EOM = new Object();

    /**
     * Attempts to retrieve a message from a message source and if necessary,
     * waits the specified amount of time until a message becomes available.
     * <p>
     * Some implementations may choose to return the static Object {@link #EOM} to
     * indicate that the source has stopped producing messages.
     * 
     * @param milliseconds
     *            The amount of time allotted to complete the operation.
     * @return A <code>MessageType</code> object or the object {@link #EOM}.
     * @throws IOException
     *             The receive operation has failed.
     * @throws InterruptedIOException
     *             The receive operation was interrupted or timed out.
     * @throws InterruptedException
     *             The calling thread was interrupted before the receive operation could
     *             complete.
     */
    MessageType receive(int milliseconds) throws IOException, InterruptedIOException, InterruptedException;

}
