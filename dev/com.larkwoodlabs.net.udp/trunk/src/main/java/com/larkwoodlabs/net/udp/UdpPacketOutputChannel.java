package com.larkwoodlabs.net.udp;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.util.logging.Log;

/**
 * An {@link OutputChannel} that can be used to send a byte array containing an
 * RTP/RTCP packet to a specific address and port via UDP.
 *
 * @author gbumgard@cisco.com
 */
public class UdpPacketOutputChannel implements OutputChannel<ByteBuffer> {

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(UdpPacketOutputChannel.class.getName());

    private final Log log = new Log(this);

    private final DatagramSocket socket;
    
    /**
     * Constructs an output channel that can be used to send packets via UDP
     * to the specified destination address and port.
     * @param socket - The datagram socket to use when sending packets.
     * @throws IOException If an I/O occurs during construction of the underlying DatagramSocket.
     */
    public UdpPacketOutputChannel(final DatagramSocket socket) {
        this.socket = socket;
    }

    /**
     * Constructs an output channel that can be used to send packets via UDP
     * to the specified destination address and port.
     * @param remoteAddress - The IP address and port of the receiving host.
     * @throws IOException If an I/O occurs during construction of the underlying DatagramSocket.
     */
    public UdpPacketOutputChannel(InetSocketAddress remoteAddress) throws IOException {
        this.socket = new DatagramSocket(0);
        this.socket.connect(remoteAddress);
    }

    @Override
    public void send(final ByteBuffer packet, final int milliseconds) throws IOException, InterruptedIOException, InterruptedException {

        try {
            this.socket.send(new DatagramPacket(packet.array(),
                                                packet.arrayOffset(),
                                                packet.limit()));
        }
        catch (IOException e) {
            logger.finer(log.msg("attempt to send packet failed with exception: "+e.getClass().getSimpleName()+": "+e.getMessage()));
            throw e;
        }
    }

    @Override
    public void close() throws IOException, InterruptedException {
        this.socket.close();
    }

}
