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
import java.io.InterruptedIOException;



/**
 * An output channel adapter that uses a filter to determine
 * which messages sent to the adapter channel should be sent
 * to the inner output channel.
 *
 * @param <MessageType> - The message object type.
 *
 * @author Gregory Bumgardner
 */
public final class OutputChannelFilter<MessageType>
                   extends OutputChannelAdapter<MessageType, MessageType> {

    /*-- Inner Classes -------------------------------------------------------*/

    /*-- Static Variables ----------------------------------------------------*/

    /*-- Static Functions ----------------------------------------------------*/

    /*-- Member Variables ----------------------------------------------------*/

    protected final MessageFilter<MessageType> filter;
    
    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs an output channel filter.
     * @param innerChannel - The channel that will receive messages from the adapter.
     * @param filter - The {@link MessageFilter} that will be used to filter messages.
     */
    public OutputChannelFilter(final OutputChannel<MessageType> innerChannel,
                               final MessageFilter<MessageType> filter) {
        super(innerChannel);
        this.filter = filter;
    }

    @Override
    public final void send(final MessageType message, final int milliseconds) throws IOException,
                                                                                     InterruptedIOException,
                                                                                     InterruptedException {
        if (this.filter.isMatch(message)) {
            this.innerChannel.send(message, milliseconds);
        }
    }

}
