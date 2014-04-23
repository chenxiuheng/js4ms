package org.js4ms.io.net;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * UdpDatagramPayloadSource.java [org.js4ms.jsdk:io]
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
import java.nio.ByteBuffer;

import org.js4ms.io.channel.ChannelPump;
import org.js4ms.io.channel.MessageSource;
import org.js4ms.io.channel.MessageTransform;
import org.js4ms.io.channel.OutputChannel;
import org.js4ms.io.channel.OutputChannelTransform;



/**
 * A {@link MessageSource} that receives UDP packets via a {@link UdpEndpoint} and
 * sends the datagram payload to an {@link OutputChannel}.
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public class UdpDatagramPayloadSource
                extends MessageSource<ByteBuffer> {

    /*-- Inner Classes -------------------------------------------------------*/

    /**
     * Transform used to extract the datagram payload from a {@link UdpDatagram}.
     */
    final static class Transform
                    implements MessageTransform<UdpDatagram, ByteBuffer> {

        public Transform() {
        }

        @Override
        public ByteBuffer transform(final UdpDatagram message) throws IOException {
            return message.getPayload();
        }
    }

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    private final ChannelPump<UdpDatagram> pump;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a packet source that will receive UDP datagrams from a UDP endpoint
     * and send them to an {@link OutputChannel}.
     */
    public UdpDatagramPayloadSource(final UdpEndpoint udpEndpoint,
                                    final OutputChannel<ByteBuffer> outputChannel) throws IOException {
        super(outputChannel);

        this.pump = new ChannelPump<UdpDatagram>(new UdpInputChannel(udpEndpoint),
                                                 new OutputChannelTransform<UdpDatagram, ByteBuffer>(outputChannel,
                                                                                                     new Transform()));
    }

    @Override
    protected void doStart() throws IOException, InterruptedException {
        this.pump.start();
    }

    @Override
    protected void doStop() throws IOException, InterruptedException {
        this.pump.stop(Integer.MAX_VALUE);
    }

    @Override
    protected void doClose() throws IOException, InterruptedException {
        this.pump.close();
    }

}
