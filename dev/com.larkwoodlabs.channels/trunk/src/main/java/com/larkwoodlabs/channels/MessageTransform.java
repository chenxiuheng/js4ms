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
 * Interface exposed by objects that transform or modify messages.
 *
 * @param <InputMessageType> - The input or upstream message type.
 * @param <OutputMessageType> - The output or downstream message type.
 *
 * @author Gregory Bumgardner
 */
public interface MessageTransform<InputMessageType, OutputMessageType> {

    /**
     * Transforms or modifies the input message to produce an output message.
     * @param message - The message to transform or modify.
     * @return The new or modified message.
     * @throws Exception - The transformation failed.
     */
    OutputMessageType transform(InputMessageType message) throws IOException;

}
