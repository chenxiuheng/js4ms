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
 * Abstract base class for all message input channels that transform or
 * filter messages received from an inner message channel.
 *
 * @param <InnerMessageType> - The type of message produced by the inner message channel.
 * @param <OuterMessageType> - The type of message produced by the outer message channel.
 *
 * @author Gregory Bumgardner
 */
public abstract class InputChannelAdapter<InnerMessageType, OuterMessageType>
                      extends ChannelBase
                      implements InputChannel<OuterMessageType> {

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * The input channel that will receive messages for the adapter channel.
     */
    protected final InputChannel<InnerMessageType> innerChannel;


    /*-- Member Functions ----------------------------------------------------*/

    public InputChannelAdapter(final InputChannel<InnerMessageType> innerChannel) {
        this.innerChannel = innerChannel;
    }
    
    @Override
    public void close(boolean closeInner) throws IOException {
        if (closeInner) {
            this.innerChannel.close(true);
        }
    }
}
