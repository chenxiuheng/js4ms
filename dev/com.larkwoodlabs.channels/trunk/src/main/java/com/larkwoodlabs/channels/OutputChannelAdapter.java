/*
 * Copyright © 2009-2010 Larkwood Labs Software.
 *
 * Licensed under the Larkwood Labs Software Source Code License, Version 1.0.
 * You may not use this file except in compliance with this License.
 *
 * You may view the Source Code License at
 * http://www.larkwoodlabs.com/source-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the license.
 */

package com.larkwoodlabs.channels;

import java.io.IOException;


/**
 * Abstract base class for all message output channels that transform or filter
 * messages before sending them to an inner message channel.
 *
 * @param <OuterMessageType> - The type of message accepted by the outer message channel.
 * @param <InnerMessageType> - The type of message accepted by the inner message channel.
 *
 * @author Gregory Bumgardner
 */
public abstract class OutputChannelAdapter<OuterMessageType, InnerMessageType>
                      extends ChannelBase
                      implements OutputChannel<OuterMessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * The output channel that will send messages for the adapter channel.
     */
    protected final OutputChannel<InnerMessageType> innerChannel;
    

    /*-- Member Functions ----------------------------------------------------*/

    protected OutputChannelAdapter(final OutputChannel<InnerMessageType> innerChannel) {
        this.innerChannel = innerChannel;
    }

    @Override
    public void close() throws IOException, InterruptedException {
        this.innerChannel.close();
    }
}
