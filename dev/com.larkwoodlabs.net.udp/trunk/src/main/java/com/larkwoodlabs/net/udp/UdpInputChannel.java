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

package com.larkwoodlabs.net.udp;

import java.io.IOException;

import com.larkwoodlabs.channels.InputChannel;

/**
 * An {@link InputChannel} that can be used to receive {@link UdpDatagram} instances from a {@link UdpEndpoint}.
 *
 * @author Gregory Bumgardner
 */
public final class UdpInputChannel implements InputChannel<UdpDatagram> {

    /*-- Member Variables ----------------------------------------------------*/

    protected final UdpEndpoint endpoint;
    

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a UDP output channel bound the the specified endpoint.
     * @param endpoint - The source for datagrams received from this channel.
     */
    public UdpInputChannel(final UdpEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public final UdpDatagram receive(final int milliseconds) throws IOException, InterruptedException {
        return endpoint.receive(milliseconds);
    }

    /**
     * Closes this channel. This implementation does nothing.
     * Call {@link UdpEndpoint#close()} to close the endpoint.
     */
    @Override
    public void close(final boolean isCloseAll) {
        // NO-OP
    }
}
