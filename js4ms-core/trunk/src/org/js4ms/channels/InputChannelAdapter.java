/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: InputChannelAdapter.java (org.js4ms.channels)
 * 
 * Copyright � 2009-2012 Cisco Systems, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.js4ms.channels;

import java.io.IOException;

/**
 * Abstract base class for all message input channels that transform or
 * filter messages received from an inner message channel.
 * 
 * @param <InnerMessageType>
 *            The type of message produced by the inner message channel.
 * @param <OuterMessageType>
 *            The type of message produced by the outer message channel.
 * @author Greg Bumgardner (gbumgard)
 */
public abstract class InputChannelAdapter<InnerMessageType, OuterMessageType>
                implements InputChannel<OuterMessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * The input channel that will produce messages for the adapter channel.
     */
    protected final InputChannel<InnerMessageType> innerChannel;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs an input channel adapter for the specified input channel.
     * 
     * @param innerChannel
     */
    protected InputChannelAdapter(final InputChannel<InnerMessageType> innerChannel) {
        this.innerChannel = innerChannel;
    }

    @Override
    public void close() throws IOException {
        this.innerChannel.close();
    }
}
