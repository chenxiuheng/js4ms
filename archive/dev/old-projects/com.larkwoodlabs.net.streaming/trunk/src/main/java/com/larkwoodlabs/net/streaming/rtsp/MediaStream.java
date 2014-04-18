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

package com.larkwoodlabs.net.streaming.rtsp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.net.amt.SourceFilter;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Base class for concrete media stream implementations.
 * A media stream constructs and controls the communication pathway used to
 * deliver a single media stream from the server to a client.
 * 
 * @author Gregory Bumgardner
 */
final class MediaStream {
    

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(MediaStream.class.getName());


    /*-- Static Functions ----------------------------------------------------*/
    
    
    /*-- Member Variables ----------------------------------------------------*/
    
    private final String ObjectId = Logging.identify(this);
    
    private final Object lock = new Object();
    
    private final int streamIndex;
    
    private final MediaStreamDescription description;
    
    private TransportDescription transportDescription = null;
    
    private final LinkedList<PacketSource> sources = new LinkedList<PacketSource>();
    

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     * @param streamIndex - The track ID used to identify the stream.
     * @param description
     */
    public MediaStream(final int streamIndex,
                       final MediaStreamDescription description) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MediaStream.MediaStream", streamIndex, description));
        }
        
        this.streamIndex = streamIndex;
        this.description = description;
    }


    /** 
     * Returns the {@link TransportDescription} that describes the
     * actual transport configuration provided by this media stream.
     */
    public TransportDescription getTransportDescription() {
        return this.transportDescription;
    }

    public int getStreamIndex() {
        return this.streamIndex;
    }

    public void close() throws InterruptedException {

        synchronized (this.lock) {

            while (!this.sources.isEmpty()) {
                PacketSource source = this.sources.removeFirst();
                if (source.getState() != PacketSource.State.Closed) {
                    source.close();
                }
            }
            
        }
    }

    /**
     * Attempts to configure the media stream to match client preferences.
     * @throws RtspException 
     * @throws IOException 
     * @throws InterruptedException 
     */
    public void setup(TransportPreferences preferences, ConnectionHandler handler) throws RtspException, IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MediaStream.setup", preferences));
        }

        Iterator<TransportDescription> iter = preferences.getIterator();
        
        while (iter.hasNext()) {
            
            TransportDescription preference = iter.next();
            
            // Check for protocol match
            if (preference.getProtocol() != this.description.getProtocol()) {
                logger.fine(ObjectId + " transport preference rejected because protocol does not match source");
                continue;
            }
            
            if (preference.getProfile() != this.description.getProfile()) {
                logger.fine(ObjectId + " transport preference rejected because profile does not match source");
                continue;
            }

            try {
                switch (preference.getTransport()) {
                case UDP:
                    this.transportDescription = setupUdpPacketStreams(preference, handler.getConnection().getRemoteAddress());
                    break;
                case TCP:
                    this.transportDescription = setupTcpPacketStreams(preference, handler);
                    continue;
                }
            }
            catch (IOException e) {
                close();
                throw e;
            }
            
        }
        
        if (this.transportDescription == null) {
            throw RtspException.create(StatusCode.UnsupportedTransport, "cannot support any requested transports", ObjectId, logger);
        }
        
    }

    public void play() throws IOException, InterruptedException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MediaStream.play"));
        }
        
        synchronized (this.lock) {
            for (PacketSource source : this.sources) {
                source.start();
            }
        }
    }
    
    public void pause() throws IOException, InterruptedException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MediaStream.pause"));
        }
        
        synchronized (this.lock) {
            for (PacketSource source : this.sources) {
                source.stop();
            }
        }
    }
    
    public void teardown() throws InterruptedException, RtspException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MediaStream.teardown"));
        }

        close();
    }

    private TransportDescription setupUdpPacketStreams(final TransportDescription preference,
                                                       /*final*/ InetAddress remoteAddress) throws RtspException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MediaStream.setupUdpPacketStreams", preference));
        }

        int addressIndex = 0;
        int addressCount = this.description.getFilterCount();
        int portsPerStream = preference.getPortsPerStream();
        
        int firstSourcePort = this.description.getFirstSourcePort();
        int sourceStreamCount = this.description.getSourcePortCount(); // TODO only handle 1 attm
        int firstClientPort;
        int clientStreamCount;
        
        if (preference.isClientPortSpecified()) {
            
            firstClientPort = preference.getFirstClientStreamPort();
            clientStreamCount = preference.getClientStreamCount();

            // TODO handle address count > 1
            
            if (clientStreamCount != sourceStreamCount) {
                // Client port range exceeds range required by media description
                throw RtspException.create(StatusCode.BadRequest,
                                           "Transport header in SETUP request specifies an invalid client port range",
                                           ObjectId, logger);
            }
        }
        else {
            firstClientPort = this.description.getFirstSourcePort() + streamIndex * portsPerStream;
            clientStreamCount = sourceStreamCount;
        }

        if (remoteAddress.isLoopbackAddress()) {
            // Workaround for OS X (or QuickTime?) behavior where client
            // connects from IPv6 loopback address when "localhost" is used in URL,
            // when the relay attempts to send RTP/UDP packets to that address,
            // an "port-unreachable" ICMP message is produced.
            
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " translating remote loopback address from " + Logging.address(remoteAddress));
            }
            
            // Replace the loopback address with the default host address
            remoteAddress = InetAddress.getLocalHost();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " to remote address " + Logging.address(remoteAddress));
            }
        }

        // firstClientStreamPort specifies the port to use for the first client endpoint
        // clientStreamCount specifies the number of client endpoints to create.
        // addressIndex specifies the source filter (address) to use for the first server endpoint.
        // addressCount specifies the number of server endpoints to create.
        
        // Create a packet stream for each client/server endpoint pair.

        Vector<SourceFilter> filters = this.description.getFilters();
        int clientPort = firstClientPort;
        for (int i = addressIndex; i < (addressIndex + addressCount); i++) {
            SourceFilter filter = filters.elementAt(i);
            int serverPort = firstSourcePort;
            for (int j = 0; j < portsPerStream; j++) {
                PacketSink clientEndpoint = new UdpPacketSink(new InetSocketAddress(preference.isDestinationSpecified() ? preference.getDestination() : remoteAddress, clientPort++));
                PacketSource serverEndpoint = new MulticastPacketSource(serverPort++, this.description.getRelayDiscoveryAddress(), filter, clientEndpoint);
                this.sources.add(serverEndpoint);
            }
        }
        
        TransportDescription transportDescription = new TransportDescription();

        transportDescription.setProtocol(preference.getProtocol());
        transportDescription.setProfile(preference.getProfile());
        transportDescription.setTransport(TransportDescription.Transport.UDP);

        if (preference.isDestinationSpecified() && preference.getDestination().isMulticastAddress()) {
            transportDescription.setDistribution(TransportDescription.Distribution.multicast);
        }
        else {
            transportDescription.setDistribution(TransportDescription.Distribution.unicast);
        }
        
        transportDescription.setClientPortRange(firstClientPort, clientStreamCount);
        
        return transportDescription;

    }
    
    private TransportDescription setupTcpPacketStreams(final TransportDescription preference,
                                                       final ConnectionHandler handler) throws RtspException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "MediaStream.setupTcpPacketStreams", preference, handler));
        }

        int addressIndex = 0;
        int addressCount = this.description.getFilterCount();
        int portsPerStream = preference.getPortsPerStream(); // TODO doesn't come from preferences
        
        int firstSourcePort = this.description.getFirstSourcePort();
        int sourceStreamCount = this.description.getSourcePortCount();
        int firstClientChannel;
        int clientStreamCount;
        
        if (preference.isInterleavedChannelSpecified()) {
            
            firstClientChannel = preference.getFirstInterleavedStreamChannel();
            clientStreamCount = preference.getInterleavedStreamCount();

            // TODO handle address count > 1
            
            if (clientStreamCount != sourceStreamCount) {
                throw RtspException.create(StatusCode.BadRequest,
                                           "Transport header in SETUP request specifies an invalid channel range",
                                           ObjectId, logger);
            }
        }
        else {
            firstClientChannel = this.streamIndex * sourceStreamCount * portsPerStream;
            clientStreamCount = sourceStreamCount;
        }

        // Create a packet stream for each client/server endpoint pair.

        Vector<SourceFilter> filters = this.description.getFilters();
        int channelNumber = firstClientChannel;
        for (int i = addressIndex; i < (addressIndex + addressCount); i++) {
            SourceFilter filter = filters.elementAt(i);
            int serverPort = firstSourcePort;
            for (int j = 0; j < portsPerStream; j++) {
                PacketSink clientEndpoint = new TcpPacketSink(channelNumber++,handler);
                PacketSource serverEndpoint = new MulticastPacketSource(serverPort++, filter, clientEndpoint);
                this.sources.add(serverEndpoint);
            }
        }

        TransportDescription transportDescription = new TransportDescription();
        
        transportDescription.setProtocol(preference.getProtocol());
        transportDescription.setProfile(preference.getProfile());
        transportDescription.setTransport(TransportDescription.Transport.TCP);
        transportDescription.setDistribution(TransportDescription.Distribution.unicast);
        transportDescription.setInterleavedChannelRange(firstClientChannel, clientStreamCount);

        return transportDescription;
    }
    
}
