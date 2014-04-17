package org.js4ms.app.reflector;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import org.js4ms.io.channel.MessageSource;
import org.js4ms.io.channel.MessageTransform;
import org.js4ms.io.channel.OutputChannel;
import org.js4ms.io.channel.OutputChannelTransform;
import org.js4ms.io.net.udp.UdpDatagram;
import org.js4ms.net.ip.multicast.service.amt.gateway.AmtDatagramSource;
import org.js4ms.net.ip.multicast.service.proxy.SourceFilter;




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
