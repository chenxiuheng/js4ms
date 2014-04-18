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
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.net.udp.UdpSocketEndpoint;
import com.larkwoodlabs.util.logging.LoggableBase;
import com.larkwoodlabs.util.logging.Logging;


/**
 * An RTSP session controller that handles 
 * {@link Method#SETUP SETUP},
 * {@link Method#TEARDOWN TEARDOWN},
 * {@link Method#PLAY PLAY},
 * {@link Method#PAUSE PAUSE},
 * {@link Method#OPTIONS OPTIONS}, and
 * {@link Method#DESCRIBE DESCRIBE} requests.
 *
 * @author Gregory Bumgardner
 */
public final class Session extends LoggableBase {

    /**
     * An enumeration of media stream transport types.
     */
    enum ConnectionType {
        Unknown,
        UDP,
        TCP
    }

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(Session.class.getName());

    /**
     * The global session timeout period.
     * See [<a href="http://tools.ietf.org/html/rfc2326#page-57">RFC-2326, Section 12.37</a>]
     */
    static int sessionTimeoutPeriod = 60000; // Default;
    

    /*-- Static Functions ----------------------------------------------------*/

    /**
     * Sets the global session timeout period.
     * See [<a href="http://tools.ietf.org/html/rfc2326#page-57">RFC-2326, Section 12.37</a>]
     * @param period - The timeout in milliseconds.
     */
    public static void setSessionTimeoutPeriod(final int period) {
        sessionTimeoutPeriod = period;
    }

    /**
     * Gets the global session timeout period in milliseconds.
     */
    public static int getSessionTimeoutPeriod() {
        return sessionTimeoutPeriod;
    }

    /*-- Member Variables ----------------------------------------------------*/

    private final Object lock = new Object();
    
    private final String sessionId;
    
    private final Server server;

    private final PresentationDescription description;
    
    private TimerTask sessionTimeoutTask;
    
    private ConnectionType connectionType = ConnectionType.Unknown;
    
    private int serverPort;
    
    private PacketSource rtpChannel;
    private PacketSource rtcpChannel;
    
    private OutputChannel<ByteBuffer> rtpPacketSink;
    private OutputChannel<ByteBuffer> rtcpPacketSink;
    
    private final Vector<MediaStream> streams = new Vector<MediaStream>();
    
    private boolean isClosed = false;
    

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a Session for the specified {@link Server} and {@link PresentationDescription}.
     * This constructor creates a pair of {@link OutputChannel} objects that are used to 
     * receive RTP/RTCP packets from the client. It also starts the timer task that will close
     * the Session should no client requests or packets be received within the session timeout period.
     * 
     * @param server - The {@link Server} for which this Session is being created.
     * @param description - The {@link PresentationDescription} for this Session.
     */
    public Session(final Server server, final PresentationDescription description) {
        this.sessionId = String.valueOf(hashCode());
        
        this.server = server;
        this.description = description;

        this.rtpPacketSink = new OutputChannel<ByteBuffer>() {
            @Override
            public void send(ByteBuffer message, int milliseconds) throws IOException, InterruptedIOException, InterruptedException {
                logger.fine(ObjectId + " received RTP packet from client");
            }

            @Override
            public void close(boolean isCloseAll) throws IOException, InterruptedException {
            }
        };

        this.rtcpPacketSink = new OutputChannel<ByteBuffer>() {
            @Override
            public void send(ByteBuffer message, int milliseconds) throws IOException, InterruptedIOException, InterruptedException {
                logger.fine(ObjectId + " received RTCP packet from client");
                restartTimeout();
            }

            @Override
            public void close(boolean isCloseAll) throws IOException, InterruptedException {
            }
        };

        restartTimeout();
    }
    
    @Override
    public Logger getLogger() {
        return logger;
    }
    
    /**
     * Returns the session identifier that was generated when this Session was constructed.
     * A session identifier is sent to the client in response to the initial
     * {@link Method#SETUP SETUP} request. The client will attach a <code>Session</code> message
     * header that carries this value in subsequent requests that target this Session.
     */
    public String getSessionId() {
        return this.sessionId;
    }

    /**
     * Returns the {@link PresentationDescription} that describes this Session.
     */
    public PresentationDescription getPresentationDescription() {
        return this.description;
    }

