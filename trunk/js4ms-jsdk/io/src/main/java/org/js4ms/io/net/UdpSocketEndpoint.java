package org.js4ms.io.net;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * UdpSocketEndpoint.java [org.js4ms.jsdk:io]
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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Logging;


/**
 * A {@link UdpEndpoint} implementation that uses a DatagramChannel to provide transport.
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public final class UdpSocketEndpoint
                implements UdpEndpoint {

    /*-- Static Variables ---------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(UdpSocketEndpoint.class.getName());

    /*-- Member Variables ---------------------------------------------------*/

    protected final Object receiveLock = new Object();

    protected final DatagramSocket socket;

    protected final String ObjectId = Logging.identify(this);

    protected InetSocketAddress localHostBinding;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param socket
     */
    public UdpSocketEndpoint(final DatagramSocket socket) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "UdpSocketEndpoint.UdpSocketEndpoint", socket));
        }
        this.socket = socket;
        this.localHostBinding = (InetSocketAddress) this.socket.getLocalSocketAddress();
    }

    public UdpSocketEndpoint(final int port) throws IOException {
        this(new InetSocketAddress(port));
        this.localHostBinding = (InetSocketAddress) this.socket.getLocalSocketAddress();
    }

    /**
     * @param localHostBinding
     * @throws IOException
     */
    public UdpSocketEndpoint(final InetSocketAddress localHostBinding) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "UdpSocketEndpoint.UdpSocketEndpoint", Logging.address(localHostBinding)));
        }

        this.socket = new DatagramSocket(localHostBinding);

        this.localHostBinding = localHostBinding;
    }

    /**
     * Connects to the remote host address to eliminate the address security check
     * that occurs for each channel I/O operation when a channel is not connected.
     * 
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
     * Connects to the remote host address to eliminate the address security check
     * that occurs for each channel I/O operation when a channel is not connected.
     * 
     * @param remoteSocketAddress
     * @throws IOException
     */
    public void connect(final InetSocketAddress remoteSocketAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "UdpSocketEndpoint.connect", Logging.address(remoteSocketAddress)));
        }

        this.socket.connect(remoteSocketAddress);
    }

    /**
     * @throws IOException
     */
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
     * 
     * @param milliseconds
     *            The amount of time to allow for the receive operation to complete.
     * @return A new UdpDatagram instance. The destination address and port is the one
     *         used
     *         to construct the end-point and not that of the actual datagram (not
     *         available in Java API).
     * @throws IOException
     *             The receive operation failed because there was an IO error,
     *             the receive was interrupted or the endpoint was closed.
     */
    public final UdpDatagram receive(final int milliseconds) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "UdpSocketEndpoint.receive", milliseconds));
        }

        // TODO max size really is 65507 for UDP over IP
        byte[] buffer = new byte[8192];
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(ObjectId + " waiting to receive datagram");
        }

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        // Only allow one receiver thread at a time to access the socket to preserve the
        // timeout setting
        synchronized (this.receiveLock)
        {
            // Set the timeout prior to starting the receive
            // A value of 0 results in an infinite timeout.
            this.socket.setSoTimeout(milliseconds);

            try {
                this.socket.receive(packet);
            }
            catch (IOException e) {
                logger.warning(ObjectId + " socket receive failed with an IO exception - " + e.getClass().getSimpleName() + ":"
                            + e.getMessage());
                throw e;
            }
            catch (Exception e) {
                logger.severe(ObjectId + " socket receive failed with an unexpected exception - " + e.getClass().getSimpleName()
                            + ":" + e.getMessage());
                throw new Error(e);
            }
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(ObjectId +
                        " received datagram packet from " +
                        Logging.address(packet.getSocketAddress()) +
                        " length=" + packet.getLength());
        }

        /*
         * Workaround for situation where socket binding address and packet source address
         * are not of the same type (IPv4 vs. IPv6).
         * TODO: Fix this in code that constructs the endpoint.
         */
        if (!packet.getAddress().getClass().equals(this.localHostBinding.getAddress().getClass())) {
            if (this.localHostBinding.getAddress().isAnyLocalAddress()) {
                this.localHostBinding = new InetSocketAddress(InetAddress.getByAddress(new byte[packet.getAddress().getAddress().length]),
                                                              this.localHostBinding.getPort());
            }
        }

        return new UdpDatagram((InetSocketAddress) packet.getSocketAddress(),
                               (InetSocketAddress) this.localHostBinding,
                               ByteBuffer.wrap(buffer, 0, packet.getLength()));
    }

    /**
     * Sends the datagram payload to the destination addresss and port specified in the
     * datagram.
     * 
     * @param datagram
     *            The UdpDatagram whose payload will be sent.
     * @param milliseconds
     *            The amount of time to allow for the send operation to complete.
     *            Ignored in this class.
     * @throws IOException
     *             The send operation failed because there was an IO error, the send was
     *             interrupted or the endpoint was closed.
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

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(ObjectId + " datagram payload buffer=" + payload.array() + " offset=" + payload.arrayOffset() + " limit="
                         + payload.limit() + " remaining=" + payload.remaining());
            logger.fine(ObjectId + " sending DatagramPacket to " + Logging.address(datagram.getDestinationSocketAddress())
                        + " length=" + payload.limit());
        }

        this.socket.send(packet);
    }

    /**
     * @return
     */
    public final InetSocketAddress getLocalSocketAddress() {
        return (InetSocketAddress) this.socket.getLocalSocketAddress();
    }

    /**
     * @return
     */
    public final InetSocketAddress getRemoteSocketAddress() {
        return (InetSocketAddress) this.socket.getRemoteSocketAddress();
    }
}
