package org.js4ms.reflector;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MulticastPacketSource.java [org.js4ms.jsdk:reflector]
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
import java.net.InetAddress;
import java.nio.ByteBuffer;

import org.js4ms.amt.gateway.AmtDatagramSource;
import org.js4ms.amt.proxy.SourceFilter;
import org.js4ms.io.channel.MessageSource;
import org.js4ms.io.channel.MessageTransform;
import org.js4ms.io.channel.OutputChannel;
import org.js4ms.io.channel.OutputChannelTransform;
import org.js4ms.io.net.UdpDatagram;




public class MulticastPacketSource extends MessageSource<ByteBuffer> {

    /*-- Inner Classes -------------------------------------------------------*/

    /**
     * Simple transform that returns the payload of a {@link UdpDatagram} as a result.
     */
    final static class Transform implements MessageTransform<UdpDatagram, ByteBuffer> {

        public Transform() {
        }
        
        @Override
        public ByteBuffer transform(final UdpDatagram message) throws IOException {
            return message.getPayload();
        }
    }

    private final AmtDatagramSource packetSource;

    /**
     * 
     * @param outputChannel
     * @throws IOException 
     */
    public MulticastPacketSource(final int port,
                                 final SourceFilter filter,
                                 final InetAddress relayDiscoveryAddress,
                                 final OutputChannel<ByteBuffer> outputChannel) throws IOException {
        super(outputChannel);
        this.packetSource = new AmtDatagramSource(port,
                                                  filter,
                                                  relayDiscoveryAddress,
                                                  new OutputChannelTransform<UdpDatagram,ByteBuffer>(outputChannel,new Transform()));
    }

    @Override
    protected void doStart() throws IOException, InterruptedException {
        this.packetSource.start();
    }

    @Override
    protected void doStop() throws IOException, InterruptedException {
        this.packetSource.stop();
    }

    @Override
    protected void doClose() throws IOException, InterruptedException {
        this.packetSource.close();
        super.doClose();
    }

}
