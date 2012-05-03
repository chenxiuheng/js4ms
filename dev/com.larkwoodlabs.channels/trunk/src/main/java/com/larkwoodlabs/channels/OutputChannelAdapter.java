/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: OutputChannelAdapter.java (com.larkwoodlabs.channels)
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

package com.larkwoodlabs.channels;

import java.io.IOException;

/**
 * Abstract base class for all message output channels that transform or filter
 * messages before sending them to an inner message channel.
 * 
 * @param <OuterMessageType>
 *            The type of message accepted by the outer message channel.
 * @param <InnerMessageType>
 *            The type of message accepted by the inner message channel.
 * @author Greg Bumgardner (gbumgard)
 */
public abstract class OutputChannelAdapter<OuterMessageType, InnerMessageType>
                implements OutputChannel<OuterMessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * The output channel that will receive messages produced by this adapter channel.
     */
    protected final OutputChannel<InnerMessageType> innerChannel;

    /*-- Member Functions ----------------------------------------------------*/
    ;

    /**
     * Constructs an output channel adapter for the specified output channel.
     * 
     * @param innerChannel
     *            The output channel to be wrapped.
     */
    protected OutputChannelAdapter(final OutputChannel<InnerMessageType> innerChannel) {
        this.innerChannel = innerChannel;
    }

    @Override
    public void close() throws IOException, InterruptedException {
        this.innerChannel.close();
    }
}
