/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: MessageFilter.java (com.larkwoodlabs.channels)
 * 
 * Copyright © 2009-2012 Cisco Systems, Inc.
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

/**
 * Interface exposed by objects used to filter messages based on some
 * form of selection criteria.
 * 
 * @param <MessageType>
 *            The message object type.
 * @author Greg Bumgardner (gbumgard)
 */
public interface MessageFilter<MessageType> {

    /**
     * Tests the message to see if it matches filter criteria.
     * 
     * @param message
     *            The message to be tested.
     * @return A boolean value indicating whether the message matched.
     */
    boolean isMatch(MessageType message);

}
