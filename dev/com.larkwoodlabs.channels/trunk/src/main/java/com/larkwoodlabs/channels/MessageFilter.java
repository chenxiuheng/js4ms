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

/**
 * Interface exposed by objects used to filter messages based on some
 * form of selection criteria.
 *
 * @param <MessageType> - The message object type.
 *
 * @author Gregory Bumgardner
 */
public interface MessageFilter<MessageType> {

    /**
     * Tests the message to see if it matches filter criteria.
     * @param message - The message to be tested.
     * @return A boolean value indicating whether the message matched.
     */
    boolean isMatch(MessageType message);

}
