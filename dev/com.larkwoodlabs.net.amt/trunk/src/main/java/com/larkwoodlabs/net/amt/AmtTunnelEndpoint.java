/*
 * Copyright � 2009-2010 Larkwood Labs Software.
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

final class AmtTunnelEndpoint implements Runnable {

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
    private final OutputChannel<IPPacket> outgoingDataChannel;
    
    private UdpSocketEndpoint udpEndpoint;
    private UdpOutputChannel udpOutputChannel;
    private UdpInputChannel udpInputChannel;

    private final AmtMessage.Parser amtMessageParser;

    private final Thread handlerThread;
    private boolean isRunning = false;

    private final InetAddress relayDiscoveryAddress;
    private byte[] relayAddress = null;
    
    private final Timer taskTimer;
    private TimerTask relayDiscoveryTask = null;
    
    private boolean isRelayDiscoveryStarted = false;
    private boolean isRequestSent = false;
    private boolean isResponseReceived = false;

    private int lastDiscoveryNonce;
    private int lastRequestNonce;
    private byte[] lastResponseMac;
    

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
                                          "AmtMessageEndpoint.AmtMessageEndpoint",
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
            public void close(boolean isCloseAll) {
            }
        };

        this.outgoingDataChannel = new OutputChannel<IPPacket>() {
            @Override
            public void send(IPPacket packet, int milliseconds) throws IOException, InterruptedException {
                sendData(packet, milliseconds);
            }

            @Override
            public void close(boolean isCloseAll) {
            }
        };

        this.handlerThread = new Thread(this,this.toString());
        this.handlerThread.setDaemon(true);

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
     * Returns the channel that accepts IP packets for encapsulation
     * into AMT multicast data messages.
     * @return
     */
    public OutputChannel<IPPacket> getOutgoingDataChannel() {
        return this.outgoingDataChannel;
    }

    /**
     * 
     * @throws IOException
     */
    public void start() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMessageEndpoint.start"));
        }

        synchronized (this.lock) {
            
            if (!this.isRunning) {

                // Build UDP transport for sending and receiving packets from AMT relay
                // Construct end point bound to appropriate wild card address and ethereal port.
                InetAddress wildcardAddress;
                try {
                    wildcardAddress = InetAddress.getByAddress(this.relayDiscoveryAddress.getAddress().length == 4 ? new byte[4] : new byte[16]);
                } catch (UnknownHostException e) {
                    throw new Error(e);
                }
                
                this.udpEndpoint = new UdpSocketEndpoint(new InetSocketAddress(wildcardAddress, 0));
                this.udpOutputChannel = new UdpOutputChannel(this.udpEndpoint);
                this.udpInputChannel = new UdpInputChannel(this.udpEndpoint);

                this.isRunning = true;

                this.handlerThread.start();

                startRelayDiscovery();
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
            logger.finer(Logging.entering(ObjectId, "AmtMessageEndpoint.stop"));
        }

        if (this.relayDiscoveryTask != null) this.relayDiscoveryTask.cancel();

        synchronized (this.lock) {

            if (this.isRunning) {

                if (this.relayDiscoveryTask != null) {
                    this.relayDiscoveryTask.cancel();
                    this.relayDiscoveryTask = null;
                }

                this.isRunning = false;

                // Close the endpoint to abort the read operation on socket
                this.udpEndpoint.close(true);
                
                this.handlerThread.interrupt();

                // GB: commented out for now because I don't think its necessary
                // this.handlerThread.join();

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
    private void sendUpdate(final IPPacket packet, final int milliseconds) throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMessageEndpoint.sendUpdate", packet, milliseconds));
        }
        
        synchronized (this.lock) {
            if (!this.isResponseReceived) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(ObjectId + " cannot send message because the relay has not responded to the last request");
                }
                throw new PortUnreachableException();
            }
        }

        AmtMembershipUpdateMessage message = new AmtMembershipUpdateMessage(this.lastResponseMac, this.lastRequestNonce, packet);
        
        send(message);
    }

    /**
     * 
     * @param packet
     * @param milliseconds
     * @throws IOException
     * @throws InterruptedException
     */
    private void sendData(final IPPacket packet, final int milliseconds) throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMessageEndpoint.sendData", packet, milliseconds));
        }

        synchronized (this.lock) {
            if (!this.isResponseReceived) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(ObjectId + " cannot send message because the relay has not responded to the last request");
                }
                throw new PortUnreachableException();
            }
        }

        AmtMulticastDataMessage message = new AmtMulticastDataMessage(packet);
        
        send(message);
    }

    /**
     * 
     * @param message
     * @throws IOException
     * @throws InterruptedException
     */
    private void send(final AmtMessage message) throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMessageEndpoint.send", message));
            message.log(logger);
        }

        if (this.relayAddress == null) {
            if (logger.isLoggable(Level.INFO)) {
                logger.info(ObjectId + " cannot send message because connection to relay has not been established");
                message.log(logger);
            }

            throw new PortUnreachableException();
        }

        if (this.isResponseReceived || message.getType() == AmtRequestMessage.MESSAGE_TYPE) {

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " sending " + message.getClass().getSimpleName());
            }

            ByteBuffer buffer = ByteBuffer.allocate(message.getTotalLength());
            message.writeTo(buffer);
            buffer.flip();

            send(new UdpDatagram(this.relayAddress, AMT_PORT, buffer));
        }
        else if (this.isRequestSent) {
            if (logger.isLoggable(Level.INFO)) {
                logger.info(ObjectId + " cannot send message because the relay has not responded to the last request");
                message.log(logger);
            }
        }
        else {
            sendRequestMessage();
        }
    }

    /**
     * 
     * @param datagram
     * @throws IOException
     * @throws InterruptedException
     */
    private void send(final UdpDatagram datagram) throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMessageEndpoint.send", datagram));
            datagram.log(logger);
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
    }

    /**
     * 
     * @throws IOException
     */
    private void startRelayDiscovery() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMessageEndpoint.startRelayDiscovery"));
        }

        synchronized (this.lock) {

            if (this.relayAddress != null) {
             
                // We had an address so we must be restarting the discovery process
                // Stop the timer task that performed the daily refresh of the relay address.
                this.relayDiscoveryTask.cancel();

                // Reset the state
                this.isRequestSent = false;
                this.isResponseReceived = false;
                this.relayAddress = null;
                this.isRelayDiscoveryStarted = false;
                this.udpEndpoint.disconnect();
            }

            if (!this.isRelayDiscoveryStarted) {
                this.isRelayDiscoveryStarted = true;
                // Schedule relay discovery task for immediate execution with short retry period
                startRelayDiscoveryTask(0, DISCOVERY_RETRY_PERIOD);
            }
        }
    }
    
    /**
     * 
     * @param delay
     * @param period
     */
    private void startRelayDiscoveryTask(final long delay, final long period) {

        final AmtTunnelEndpoint endpoint = this;
        
        this.relayDiscoveryTask = new TimerTask() {
            @Override
            public void run() {
                if (AmtTunnelEndpoint.logger.isLoggable(Level.FINER)) {
                    AmtTunnelEndpoint.logger.finer(ObjectId + " running relay discovery task");
                }
                try {
                    endpoint.sendRelayDiscoveryMessage();
                }
                catch (Exception e) {
                    AmtTunnelEndpoint.logger.warning(ObjectId + " attempt to send periodic AMT Relay Discovery Message failed - " + e.getMessage());
                }
            }
        };

        // Schedule relay discovery task for immediate execution with short retry period
        this.taskTimer.schedule(this.relayDiscoveryTask, delay, period);
    }

    /**
     * 
     * @throws Exception
     */
    private void sendRelayDiscoveryMessage() throws Exception {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMessageEndpoint.sendRelayDiscoveryMessage"));
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
            if (logger.isLoggable(Level.FINER)) {
                message.log();
            }
        }

        send(new UdpDatagram(this.relayDiscoveryAddress.getAddress(), AMT_PORT, buffer));
    }

    /**
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    private void sendRequestMessage() throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "AmtMessageEndpoint.sendRequestMessage"));
        }

        AmtRequestMessage message = new AmtRequestMessage();
        this.lastRequestNonce = message.getRequestNonce();

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
            logger.finer(Logging.entering(ObjectId, "AmtInterface.handle", message));
        }

        synchronized (this) {

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

                // Stop the timer task that started the relay discovery process.
                this.relayDiscoveryTask.cancel();

                this.isRequestSent = false;
                this.isResponseReceived = false;
                this.relayAddress = message.getRelayAddress();

                try {
                    this.udpEndpoint.connect(new InetSocketAddress(InetAddress.getByAddress(this.relayAddress),AMT_PORT));
                }
                catch (UnknownHostException e) {
                    throw new Error(e);
                }
    
                // Restart the normal periodic relay discovery task to refresh the relay address [AMT 7.1]
                startRelayDiscoveryTask(DISCOVERY_DELAY, DISCOVERY_PERIOD);
                
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
            logger.finer(Logging.entering(ObjectId, "AmtMessageEndpoint.handle", message));
        }

        synchronized (this.lock) {
            this.isResponseReceived = true;
            this.lastResponseMac = message.getResponseMac();
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
            logger.finer(Logging.entering(ObjectId, "AmtMessageEndpoint.handle", message));
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
            logger.finer(Logging.entering(ObjectId, "AmtMessageEndpoint.run"));
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
                        if (logger.isLoggable(Level.FINER)) {
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