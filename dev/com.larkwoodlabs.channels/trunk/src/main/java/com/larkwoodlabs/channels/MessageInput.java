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
 * Interface used to receive a message from a message source. 
 *
 * @param <MessageType> - The message object type.
 *
 * @author gbumgard@cisco.com
 */
public interface MessageInput<MessageType> {
    
    /**
     * Special message object used to indicate that the
     * source has stopped producing messages.
     */
    public static final Object EOM = new Object();

    /**
     * Attempts to retrieve a message from a message source and if necessary,
     * waits the specified amount of time until a message becomes available.<p>
     * Some implementations may choose to return the static Object {@link #EOM}
     * to indicate that the source has stopped producing messages.
     * @param milliseconds - The amount of time allotted to complete the operation.
     * @return A <code>MessageType</code> object or the object {@link #EOM}.
     * @throws IOException - The receive operation has failed.
     * @throws InterruptedIOException - The receive operation was interrupted or timed out.
     * @throws InterruptedException - The calling thread was interrupted before the receive operation could complete.
     */
    MessageType receive(int milliseconds) throws IOException,
                                                 InterruptedIOException,
                                                 InterruptedException;

}
