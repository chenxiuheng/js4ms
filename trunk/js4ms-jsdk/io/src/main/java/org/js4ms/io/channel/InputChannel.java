package org.js4ms.io.channel;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * InputChannel.java [org.js4ms.jsdk:io]
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

/**
 * Interface exposed by all message input channel objects.
 * A message input channel provides the means for retrieving a message
 * from a message source via the {@link MessageInput#receive(int)} method.
 * 
 * @param <MessageType>
 *            The message object type.
 * @author Greg Bumgardner (gbumgard)
 */
public interface InputChannel<MessageType>
                extends MessageInput<MessageType> {

    /**
     * Closes this channel and optionally closes any channels wrapped or attached to this
     * channel.
     * 
     * @throws IOException
     *             The close operation has failed.
     */
    public void close() throws IOException;

}
