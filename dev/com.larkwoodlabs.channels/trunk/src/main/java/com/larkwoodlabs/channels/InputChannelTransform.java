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
 * @param <InnerMessageType>
 * @param <OuterMessageType>
 *
 * @author gbumgard@cisco.com
 */
public final class InputChannelTransform<InnerMessageType, OuterMessageType>
                   extends InputChannelAdapter<InnerMessageType, OuterMessageType> {


    /*-- Member Variables ----------------------------------------------------*/

    /**
     * A message transform object that will convert messages produced by
     * the inner channel into the messages that are produced by this channel.
     */
    protected final MessageTransform<InnerMessageType, OuterMessageType> transform;
    

    /*-- Member Functions ----------------------------------------------------*/
;
    /**
     * Constructs an input channel that uses the specified transform object to
     * transform messages produced by the specified inner input channel into the
     * message produced by this channel.
     * @param innerChannel
     * @param transform
     */
    public InputChannelTransform(final InputChannel<InnerMessageType> innerChannel,
                                 final MessageTransform<InnerMessageType, OuterMessageType> transform) {
        super(innerChannel);
        this.transform = transform;
    }

    @Override
    public OuterMessageType receive(int milliseconds) throws IOException, InterruptedIOException, InterruptedException {
        return this.transform.transform(this.innerChannel.receive(milliseconds));
    }

}
