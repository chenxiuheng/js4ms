package org.js4ms.amt.gateway;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * AmtTunnelEndpoint.java [org.js4ms.jsdk:amt]
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
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.amt.message.AmtMembershipQueryMessage;
import org.js4ms.amt.message.AmtMembershipUpdateMessage;
import org.js4ms.amt.message.AmtMessage;
import org.js4ms.amt.message.AmtMulticastDataMessage;
import org.js4ms.amt.message.AmtRelayAdvertisementMessage;
import org.js4ms.amt.message.AmtRelayDiscoveryMessage;
import org.js4ms.amt.message.AmtRequestMessage;
import org.js4ms.amt.message.AmtTeardownMessage;
import org.js4ms.common.util.logging.Log;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.io.channel.OutputChannel;
import org.js4ms.io.net.UdpDatagram;
import org.js4ms.io.net.UdpInputChannel;
import org.js4ms.io.net.UdpOutputChannel;
import org.js4ms.io.net.UdpSocketEndpoint;
import org.js4ms.ip.IPMessage;
import org.js4ms.ip.IPPacket;
import org.js4ms.ip.ipv4.IPv4Packet;
import org.js4ms.ip.ipv6.IPv6Packet;
import org.js4ms.ip.protocol.igmp.IGMPMessage;
import org.js4ms.ip.protocol.igmp.IGMPQueryMessage;
import org.js4ms.ip.protocol.igmp.IGMPv2ReportMessage;
import org.js4ms.ip.protocol.igmp.IGMPv3QueryMessage;
import org.js4ms.ip.protocol.igmp.IGMPv3ReportMessage;
import org.js4ms.ip.protocol.mld.MLDMessage;
import org.js4ms.ip.protocol.mld.MLDQueryMessage;
import org.js4ms.ip.protocol.mld.MLDv1ReportMessage;
import org.js4ms.ip.protocol.mld.MLDv2QueryMessage;
import org.js4ms.ip.protocol.mld.MLDv2ReportMessage;




