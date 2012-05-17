package com.larkwoodlabs.service.protocol.rtsp.rtp;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import com.larkwoodlabs.channels.OutputChannel;

/**
 * An {@link OutputChannel} that can be used to send a byte array containing an
 * RTP/RTCP packet over a TCP connection using the framing method described in RFC-4571.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public class TcpPacketOutputChannel implements OutputChannel<ByteBuffer> {

    private final Socket socket;

    private final Object lock = new Object();

    /**
     * Constructs an output channel that can be used to send packets over the specified connection.
     * @param connection - The TCP connection over which the packets will be sent.
     * @throws IOException 
     */
    public TcpPacketOutputChannel(final InetSocketAddress remoteAddress) throws IOException {
        this.socket = new Socket();
        this.socket.connect(remoteAddress);
    }

    @Override
    public void send(final ByteBuffer packet, final int milliseconds) throws IOException, InterruptedIOException, InterruptedException {

        OutputStream outputStream = this.socket.getOutputStream();

        synchronized (this.lock) {
            int count = packet.limit();
            outputStream.write((byte)((count >> 8) & 0xFF));
            outputStream.write((byte)(count & 0xFF));
            outputStream.write(packet.array(), packet.arrayOffset(), count);
        }
    }

    @Override
    public void close() throws IOException, InterruptedException {
        this.socket.close();
    }
}
