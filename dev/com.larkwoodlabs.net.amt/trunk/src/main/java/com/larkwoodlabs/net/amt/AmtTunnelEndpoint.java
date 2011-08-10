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

package com.larkwoodlabs.net.amt;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.udp.UdpDatagram;
import com.larkwoodlabs.net.udp.UdpInputChannel;
import com.larkwoodlabs.net.udp.UdpOutputChannel;
import com.larkwoodlabs.net.udp.UdpSocketEndpoint;
import com.larkwoodlabs.util.logging.Logging;

public final class AmtTunnelEndpoint implements Runnable {

        /*-- Static Variables ---------------------------------------------------*/
    
    /**
     * The static {@link Logger} instance for this class.
     */
    public static final Logger logger = Logger.getLogger(AmtTunnelEndpoint.class.getName());

    /**
     * The IANA assigned port used when sending AMT messages.
     */
    public static final short AMT_PORT = 2268;
    
    public static final long DISCOVERY_RETRY_PERIOD = 10*1000; // 10 secs
    public static final long REQUEST_RETRY_PERIOD = 10*1000; // 10 secs

    public static final int MAX_REASSEMBLY_CACHE_SIZE = 100;
    

    /*-- Member Variables ---------------------------------------------------*/
    
    private final String ObjectId = Logging.identify(this);

    /**
     * Object used for synchronizing access to state variables.
     */
    private final Object lock = new Object();

    private final AmtInterface amtInterface;
    private OutputChannel<IPPacket> incomingQueryChannel = null;
    private final OutputChannel<IPPacket> outgoingUpdateChannel;
    private OutputChannel<IPPacket> incomingDataChannel = null;
    
    private UdpSocketEndpoint udpEndpoint;
    private UdpOutputChannel udpOutputChannel;
    private UdpInputChannel udpInputChannel;

    private final AmtMessage.Parser amtMessageParser;

    private Thread handlerThread;
    private boolean isRunning = false;

    private final InetAddress relayDiscoveryAddress;
    
    private final Timer taskTimer;

    private boolean isUpdating = false;
    
    private TimerTask discoveryTask = null;
    private AmtRelayDiscoveryMessage lastDiscoveryMessageSent = null;
    private long discoveryRetransmissionInterval = DISCOVERY_RETRY_PERIOD;
    private int discoveryMaxRetransmissions = Integer.MAX_VALUE;
    private int discoveryRetransmissionCount = 0;

    private AmtRelayAdvertisementMessage lastAdvertisementMessageReceived = null;
    private InetAddress relayAddress = null;

    private TimerTask requestTask = null;
    private AmtRequestMessage lastRequestMessageSent = null;
    private long requestRetransmissionInterval = REQUEST_RETRY_PERIOD;
    private int requestMaxRetransmissions = 2;
    private int requestRetransmissionCount = 0;

    private AmtMembershipQueryMessage lastQueryMessageReceived = null;
    private InetSocketAddress lastGatewayAddress;

