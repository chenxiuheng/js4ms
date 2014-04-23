package org.js4ms.io.channel;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MessageOutput.java [org.js4ms.jsdk:io]
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
 * Interface exposed by objects that may be used to send a message to a message sink.
 * 
 * @param <MessageType>
 *            The message object type.
 * @author Greg Bumgardner (gbumgard)
 */
public interface MessageOutput<MessageType> {

    /**
     * Special message object used to indicate that the
     * source has stopped producing messages.
     */
    public static final Object EOM = MessageInput.EOM;

    /**
     * Attempts to send a message to a message sink within a specified amount of time.
     * <p>
     * Some channel implementations may choose to accept the static object {@link #EOM
     * EOM} to indicate that the caller has stopped producing messages.
     * 
     * @param message
     *            A <code>MessageType</code> object or the static object {@link #EOM EOM}
     *            (if supported).
     * @param milliseconds
     *            The amount of time allotted to complete the operation.
     * @throws IOException
     *             The send operation has failed.
     * @throws InterruptedIOException
     *             The send operation was interrupted or timed out.
     * @throws InterruptedException
     *             The calling thread was interrupted before the send operation could
     *             complete.
     */
    void send(MessageType message, int milliseconds) throws IOException,
                                                    InterruptedIOException,
                                                    InterruptedException;

}
