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
    
    public static final long DISCOVERY_DELAY = 24*60*60*1000; // 1 DAY
    public static final long DISCOVERY_PERIOD = 24*60*60*1000; // 1 DAY
    public static final long DISCOVERY_RETRY_PERIOD = 10*1000; // 10 secs

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
    private byte[] relayAddress = null;
    
    private final Timer taskTimer;
    private TimerTask requestTask = null;

    private boolean isRequestSent = false;
    private boolean isResponseReceived = false;

    private long requestInterval = 0;
    private int lastDiscoveryNonce;
    private int lastRequestNonceSent;
    private int lastRequestNonceReceived;
    private byte[] lastResponseMac;
    private InetSocketAddress lastGatewayAddress;
    

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
                sendUpdate(packet, milliseconds);
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

                this.udpEndpoint.connect(new InetSocketAddress(InetAddress.getByAddress(this.relayDiscoveryAddress.getAddress()),AMT_PORT));

                this.udpOutputChannel = new UdpOutputChannel(this.udpEndpoint);
                this.udpInputChannel = new UdpInputChannel(this.udpEndpoint);

                this.isRunning = true;
                this.relayAddress = null;
                this.isRequestSent = false;
                this.isResponseReceived = false;

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

                if (this.requestTask != null) {
                    this.requestTask.cancel();
                    this.requestTask = null;
                }

                // Close the endpoint to abort the read operation on socket
                this.udpEndpoint.close(true);

                this.handlerThread.interrupt();

            }
        }
    }

    /**
     * 
     * @param packet
     * @param milliseconds
     * @throws IOException
     * @throws InterruptedException
     */
    private void sendUpdate(final IPPacket packet, final int milliseconds) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.sendUpdate", packet, milliseconds));
        }

        synchronized (this.lock) {

            /*
             * We can only send the update message if a tunnel has been established;
             * The gateway interface must receive a relay advertisement and initial query message before
             * it can send an update.
             * We can simply discard the update message if we cannot yet send it to the relay,
             * since the first message that we will receive from the relay will be a membership query
             * that will trigger generation of a new update message.
             */

            if (this.relayAddress == null) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(ObjectId + " cannot send AMT update message because connection to AMT relay has not been established");
                }
                return;
            }
            else if (!this.isResponseReceived && this.isRequestSent) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(ObjectId + " cannot send AMT update message because the AMT relay has not responded to the last AMT request");
                }
                return;
            }
            else if (!this.isRequestSent) {
                logger.severe(ObjectId + " illegal attempt made to send AMT update message before AMT request has been sent");
                throw new Error("illegal attempt made to send AMT query or update message before AMT request has been sent");
            }
        }

        AmtMembershipUpdateMessage message = new AmtMembershipUpdateMessage(this.lastResponseMac, this.lastRequestNonceReceived, packet);

        send(message);
    }

    /**
     * 
     * @param message
     * @throws IOException
     * @throws InterruptedException
     */
    private void send(final AmtMessage message) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.send", message));
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(ObjectId + " sending " + message.getClass().getSimpleName());
            if (logger.isLoggable(Level.FINEST)) {
                message.log(logger);
            }
        }

        ByteBuffer buffer = ByteBuffer.allocate(message.getTotalLength());
        message.writeTo(buffer);
        buffer.flip();

        send(new UdpDatagram(this.relayAddress, AMT_PORT, buffer));
        
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
            startRelayDiscovery();
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
     * @throws IOException
     * @throws InterruptedException 
     */
    void startRelayDiscovery() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.startRelayDiscovery"));
        }

        synchronized (this.lock) {

            // Reset the state
            this.relayAddress = null;
            this.isRequestSent = false;
            this.isResponseReceived = false;
            this.udpEndpoint.disconnect();

            this.sendRelayDiscoveryMessage();
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

        AmtRelayDiscoveryMessage message = new AmtRelayDiscoveryMessage();

        synchronized (this) {
            this.lastDiscoveryNonce = message.getDiscoveryNonce();
        }

        ByteBuffer buffer = ByteBuffer.allocate(message.getTotalLength());
        message.writeTo(buffer);
        buffer.flip();

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(ObjectId + " sending AMT Relay Discovery Message");
            if (logger.isLoggable(Level.FINEST)) {
                message.log();
            }
        }

        send(new UdpDatagram(this.relayDiscoveryAddress.getAddress(), AMT_PORT, buffer));
    }

    /**
     * 
     * @param interval
     */
    void startRequestTask(final long interval) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.startRequestTask", interval));
        }

        // (Re)schedule request generation task if interval changes
        if (interval != this.requestInterval) {

            this.requestInterval = interval;

            if (this.requestTask != null) {
                this.requestTask.cancel();
            }

            this.requestTask = new TimerTask() {
                @Override
                public void run() {
                    if (AmtTunnelEndpoint.logger.isLoggable(Level.FINER)) {
                        AmtTunnelEndpoint.logger.finer(ObjectId + " running periodic request task");
                    }
                    try {
                        AmtTunnelEndpoint.this.sendRequestMessage();
                    }
                    catch (Exception e) {
                        // Schedule relay discovery task for immediate execution with short retry period
                        AmtTunnelEndpoint.logger.warning(ObjectId + " attempt to send periodic AMT Relay Discovery Message failed - " + e.getMessage());
                    }
                }
            };
    
            this.taskTimer.schedule(this.requestTask, (long)(interval * Math.random()), interval);
        }
    }

    /**
     * 
     */
    public void stopRequestTask() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtTunnelEndpoint.stopRequestTask"));
        }

        if (this.requestTask != null) {
            this.requestTask.cancel();
            this.requestTask = null;
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

        AmtRequestMessage message = new AmtRequestMessage();

        synchronized (this.lock) {
            this.lastRequestNonceSent = message.getRequestNonce();
            this.isRequestSent = true;
        }

        send(message);

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

            if (message.getDiscoveryNonce() != this.lastDiscoveryNonce) {

                logger.warning(ObjectId +
                               " received unexpected AMT Relay Advertisement Message: discovery-nonce=" + 
                               message.getDiscoveryNonce() + 
                               " expected-nonce=" + 
                               this.lastDiscoveryNonce);
                
                // Let the relay discovery process continue
                return;
            }
            else {

                this.isRequestSent = false;
                this.isResponseReceived = false;
                this.relayAddress = message.getRelayAddress();

                try {
                    this.udpEndpoint.connect(new InetSocketAddress(InetAddress.getByAddress(this.relayAddress),AMT_PORT));
                }
                catch (UnknownHostException e) {
                    throw new Error(e);
                }

                // Initiate request/query/report handshake with the relay so we
                // have a response MAC and nonce for reception state change reports
                sendRequestMessage();

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
            this.isResponseReceived = true;
            this.lastRequestNonceReceived = message.getRequestNonce();
            this.lastResponseMac = message.getResponseMac();
            if (message.getGatewayAddressFlag()) {
                InetSocketAddress gatewayAddress = message.getGatewayAddress();
                if (this.lastGatewayAddress != null) {
                    if (!this.lastGatewayAddress.equals(gatewayAddress)) {
                        // The source address for the request message has changed since the last request
                        // This implies that the relay will construct a new session when the Membership Update is sent
                        // Here we'll send a Teardown message to destroy the old session
                        AmtTeardownMessage teardown = new AmtTeardownMessage(this.lastResponseMac, this.lastRequestNonceSent, this.lastGatewayAddress);
                        send(teardown);
                    }
                }
                this.lastGatewayAddress = message.getGatewayAddress();
            }

            if (this.lastRequestNonceReceived != this.lastRequestNonceSent) {
                logger.info(ObjectId + " AMT interface received Query message containing a request nonce that does not match that sent");
            }
        }

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