/**
 * An AmtTunnelEndpoint executes the AMT protocol by exchanging AMT messages with
 * an AMT relay.
 * An AmtTunnelEndpoint provides functions for registering {@link OutputChannel} objects
 * to receive packets containing IGMP/MLD messages and multicast data extracted from the
 * Membership Query and Multicast Data messages.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
class AmtTunnelEndpoint
                implements Runnable {

    enum Protocol {
        IPv4,
        IPv6
    };

    /*-- Static Variables ---------------------------------------------------*/

    /**
     * The static logger instance for this class.
     */
    public static final Logger logger = Logger.getLogger(AmtTunnelEndpoint.class.getName());

    /**
     * The IANA assigned port used when sending AMT messages.
     */
    static final short AMT_PORT = 2268;

    static final long DISCOVERY_RETRY_PERIOD = 10 * 1000; // 10 secs

    static final long REQUEST_RETRY_PERIOD = 10 * 1000; // 10 secs

    static final int MAX_REASSEMBLY_CACHE_SIZE = 100;

    /*-- Member Variables ---------------------------------------------------*/

    protected final Log log = new Log(this);

    /**
     * Object used for synchronizing access to state variables.
     */
    private final Object lock = new Object();

    private OutputChannel<IPPacket> dispatchChannel = null;

    private UdpSocketEndpoint udpEndpoint;

    private UdpOutputChannel udpOutputChannel;

    private UdpInputChannel udpInputChannel;

    private final AmtMessage.Parser amtMessageParser;

    private Thread handlerThread;

    private boolean isRunning = false;

    private final InetAddress relayDiscoveryAddress;

    private final Timer taskTimer;

    private TimerTask discoveryTask = null;

    private AmtRelayDiscoveryMessage lastDiscoveryMessageSent = null;

    private long discoveryRetransmissionInterval = DISCOVERY_RETRY_PERIOD;

    private int discoveryMaxRetransmissions = Integer.MAX_VALUE;

    private int discoveryRetransmissionCount = 0;

    private AmtRelayAdvertisementMessage lastAdvertisementMessageReceived = null;

    private InetAddress relayAddress = null;

    private Protocol protocol = null;

    private TimerTask requestTask = null;

    private AmtRequestMessage lastRequestMessageSent = null;

    private long requestRetransmissionInterval = REQUEST_RETRY_PERIOD;

    private int requestMaxRetransmissions = 2;

    private int requestRetransmissionCount = 0;

    private AmtMembershipQueryMessage lastQueryMessageReceived = null;

    private InetSocketAddress lastGatewayAddress;

    private TimerTask periodicRequestTask = null;

    private int queryInterval = 125000;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * @param relayDiscoveryAddress
     * @param dispatchChannel
     *            The channel that will receive packets extracted from Membership Query
     *            and Multicast Data messages.
     * @throws IOException
     */
    protected AmtTunnelEndpoint(final InetAddress relayDiscoveryAddress,
                                final OutputChannel<IPPacket> incomingPacketChannel,
                                Protocol protocol) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.AmtTunnelEndpoint",
                                        Logging.address(relayDiscoveryAddress), incomingPacketChannel));
        }

        this.taskTimer = new Timer("AMT Tunnel Endpoint");

        this.relayDiscoveryAddress = relayDiscoveryAddress;

        this.amtMessageParser = AmtMessage.constructAmtGatewayParser();
        this.protocol = protocol;

        this.dispatchChannel = incomingPacketChannel;

        start();

    }

    /**
     * @throws InterruptedException
     * @throws IOException
     */
    public final void close() throws InterruptedException, IOException {
        stop();
    }

    /**
     * @throws IOException
     * @throws InterruptedException
     */
    private void start() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.start"));
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.log.msg("starting AMT tunnel endpoint"));
        }

        synchronized (this.lock) {

            if (!this.isRunning) {

                this.udpEndpoint = new UdpSocketEndpoint(0);

                this.udpEndpoint.connect(new InetSocketAddress(this.relayDiscoveryAddress, AMT_PORT));

                this.udpOutputChannel = new UdpOutputChannel(this.udpEndpoint);
                this.udpInputChannel = new UdpInputChannel(this.udpEndpoint);

                this.isRunning = true;

                this.lastDiscoveryMessageSent = null;
                this.lastAdvertisementMessageReceived = null;
                this.lastRequestMessageSent = null;
                this.lastQueryMessageReceived = null;

                this.handlerThread = new Thread(this, this.toString());
                this.handlerThread.setDaemon(true);

                this.handlerThread.start();

                startRelayDiscoveryTask();

            }
        }
    }

    /**
     * @throws InterruptedException
     * @throws IOException
     */
    private void stop() throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.stop"));
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.log.msg("stopping AMT tunnel endpoint"));
        }

        synchronized (this.lock) {

            if (this.isRunning) {

                this.isRunning = false;

                stopTasks();

                // Close the endpoint to abort the read operation on socket
                this.udpEndpoint.close(true);

                this.handlerThread.interrupt();

            }
        }
    }

    /**
     * @param packet
     * @throws IOException
     * @throws InterruptedException
     */
    void send(final IPPacket packet) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.sendUpdate", packet));
        }

        boolean performPeriodicUpdates = false;

        if (packet.getVersion() == IPv4Packet.INTERNET_PROTOCOL_VERSION) {
            if (this.protocol == Protocol.IPv6) {
                throw new ProtocolException("cannot send an IPv6 packet over an IPv4 tunnel");
            }
            IPMessage ipMessage = packet.getProtocolMessage(IGMPMessage.IP_PROTOCOL_NUMBER);
            if (ipMessage == null || !(ipMessage instanceof IGMPv3ReportMessage || ipMessage instanceof IGMPv2ReportMessage)) {
                throw new ProtocolException("IPv4 packet rejected because it does not contain an IGMP report");
            }
            performPeriodicUpdates = ((IGMPv3ReportMessage) ipMessage).getNumberOfGroupRecords() > 0;
        }
        else {
            if (this.protocol == Protocol.IPv4) {
                throw new ProtocolException("cannot send an IPv4 packet over an IPv6 tunnel");
            }
            IPMessage ipMessage = packet.getProtocolMessage(MLDMessage.IP_PROTOCOL_NUMBER);
            if (ipMessage == null || !(ipMessage instanceof MLDv1ReportMessage || ipMessage instanceof MLDv2ReportMessage)) {
                throw new ProtocolException("IPv6 packet rejected because it does not contain an MLD report");
            }
            performPeriodicUpdates = ((MLDv2ReportMessage) ipMessage).getNumberOfGroupRecords() > 0;
        }

        synchronized (this.lock) {

            /*
             * We can only send the update message if a tunnel has been established;
             * The gateway interface must receive a relay advertisement and initial query
             * message before it can send an update.
             * We can simply discard the update message if we cannot yet send it to the
             * relay, since the first message that we will receive from the relay will be
             * a
             * general query that will trigger generation of a new update message.
             */
            if (this.lastDiscoveryMessageSent == null) {
                logger.info(this.log.msg("cannot send AMT update message because AMT discovery message has not been sent"));
                startRelayDiscoveryTask();
                return;
            }
            else if (this.lastAdvertisementMessageReceived == null) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(this.log
                                    .msg("cannot send AMT update message because no AMT relay has responded to the last AMT discovery message"));
                }
                return;
            }
            else if (this.lastRequestMessageSent == null) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(this.log.msg("cannot send AMT update message because AMT request message has not been sent"));
                }
                return;
            }
            else if (this.lastQueryMessageReceived == null) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(this.log
                                    .msg("cannot send AMT update message because the AMT relay has not responded to the last AMT request"));
                }
                return;
            }
        }

        AmtMembershipUpdateMessage message = new AmtMembershipUpdateMessage(this.lastQueryMessageReceived.getResponseMac(),
                                                                            this.lastQueryMessageReceived.getRequestNonce(), packet);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.log.msg("sending AMT Membership Update Message"));
            if (logger.isLoggable(Level.FINEST)) {
                message.log(Level.FINEST);
            }
        }

        send(this.relayAddress, message);

        if (performPeriodicUpdates) {
            this.startPeriodicRequestTask(this.queryInterval);
        }

    }

    /**
     * @param message
     * @throws IOException
     * @throws InterruptedException
     */
    private void send(final InetAddress relayAddress, final AmtMessage message) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.send", message));
        }

        /*
         * if (logger.isLoggable(Level.FINE)) {
         * logger.fine(this.log.msg("sending " + message.getClass().getSimpleName());
         * if (logger.isLoggable(Level.FINEST)) {
         * message.log(logger);
         * }
         * }
         */

        ByteBuffer buffer = ByteBuffer.allocate(message.getTotalLength());
        message.writeTo(buffer);
        buffer.flip();

        send(new UdpDatagram(relayAddress, AMT_PORT, buffer));

    }

    /**
     * @param datagram
     * @throws IOException
     * @throws InterruptedException
     */
    private void send(final UdpDatagram datagram) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.send", datagram));
            if (logger.isLoggable(Level.FINEST)) {
                datagram.log(logger,Level.FINEST);
            }
        }

        try {
            this.udpOutputChannel.send(datagram, Integer.MAX_VALUE);
        }
        catch (PortUnreachableException e) {

            if (logger.isLoggable(Level.FINE)) {
                logger.info(this.log.msg("unable send datagram to relay " +
                                         Logging.address(this.relayAddress) +
                                         " - " + e.getClass().getSimpleName() + ":" + e.getMessage()));
            }

            // Restart relay discovery process to locate another relay
            stopTasks();
            startRelayDiscoveryTask();
        }
        catch (IOException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(this.log.msg("attempt to send datagram containing AMT message failed - " + e.getClass().getName() + " "
                                         + e.getMessage()));
            }
            throw e;
        }
        catch (InterruptedException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(this.log.msg("attempt to send datagram interrupted"));
            }
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @param interval
     */
    private void startRelayDiscoveryTask() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.startRelayDiscoveryTask"));
        }

        synchronized (this.lock) {

            this.lastDiscoveryMessageSent = null;

            if (this.discoveryTask != null) {
                this.discoveryTask.cancel();
            }

            this.discoveryTask = new TimerTask() {

                @Override
                public void run() {
                    if (AmtTunnelEndpoint.logger.isLoggable(Level.FINER)) {
                        AmtTunnelEndpoint.logger.finer(AmtTunnelEndpoint.this.log.msg("running discovery task"));
                    }
                    synchronized (AmtTunnelEndpoint.this.lock) {
                        try {
                            if (AmtTunnelEndpoint.this.discoveryRetransmissionCount < AmtTunnelEndpoint.this.discoveryMaxRetransmissions) {
                                AmtTunnelEndpoint.this.discoveryRetransmissionCount++;
                                AmtTunnelEndpoint.this.sendRelayDiscoveryMessage();
                            }
                            else {
                                AmtTunnelEndpoint.logger
                                                .info(AmtTunnelEndpoint.this.log
                                                                .msg("maximum allowable relay discovery message retransmissions exceeded"));
                                AmtTunnelEndpoint.this.discoveryRetransmissionCount = 0;
                                AmtTunnelEndpoint.this.lastDiscoveryMessageSent = null;
                                this.cancel();
                            }
                        }
                        catch (Exception e) {
                            AmtTunnelEndpoint.logger.warning(AmtTunnelEndpoint.this.log
                                            .msg("attempt to send AMT Relay Discovery Message failed - "
                                                 + e.getMessage()));
                            AmtTunnelEndpoint.this.discoveryRetransmissionCount = 0;
                            AmtTunnelEndpoint.this.lastDiscoveryMessageSent = null;
                            this.cancel();
                        }
                    }
                }
            };

            // Schedule relay discovery task for immediate execution with short retry
            // period
            this.taskTimer.schedule(this.discoveryTask, 0, this.discoveryRetransmissionInterval);
        }
    }

    /**
     * @throws InterruptedException
     * @throws IOException
     * @throws Exception
     */
    private void sendRelayDiscoveryMessage() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.sendRelayDiscoveryMessage"));
        }

        synchronized (this.lock) {

            if (this.lastDiscoveryMessageSent == null) {
                this.lastDiscoveryMessageSent = new AmtRelayDiscoveryMessage();
                this.lastAdvertisementMessageReceived = null;
                this.lastRequestMessageSent = null;
                this.lastQueryMessageReceived = null;
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(this.log.msg("sending AMT Relay Discovery Message: relay-discovery-address="
                                         + Logging.address(this.relayDiscoveryAddress)));
                if (logger.isLoggable(Level.FINEST)) {
                    this.lastDiscoveryMessageSent.log(Level.FINEST);
                }
            }

            send(this.relayDiscoveryAddress, this.lastDiscoveryMessageSent);

        }

    }

    /**
     * @param interval
     */
    private void startRequestTask() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.startRequestTask"));
        }

        synchronized (this.lock) {

            if (this.lastAdvertisementMessageReceived == null) {
                startRelayDiscoveryTask();
                return;
            }

            this.lastRequestMessageSent = null;

            if (this.requestTask != null) {
                this.requestTask.cancel();
            }

            this.requestTask = new TimerTask() {

                @Override
                public void run() {
                    if (AmtTunnelEndpoint.logger.isLoggable(Level.FINER)) {
                        AmtTunnelEndpoint.logger.finer(AmtTunnelEndpoint.this.log.msg("running request task"));
                    }
                    synchronized (AmtTunnelEndpoint.this.lock) {
                        try {
                            if (AmtTunnelEndpoint.this.requestRetransmissionCount < AmtTunnelEndpoint.this.requestMaxRetransmissions) {
                                AmtTunnelEndpoint.this.requestRetransmissionCount++;
                                AmtTunnelEndpoint.this.sendRequestMessage();
                            }
                            else {

                                // The relay did not respond with a query within the
                                // retransmission interval
                                AmtTunnelEndpoint.logger
                                                .info(AmtTunnelEndpoint.this.log
                                                                .msg("maximum allowable request message retransmissions exceeded"));
                                AmtTunnelEndpoint.this.requestRetransmissionCount = 0;
                                AmtTunnelEndpoint.this.lastRequestMessageSent = null;
                                this.cancel();

                                // Restart discovery process to locate another relay
                                AmtTunnelEndpoint.this.startRelayDiscoveryTask();
                            }
                        }
                        catch (Exception e) {
                            // Schedule request task for immediate execution with short
                            // retry period
                            AmtTunnelEndpoint.logger.warning(AmtTunnelEndpoint.this.log
                                            .msg("attempt to send AMT Request Message failed - "
                                                 + e.getMessage()));
                            AmtTunnelEndpoint.this.requestRetransmissionCount = 0;
                            this.cancel();
                        }
                    }
                }
            };

            this.taskTimer.schedule(this.requestTask, 0, this.requestRetransmissionInterval);
        }
    }

    /**
     * @throws IOException
     * @throws InterruptedException
     */
    private void sendRequestMessage() throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.sendRequestMessage"));
        }

        synchronized (this.lock) {

            if (this.lastRequestMessageSent == null) {
                this.lastRequestMessageSent = new AmtRequestMessage(this.protocol == Protocol.IPv6);
                this.lastQueryMessageReceived = null;
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(this.log.msg("sending AMT Request Message"));
                if (logger.isLoggable(Level.FINEST)) {
                    this.lastRequestMessageSent.log(Level.FINEST);
                }
            }

            send(this.relayAddress, this.lastRequestMessageSent);
        }

    }

    /**
     * @param delay
     */
    void startPeriodicRequestTask(final long delay) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.startPeriodicRequestTask", delay));
        }

        synchronized (this.lock) {

            if (this.periodicRequestTask != null) {
                this.periodicRequestTask.cancel();
            }

            this.periodicRequestTask = new TimerTask() {

                @Override
                public void run() {
                    if (AmtTunnelEndpoint.logger.isLoggable(Level.FINER)) {
                        AmtTunnelEndpoint.logger.finer(AmtTunnelEndpoint.this.log.msg("running request task"));
                    }
                    AmtTunnelEndpoint.this.startRequestTask();
                }
            };

            this.taskTimer.schedule(this.periodicRequestTask, delay);
        }
    }

    /**
     * 
     */
    private void stopTasks() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.stopTasks"));
        }

        synchronized (this.lock) {

            if (this.periodicRequestTask != null) {
                this.periodicRequestTask.cancel();
                this.periodicRequestTask = null;
            }

            if (this.requestTask != null) {
                this.requestTask.cancel();
                this.requestTask = null;
            }

            if (this.discoveryTask != null) {
                this.discoveryTask.cancel();
                this.discoveryTask = null;
            }

            this.lastDiscoveryMessageSent = null;
            this.lastAdvertisementMessageReceived = null;
            this.lastRequestMessageSent = null;
            this.lastQueryMessageReceived = null;
        }

    }

    /**
     * @param message
     * @throws IOException
     * @throws InterruptedException
     */
    private void handleAdvertisementMessage(final AmtRelayAdvertisementMessage message) throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.handleAdvertisementMessage", message));
        }

        synchronized (this.lock) {

            if (message.getDiscoveryNonce() != this.lastDiscoveryMessageSent.getDiscoveryNonce()) {

                logger.info(this.log.msg("received unexpected AMT Relay Advertisement Message: discovery-nonce=" +
                                         message.getDiscoveryNonce() +
                                         " expected-nonce=" +
                                         this.lastDiscoveryMessageSent.getDiscoveryNonce()));

                // Let the relay discovery process continue
                return;
            }
            else {

                if (this.discoveryTask != null) {
                    this.discoveryTask.cancel();
                    this.discoveryTask = null;
                }

                this.discoveryRetransmissionCount = 0;
                this.lastAdvertisementMessageReceived = message;
                this.relayAddress = InetAddress.getByAddress(message.getRelayAddress());

                try {
                    this.udpEndpoint.connect(new InetSocketAddress(this.relayAddress, AMT_PORT));
                }
                catch (UnknownHostException e) {
                    throw new Error(e);
                }

                // Initiate request/query/report handshake with the relay so we
                // have a response MAC and nonce for reception state change reports
                startRequestTask();

            }
        }

        if (logger.isLoggable(Level.INFO)) {
            logger.info(this.log.msg("interface connected to AMT Relay " + Logging.address(this.relayAddress)));
        }

    }

    /**
     * @param message
     * @throws IOException
     * @throws InterruptedException
     */
    private void handleQueryMessage(final AmtMembershipQueryMessage message) throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.handleQueryMessage", message));
        }

        synchronized (this.lock) {

            if (message.getRequestNonce() != this.lastRequestMessageSent.getRequestNonce()) {
                logger.info(this.log.msg("received unexpected AMT Membership Query Message: request-nonce=" +
                                         message.getRequestNonce() +
                                         " expected-nonce=" +
                                         this.lastRequestMessageSent.getRequestNonce()));
                return;
            }

            if (this.requestTask != null) {
                this.requestTask.cancel();
                this.requestTask = null;
            }

            this.requestRetransmissionCount = 0;

            if (message.getGatewayAddressFlag()) {
                InetSocketAddress gatewayAddress = message.getGatewayAddress();
                if (this.lastGatewayAddress != null) {
                    if (!this.lastGatewayAddress.equals(gatewayAddress)) {

                        // The source address for the request message has changed since
                        // the last request
                        // This implies that the relay will construct a new session when
                        // the Membership Update is sent
                        // Here we'll send a Teardown message to destroy the old session
                        AmtTeardownMessage teardown = new AmtTeardownMessage(this.lastQueryMessageReceived.getResponseMac(),
                                                                             this.lastQueryMessageReceived.getRequestNonce(),
                                                                             this.lastGatewayAddress);

                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine(this.log.msg("sending AMT Teardown Message"));
                            if (logger.isLoggable(Level.FINEST)) {
                                message.log(Level.FINEST);
                            }
                        }

                        send(this.relayAddress, teardown);
                    }
                }

                this.lastGatewayAddress = message.getGatewayAddress();
            }

            this.lastQueryMessageReceived = message;
        }

        // Save interval for periodic queries
        IPPacket packet = message.getPacket();

        if (packet.getVersion() == IPv4Packet.INTERNET_PROTOCOL_VERSION) {

            IPMessage ipMessage = packet.getProtocolMessage(IGMPMessage.IP_PROTOCOL_NUMBER);

            if (ipMessage == null || !(ipMessage instanceof IGMPQueryMessage)) {
                throw new ProtocolException("IP packet does not contain an IGMP Membership Query Message");
            }

            IGMPQueryMessage queryMessage = (IGMPQueryMessage) ipMessage;
            this.queryInterval = 125000; // Default query interval
            if (queryMessage instanceof IGMPv3QueryMessage) {
                IGMPv3QueryMessage v3QueryMessage = (IGMPv3QueryMessage) queryMessage;
                this.queryInterval = v3QueryMessage.getQueryIntervalTime() * 1000;
            }
        }
        else if (packet.getVersion() == IPv6Packet.INTERNET_PROTOCOL_VERSION) {

            IPMessage ipMessage = packet.getProtocolMessage(MLDMessage.IP_PROTOCOL_NUMBER);

            if (ipMessage == null || !(ipMessage instanceof MLDQueryMessage)) {
                throw new ProtocolException("IP packet does not contain an MLD Membership Query Message");
            }

            MLDQueryMessage queryMessage = (MLDQueryMessage) ipMessage;
            this.queryInterval = 125000; // Default query interval
            if (queryMessage instanceof MLDv2QueryMessage) {
                MLDv2QueryMessage v2QueryMessage = (MLDv2QueryMessage) queryMessage;
                this.queryInterval = v2QueryMessage.getQueryIntervalTime() * 1000;
            }
        }
        else {
            throw new ProtocolException("IP packet does not contain an IGMP Membership Query Message");
        }

        // Forward the IGMP/MLD general query packet to the output channel (the
        // AmtPseudoInterface)
        this.dispatchChannel.send(message.getPacket(), Integer.MAX_VALUE);
    }

    /**
     * @param message
     * @throws InterruptedException
     * @throws InterruptedIOException
     */
    private void handleDataMessage(final AmtMulticastDataMessage message) throws InterruptedException, InterruptedIOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.handleDataMessage", message));
        }

        try {
            // Forward the IP packet to the output channel (the AmtPseudoInterface)
            this.dispatchChannel.send(message.getPacket(), Integer.MAX_VALUE);
        }
        catch (InterruptedIOException e) {
            logger.fine(this.log.msg("attempt to send AMT multicast data packet was interrupted"));
            // Re-throw as this thread was interrupted in an IO operation and as is likely
            // shutting down
            throw e;
        }
        catch (IOException e) {
            logger.fine(this.log.msg("attempt to send AMT multicast data packet failed - " + e.getClass().getName() + ":"
                                     + e.getMessage()));
            // Continue on...
        }
        catch (InterruptedException e) {
            logger.fine(this.log.msg("thread attempting to send AMT multicast data packet was interrupted"));
            // Re-throw as this thread has been interrupted.
            throw e;
        }
    }

    @Override
    public void run() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtTunnelEndpoint.run"));
        }

        while (this.isRunning) {

            if (logger.isLoggable(Level.FINER)) {
                logger.finer(this.log.msg("waiting to receive AMT message..."));
            }

            UdpDatagram inputDatagram = null;

            try {
                inputDatagram = this.udpInputChannel.receive(Integer.MAX_VALUE);
            }
            catch (InterruptedIOException e) {
                logger.fine(this.log.msg("I/O operation interrupted - exiting message hander thread"));
                break;
            }
            catch (InterruptedException e) {
                logger.fine(this.log.msg("thread interrupted - exiting message hander thread"));
                break;
            }
            catch (SocketException e) {
                logger.warning(this.log.msg("receive operation failed - " + e.getClass().getSimpleName() + ":" + e.getMessage()));
                break;
            }
            catch (Exception e) {
                logger.severe(this.log.msg("receive operation failed unexpectedly - " + e.getClass().getSimpleName() + ":" + e.getMessage()));
                e.printStackTrace();
                throw new Error(e);
            }

            if (this.isRunning) {

                AmtMessage message = null;

                try {

                    message = (AmtMessage) amtMessageParser.parse(inputDatagram.getPayload());

                    if (message instanceof AmtMulticastDataMessage) {
                        if (logger.isLoggable(Level.FINER)) {
                            logger.fine(this.log.msg("received AMT message AmtMulticastDataMessage"));
                            if (logger.isLoggable(Level.FINEST)) {
                                message.log(Level.FINEST);
                            }
                        }
                    }
                    else {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine(this.log.msg("received AMT message " + message.getClass().getSimpleName()));
                            if (logger.isLoggable(Level.FINEST)) {
                                message.log(Level.FINEST);
                            }
                        }
                    }

                    switch (message.getType()) {

                        case AmtMulticastDataMessage.MESSAGE_TYPE:
                            handleDataMessage((AmtMulticastDataMessage) message);
                            break;

                        case AmtMembershipQueryMessage.MESSAGE_TYPE:
                            handleQueryMessage((AmtMembershipQueryMessage) message);
                            break;

                        case AmtRelayAdvertisementMessage.MESSAGE_TYPE:
                            handleAdvertisementMessage((AmtRelayAdvertisementMessage) message);
                            break;

                        default:
                            if (logger.isLoggable(Level.FINE)) {
                                logger.fine(this.log.msg("ignoring AMT message " + message.getClass().getSimpleName()));
                            }
                            break;
                    }
                }
                catch (InterruptedIOException e) {
                    logger.info(this.log.msg("I/O operation interrupted - exiting message hander thread"));
                    break;
                }
                catch (InterruptedException e) {
                    logger.info(this.log.msg("thread interrupted - exiting message hander thread"));
                    break;
                }
                catch (Exception e) {
                    logger.severe(this.log.msg("message handler failed unexpectedly - " + e.getClass().getSimpleName() + ":"
                                               + e.getMessage()));
                    e.printStackTrace();
                    throw new Error(e);
                }
            }
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.msg("exiting message handler thread"));
        }
    }

}
