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
 * Interface used to send a message to a message sink.
 *
 * @param <MessageType> - The message object type.
 *
 * @author gbumgard@cisco.com
 */
public interface MessageOutput<MessageType> {

    /**
     * Special message object used to indicate that the
     * source has stopped producing messages.
     */
    public static final Object EOM = MessageInput.EOM;

    /**
     * Attempts to send a message to a message sink within a specified amount of time.<p>
     * Some channel implementations may choose to accept the static object
     * {@link #EOM EOM} to indicate that the caller has stopped producing messages.
     * @param message - A <code>MessageType</code> object or the static object
     *                 {@link #EOM EOM} (if supported).
     * @param milliseconds - The amount of time allotted to complete the operation.
     * @throws IOException - The send operation has failed.
     * @throws InterruptedIOException - The send operation was interrupted or timed out.
     * @throws InterruptedException - The calling thread was interrupted before the send operation could complete.
     */
    void send(MessageType message, int milliseconds) throws IOException,
                                                            InterruptedIOException,
                                                            InterruptedException;

}
