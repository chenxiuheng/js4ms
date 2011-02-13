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
 * Interface exposed by all duplex message channel classes.
 * A duplex channel provides the means for both sending message to and
 * receiving messages from a single endpoint.
 *
 * @param <MessageType> - The message object type.
 *
 * @author gbumgard@cisco.com
 */
public interface DuplexChannel<MessageType> 
                 extends MessageInput<MessageType>, MessageOutput<MessageType> {

    /**
     * Closes this channel and optionally closes any channels wrapped or attached to this channel.
     * @param isCloseAll - Indicates whether attached channels should also be closed.
     * @throws IOException - The close operation has failed.
     */
    public void close(boolean isCloseAll) throws IOException;

}
