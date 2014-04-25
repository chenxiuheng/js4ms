package org.js4ms.rtsp.presentation;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MediaStream.java [org.js4ms.jsdk:rtsp]
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
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;

import org.js4ms.common.util.logging.Log;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.io.channel.MessageSource;
import org.js4ms.io.channel.OutputChannel;
import org.js4ms.io.net.UdpDatagramPayloadSource;
import org.js4ms.io.net.UdpEndpoint;
import org.js4ms.io.net.UdpPacketOutputChannel;
import org.js4ms.io.net.UdpSocketEndpoint;
import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.entity.Entity;
import org.js4ms.rest.header.SimpleMessageHeader;
import org.js4ms.rest.message.MessageHeader;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;
import org.js4ms.rtsp.message.RtspHeaderName;
import org.js4ms.rtsp.message.RtspStatusCode;
import org.js4ms.rtsp.presentation.Presentation.Source;
import org.js4ms.rtsp.rtp.InterleavedPacketOutputChannel;





/**
 * 
 * 
 *
 * @author gbumgard
 */
public abstract class MediaStream {

    /**
     * An enumeration of stream states.
     */
    public enum State {
        /**
         * State that exists prior to SETUP request and following TEARDOWN request.
         * <li>SETUP -> READY
         */
        INITIAL,
        /**
         * State that exists following SETUP request Allows PLAY, RECORD, SETUP, TEARDOWN.
         * <li>PLAY -> PLAYING
         * <li>RECORD -> RECORDING
         * <li>TEARDOWN -> INITIAL
         * <li>SETUP -> READY
         */
        READY,
        /**
         * State that exists following PLAY request. Allows PAUSE, PLAY, SETUP, TEARDOWN.
         * <li>PLAY -> PLAYING (Changed range)
         * <li>PAUSE -> READY
         * <li>SETUP -> PLAYING (Changed Transport)
         * <li>TEARDOWN -> INITIAL
         */
        PLAYING,
        /**
         * State that exists following RECORD request. Allows PAUSE, RECORD, SETUP, TEARDOWN.
         * <li>PAUSE -> READY
         * <li>TEARDOWN -> INITIAL
         * <li>RECORD -> RECORDING
         * <li>SETUP -> RECORDING (Changed transport)
         */
        RECORDING
    };

    /**
     * An enumeration of stream transport modes.
     */
    public enum Mode {

        /**
         * Indicates that the client has sent a SETUP request with transport mode '<b>play</b>'.
         * A presentation can only be played if its {@link #Source} is {@link Source.DESCRIBE DESCRIBE}.
         */
        PLAY,

        /**
         * Indicates that the client has sent a SETUP request with a transport mode '<b>record</b>'.
         * A presentation can only be recorded if its {@link #Source} is {@link Source.ANNOUNCE ANNOUNCE}.
         */
        RECORD,

        /**
         * Indicates that the client has sent a SETUP request with a transport mode '<b>append</b>'.
         * A presentation can only be recorded if its {@link #Source} is {@link Source.ANNOUNCE ANNOUNCE}.
         */
        APPEND
    }

    enum Transport {
        UDP,
        INTERLEAVED,
        TCP
    }

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(MediaStream.class.getName());


    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final Log log = new Log(this);

    protected final Presentation presentation;

    protected final MediaDescription mediaDescription;

    protected int streamIndex;

    protected final TransportDescription transportDescription;

    protected State state;

    protected Mode mode;

    protected Transport transport;

    // Collection of active packet channels
    protected final Vector<MessageSource<ByteBuffer>> serverPacketChannels = new Vector<MessageSource<ByteBuffer>>();
    protected final Vector<MessageSource<ByteBuffer>> clientPacketChannels = new Vector<MessageSource<ByteBuffer>>();

    protected int firstChannelIndex = 0;
    protected int channelCount = 0;

