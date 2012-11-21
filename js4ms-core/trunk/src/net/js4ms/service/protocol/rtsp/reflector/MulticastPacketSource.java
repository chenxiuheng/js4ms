package net.js4ms.service.protocol.rtsp.reflector;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import net.js4ms.channels.MessageSource;
import net.js4ms.channels.MessageTransform;
import net.js4ms.channels.OutputChannel;
import net.js4ms.channels.OutputChannelTransform;
import net.js4ms.net.amt.SourceFilter;
import net.js4ms.net.amt.gateway.AmtDatagramSource;
import net.js4ms.net.udp.UdpDatagram;



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
