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
 * Interface exposed by all message output channels.
 * A message output channel provides the means for sending a message
 * to a message sink via the {@link MessageOutput#send(MessageType, int)} method.
 *
 * @param <MessageType> - The message object type.
 *
 * @author Gregory Bumgardner
 */
public interface OutputChannel<MessageType>
                 extends MessageOutput<MessageType> {


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Closes this channel and optionally closes any channels wrapped or attached to this channel.
     * @throws IOException - The close operation has failed.
     * @throws IllegalStateException - The close request is not allowed in current state.
     * @throws InterruptedException - The calling thread was interrupted before the close operation could complete.
     */
    public void close() throws IOException, InterruptedException;

}
