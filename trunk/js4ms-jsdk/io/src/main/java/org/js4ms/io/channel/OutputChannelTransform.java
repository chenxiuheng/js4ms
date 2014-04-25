package org.js4ms.io.channel;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * OutputChannelTransform.java [org.js4ms.jsdk:io]
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
 * An output channel adapter that transforms message objects sent to it into message
 * objects of the same or different type that it then forwards to another OutputChannel.
 * Instances of this class use a {@link MessageTransform} instance to perform the
 * the desired transformation.
 * 
 * @param <OuterMessageType>
 *            The message object type to be transformed.
 * @param <InnerMessageType>
 *            The message object type produced by the transformation.
 * @author Greg Bumgardner (gbumgard)
 */
public final class OutputChannelTransform<OuterMessageType, InnerMessageType>
                extends OutputChannelAdapter<OuterMessageType, InnerMessageType> {

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * A message transform object that will convert messages received by
     * this channel into the messages that are then sent to the inner output channel.
     */
    protected final MessageTransform<OuterMessageType, InnerMessageType> transform;

    /**
     * @param innerChannel
     * @param transform
     */
    public OutputChannelTransform(final OutputChannel<InnerMessageType> innerChannel,
                                  final MessageTransform<OuterMessageType, InnerMessageType> transform) {
        super(innerChannel);
        this.transform = transform;
    }

    @Override
    public void send(final OuterMessageType message, final int milliseconds) throws IOException, InterruptedIOException, InterruptedException {
        this.innerChannel.send(this.transform.transform(message), milliseconds);
    }

}
