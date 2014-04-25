package org.js4ms.io.channel;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * InputChannelTransform.java [org.js4ms.jsdk:io]
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
 * An input channel adapter that transforms message objects received from another
 * InputChannel into message objects of the same or different type.
 * Instances of this class use a {@link MessageTransform} instance to perform the
 * the desired transformation.
 * 
 * @param <InnerMessageType>
 *            The message object type to be transformed.
 * @param <OuterMessageType>
 *            The message object type produced by the transformation.
 * @author Greg Bumgardner (gbumgard)
 */
public final class InputChannelTransform<InnerMessageType, OuterMessageType>
                extends InputChannelAdapter<InnerMessageType, OuterMessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * A message transform object that will convert messages produced by
     * the inner channel into the messages that are produced by this channel.
     */
    protected final MessageTransform<InnerMessageType, OuterMessageType> transform;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs an input channel that uses the specified transform object to
     * transform messages produced by the specified inner input channel into the
     * message produced by this channel.
     * 
     * @param innerChannel
     * @param transform
     */
    public InputChannelTransform(final InputChannel<InnerMessageType> innerChannel,
                                 final MessageTransform<InnerMessageType, OuterMessageType> transform) {
        super(innerChannel);
        this.transform = transform;
    }

    @Override
    public OuterMessageType receive(int milliseconds) throws IOException, InterruptedIOException, InterruptedException {
        return this.transform.transform(this.innerChannel.receive(milliseconds));
    }

}
