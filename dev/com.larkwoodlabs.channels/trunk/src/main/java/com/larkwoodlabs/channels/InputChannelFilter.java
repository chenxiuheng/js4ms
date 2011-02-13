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
 * An input channel adapter that uses a filter to determine
 * which messages received from an inner input channel can be
 * received from the adapter channel.
 *
 * @param <MessageType> - The message object type.
 *
 * @author gbumgard@cisco.com
 */
public final class InputChannelFilter<MessageType>
                   extends InputChannelAdapter<MessageType, MessageType> {

    
    /*-- Member Variables ----------------------------------------------------*/

    /**
     * The message filter used to filter messages received from the inner input channel.
     */
    protected final MessageFilter<MessageType> filter;

    /**
     * Monitor object used for thread synchronization.
     */
    private final Object lock = new Object();


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs an input channel filter.
     * @param innerChannel - The channel that will provide messages to the adapter.
     * @param filter - The {@link MessageFilter} that will be used to filter messages.
     */
    public InputChannelFilter(final InputChannel<MessageType> innerChannel,
                              final MessageFilter<MessageType> filter) {
        super(innerChannel);
        this.filter = filter;
    }

    @Override
    public MessageType receive(int milliseconds) throws IOException, InterruptedIOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        int timeRemaining = milliseconds;
        while (timeRemaining > 0) {
            synchronized (this.lock) {
                MessageType message = this.innerChannel.receive(timeRemaining);
                if (this.filter.isMatch(message)) {
                        return message;
                }
                timeRemaining = milliseconds - (int)(System.currentTimeMillis() - startTime);
            }
        }
        throw new InterruptedIOException("receive operation timed-out");
    }

}