    /**
     * Closes this Session.
     * This method closes all packet input and output streams and cancels the session timeout task.
     * @throws InterruptedException If the calling thread is interrupted while closing the session.
     */
    public void close() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Session.close"));
        }
        
        if (this.isClosed) {
            return;
        }

        if (this.sessionTimeoutTask != null) {
            this.sessionTimeoutTask.cancel();
        }
        
        for (MediaStream stream : this.streams) {
            stream.close();
        }
        
        this.streams.clear();
        
        this.rtpChannel.close();

        this.rtcpChannel.close();
        
        this.isClosed = true;

    }
    
    /**
     * Handles session-specific RTSP requests for
     * {@link Method#SETUP SETUP},
     * {@link Method#TEARDOWN TEARDOWN},
     * {@link Method#PLAY PLAY},
     * {@link Method#PAUSE PAUSE},
     * {@link Method#OPTIONS OPTIONS}, and
     * {@link Method#DESCRIBE DESCRIBE}.
     * This method restarts the session timeout timer and dispatches each request
     * to the appropriate handler method.
     * @param request - The incoming RTSP request.
     * @param response - The outgoing RTSP response.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If a thread interrupt occurs.
     * @throws RtspException If request is malformed or cannot be satisfied.
     */
    public void handleRequest(Request request, Response response) throws InterruptedException, IOException, RtspException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Session.handleRequest", request, response));
        }

        response.setHeader(new Header(Header.SESSION, this.sessionId + ";timeout=" + sessionTimeoutPeriod / 1000));

        restartTimeout();
        
        switch (request.getRequestLine().getMethod()) {
        case OPTIONS:
            handleOptions(request, response);
            break;
        case DESCRIBE:
            handleDescribe(request, response);
            break;
        case SETUP:
            handleSetup(request, response);
            break;
        case PLAY:
            handlePlay(request, response);
            break;
        case PAUSE:
            handlePause(request, response);
            break;
        case TEARDOWN:
            handleTeardown(request, response);
            break;
        }
    }
    
    /**
     * Handles a session-specific response.
     * This method currently does nothing.
     * @param request - The original request.
     * @param response - The incoming response.
     */
    public void handleResponse(Request request, Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Session.handleResponse", request, response));
        }
        
    }
    
    /**
     * Handles a session-specific RTSP {@link Method#OPTIONS OPTIONS} request.
     * Clients typically send periodic OPTIONS requests to restart the session timeout timer.
     * @param request - The incoming RTSP OPTIONS request.
     * @param response - The outgoing RTSP response.
     */
    void handleOptions(Request request, Response response) {
        response.setHeader(new Header("Public","DESCRIBE, SETUP, TEARDOWN, PLAY" + (this.description.isPauseAllowed() ? ", PAUSE" : "")));
    }
    
    /**
     * Handles a session-specific RTSP {@link Method#DESCRIBE DESCRIBE} request.
     * Most RTSP clients do not send session-specific DESCRIBE requests as
     * they will have already sent a DESCRIBE prior to sending the first
     * {@link Method#SETUP SETUP} request that initiates the session.
     * @param request - The incoming RTSP DESCRIBE request.
     * @param response - The outgoing RTSP response.
     */
    void handleDescribe(Request request, Response response) throws RtspException {

        String sdp = this.description.describe(MimeType.application.sdp);

        // TODO: get this from HTTP server for SDP file?
        String date = Header.DATE_FORMAT_RFC_1123.format(new Date());
        response.addHeader(new Header(Header.DATE, date));
        response.addHeader(new Header(Header.EXPIRES, date));
        
        response.addHeader(Header.CACHE_CONTROL_IS_NO_CACHE);
        response.addHeader(new Header(Header.CONTENT_BASE, this.description.getUri().toString()+"/"));
        response.setEntity(new StringEntity(sdp,
                                            MimeType.application.sdp,
                                            StringEntity.UTF_8,
                                            false /* no charset= in Content-Type*/));
   
    }

    /**
     * Handles an RTSP media stream {@link Method#SETUP SETUP} request.
     * The first SETUP request that is received for a presentation results
     * in the creation of this Session object. The first and all subsequent
     * SETUP requests targeting this session are handled within this method.
     * 
     * @param request - The incoming RTSP SETUP request.
     * @param response - The outgoing RTSP response.
     * @throws InterruptedException If the calling thread is interrupted while processing the request.
     * @throws IOException If an I/O error occurs.
     * @throws RtspException If the setup request cannot be satisfied.
     */
    void handleSetup(Request request, Response response) throws InterruptedException, IOException, RtspException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Session.handleSetup", request, response));
        }

        String controlUri = request.getRequestLine().getUri().toString();
        int lastSlashIndex = controlUri.lastIndexOf("/");
        if ((lastSlashIndex != -1) &&
            (lastSlashIndex < controlUri.length() - 1) &&
            (controlUri.startsWith(this.description.getStreamControlIdentifier(), lastSlashIndex + 1))) {
            String streamIdentifier = controlUri.substring(lastSlashIndex + this.description.getStreamControlIdentifier().length() + 1);
            if (streamIdentifier.length() > 0) {
                int streamIndex;
                try {
                    streamIndex = Integer.parseInt(streamIdentifier);
                }
                catch (NumberFormatException e) {
                    throw RtspException.create(StatusCode.BadRequest, "stream identifier in SETUP request is invalid", ObjectId, logger);
                }

                setupStream(streamIndex, request, response);

            }
            else {
                throw RtspException.create(StatusCode.BadRequest, "stream identifier in SETUP request is missing", ObjectId, logger);
            }
        }
        else {
            throw RtspException.create(StatusCode.BadRequest, "stream identifier in SETUP request is missing", ObjectId, logger);
        }
    }

    /**
     * Handles an RTSP {@link Method#PLAY PLAY} request.
     * If the media streams are distributed using multicast, the PLAY request causes
     * this session to join the multicast destination group(s),
     * effectively enabling the delivery of media packets from the media source.
     * @param request - The incoming RTSP PLAY request.
     * @param response - The outgoing RTSP response.
     * @throws InterruptedException If the calling thread is interrupted while processing the request.
     * @throws IOException If an I/O error occurs.
     */
    void handlePlay(Request request, Response response) throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Session.handlePlay", request, response));
        }
        
        Header header = new Header(Header.RTP_INFO);
        
        for (MediaStream stream : this.streams) {
            header.appendValue("url=" +
                               this.description.getUri() + "/" +
                               SDPPresentationDescription.STREAM_CONTROL_IDENTIFIER +
                               stream.getStreamIndex());
        }
        
        response.setHeader(header);
        
        ConnectionHandler handler = response.getConnectionHandler();
        handler.enablePacketDelivery(false);

        try {

            for (MediaStream stream : this.streams) {
                stream.play();
            }

            // Send the response before allowing the stream threads to
            // send packets so they can't possibly arrive before the response.
            response.getConnectionHandler().sendResponse(response);
            response.isSent(true);

        }
        finally {
            handler.enablePacketDelivery(true);
        }

    }
    
    /**
     * Handles an RTSP {@link Method#PAUSE PAUSE} request.
     * If the media streams are distributed using multicast, the PAUSE request causes
     * this session to leave the multicast destination group(s),
     * effectively disabling the delivery of media packets from the media source.
     * @param request - The incoming RTSP PAUSE request.
     * @param response - The outgoing RTSP response.
     * @throws InterruptedException If the calling thread is interrupted while processing the request.
     * @throws IOException If an I/O error occurs.
     */
    void handlePause(Request request, Response response) throws InterruptedException, IOException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Session.handlePause", request, response));
        }

        for (MediaStream stream : this.streams) {
            stream.pause();
        }
    }
    
    /**
     * Handles an RTSP {@link Method#TEARDOWN TEARDOWN} request.
     * A TEARDOWN request closes this Session. See {@link #close()}.
     * The RTSP response will include a <code>Connection: close</code> header to
     * indicate to the client that it can close the RTSP control connection.
     * @param request - The incoming RTSP TEARDOWN request.
     * @param response - The outgoing RTSP response.
     * @throws InterruptedException If the calling thread is interrupted while processing the request.
     */
    void handleTeardown(Request request, Response response) throws InterruptedException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Session.handleTeardown", request, response));
        }
        
        response.setHeader(Header.CONNECTION_IS_CLOSE);
        
        close();
    }
    
    /**
     * Restarts the session timeout timer.
     * This method is called for every request received from the client.
     */
    private void restartTimeout() {

        if (this.sessionTimeoutTask != null) {
            this.sessionTimeoutTask.cancel();
        }

        synchronized (this.lock) {
            this.sessionTimeoutTask = new TimerTask() {
                @Override
                public void run() {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(ObjectId + " closing session due to inactivity");
                    }
                    try {
                        close();
                    }
                    catch (InterruptedException e) {
                        return;
                    }
                }
            };
            
            this.server.schedule(this.sessionTimeoutTask, getSessionTimeoutPeriod());
        }
    }
    
    /**
     * Attempts to construct a {@link MediaStream} from a {@link MediaStreamDescription} using
     * transport preferences as specified in the <code>Transport</code> header sent by the client.
     * A description of the actual transport setup is returned to the client in a
     * <code>Transport</code> message header.
     * @param streamIndex - The stream index. Used to retrieve a {@link MediaStreamDescription}
     *                      from the {@link PresentationDescription} associated with this session.
     * @param request - The incoming RTSP SETUP request.
     * @param response - The outgoing RTSP response.
     * @throws RtspException If the server cannot satisfy the request due to an internal error
     *                       or unsupported transport preferences.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If the calling thread is interrupted while processing the request.
     */
    void setupStream(int streamIndex, Request request, Response response) throws RtspException, IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Session.setupStream", streamIndex, request, response));
        }

        MediaStreamDescription description;
        description = this.description.getStreamDescription(streamIndex);
        
        Header header = request.getHeader(Header.TRANSPORT);
        if (header == null) {
            throw RtspException.create(StatusCode.BadRequest, "required Transport header in SETUP request is missing", ObjectId, logger);
        }
        
        TransportPreferences preferences;
        try {
            preferences = new TransportPreferences(header.getValue());
        }
        catch (RtspException e) {
            e.setResponse(response);
            return;
        }
        
        MediaStream mediaStream = new MediaStream(streamIndex, description);
        
        mediaStream.setup(preferences, response.getConnectionHandler());
        
        this.streams.add(streamIndex, mediaStream);
        
        TransportDescription transportDescription = mediaStream.getTransportDescription();

        if (this.connectionType == ConnectionType.Unknown) {
            switch(transportDescription.getTransport()) {
            case UDP:
                // Client will send RTP/RTCP packets to server over UDP
                // Construct a socket pair to receive the packets.
                this.connectionType = ConnectionType.UDP;
                UdpSocketEndpoint rtpEndpoint = null;
                UdpSocketEndpoint rtcpEndpoint = null;
                int port = 0;
                boolean pairFound = false;
                int maxAttempts = 20; // TODO
                while (!pairFound) {
                    if (maxAttempts-- == 0) {
                        throw new IOException("unable to acquire socket pair");
                    }
                    try {
                        // Try to get an even port number for RTP (may be odd on first try)
                        rtpEndpoint = new UdpSocketEndpoint(port);
                        port = rtpEndpoint.getLocalSocketAddress().getPort();
                        if ((port & 0x1) != 0) {
                            // Assigned port number is odd - try next even port number
                            port++;
                            rtpEndpoint.close(true);
                            continue;
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        if (port == 0) {
                            // Ephemeral port allocation failed - that's bad
                            throw e;
                        }
                        // Requested port number is in use - try next even port number
                        port += 2;
                        continue;
                    }
                    
                    try {
                        // Try odd port number for RTCP
                        port++;
                        rtcpEndpoint = new UdpSocketEndpoint(port);
                        pairFound = true;
                    }
                    catch (IOException e) {
                        // Port in use - try next even port for RTP
                        port++;
                        rtpEndpoint.close(true);
                        continue;
                    }
                }
                
                this.rtpChannel = new UdpPacketSource(rtpEndpoint, this.rtpPacketSink);
                this.rtcpChannel = new UdpPacketSource(rtcpEndpoint, this.rtcpPacketSink);

                this.serverPort = port - 1;
                break;
                
            case TCP:
                // Client will send RTP/RTCP packets over RTSP connection
                this.connectionType = ConnectionType.TCP;
                int channelNumber = transportDescription.getFirstInterleavedStreamChannel();
                this.rtpChannel = new TcpPacketSource(channelNumber, this.rtpPacketSink, response.getConnectionHandler());
                this.rtcpChannel = new TcpPacketSource(channelNumber+1, this.rtcpPacketSink, response.getConnectionHandler());
                break;
            }
        }

        if (transportDescription.getTransport() == TransportDescription.Transport.UDP) {
            transportDescription.setServerPortRange(this.serverPort, 1);
        }

        String date = Header.DATE_FORMAT_RFC_1123.format(new Date());
        response.setHeader(new Header(Header.DATE, date));
        response.setHeader(new Header(Header.EXPIRES, date));
        response.addHeader(Header.CACHE_CONTROL_IS_NO_CACHE);
        response.setHeader(new Header(Header.TRANSPORT, transportDescription.getHeaderValue()));

    }
    
}
