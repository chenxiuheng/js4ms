package org.js4ms.amt.gateway;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * AmtMulticastEndpoint.java [org.js4ms.jsdk:amt]
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
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.io.channel.MessageQueue;
import org.js4ms.io.channel.OutputChannel;
import org.js4ms.io.channel.OutputChannelPipe;
import org.js4ms.io.net.MulticastEndpoint;
import org.js4ms.io.net.UdpDatagram;



/**
 * A {@link MulticastEndpoint} that uses AMT to request and receive multicast
 * datagrams sent to a specific multicast group address and UDP port number.
 * The endpoint provides "push" and "pull" methods for datagram delivery.
 * If an {@link OutputChannel} is attached to the endpoint, incoming datagrams
 * are pushed into the output channel as they arrive. If no output channel is
 * provided, the incoming datagrams are queued internally and retrieved by calling
 * the {@link #receive(int)} method.
 * <p>
 * A single multicast endpoint may be used to join both IPv4 and IPv6 multicast groups
 * irrespective of the relay discovery address used.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class AmtMulticastEndpoint
                implements MulticastEndpoint {

    /*-- Static Variables ---------------------------------------------------*/

    /**
     * That static logger used to trace member function calls in this class.
     */
    public static final Logger logger = Logger.getLogger(AmtMulticastEndpoint.class.getName());

    /*-- Member Variables ---------------------------------------------------*/

    private final Log log = new Log(this);

    /**
     * AMT interface used to handle joins on group addresses.
     */
    private AmtUDPInterface udpInterface = null;

    private final int port;

    private final InetAddress relayDiscoveryAddress;

    /**
     * This is the channel that the AMT interface will push datagrams into.
     * This channel may be constructed externally or internally.
     * If the channel is constructed externally and supplied to a constructor,
     * then the {@link #receive(int)} method will throw an IOException since
     * datagrams are being delivered directly to the external channel.
     * If an external channel is not used, this class will construct an internal
     * buffer that can be read using the {@link #receive(int)} method.
     * The size of the internal buffer is set in the constructor.
     */
    private OutputChannel<UdpDatagram> pushChannel;

    /**
     * The queue used to buffer datagrams. Not used if a push channel is used.
     */
    private MessageQueue<UdpDatagram> datagramQueue = null;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs an instance that buffers incoming UDP datagrams in
     * an internal queue that can be read using the {@link #receive(int)} method.
     * 
     * @param port
     *            The endpoint will forward datagrams sent to this port number.
     *            Additional port numbers may be included by calling an appropriate join
     *            method, e.g. {@link #join(InetAddress, int)}.
     * @param bufferCapacity
     *            The desired internal queue size.
     */
    public AmtMulticastEndpoint(final int port,
                                final int bufferCapacity) {
        this(port, AmtUDPInterfaceManager.getDefaultRelayDiscoveryAddress(), bufferCapacity);
    }

    /**
     * @param port
     *            The endpoint will receive datagrams sent to this port number.
     *            Additional port numbers may be included by calling an appropriate join
     *            method,
     *            e.g. {@link #join(InetAddress, int)}.
     * @param pushChannel
     *            The endpoint will send incoming datagrams to this channel.
     */
    public AmtMulticastEndpoint(final int port,
                                final OutputChannel<UdpDatagram> pushChannel) {
        this(port, AmtUDPInterfaceManager.getDefaultRelayDiscoveryAddress(), pushChannel);
    }

    /**
     * @param port
     *            The endpoint will forward datagrams sent to this port number.
     *            Additional port numbers may be included by calling an appropriate join
     *            method, e.g. {@link #join(InetAddress, int)}.
     * @param relayDiscoveryAddress
     *            The address (anycast or unicast) that the endpoint will use to
     *            locate an AMT relay that can be used to join specific multicast
     *            group(s).
     * @param bufferCapacity
     */
    public AmtMulticastEndpoint(final int port,
                                final InetAddress relayDiscoveryAddress,
                                final int bufferCapacity) {
        this(port, relayDiscoveryAddress);
        this.datagramQueue = new MessageQueue<UdpDatagram>(bufferCapacity);
        this.pushChannel = new OutputChannelPipe<UdpDatagram>(this.datagramQueue);
    }

    /**
     * @param port
     *            The endpoint will forward datagrams sent to this port number.
     *            Additional port numbers may be included by calling an appropriate join
     *            method, e.g. {@link #join(InetAddress, int)}.
     * @param relayDiscoveryAddress
     *            The address (anycast or unicast) that the endpoint will use to
     *            locate an AMT relay that can be used to join specific multicast
     *            group(s).
     * @param pushChannel
     */
    public AmtMulticastEndpoint(final int port,
                                final InetAddress relayDiscoveryAddress,
                                final OutputChannel<UdpDatagram> pushChannel) {
        this(port, relayDiscoveryAddress);
        this.pushChannel = pushChannel; // TODO wrap channel to intercept exceptions so
                                        // channel can leave AMT interface
    }

    /**
     * @param port
     *            The endpoint will forward datagrams sent to this port number.
     *            Additional port numbers may be included by calling an appropriate join
     *            method, e.g. {@link #join(InetAddress, int)}.
     * @param relayDiscoveryAddress
     *            The address (anycast or unicast) that the endpoint will use to
     *            locate an AMT relay that can be used to join specific multicast
     *            group(s).
     */
    protected AmtMulticastEndpoint(final int port,
                                   final InetAddress relayDiscoveryAddress) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMulticastEndpoint.AmtMulticastEndpoint",
                                        port,
                                        Logging.address(relayDiscoveryAddress)));
        }

        this.port = port;
        this.relayDiscoveryAddress = relayDiscoveryAddress;
    }

    /**
     * Closes the endpoint.
     * 
     * @throws IOException
     *             The AMT interface(s) used by this endpoint has reported an I/O error.
     * @throws InterruptedException
     *             The calling thread was interrupted while waiting for the
     *             underlying AMT interface to complete an operation.
     */
    public final void close() throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMulticastEndpoint.close"));
        }

        if (this.udpInterface != null) {
            this.udpInterface.leave(this.pushChannel);
            this.udpInterface.release();
        }
    }

    /**
     * Gets the UDP port number assigned to the endpoint when it was constructed.
     * 
     * @return An integer port number in the range 0-32767.
     */
    public final int getPort() {
        return this.port;
    }

    /**
     * Gets the relay discovery address assigned to the endpoint when it was constructed.
     * 
     * @return An IPv4 or IPv6 address.
     */
    public final InetAddress getRelayDiscoveryAddress() {
        return this.relayDiscoveryAddress;
    }

    @Override
    public final void join(final InetAddress groupAddress) throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMulticastEndpoint.join", Logging.address(groupAddress)));
        }

        join(groupAddress, this.port);

    }

    @Override
    public final void join(final InetAddress groupAddress, final int port) throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMulticastEndpoint.join", Logging.address(groupAddress), port));
        }

        if (this.udpInterface == null) {
            this.udpInterface = AmtUDPInterfaceManager.getInstance().getInterface(this.relayDiscoveryAddress);
        }
        this.udpInterface.join(this.pushChannel, groupAddress, port);
    }

    @Override
    public final void join(final InetAddress groupAddress, final InetAddress sourceAddress) throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMulticastEndpoint.join", Logging.address(groupAddress),
                                        Logging.address(sourceAddress)));
        }

        join(groupAddress, sourceAddress, this.port);
    }

    @Override
    public final void join(final InetAddress groupAddress, final InetAddress sourceAddress, final int port) throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMulticastEndpoint.join", Logging.address(groupAddress),
                                        Logging.address(sourceAddress), port));
        }

        if (this.udpInterface == null) {
            this.udpInterface = AmtUDPInterfaceManager.getInstance().getInterface(this.relayDiscoveryAddress);
        }
        this.udpInterface.join(this.pushChannel, groupAddress, sourceAddress, port);
    }

    @Override
    public final void leave(final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMulticastEndpoint.leave", Logging.address(groupAddress)));
        }

        if (this.udpInterface != null) {
            this.udpInterface.leave(this.pushChannel, groupAddress);
        }
    }

    @Override
    public final void leave(final InetAddress groupAddress, final InetAddress sourceAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMulticastEndpoint.leave", Logging.address(groupAddress),
                                        Logging.address(sourceAddress)));
        }

        leave(groupAddress, sourceAddress, this.port);
    }

    @Override
    public final void leave(final InetAddress groupAddress, final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMulticastEndpoint.leave", Logging.address(groupAddress), port));
        }

        if (this.udpInterface != null) {
            this.udpInterface.leave(this.pushChannel, groupAddress, port);
        }
    }

    @Override
    public final void leave(final InetAddress groupAddress, final InetAddress sourceAddress, final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMulticastEndpoint.join", Logging.address(groupAddress),
                                        Logging.address(sourceAddress), port));
        }

        if (this.udpInterface != null) {
            this.udpInterface.leave(this.pushChannel, groupAddress, sourceAddress, port);
        }
    }

    @Override
    public final void leave() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtMulticastEndpoint.leave"));
        }

        if (this.udpInterface != null) {
            this.udpInterface.leave(this.pushChannel);
        }
    }

    @Override
    public final UdpDatagram receive(final int milliseconds) throws IOException, InterruptedIOException, InterruptedException {
        return this.datagramQueue.receive(milliseconds);
    }

}
