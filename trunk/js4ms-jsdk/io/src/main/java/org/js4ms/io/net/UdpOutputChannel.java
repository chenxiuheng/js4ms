package org.js4ms.io.net;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * UdpOutputChannel.java [org.js4ms.jsdk:io]
 * %%
 * Copyright (C) 2009 - 2014 Cisco Systems, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.io.IOException;

import org.js4ms.io.channel.OutputChannel;



/**
 * An {@link OutputChannel} that can be used to send {@link UdpDatagram} instances to a
 * {@link UdpEndpoint}.
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class UdpOutputChannel
                implements OutputChannel<UdpDatagram> {

    /*-- Member Variables ----------------------------------------------------*/

    private final UdpEndpoint endpoint;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a UDP input channel bound the the specified endpoint.
     * 
     * @param endpoint
     *            The destination for datagrams sent to this channel.
     */
    public UdpOutputChannel(final UdpEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public final void send(final UdpDatagram message, final int milliseconds) throws IOException, InterruptedException {
        this.endpoint.send(message, milliseconds);
    }

    /**
     * Closes this channel. This implementation does nothing.
     * Call {@link UdpEndpoint#close()} to close the UDP endpoint.
     */
    @Override
    public final void close() {
        // NO-OP
    }

}