    private TimerTask periodicRequestTask = null;

    

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param amtInterface
     * @param relayDiscoveryAddress
     * @param taskTimer
     */
    public AmtTunnelEndpoint(final AmtInterface amtInterface, final InetAddress relayDiscoveryAddress, final Timer taskTimer) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId,
                                          "AmtTunnelEndpoint.AmtTunnelEndpoint",
                                          Logging.address(relayDiscoveryAddress),
                                          taskTimer));
        }

        this.amtInterface = amtInterface;
        this.relayDiscoveryAddress = relayDiscoveryAddress;
        this.amtMessageParser = AmtMessage.constructAmtGatewayParser();

        this.outgoingUpdateChannel = new OutputChannel<IPPacket>() {
            @Override
            public void send(IPPacket packet, int milliseconds) throws IOException, InterruptedException {
                sendUpdate(packet);
            }

            @Override
            public void close() {
            }
        };

        this.taskTimer = taskTimer;

   }


    /**
     * Sets the destination channel for query packets extracted from AMT membership query messsages.
     * @param incomingDataChannel
     */
    public void setIncomingQueryChannel(final OutputChannel<IPPacket> incomingQueryChannel) {
        this.incomingQueryChannel = new Defragmenter(incomingQueryChannel, MAX_REASSEMBLY_CACHE_SIZE, this.taskTimer);
    }

    /**
     * Returns the channel that accepts IGMP and MLD reports for encapsulation
     * into AmtMembershipUpdateMessages.
     * @return
     */
    public OutputChannel<IPPacket> getOutgoingUpdateChannel() {
        return this.outgoingUpdateChannel;
    }

    /**
     * Sets the destination channel for data packets extracted from AMT multicast data messages.
     * @param incomingDataChannel
     */
    public void setIncomingDataChannel(final OutputChannel<IPPacket> incomingDataChannel) {
        this.incomingDataChannel = new Defragmenter(incomingDataChannel, MAX_REASSEMBLY_CACHE_SIZE, this.taskTimer);
    }

    /**
     * 
     * @throws IOException
     * @throws InterruptedException 
     */
    public void start() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.start"));
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(ObjectId + " starting AMT tunnel endpoint");
        }

        synchronized (this.lock) {
            
            if (!this.isRunning) {

                this.udpEndpoint = new UdpSocketEndpoint(0);

                this.udpEndpoint.connect(new InetSocketAddress(this.relayDiscoveryAddress,AMT_PORT));

                this.udpOutputChannel = new UdpOutputChannel(this.udpEndpoint);
                this.udpInputChannel = new UdpInputChannel(this.udpEndpoint);

                this.isRunning = true;

                this.lastDiscoveryMessageSent = null;
                this.lastAdvertisementMessageReceived = null;
                this.lastRequestMessageSent = null;
                this.lastQueryMessageReceived = null;

                this.handlerThread = new Thread(this,this.toString());
                this.handlerThread.setDaemon(true);

                this.handlerThread.start();

            }
        }
    }

    /**
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    public void stop() throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.stop"));
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(ObjectId + " stopping AMT tunnel endpoint");
        }

        synchronized (this.lock) {

            if (this.isRunning) {

                this.isRunning = false;

                stopUpdates();

                // Close the endpoint to abort the read operation on socket
                this.udpEndpoint.close(true);

                this.handlerThread.interrupt();

            }
        }
    }

    /**
     * 
     * @param packet
     * @throws IOException
     * @throws InterruptedException
     */
    private void sendUpdate(final IPPacket packet) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.sendUpdate", packet));
        }

        synchronized (this.lock) {

            /*
             * We can only send the update message if a tunnel has been established;
             * The gateway interface must receive a relay advertisement and initial query message before
             * it can send an update.
             * We can simply discard the update message if we cannot yet send it to the relay,
             * since the first message that we will receive from the relay will be a general query
             * that will trigger generation of a new update message.
             */

            this.isUpdating = true;

            if (this.lastDiscoveryMessageSent == null) {
                logger.info(ObjectId + " cannot send AMT update message because AMT discovery message has not been sent");
                startRelayDiscoveryTask();
                return;
            }
            else if (this.lastAdvertisementMessageReceived == null) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(ObjectId + " cannot send AMT update message because no AMT relay has responded to the last AMT discovery message");
                }
                return;
            }
            else if (this.lastRequestMessageSent == null) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(ObjectId + " cannot send AMT update message because AMT request message has not been sent");
                }
                return;
            }
            else if (this.lastQueryMessageReceived == null) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(ObjectId + " cannot send AMT update message because the AMT relay has not responded to the last AMT request");
                }
                return;
            }
        }

        AmtMembershipUpdateMessage message = new AmtMembershipUpdateMessage(this.lastQueryMessageReceived.getResponseMac(),
                                                                            this.lastQueryMessageReceived.getRequestNonce(), packet);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(ObjectId + " sending AMT Membership Update Message");
            if (logger.isLoggable(Level.FINEST)) {
                message.log();
            }
        }

        send(this.relayAddress, message);
    }

    /**
     * 
     * @param message
     * @throws IOException
     * @throws InterruptedException
     */
    private void send(final InetAddress relayAddress, final AmtMessage message) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.send", message));
        }

        /*
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(ObjectId + " sending " + message.getClass().getSimpleName());
            if (logger.isLoggable(Level.FINEST)) {
                message.log(logger);
            }
        }
        */

        ByteBuffer buffer = ByteBuffer.allocate(message.getTotalLength());
        message.writeTo(buffer);
        buffer.flip();

        send(new UdpDatagram(relayAddress, AMT_PORT, buffer));
        
    }

    /**
     * 
     * @param datagram
     * @throws IOException
     * @throws InterruptedException
     */
    private void send(final UdpDatagram datagram) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.send", datagram));
            if (logger.isLoggable(Level.FINEST)) {
                datagram.log(logger);
            }
        }

        try {
            this.udpOutputChannel.send(datagram, Integer.MAX_VALUE);
        }
        catch (PortUnreachableException e) {

            if (logger.isLoggable(Level.FINE)) {
                logger.info(ObjectId +
                            " unable send datagram to relay " +
                            Logging.address(this.relayAddress) +
                            " - " + e.getClass().getSimpleName() + ":" + e.getMessage());
            }

            // Restart relay discovery process to locate another relay
            stopUpdates();
            this.isUpdating = true;
            
            startRelayDiscoveryTask();
        }
        catch (IOException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " attempt to send datagram containing AMT message failed - " + e.getClass().getName() + " " + e.getMessage());
            }
            throw e;
        }
        catch (InterruptedException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " attempt to send datagram interrupted");
            }
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 
     * @param interval
     */
    void startRelayDiscoveryTask() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.startRelayDiscoveryTask"));
        }

        synchronized (this.lock) {

            this.lastDiscoveryMessageSent = null;

            if (!this.isUpdating) {
                return;
            }

            if (this.discoveryTask != null) {
                this.discoveryTask.cancel();
            }
    
            this.discoveryTask = new TimerTask() {
                @Override
                public void run() {
                    if (AmtTunnelEndpoint.logger.isLoggable(Level.FINER)) {
                        AmtTunnelEndpoint.logger.finer(ObjectId + " running discovery task");
                    }
                    synchronized (AmtTunnelEndpoint.this.lock) {
                        try {
                            if (!AmtTunnelEndpoint.this.isUpdating) {
                                this.cancel();
                            }
                            else if (AmtTunnelEndpoint.this.discoveryRetransmissionCount < AmtTunnelEndpoint.this.discoveryMaxRetransmissions) {
                                AmtTunnelEndpoint.this.discoveryRetransmissionCount++;
                                AmtTunnelEndpoint.this.sendRelayDiscoveryMessage();
                            }
                            else {
                                AmtTunnelEndpoint.logger.info(ObjectId + " maximum allowable relay discovery message retransmissions exceeded");
                                AmtTunnelEndpoint.this.discoveryRetransmissionCount = 0;
                                AmtTunnelEndpoint.this.lastDiscoveryMessageSent = null;
                                this.cancel();
                            }
                        }
                        catch (Exception e) {
                            AmtTunnelEndpoint.logger.warning(ObjectId + " attempt to send AMT Relay Discovery Message failed - " + e.getMessage());
                            AmtTunnelEndpoint.this.discoveryRetransmissionCount = 0;
                            AmtTunnelEndpoint.this.lastDiscoveryMessageSent = null;
                            this.cancel();
                        }
                    }
                }
            };
    
            // Schedule relay discovery task for immediate execution with short retry period
            this.taskTimer.schedule(this.discoveryTask, 0, this.discoveryRetransmissionInterval);
        }
    }

    /**
     * 
     * @throws InterruptedException 
     * @throws IOException 
     * @throws Exception
     */
    private void sendRelayDiscoveryMessage() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.sendRelayDiscoveryMessage"));
        }

        synchronized (this.lock) {

            if (this.lastDiscoveryMessageSent == null) {
                this.lastDiscoveryMessageSent = new AmtRelayDiscoveryMessage();
                this.lastAdvertisementMessageReceived = null;
                this.lastRequestMessageSent = null;
                this.lastQueryMessageReceived = null;
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " sending AMT Relay Discovery Message: relay-discovery-address="+Logging.address(this.relayDiscoveryAddress));
                if (logger.isLoggable(Level.FINEST)) {
                    this.lastDiscoveryMessageSent.log();
                }
            }

            send(this.relayDiscoveryAddress, this.lastDiscoveryMessageSent);

        }

    }

    /**
     * 
     * @param interval
     */
    void startRequestTask() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.startRequestTask"));
        }

        synchronized (this.lock) {

            if (this.lastAdvertisementMessageReceived == null) {
                startRelayDiscoveryTask();
                return;
            }

            this.lastRequestMessageSent = null;

            if (!this.isUpdating) {
                return;
            }

            if (this.requestTask != null) {
                this.requestTask.cancel();
            }
    
            this.requestTask = new TimerTask() {
                @Override
                public void run() {
                    if (AmtTunnelEndpoint.logger.isLoggable(Level.FINER)) {
                        AmtTunnelEndpoint.logger.finer(ObjectId + " running request task");
                    }
                    synchronized (AmtTunnelEndpoint.this.lock) {
                        try {
                            if (!AmtTunnelEndpoint.this.isUpdating) {
                                this.cancel();
                            }
                            else if (AmtTunnelEndpoint.this.requestRetransmissionCount < AmtTunnelEndpoint.this.requestMaxRetransmissions) {
                                AmtTunnelEndpoint.this.requestRetransmissionCount++;
                                AmtTunnelEndpoint.this.sendRequestMessage();
                            }
                            else {
    
                                // The relay did not respond with a query within the retransmission interval
                                AmtTunnelEndpoint.logger.info(ObjectId + " maximum allowable request message retransmissions exceeded");
                                AmtTunnelEndpoint.this.requestRetransmissionCount = 0;
                                AmtTunnelEndpoint.this.lastRequestMessageSent = null;
                                this.cancel();
    
                                // Restart discovery process to locate another relay
                                AmtTunnelEndpoint.this.startRelayDiscoveryTask();
                            }
                        }
                        catch (Exception e) {
                            // Schedule request task for immediate execution with short retry period
                            AmtTunnelEndpoint.logger.warning(ObjectId + " attempt to send AMT Request Message failed - " + e.getMessage());
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
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    private void sendRequestMessage() throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.sendRequestMessage"));
        }

        synchronized (this.lock) {

            if (this.lastRequestMessageSent == null) {
                this.lastRequestMessageSent = new AmtRequestMessage();
                this.lastQueryMessageReceived = null;
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " sending AMT Request Message");
                if (logger.isLoggable(Level.FINEST)) {
                    this.lastRequestMessageSent.log();
                }
            }

            send(this.relayAddress, this.lastRequestMessageSent);
        }

    }


    /**
     * 
     * @param interval
     */
    void startPeriodicRequestTask(final long delay) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.startPeriodicRequestTask",delay));
        }

        synchronized (this.lock) {

            if (!this.isUpdating) {
                return;
            }

            if (this.periodicRequestTask != null) {
                this.periodicRequestTask.cancel();
            }
    
            this.periodicRequestTask = new TimerTask() {
                @Override
                public void run() {
                    if (AmtTunnelEndpoint.logger.isLoggable(Level.FINER)) {
                        AmtTunnelEndpoint.logger.finer(ObjectId + " running request task");
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
    public void stopUpdates() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.stopPeriodicRequestTask"));
        }

        synchronized (this.lock) {
            
            this.isUpdating = false;
            
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
     * 
     * @param message
     * @throws IOException
     * @throws InterruptedException
     */
    private void handleAdvertisementMessage(final AmtRelayAdvertisementMessage message) throws IOException, InterruptedException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.handleAdvertisementMessage", message));
        }

        synchronized (this.lock) {

            if (message.getDiscoveryNonce() != this.lastDiscoveryMessageSent.getDiscoveryNonce()) {

                logger.info(ObjectId +
                            " received unexpected AMT Relay Advertisement Message: discovery-nonce=" + 
                            message.getDiscoveryNonce() + 
                            " expected-nonce=" + 
                            this.lastDiscoveryMessageSent.getDiscoveryNonce());
                
                // Let the relay discovery process continue
                return;
            }
            else {

                this.discoveryRetransmissionCount = 0;
                this.lastAdvertisementMessageReceived = message;
                this.relayAddress = InetAddress.getByAddress(message.getRelayAddress());

                try {
                    this.udpEndpoint.connect(new InetSocketAddress(this.relayAddress,AMT_PORT));
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
            logger.info(ObjectId + " interface connected to AMT Relay " + Logging.address(this.relayAddress));
        }
        
    }

    /**
     * 
     * @param message
     * @throws IOException
     * @throws InterruptedException
     */
    private void handleQueryMessage(final AmtMembershipQueryMessage message) throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.handleQueryMessage", message));
        }

        synchronized (this.lock) {

            if (message.getRequestNonce() != this.lastRequestMessageSent.getRequestNonce()) {
                logger.info(ObjectId +
                            " received unexpected AMT Membership Query Message: request-nonce=" + 
                            message.getRequestNonce() + 
                            " expected-nonce=" + 
                            this.lastRequestMessageSent.getRequestNonce());
                return;
            }

            this.requestRetransmissionCount = 0;

            if (message.getGatewayAddressFlag()) {
                InetSocketAddress gatewayAddress = message.getGatewayAddress();
                if (this.lastGatewayAddress != null) {
                    if (!this.lastGatewayAddress.equals(gatewayAddress)) {

                        // The source address for the request message has changed since the last request
                        // This implies that the relay will construct a new session when the Membership Update is sent
                        // Here we'll send a Teardown message to destroy the old session
                        AmtTeardownMessage teardown = new AmtTeardownMessage(this.lastQueryMessageReceived.getResponseMac(),
                                                                             this.lastQueryMessageReceived.getRequestNonce(),
                                                                             this.lastGatewayAddress);

                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine(ObjectId + " sending AMT Teardown Message");
                            if (logger.isLoggable(Level.FINEST)) {
                                message.log();
                            }
                        }

                        send(this.relayAddress, teardown);
                    }
                }

                this.lastGatewayAddress = message.getGatewayAddress();
            }

            this.lastQueryMessageReceived = message;
        }

        // Send the IGMP/MLD general query back to the interface membership manager
        this.incomingQueryChannel.send(message.getPacket(), Integer.MAX_VALUE);
    }

    /**
     * 
     * @param message
     * @throws InterruptedException
     * @throws InterruptedIOException
     */
    private void handleDataMessage(final AmtMulticastDataMessage message) throws InterruptedException, InterruptedIOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.handleDataMessage", message));
        }
        
        try {
            this.incomingDataChannel.send(message.getPacket(), Integer.MAX_VALUE);
        }
        catch (InterruptedIOException e) {
            logger.fine(ObjectId + " attempt to send AMT multicast data packet was interrupted");
            // Re-throw as this thread was interrupted in an IO operation and as is likely shutting down
            throw e;
        }
        catch (IOException e) {
            logger.fine(ObjectId + " attempt to send AMT multicast data packet failed - " + e.getClass().getName() + ":" + e.getMessage());
            // Continue on...
        }
        catch (InterruptedException e) {
            logger.fine(ObjectId + " thread attempting to send AMT multicast data packet was interrupted");
            // Re-throw as this thread has been interrupted.
            throw e;
        }
    }

    
    @Override
    public void run() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.run"));
        }
        
        while (this.isRunning) {

            if (logger.isLoggable(Level.FINER)) {
                logger.finer(ObjectId + " waiting to receive AMT message...");
            }

            UdpDatagram inputDatagram = null;
            
            try {
                inputDatagram = this.udpInputChannel.receive(Integer.MAX_VALUE);
            }
            catch (InterruptedIOException e) {
                logger.info(ObjectId + " I/O operation interrupted - exiting message hander thread");
                break;
            }
            catch (SocketException e) {
                logger.info(ObjectId + " receive operation interrupted - exiting message hander thread");
                break;
            }
            catch (Exception e) {
                logger.severe(ObjectId + " receive operation failed unexpectedly - " + e.getClass().getSimpleName() + ":" + e.getMessage());
                e.printStackTrace();
                throw new Error(e);
            }

            if (this.isRunning) {

                AmtMessage message = null;

                try {

                    message = (AmtMessage)amtMessageParser.parse(inputDatagram.getPayload());

                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(ObjectId + " received AMT message "+message.getClass().getSimpleName());
                        if (logger.isLoggable(Level.FINEST)) {
                            message.log();
                        }
                    }

                    switch (message.getType()) {

                    case AmtMulticastDataMessage.MESSAGE_TYPE:
                        handleDataMessage((AmtMulticastDataMessage)message);
                        break;

                    case AmtMembershipQueryMessage.MESSAGE_TYPE:
                        handleQueryMessage((AmtMembershipQueryMessage)message);
                        break;

                    case AmtRelayAdvertisementMessage.MESSAGE_TYPE:
                        handleAdvertisementMessage((AmtRelayAdvertisementMessage)message);
                        break;

                    default:
                        if (logger.isLoggable(Level.INFO)) {
                            logger.info(ObjectId + " ignoring AMT message " + message.getClass().getSimpleName());
                        }
                        break;
                    }
                }
                catch (InterruptedIOException e) {
                    logger.info(ObjectId + " I/O operation interrupted - exiting message hander thread");
                    break;
                }
                catch (InterruptedException e) {
                    logger.info(ObjectId + " thread interrupted - exiting message hander thread");
                    break;
                }
                catch (Exception e) {
                    logger.severe(ObjectId + " message handler failed unexpectedly - " + e.getClass().getSimpleName() + ":" + e.getMessage());
                    e.printStackTrace();
                    throw new Error(e);
                }
            }
        }

        // Shutdown the AmtInterface to force it leave all joined streams.
        try {
            this.amtInterface.shutdown();
        }
        catch (InterruptedException e) {
            // Ignore
        }
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(ObjectId + " exiting message handler thread");
        }
    }

}
