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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Logging;

/**
 * A {@link UdpEndpoint} implementation that uses a DatagramChannel to provide transport.
 *
 * @author Gregory Bumgardner
 */
public final class UdpSocketEndpoint
                   implements UdpEndpoint {

    /*-- Static Variables ---------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(UdpSocketEndpoint.class.getName());


    /*-- Member Variables ---------------------------------------------------*/

    protected final Object receiveLock = new Object();
    
    protected final DatagramSocket socket;

    protected final String ObjectId = Logging.identify(this);
    

    /*-- Member Functions ---------------------------------------------------*/

    public UdpSocketEndpoint(final int i) throws IOException {
        this(new InetSocketAddress(i));
    }


    public UdpSocketEndpoint(final InetSocketAddress localHostBinding) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "UdpSocketEndpoint.UdpSocketEndpoint", Logging.address(localHostBinding)));
        }

        this.socket = new DatagramSocket(localHostBinding);
        
    }


    /**
     * Connecting to the remote host address eliminates the address security check
     * that occurs for each channel I/O operation when a channel is not connected.
     * @param remoteHost
     * @param remotePort
     * @throws IOException
     */
    public void connect(final InetAddress remoteHost, final int remotePort) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "UdpSocketEndpoint.connect", Logging.address(remoteHost), remotePort));
        }

        this.socket.connect(remoteHost, remotePort);
    }

    /**
     * Connecting to the remote host address eliminates the address security check
     * that occurs for each channel I/O operation when a channel is not connected.
     * @param remoteSocketAddress
     * @throws IOException
     */
    public void connect(final InetSocketAddress remoteSocketAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "UdpSocketEndpoint.connect", Logging.address(remoteSocketAddress)));
        }

        this.socket.connect(remoteSocketAddress);
    }
    
    public void disconnect() throws IOException {
        this.socket.disconnect();
    }


    @Override
    public void close(boolean isCloseAll) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "UdpSocketEndpoint.close", isCloseAll));
        }

        this.socket.close();
    }

    
    /**
     * Waits to receive a datagram from the socket.
     * @param milliseconds - The amount of time to allow for the receive operation to complete.
     * @return A new UdpDatagram instance. The destination address and port is the one used
     *         to construct the end-point and not that of the actual datagram (not available in Java API).
     * @throws IOException The receive operation failed because there was an IO error,
     *         the receive was interrupted or the endpoint was closed.
     */
    public final UdpDatagram receive(final int milliseconds) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "UdpSocketEndpoint.receive", milliseconds));
        }

        // TODO max size really is 65507 for UDP over IP
        byte[] buffer = new byte[8192];
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(ObjectId + " waiting to receive datagram");
        }

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        // Only allow one receiver thread at a time to access the socket to preserve the timeout setting
        synchronized (this.receiveLock)
        {
            // Set the timeout prior to starting the receive
            // A value of 0 results in an infinite timeout.
            this.socket.setSoTimeout(milliseconds);

            
            try {
                this.socket.receive(packet);
            }
            catch (IOException e) {
                logger.info(ObjectId + " socket receive failed with an IO exception - " + e.getClass().getSimpleName() + ":" + e.getMessage());
                throw e;
            }
            catch (Exception e) {
                logger.info(ObjectId + " socket receive failed with an unexpected exception - " + e.getClass().getSimpleName() + ":" + e.getMessage());
                throw new Error(e);
            }
        }
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(ObjectId + 
                    " received datagram packet from " +
                    Logging.address(packet.getSocketAddress()) +
                    " length=" + packet.getLength());
        }

        return new UdpDatagram((InetSocketAddress)packet.getSocketAddress(),
                               (InetSocketAddress)this.socket.getLocalSocketAddress(),
                               ByteBuffer.wrap(buffer, 0, packet.getLength()));
    }


    /**
     * Sends the datagram payload to the destination addresss and port specified in the datagram.
     * @param datagram - The UdpDatagram whose payload will be sent.
     * @param milliseconds - The amount of time to allow for the send operation to complete. Ignored in this class.
     * @throws IOException The send operation failed because there was an IO error, the send was interrupted or the endpoint was closed.
     */
    @Override
    public final void send(final UdpDatagram datagram, final int milliseconds) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "UdpSocketEndpoint.send", datagram, milliseconds));
        }

        ByteBuffer payload = datagram.getPayload();

        DatagramPacket packet = new DatagramPacket(payload.array(),
                                                   payload.arrayOffset(),
                                                   payload.limit(),
                                                   datagram.getDestinationSocketAddress());

        if (logger.isLoggable(Level.FINE)) {
            logger.finer(ObjectId + " datagram payload buffer="+payload.array()+" offset="+payload.arrayOffset()+" limit="+payload.limit()+" remaining="+payload.remaining());
            logger.fine(ObjectId + " sending DatagramPacket to " + Logging.address(datagram.getDestinationSocketAddress()) + " length=" + payload.limit());
        }

        this.socket.send(packet);
    }


    public final InetSocketAddress getLocalSocketAddress() {
        return (InetSocketAddress)this.socket.getLocalSocketAddress();
    }

    public final InetSocketAddress getRemoteSocketAddress() {
        return (InetSocketAddress)this.socket.getRemoteSocketAddress();
    }
}
