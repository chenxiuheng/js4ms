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
 * 
 * @param <OuterMessageType>
 * @param <InnerMessageType>
 *
 * @author gbumgard@cisco.com
 */
public final class OutputChannelTransform<OuterMessageType, InnerMessageType>
                   extends OutputChannelAdapter<OuterMessageType, InnerMessageType> {


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * A message transform object that will convert messages received by
     * this channel into the messages that are then sent to the inner output channel.
     */
    protected final MessageTransform<OuterMessageType, InnerMessageType> transform;

    public OutputChannelTransform(final OutputChannel<InnerMessageType> innerChannel,
                                  final MessageTransform<OuterMessageType, InnerMessageType> transform) {
        super(innerChannel);
        this.transform = transform;
    }

    @Override
    public void send(final OuterMessageType message, final int milliseconds) throws IOException, InterruptedIOException, InterruptedException {
        this.innerChannel.send(this.transform.transform(message), milliseconds);
    }

}