    /**
     * @param presentation
     * @param streamIndex
     * @param sessionDescription
     * @param mediaDescription
     * @throws SdpException
     */
    protected MediaStream(final Presentation presentation,
                          final int streamIndex,
                          final SessionDescription sessionDescription,
                          final MediaDescription mediaDescription) throws SdpException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("MediaStream", sessionDescription, mediaDescription));
        }

        this.presentation = presentation;
        this.streamIndex = streamIndex;
        this.mediaDescription = mediaDescription;
        this.transportDescription = new TransportDescription(sessionDescription, mediaDescription);
        this.state = State.INITIAL;
    }

    /**
     * 
     * @return
     */
    public State getState() {
        return this.state;
    }

    /**
     * Returns the mode requested in the last SETUP request.
     * The mode value is meaningless if the presentation is in the INITIAL state.
     * @return
     */
    public Mode getMode() {
        return this.mode;
    }

    public boolean isPauseSupported() {
        return doIsPauseSupported();
    }

    protected abstract boolean doIsPauseSupported();

    /**
     * Indicates whether this presentation supports recording.
     * For example, a presentation created via an DESCRIBE/SETUP request may support playback but not recording.
     * @return
     */
    public boolean isRecordSupported() {
        return doIsRecordSupported();
    }

    protected abstract boolean doIsRecordSupported();

    /**
     * Indicates whether this presentation supports playback.
     * For example, a presentation created via an ANNOUNCE request may support recording but not playback.
     * @return
     */
    public boolean isPlaySupported() {
        return doIsPlaySupported();
    }

    protected abstract boolean doIsPlaySupported();

    public boolean isGetParameterSupported() {
        // Always allow GET_PARAMETER since it may be used as a keep-alive message.
        return true;
    }

    public boolean isSetParameterSupported() {
        // Always allow SET_PARAMETER since it may be used as a keep-alive message.
        return true;
    }

    public boolean isSetupAllowed() {
        return true;
    }

    public boolean isTeardownAllowed() {
        return this.state != State.INITIAL;
    }

    public boolean isPlayAllowed() {
        return ((this.state == State.READY && this.mode == Mode.PLAY) || this.state == State.PLAYING);
    }

    public boolean isPauseAllowed() {
        return isPauseSupported() && (this.state == State.PLAYING || this.state == State.RECORDING);
    }

    public boolean isRecordAllowed() {
        return isRecordSupported()
               && ((this.state == State.READY && this.mode == Mode.RECORD) || this.state == State.RECORDING);
    }

    /**
     * Overridden in derived classes to indicate whether server-to-client
     * packet channels are required by the presentation or media stream.
     * When mode is "play" the server-to-client channels are used to send
     * media packets to the client.
     * When mode is "record" the server-to-client channels are used to send
     * receiver reports to the client.
     * @return
     */
    protected abstract boolean isServerSourceChannelRequired();

    /**
     * Overridden in derived classes to indicate whether client-to-server
     * packet channels are required by the presentation or media stream.
     * When mode is "play" the client-to-server channels are used to send
     * receiver reports to the client.
     * When mode is "record" the client-to-server channels are used to send
     * media packets to the client.
     * @return
     */
    protected abstract boolean isClientSourceChannelRequired();

    public boolean handleOptions(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleOptions", request, response));
        }

        StringBuffer headerValue = new StringBuffer();
        headerValue.append("SETUP,PLAY");
        if (isPauseSupported()) headerValue.append(",PAUSE");
        if (isRecordSupported()) headerValue.append(",RECORD");
        headerValue.append(",TEARDOWN");
        if (isGetParameterSupported()) headerValue.append(",GET_PARAMETER");
        if (isSetParameterSupported()) headerValue.append(",SET_PARAMETER");

        MessageHeader header = new SimpleMessageHeader(RtspHeaderName.PUBLIC, headerValue.toString());
        response.setStatus(RtspStatusCode.OK);
        response.setHeader(header);
        return true;
    }

    protected boolean handleSetup(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleSetup", request, response));
        }

        if (!isSetupAllowed()) {
            setMethodNotValidInThisState(request, response);
            return true;
        }

        if (doHandleSetup(request, response)) {
            if (response.getStatus().equals(RtspStatusCode.OK)) {
                this.state = State.READY;
            }
            return true;
        }

        return false;
    }

    /**
     * @param request
     * @param response
     * @return
     */
    protected boolean doHandleSetup(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("doHandleSetup", request, response));
        }

        // Media stream setup is governed by parameter values found in the Transport header carried by a request.

        MessageHeader header = request.getHeader(RtspHeaderName.TRANSPORT);
        if (header == null) {
            RequestException.create(request.getProtocolVersion(),
                                    RtspStatusCode.BadRequest,
                                    "required Transport header in SETUP request is missing",
                                    log.getPrefix(),
                                    logger).setResponse(response);
            return true;
        }

        /*
         * A client may indicate that it supports multiple stream configurations by including multiple
         * parameter sets in the Transport header. If multiple stream configurations are described in the
         * Transport header, this method assumes that order of appearance indicates order of preference.
         */

        TransportPreferences preferences;
        try {
            preferences = new TransportPreferences(header.getValue());
        }
        catch (RequestException e) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(log.msg("cannot construct transport preferences from Transport header; "+e.getMessage()));
            }
            e.setResponse(response);
            return true;
        }
        catch (UnknownHostException e) {
            RequestException.create(request.getProtocolVersion(),
                                    RtspStatusCode.BadRequest,
                                    "cannot resolve host address found in Transport header of a SETUP request",
                                    e,
                                    log.getPrefix(),
                                    logger).setResponse(response);
            return true;
        }

        boolean setupComplete = false;

        String message = "";

        TransportDescription acceptedTransportDescription = new TransportDescription();

        // Iterate over the list of client preferences and test for compatibility with media stream description

        Iterator<TransportDescription> iter = preferences.getIterator();

        while (iter.hasNext()) {

            TransportDescription preference = iter.next();

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(log.msg("testing acceptability of "+preference.toString()));
            }

            if (preference.getMode() == TransportDescription.Mode.RECORD && !isRecordSupported()) {
                RequestException.create(request.getProtocolVersion(),
                                        RtspStatusCode.UnsupportedTransport,
                                        "record mode is not supported for specified resource",
                                        log.getPrefix(),
                                        logger).setResponse(response);
                return true;
            }
            else if (preference.getMode() == TransportDescription.Mode.PLAY && !isPlaySupported()) {
                RequestException.create(request.getProtocolVersion(),
                                        RtspStatusCode.UnsupportedTransport,
                                        "play mode is not supported for specified resource",
                                        log.getPrefix(),
                                        logger).setResponse(response);
                return true;
            }
            else {
                this.mode = Mode.valueOf(preference.getMode().name());
            }

            // Check for protocol match (e.g. RTP)
            if (!preference.getProtocol().equals(this.transportDescription.getProtocol())) {
                final String reason = "transport preference rejected because protocol does not match";
                message += preference.toString() + "\n" + reason + "\n";
                logger.fine(log.msg(reason));
                continue;
            }

            // Check for profile match (e.g. AVP)
            if (!preference.getProfile().equals(this.transportDescription.getProfile())) {
                final String reason = "transport preference rejected because profile does not match";
                message += preference.toString() + "\n" + reason + "\n";
                logger.fine(log.msg(reason));
                continue;
            }

            acceptedTransportDescription.setProtocol(preference.getProtocol());
            acceptedTransportDescription.setProfile(preference.getProfile());
            acceptedTransportDescription.setTransport(preference.getTransport());
            acceptedTransportDescription.setDistribution(preference.getDistribution());

            if (preference.getTransport().equals("UDP")) {

                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("attempting to setup for UDP transport"));
                }

                this.transport = Transport.UDP;

                InetAddress destination;
                int firstDestinationPort;
                int lastDestinationPort;

                if (preference.getDistribution() == TransportDescription.Distribution.UNICAST) {

                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(log.msg("attempting to setup for unicast distribution"));
                    }

                    // Client has requested unicast UDP transport

                    destination = request.getConnection().getRemoteAddress().getAddress();
                    if (preference.isDestinationSpecified() && !preference.getDestination().equals(destination)) {
                        final String reason = "transport preference rejected because destination address does not match RTSP end-point address";
                        message += preference.toString() + "\n" + reason + "\n";
                        logger.fine(log.msg(reason));
                        continue;
                    }

                    // Is the destination a loopback address (e.g. localhost)?
                    if (destination.isLoopbackAddress()) {
                        /*
                         * Workaround for OS X (or QuickTime) behavior where client
                         * connects from IPv6 loopback address when "localhost" is used in URL,
                         * but when the relay attempts to send RTP/UDP packets to that address,
                         * a "port-unreachable" ICMP message is produced.
                         */

                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine(log.msg("translating remote loopback address from " + Logging.address(destination)));
                        }

                        // Replace the loopback address with the default host address
                        try {
                            destination = InetAddress.getLocalHost();
                        }
                        catch (UnknownHostException e) {
                            RequestException.create(request.getProtocolVersion(),
                                                    RtspStatusCode.InternalServerError,
                                                    e,
                                                    log.getPrefix(),
                                                    logger).setResponse(response);
                            return true;
                        }

                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine(log.msg("to remote address " + Logging.address(destination)));
                        }
                    }

                    // Did client include required client_port parameter in Transport header?
                    if (!preference.isClientPortRangeSpecified()) {
                        final String reason = "transport preference rejected because client did not include client_port parameter in header";
                        message += preference.toString() + "\n" + reason + "\n";
                        logger.fine(log.msg(reason));
                        continue;
                    }

                    firstDestinationPort = preference.getFirstClientPort();
                    lastDestinationPort = preference.getLastClientPort();

                    acceptedTransportDescription.setClientPortRange(firstDestinationPort, lastDestinationPort);
                }
                else {

                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(log.msg("attempting to setup for multicast distribution"));
                    }

                    // Client has requested multicast UDP transport

                    // Did client include required destination parameter in Transport header?
                    if (!preference.isDestinationSpecified()) {
                        final String reason = "transport preference rejected because destination address parameter is missing";
                        message += preference.toString() + "\n" + reason + "\n";
                        logger.fine(log.msg(reason));
                        continue;
                    }

                    destination = preference.getDestination();

                    if (!destination.isMulticastAddress()) {
                        final String reason = "transport preference rejected because destination address is not a multicast address";
                        message += preference.toString() + "\n" + reason + "\n";
                        logger.fine(log.msg(reason));
                        continue;
                    }

                    // Did client include required port parameter in Transport header?
                    if (!preference.isMulticastPortRangeSpecified()) {
                        final String reason = "transport preference rejected because client did not include port parameter in header";
                        message += preference.toString() + "\n" + reason + "\n";
                        logger.fine(log.msg(reason));
                        continue;
                    }

                    firstDestinationPort = preference.getFirstMulticastPort();
                    lastDestinationPort = preference.getLastMulticastPort();
                    
                    acceptedTransportDescription.setMulticastPortRange(firstDestinationPort, lastDestinationPort);
                    acceptedTransportDescription.setDestination(destination);
                }

                int destinationPortCount = (lastDestinationPort - firstDestinationPort) + 1;
                int destinationLayerCount = destinationPortCount / preference.getPortsPerLayer();

                // Get transport parameters generated from SDP media description
                int firstPort = this.transportDescription.getFirstClientPort();
                int lastPort = this.transportDescription.getLastClientPort();
                int portCount = (lastPort - firstPort) + 1;
                int layerCount = portCount / this.transportDescription.getPortsPerLayer();

                // Can client port range be mapped to available port range?
                if (destinationLayerCount > layerCount || (destinationPortCount % preference.getPortsPerLayer()) != 0) {
                    RequestException.create(request.getProtocolVersion(),
                                            RtspStatusCode.BadRequest,
                                            "client port range specified in SETUP Transport header cannot be mapped to media port range",
                                            log.getPrefix(),
                                            logger).setResponse(response);
                    return true;
                }

                // Determine server port range by attempting to allocate sockets with sequential port numbers

                // To support symmetric RTP/RTCP, a single socket is be used for sending
                // and receiving packets in each channel. This allows the client to punch a hole
                // through a NAT/firewall by sending packets to the server port from which packets
                // sent the opposite direction will originate.

                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("constructing sockets for UDP transport"));
                }

                DatagramSocket sockets[] = new DatagramSocket[destinationPortCount];

                int firstServerPort = 0;
                int retryCount = 0;
                int maxRetries = 32;

                while (retryCount <= maxRetries) {

                    int serverPortCount = 0;

                    // Construct sockets until an even port number is accepted
                    while (sockets[0] == null && retryCount < maxRetries) {
                        try {
                            DatagramSocket socket = new DatagramSocket(firstServerPort);
                            firstServerPort = socket.getLocalPort();
                            if ((firstServerPort & 0x1) != 0) {
                                // Skip odd port
                                socket.close();
                                firstServerPort++;
                            }
                            else {
                                sockets[serverPortCount++] = socket;
                            }
                        }
                        catch(Exception e) {
                            // Skip to next even port number
                            firstServerPort += (2 - (firstServerPort & 0x1));
                            // Keep value within dynamic/ephemeral port range
                            firstServerPort = (firstServerPort & 0x3FFF) + 0xC000;
                            retryCount++;
                        }
                    }

                    if (retryCount > maxRetries) {
                        break;
                    }

                    while (serverPortCount < destinationPortCount) {
                        int port = firstServerPort + serverPortCount;
                        try {
                            @SuppressWarnings("resource")
							DatagramSocket socket = new DatagramSocket(port);
                            sockets[serverPortCount++] = socket;
                        }
                        catch(Exception e) {
                            // Skip to next even port number
                            firstServerPort += (2 - (firstServerPort & 0x1));
                            // Keep value within dynamic/ephemeral port range
                            firstServerPort = (firstServerPort & 0x3FFF) + 0xC000;
                            retryCount++;
                        }
                    }

                    // Did we successfully allocate the ports?
                    if (serverPortCount == destinationPortCount) {
                        // Yes, we're done
                        break;
                    }

                    // We were unable to allocate the necessary number of ports
                    // Close the ones we have and start over at the next port number.
                    for (int i=0; i < serverPortCount; i++) {
                        sockets[i].close();
                        sockets[i] = null;
                    }
                }

                if (retryCount > maxRetries) {
                    RequestException.create(request.getProtocolVersion(),
                                            RtspStatusCode.InternalServerError,
                                            "cannot allocate ports required for sending or receiving media packets",
                                            log.getPrefix(),
                                            logger).setResponse(response);
                    return true;
                }

                // Construct packet channels
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("constructing packet channels UDP transport"));
                }

                int channelsPerLayer = preference.getPortsPerLayer();
                try {
                    for (int layerIndex = 0; layerIndex < destinationLayerCount; layerIndex++) {
                        for (int channelIndex = 0; channelIndex < channelsPerLayer; channelIndex++) {
                            int index = layerIndex*channelsPerLayer+channelIndex;
                            int port = firstDestinationPort + index;

                            if (isServerSourceChannelRequired()) {

                                // Construct server->client path for media packets
                                if (logger.isLoggable(Level.FINER)) {
                                    logger.finer(log.msg("constructing server->client channel; layer="+layerIndex+" channel="+channelIndex + " port="+port));
                                }

                                // Bind the socket to the client port for sending
                                sockets[index].connect(destination, port);

                                OutputChannel<ByteBuffer> clientPacketSink;
                                try {
                                    clientPacketSink = new UdpPacketOutputChannel(sockets[index]);
                                    MessageSource<ByteBuffer> serverPacketSource = constructServerPacketSource(layerIndex, channelIndex, clientPacketSink);
                                    this.serverPacketChannels.add(serverPacketSource);
                                }
                                catch (SdpException e) {
                                    RequestException.create(request.getProtocolVersion(),
                                                            RtspStatusCode.InvalidMedia,
                                                            "cannot construct channel for sending media packets",
                                                            e,
                                                            log.getPrefix(),
                                                            logger).setResponse(response);
                                    return true;
                                }
                                catch (IOException e) {
                                    RequestException.create(request.getProtocolVersion(),
                                                            RtspStatusCode.InternalServerError,
                                                            "cannot construct channel for sending media packets",
                                                            e,
                                                            log.getPrefix(),
                                                            logger).setResponse(response);
                                    return true;
                                }
                            }

                            // Construct client->server path for media packets
                            if (isClientSourceChannelRequired()) {

                                if (logger.isLoggable(Level.FINER)) {
                                    logger.finer(log.msg("constructing client->server channel; layer="+layerIndex+" channel="+channelIndex));
                                }

                                UdpEndpoint endpoint = new UdpSocketEndpoint(sockets[index]);
                                sockets[index] = null;
                                try {
                                    OutputChannel<ByteBuffer> serverPacketSink = constructServerPacketSink(layerIndex, channelIndex);
                                    MessageSource<ByteBuffer> clientPacketSource = new UdpDatagramPayloadSource(endpoint, serverPacketSink);
                                    this.clientPacketChannels.add(clientPacketSource);
                                }
                                catch (SdpException e) {
                                    RequestException.create(request.getProtocolVersion(),
                                                            RtspStatusCode.InvalidMedia,
                                                            "cannot construct channel for receiving media packets",
                                                            e,
                                                            log.getPrefix(),
                                                            logger).setResponse(response);
                                    return true;
                                }
                                catch (IOException e) {
                                    RequestException.create(request.getProtocolVersion(),
                                                            RtspStatusCode.InternalServerError,
                                                            "cannot construct channel for receiving media packets",
                                                            e,
                                                            log.getPrefix(),
                                                            logger).setResponse(response);
                                    return true;
                                }
                            }
                        }
                    }

                    acceptedTransportDescription.setServerPortRange(firstServerPort, firstServerPort + destinationPortCount - 1);

                    setupComplete = true;

                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(log.msg("candidate transport setup accepted: " + acceptedTransportDescription.toString()));
                    }

                    break;
                }
                finally {

                    if (!setupComplete) {
                        // We were unable to construct all of the necessary channels.

                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer(log.msg("candiate transport setup failed - removing channels already created"));
                        }

                        // Close the server packet channels
                        Iterator<MessageSource<ByteBuffer>> channelIter = this.serverPacketChannels.iterator();
                        while (channelIter.hasNext()) {
                            try {
                                channelIter.next().close();
                            }
                            catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            channelIter.remove();
                        }

                        // Close the client packet channels
                        channelIter = this.clientPacketChannels.iterator();
                        while (channelIter.hasNext()) {
                            try {
                                channelIter.next().close();
                            }
                            catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            channelIter.remove();
                        }

                        // there may be some sockets left to close
                        for (int i = 0; i < sockets.length; i++) {
                            if (sockets[i] != null) {
                                sockets[i].close();
                            }
                        }
                    }
                }
                
            }
            else if (preference.getTransport().equals("TCP")) {
                
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("attempting to setup TCP transport"));
                }

                // The client wants us to send and receive packets using TCP.

                // Get transport parameters generated from SDP media description
                int channelCount = this.transportDescription.getLayers() * this.transportDescription.getPortsPerLayer();

                if (!preference.isInterleavedChannelRangeSpecified()) {

                    // This implementation only supports interleaving over the RTSP control connection,
                    // so we will return a Transport description with an interleaved channel range.

                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(log.msg("no interleaved attribute specified in Transport header - generating default"));
                    }

                    int firstChannel = this.streamIndex * channelCount;
                    int lastChannel = (firstChannel + channelCount) - 1;

                    // Use the source port numbers as interleaved channel numbers
                    preference.setInterleavedChannelRange(firstChannel, lastChannel);
                }

                this.transport = Transport.INTERLEAVED;

                int firstDestinationChannel = preference.getFirstInterleavedChannel();
                int lastDestinationChannel = preference.getLastInterleavedChannel();
                int destinationChannelCount = (lastDestinationChannel - firstDestinationChannel) + 1;
                int destinationLayerCount = destinationChannelCount / preference.getPortsPerLayer();

                // Can client port range be mapped to available port range?
                if (destinationLayerCount > this.transportDescription.getLayers() || (destinationChannelCount % preference.getPortsPerLayer()) != 0) {
                    RequestException.create(request.getProtocolVersion(),
                                            RtspStatusCode.BadRequest,
                                            "interleaved channel range specified in SETUP Transport header cannot be mapped to media port range",
                                            log.getPrefix(),
                                            logger).setResponse(response);
                    return true;
                }

                try {
                    int channelsPerLayer = preference.getPortsPerLayer();
                    for (int layerIndex = 0; layerIndex < destinationLayerCount; layerIndex++) {
                        for (int channelIndex = 0; channelIndex < channelsPerLayer; channelIndex++) {
                            int index = layerIndex * channelsPerLayer + channelIndex;
                            int channel = firstDestinationChannel + index;

                            if (isServerSourceChannelRequired()) {
                                try {
                                    // Construct server->client path for media packets
                                    if (logger.isLoggable(Level.FINER)) {
                                        logger.finer(log.msg("constructing server->client channel; layer="+layerIndex+" channel="+channelIndex + " channel-number="+channel));
                                    }

                                    OutputChannel<ByteBuffer> clientPacketSink = new InterleavedPacketOutputChannel(channel, request.getConnection());
                                    MessageSource<ByteBuffer> serverPacketSource = constructServerPacketSource(layerIndex, channelIndex, clientPacketSink);
                                    this.serverPacketChannels.add(serverPacketSource);

                                }
                                catch (SdpException e) {
                                    RequestException.create(request.getProtocolVersion(),
                                                            RtspStatusCode.InvalidMedia,
                                                            "cannot construct channel for sending media packets",
                                                            e,
                                                            log.getPrefix(),
                                                            logger).setResponse(response);
                                    return true;
                                }
                                catch (IOException e) {
                                    RequestException.create(request.getProtocolVersion(),
                                                            RtspStatusCode.InternalServerError,
                                                            "cannot construct channel for sending media packets",
                                                            e,
                                                            log.getPrefix(),
                                                            logger).setResponse(response);
                                    return true;
                                }
                            }

                            if (isClientSourceChannelRequired()) {
                                try {
                                    // Construct client->server path for media packets
                                    if (logger.isLoggable(Level.FINER)) {
                                        logger.finer(log.msg("constructing client->server channel; layer="+layerIndex+" channel="+channelIndex + " channel-number="+channel));
                                    }

                                    OutputChannel<ByteBuffer> serverPacketSink = constructServerPacketSink(layerIndex, channelIndex);
                                    this.presentation.setInterleavedChannel(channel, serverPacketSink);
                                }
                                catch (SdpException e) {
                                    RequestException.create(request.getProtocolVersion(),
                                                            RtspStatusCode.InvalidMedia,
                                                            "cannot construct channel for receiving media packets",
                                                            e,
                                                            log.getPrefix(),
                                                            logger).setResponse(response);
                                    return true;
                                }
                                catch (IOException e) {
                                    RequestException.create(request.getProtocolVersion(),
                                                            RtspStatusCode.InternalServerError,
                                                            "cannot construct channel for receiving media packets",
                                                            e,
                                                            log.getPrefix(),
                                                            logger).setResponse(response);
                                    return true;
                                }
                            }
                        }
                    }

                    acceptedTransportDescription.setInterleavedChannelRange(firstDestinationChannel, lastDestinationChannel);

                    this.firstChannelIndex = firstDestinationChannel;
                    this.channelCount = destinationChannelCount;

                    setupComplete = true;

                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(log.msg("candidate transport setup accepted: " + acceptedTransportDescription.toString()));
                    }

                    break;
                }
                finally {
                    if (!setupComplete) {

                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer(log.msg("candiate transport setup failed - removing channels already created"));
                        }

                        // Close client packet channels
                        for (int i = firstDestinationChannel; i < destinationChannelCount; i++) {
                            try {
                                this.presentation.closeInterleavedChannel(i);
                            }
                            catch (IOException e) {
                            }
                            catch (InterruptedException e1) {
                                Thread.currentThread().interrupt();
                            }
                        }

                        // Close the server packet channels
                        Iterator<MessageSource<ByteBuffer>> channelIter = this.serverPacketChannels.iterator();
                        while (channelIter.hasNext()) {
                            try {
                                channelIter.next().close();
                            }
                            catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            channelIter.remove();
                        }
                    }
                }
            }

        }

        if (setupComplete) {
            response.setStatus(RtspStatusCode.OK);
            response.setHeader(new SimpleMessageHeader(RtspHeaderName.TRANSPORT, acceptedTransportDescription.toString()));
        }
        else {
            RequestException.create(request.getProtocolVersion(),
                                    RtspStatusCode.UnsupportedTransport,
                                    message,
                                    log.getPrefix(),
                                    logger).setResponse(response);
        }

        return true;
    }

    /**
     * 
     * @param request
     * @param response
     * @return
     */
    public boolean handlePlay(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handlePlay", request, response));
        }

        if (!isPlayAllowed()) {
            setMethodNotValidInThisState(request, response);
            return true;
        }

        if (doHandlePlay(request, response)) {
            if (response.getStatus().equals(RtspStatusCode.OK)) {
                this.state = State.PLAYING;
            }
            return true;
        }

        return false;
    }

    /**
     * 
     * @param request
     * @param response
     * @return
     */
    protected boolean doHandlePlay(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("doHandlePlay", request, response));
        }

        // Start the server packet channels
        boolean startFailed = false;
        Iterator<MessageSource<ByteBuffer>> channelIter = this.serverPacketChannels.iterator();

        while (channelIter.hasNext()) {
            try {
                channelIter.next().start();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                response.setStatus(RtspStatusCode.ServiceUnavailable);
                startFailed=true;
                break;
            }
            catch (IOException e) {
                RequestException.create(request.getProtocolVersion(),
                                        RtspStatusCode.InternalServerError,
                                        e,
                                        log.getPrefix(),
                                        logger).setResponse(response);
                startFailed = true;
                break;
            }
        }

        if (startFailed) {
            channelIter = this.serverPacketChannels.iterator();
            while (channelIter.hasNext()) {
                try {
                    channelIter.next().stop();
                }
                catch (IOException e) {
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return true;
        }
        else {

            if (this.transport == Transport.UDP) {

                // Start the client packet channels
                channelIter = this.clientPacketChannels.iterator();
                while (channelIter.hasNext()) {
                    try {
                        channelIter.next().start();
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        response.setStatus(RtspStatusCode.ServiceUnavailable);
                        startFailed = true;
                        break;
                    }
                    catch (IOException e) {
                        RequestException.create(request.getProtocolVersion(),
                                                RtspStatusCode.InternalServerError,
                                                e,
                                                log.getPrefix(),
                                                logger).setResponse(response);
                        startFailed = true;
                        break;
                    }
                }

                if (startFailed) {
                    channelIter = this.clientPacketChannels.iterator();
                    while (channelIter.hasNext()) {
                        try {
                            channelIter.next().stop();
                        }
                        catch (IOException e) {
                        }
                        catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    return true;
                }

            }
            else if (this.transport == Transport.INTERLEAVED) {
            }
            else {
                return false;
            }
        }

        response.setStatus(RtspStatusCode.OK);
        return true;
    }

    /**
     * 
     * @param request
     * @param response
     * @return
     */
    public boolean handlePause(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handlePause", request, response));
        }

        if (!isPauseSupported()) {
            setMethodNotAllowed(request, response);
            return true;
        }

        if (!isPauseAllowed()) {
            setMethodNotValidInThisState(request, response);
            return true;
        }

        if (doHandlePause(request, response)) {
            if (response.getStatus().equals(RtspStatusCode.OK)) {
                this.state = State.READY;
            }
            return true;
        }

        return false;
    }

    /**
     * 
     * @param request
     * @param response
     * @return
     */
    protected boolean doHandlePause(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("doHandlePause", request, response));
        }

        // Stop the server packet channels
        Iterator<MessageSource<ByteBuffer>> channelIter = this.serverPacketChannels.iterator();

        while (channelIter.hasNext()) {
            try {
                channelIter.next().stop();
                response.setStatus(RtspStatusCode.OK);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                response.setStatus(RtspStatusCode.ServiceUnavailable);
                break;
            }
            catch (IOException e) {
                RequestException.create(request.getProtocolVersion(),
                                        RtspStatusCode.InternalServerError,
                                        e,
                                        log.getPrefix(),
                                        logger).setResponse(response);
                break;
            }
        }

        return true;
    }

    /**
     * 
     * @param request
     * @param response
     * @return
     */
    public boolean handleRecord(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleRecord", request, response));
        }

        if (!isRecordSupported()) {
            setMethodNotAllowed(request, response);
            return true;
        }

        if (!isRecordAllowed()) {
            setMethodNotValidInThisState(request, response);
            return true;
        }

        if (doHandleRecord(request, response)) {
            if (response.getStatus().equals(RtspStatusCode.OK)) {
                this.state = State.RECORDING;
            }
            return true;
        }

        return false;
    }

    /**
     * 
     * @param request
     * @param response
     * @return
     */
    protected boolean doHandleRecord(final Request request, final Response response) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("doHandleRecord", request, response));
        }
        return false;
    }

    /**
     * 
     * @param request
     * @param response
     * @return
     */
    public boolean handleTeardown(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleTeardown", request, response));
        }

        if (!isTeardownAllowed()) {
            setMethodNotValidInThisState(request, response);
            return true;
        }

        if (doHandleTeardown(request, response)) {
            if (response.getStatus().equals(RtspStatusCode.OK)) {
                this.state = State.INITIAL;
            }
            return true;
        }

        return false;
    }

    /**
     * 
     * @param request
     * @param response
     * @return
     */
    protected boolean doHandleTeardown(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("doHandleTeardown", request, response));
        }

        // Close the server packet channels
        Iterator<MessageSource<ByteBuffer>> channelIter = this.serverPacketChannels.iterator();
        while (channelIter.hasNext()) {
            try {
                channelIter.next().close();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            channelIter.remove();
        }

        if (this.transport == Transport.UDP) {

            // Stop the client packet channels
            channelIter = this.clientPacketChannels.iterator();
            while (channelIter.hasNext()) {
                try {
                    channelIter.next().close();
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    response.setStatus(RtspStatusCode.ServiceUnavailable);
                    break;
                }
            }
    
        }
        else if (this.transport == Transport.INTERLEAVED) {

            for (int channelIndex = this.firstChannelIndex; channelIndex < this.channelCount; channelIndex++) {
                try {
                    this.presentation.closeInterleavedChannel(channelIndex);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    response.setStatus(RtspStatusCode.ServiceUnavailable);
                    break;
                }
                catch (IOException e) {
                    RequestException.create(request.getProtocolVersion(),
                                            RtspStatusCode.InternalServerError,
                                            e,
                                            log.getPrefix(),
                                            logger).setResponse(response);
                    break;
                }
            }
            

            this.firstChannelIndex = 0;
            this.channelCount = 0;

        }
        else {
            return false;
        }

        response.setStatus(RtspStatusCode.OK);
        return true;
    }

    /**
     * 
     * @param request
     * @param response
     * @return
     */
    public boolean handleGetParameter(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleGetParameter", request, response));
        }

        if (!isGetParameterSupported()) {
            setMethodNotAllowed(request, response);
            return true;
        }

        // The session will handle the GET_PARAMETER request if it has no entity body,
        // as the empty request is being used as a keep-alive message for the session.

        // If the request carries an entity body, the body will contain a list of parameter
        // names whose values should be returned in the response body as:
        // <name>:<value>

        // If the request specifies an unrecognized parameter, this handler will respond
        // with InvalidParameter with an entity body containing the parameter name

        // Some clients will use this request as a session keep-alive message.

        return doHandleGetParameter(request, response);
    }

    /**
     * 
     * @param request
     * @param response
     * @return
     */
    protected boolean doHandleGetParameter(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("doHandleGetParameter", request, response));
        }

        response.setStatus(RtspStatusCode.ParameterNotUnderstood);
        Entity entity = request.getEntity();
        if (entity != null && entity.getContentLength() > 0) {
            response.setEntity(entity);
        }
        return true;
    }

    /**
     * 
     * @param request
     * @param response
     */
    public boolean handleSetParameter(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleSetParameter", request, response));
        }

        if (!isSetParameterSupported()) {
            setMethodNotAllowed(request, response);
            return true;
        }

        // The SET_PARAM request entity body will contain a list of parameter assignments as:
        // <name>:<value>

        // If the request specifies an unrecognized parameter, this handler will respond
        // with InvalidParameter. If the request specifies an read-only parameter, this handler
        // will respond with ParameterIsReadOnly.

        // Some clients will use this request as a session keep-alive message.

        return doHandleSetParameter(request, response);
    }

    /**
     * 
     * @param request
     * @param response
     * @return
     */
    protected boolean doHandleSetParameter(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("doHandleSetParameter", request, response));
        }

        response.setStatus(RtspStatusCode.InvalidParameter);
        Entity entity = request.getEntity();
        if (entity != null && entity.getContentLength() > 0) {
            response.setEntity(entity);
        }
        return true;
    }

    public void close() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("close"));
        }

        doClose();
    }
    
    protected void doClose() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("doClose"));
        }

        // Close the server packet channels
        Iterator<MessageSource<ByteBuffer>> channelIter = this.serverPacketChannels.iterator();
        while (channelIter.hasNext()) {
            try {
                channelIter.next().close();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            channelIter.remove();
        }

        if (this.transport == Transport.UDP) {

            // Stop the client packet channels
            channelIter = this.clientPacketChannels.iterator();
            while (channelIter.hasNext()) {
                try {
                    channelIter.next().close();
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
    
        }
        else if (this.transport == Transport.INTERLEAVED) {

            for (int channelIndex = this.firstChannelIndex; channelIndex < this.channelCount; channelIndex++) {
                try {
                    this.presentation.closeInterleavedChannel(channelIndex);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (IOException e) {
                }
            }
        }
        
    }

    protected void setMethodNotAllowed(Request request, Response response) {
        response.setStatus(RtspStatusCode.MethodNotAllowed);
        StringBuffer headerValue = new StringBuffer();
        headerValue.append("OPTIONS,SETUP,PLAY");
        if (isPauseSupported()) headerValue.append(",PAUSE,");
        if (isRecordSupported()) headerValue.append(",RECORD,");
        headerValue.append(",TEARDOWN");
        if (isGetParameterSupported()) headerValue.append(",GET_PARAMETER,");
        if (isSetParameterSupported()) headerValue.append(",SET_PARAMETER,");
        MessageHeader header = new SimpleMessageHeader(RtspHeaderName.ALLOW, headerValue.toString());
        response.setHeader(header);
    }

    protected void setMethodNotValidInThisState(Request request, Response response) {
        response.setStatus(RtspStatusCode.MethodNotValidInThisState);

        StringBuffer headerValue = new StringBuffer();
        headerValue.append("OPTIONS");

        switch (this.state) {
        case INITIAL:
            headerValue.append(",SETUP");
            break;
        case READY:
            headerValue.append(",PLAY");
            if (isRecordSupported()) {
                headerValue.append(",RECORD");
            }
            headerValue.append(",SETUP");
            headerValue.append(",TEARDOWN");
            break;
        case PLAYING:
            headerValue.append(",PLAY");
            if (isPauseSupported()) {
                headerValue.append(",PAUSE");
            }
            headerValue.append(",SETUP");
            headerValue.append(",TEARDOWN");
            break;
        case RECORDING:
            headerValue.append(",RECORD");
            if (isPauseSupported()) {
                headerValue.append(",PAUSE");
            }
            headerValue.append(",SETUP");
            headerValue.append(",TEARDOWN");
            break;
        }

        if (isGetParameterSupported()) headerValue.append("GET_PARAMETER");
        if (isSetParameterSupported()) headerValue.append("SET_PARAMETER");

        MessageHeader header = new SimpleMessageHeader(RtspHeaderName.ALLOW, headerValue.toString());

        response.setHeader(header);
    }

    /**
     * 
     * @param layer - index used to select a layer (a port pair in RTP/RTCP).
     * @param channelIndex - index used to select a port within a layer (e.g. 0=RTP and 1=RTCP).
     * @return
     * @throws IOException 
     * @throws SdpException 
     */
    protected abstract MessageSource<ByteBuffer> constructServerPacketSource(final int layer,
                                                                             final int channelIndex,
                                                                             final OutputChannel<ByteBuffer> clientPacketSink) throws SdpException, IOException;

    /**
     * 
     * @param layer - index used to select a layer (a port pair in RTP/RTCP).
     * @param channelIndex - index used to select a port within a layer (e.g. 0=RTP and 1=RTCP).
     * @return
     */
    protected abstract OutputChannel<ByteBuffer> constructServerPacketSink(final int layer, final int channelIndex) throws SdpException, IOException;

}
