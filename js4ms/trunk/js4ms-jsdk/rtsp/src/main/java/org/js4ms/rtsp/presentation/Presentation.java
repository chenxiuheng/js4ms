package org.js4ms.rtsp.presentation;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * Presentation.java [org.js4ms.jsdk:rtsp]
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
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;

import org.js4ms.common.util.logging.Log;
import org.js4ms.io.channel.OutputChannel;
import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.entity.Entity;
import org.js4ms.rest.entity.StringEntity;
import org.js4ms.rest.handler.TransactionHandler;
import org.js4ms.rest.header.SimpleMessageHeader;
import org.js4ms.rest.message.MessageHeader;
import org.js4ms.rest.message.Method;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;
import org.js4ms.rtsp.message.RtspHeaderName;
import org.js4ms.rtsp.message.RtspMethod;
import org.js4ms.rtsp.message.RtspStatusCode;
import org.js4ms.rtsp.rtp.InterleavedPacketReader;





/**
 * A representation of a RTSP presentation.
 * Presentations may be instantiated as a result of an OPTIONS or DESCRIBE request.
 * An RTSP session 
 * 
 *
 * @author gbumgard
 */
public abstract class Presentation implements TransactionHandler {

    /**
     * An enumeration of presentation description source types.
     */
    public enum Source {

        /**
         * Indicates that a presentation description was sent or received as the body of a DESCRIBE response.
         * A server generates the description and delivers it to a client. 
         */
        DESCRIBE,

        /**
         * Indicates that a presentation description was sent or received as the body of an ANNOUNCE request.
         * A client generates the description and sends it to the server to establish a recording session.
         * A server generates the description and sends it to the client to update the presentation description.
         */
        ANNOUNCE
    }


    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(Presentation.class.getName());

    protected final static String STREAM_CONTROL_PREFIX = "trackID=";

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final Log log = new Log(this);

    /**
     * The presentation URI.
     */
    protected final URI uri;

    /**
     * An SDP description of the presentation.
     */
    protected final SessionDescription sessionDescription;

    protected Source source;

    /**
     * String representation of the session description.
     * Used to construct response for DESCRIBE requests.
     */
    protected String cachedSdp = null;

    /**
     * Indicates whether an aggregate TEARDOWN request is required
     * to terminate the current session.
     */
    protected boolean isTeardownRequired = false;

    protected InterleavedPacketReader reader = null;

    protected final ArrayList<MediaStream> mediaStreams = new ArrayList<MediaStream>();


    /*-- Member functions ----------------------------------------------------*/

    /**
     * 
     * @param identifier
     */
    protected Presentation(final URI uri,
                           final Source source,
                           final SessionDescription sessionDescription) {
        this.uri = uri;
        this.source = source;
        this.sessionDescription = sessionDescription;
    }

    public URI getUri() {
        return this.uri;
    }

    /**
     * 
     */
    public Source getSource() {
        return this.source;
    }

    public boolean isDescribeSupported() {
        return true;
    }

    public boolean isPauseSupported() {
        return doIsPauseSupported();
    }

    protected abstract boolean doIsPauseSupported();

    public boolean isRecordSupported() {
        return this.source == Source.ANNOUNCE && doIsRecordSupported();
    }

    protected abstract boolean doIsRecordSupported();

    public boolean isGetParameterSupported() {
        // Always allow GET_PARAMETER since it may be used as a keep-alive message
        return true;
    }

    public boolean isSetParameterSupported() {
        // Always allow SET_PARAMETER since it may be used as a keep-alive message? But returns BadRequest?
        // TODO:
        return false;
    }

    /**
     * Indicates whether presentation accepts PLAY, PAUSE, RECORD and TEARDOWN
     * requests that target the entire presentation.
     * @return
     */
    public boolean isAggregateControlAllowed() {
        return doIsAggregateControlAllowed();
    }

    /**
     * 
     * @return
     */
    protected boolean doIsAggregateControlAllowed() {
        return true;
    }

    /**
     * Indicates whether presentation accepts PLAY, PAUSE, RECORD and TEARDOWN
     * requests that target individual streams.
     * @return
     */
    public boolean isAggregateControlRequired() {
        return doIsAggregateControlRequired();
    }

    /**
     * 
     * @return
     */
    protected boolean doIsAggregateControlRequired() {
        return false;
    }

    public boolean handleTransaction(final Request request, final Response response) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleTransaction", request, response));
        }

        boolean handled = false;

        Method method = request.getRequestLine().getMethod();

        if (method.equals(RtspMethod.OPTIONS)) {
            handled = handleOptions(request, response);
        }
        else if (method.equals(RtspMethod.DESCRIBE)) {
            handled = handleDescribe(request, response);
        }
        else if (method.equals(RtspMethod.SETUP)) {
            handled = handleSetup(request, response);
        }
        else if (method.equals(RtspMethod.PLAY)) {
            handled = handlePlay(request, response);
        }
        else if (method.equals(RtspMethod.PAUSE)) {
            handled = handlePause(request, response);
        }
        else if (method.equals(RtspMethod.RECORD)) {
            handled = handleRecord(request, response);
        }
        else if (method.equals(RtspMethod.TEARDOWN)) {
            handled = handleTeardown(request, response);
        }
        else if (method.equals(RtspMethod.GET_PARAMETER)) {
            handled = handleGetParameter(request, response);
        }
        else if (method.equals(RtspMethod.SET_PARAMETER)) {
            handled = handleSetParameter(request, response);
        }
        else {
            setMethodNotAllowed(request, response);
            handled = true;
        }

        // Does the server need to handle incoming interleaved packets?
        if (this.reader != null && this.reader.hasActiveChannels()) {
            if (handled) {
                // Send the response immediately - sets isSent flag which will prevent retransmission later.

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(log.msg("sending response:"));
                    response.log(logger, Level.FINE);
                }

                logger.info(log.msg(response.getConnection().getRemoteAddress().getAddress().toString()+" "+response.getStatusLine().toString()));

                response.send();
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.finer(log.msg("waiting for interleaved packets..."));
            }

            // Wait for packets to arrive over control connection and
            // deliver them registered output channels.
            // Blocking call that returns when an RTSP message is received or the connection is closed.
            // TODO: do this even if request is NOT handled? Should not be possible anyway.
            //this.reader.readPackets(request.getConnection());
        }

        return handled;
    }

    private boolean handleOptions(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleOptions", request, response));
        }

        int streamIndex;
        try {
            streamIndex = getStreamIndexFromControlUri(request);
        }
        catch (RequestException e) {
            e.setResponse(response);
            return true;
        }

        if (streamIndex == -1) {
            // Request URI is an aggregate control URI
            StringBuffer headerValue = new StringBuffer();
            headerValue.append("DESCRIBE,SETUP,PLAY");
            if (isPauseSupported()) headerValue.append(",PAUSE");
            if (isRecordSupported()) headerValue.append(",RECORD");
            headerValue.append(",TEARDOWN");
            if (isGetParameterSupported()) headerValue.append(",GET_PARAMETER");
            if (isSetParameterSupported()) headerValue.append(",SET_PARAMETER");

            MessageHeader header = new SimpleMessageHeader(RtspHeaderName.PUBLIC,headerValue.toString());
            response.setStatus(RtspStatusCode.OK);
            response.setHeader(header);
            return true;
        }
        else {
            // Request URI is a stream control URI
            if (!isAggregateControlRequired()) {
                response.setStatus(RtspStatusCode.OnlyAggregateOperationAllowed);
                return true;
            }

            MediaStream mediaStream = this.mediaStreams.get(streamIndex);
            if (mediaStream == null) {
                RequestException.create(request.getProtocolVersion(),
                                        RtspStatusCode.BadRequest,
                                        "stream identified in PLAY request does not exist",
                                        log.getPrefix(),
                                        logger).setResponse(response);
                return true;
            }

            return mediaStream.handleOptions(request, response);
        }

    }

    private boolean handleDescribe(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleDescribe", request, response));
        }

        if (this.cachedSdp == null) {
            try {
                // Rewrite a=control records to ensure we receive something predictable from clients
                // Aggregate control URI is original RTSP URL
                sessionDescription.setAttribute("control", "*");
                Vector<?> mediaDescriptions = this.sessionDescription.getMediaDescriptions(false);
                if (mediaDescriptions != null) { 
                    for(int i = 0; i < mediaDescriptions.size(); i++) {
                        MediaDescription mediaDescription = (MediaDescription)mediaDescriptions.get(i);
                        mediaDescription.setAttribute("control", STREAM_CONTROL_PREFIX + i);
                    }
                }
            }
            catch (SdpException e) {
                RequestException.create(request.getProtocolVersion(),
                        RtspStatusCode.UnsupportedMediaType,
                        "malformed SDP description detected",
                        e,
                        log.getPrefix(),
                        logger).setResponse(response);
                return true;
            }
            this.cachedSdp = this.sessionDescription.toString();
        }

        StringEntity entity = new StringEntity(this.cachedSdp);

        entity.setContentType("application/sdp");

        // Set Content-Base header - clients should use this to generate control URIs.
        entity.setContentBase(request.getRequestLine().getUri().toString()+"/");

        response.setStatus(RtspStatusCode.OK);
        response.setEntity(entity);

        return true;
    }

    private boolean handleSetup(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleSetup", request, response));
        }

        int streamIndex;

        try {
            streamIndex = getStreamIndexFromControlUri(request);
        }
        catch (RequestException e) {
            e.setResponse(response);
            return true;
        }

        if (streamIndex == -1) {
            // The SETUP request does not specified stream ID
            // This is OK if the presentation only contains a single stream,
            // otherwise, this is treated as an illegal attempt to use aggregate control.
            try {
                if (this.sessionDescription.getMediaDescriptions(false).size() > 1) {
                    RequestException.create(request.getProtocolVersion(),
                                            RtspStatusCode.AggregateOperationNotAllowed,
                                            "stream identifier in SETUP request is missing",
                                            log.getPrefix(),
                                            logger).setResponse(response);
                    return true;
                }
                else {
                    streamIndex = 0;
                }
            }
            catch (SdpException e) {
                RequestException.create(request.getProtocolVersion(),
                                        RtspStatusCode.BadRequest,
                                        "SDP description is malformed",
                                        e,
                                        log.getPrefix(),
                                        logger).setResponse(response);
                return true;
            }
        }

        MediaStream mediaStream = this.mediaStreams.get(streamIndex);
        if (mediaStream == null) {
            RequestException.create(request.getProtocolVersion(),
                                    RtspStatusCode.BadRequest,
                                    "stream identified in SETUP request does not exist",
                                    log.getPrefix(),
                                    logger).setResponse(response);
            return true;
        }

        if (mediaStream.handleSetup(request, response)) {
            if (response.getStatus().equals(RtspStatusCode.OK)) {
                this.isTeardownRequired = true;
            }

            if (logger.isLoggable(Level.FINER)) {
                logger.finer(log.msg("setup media stream "+request.getRequestLine().getUri().toString()+" response is "+response.getStatusLine().toString()));
            }

            return true;
        }

        return false;
    }

    private boolean handlePlay(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handlePlay", request, response));
        }

        int streamIndex;
        try {
            streamIndex = getStreamIndexFromControlUri(request);
        }
        catch (RequestException e) {
            e.setResponse(response);
            return true;
        }

        StringBuffer headerValue = new StringBuffer();

        if (streamIndex == -1) {
            // Request URI is an aggregate control URI
            if (!isAggregateControlAllowed()) {
                response.setStatus(RtspStatusCode.AggregateOperationNotAllowed);
                return true;
            }

            boolean handled = false;
            for (int i=0; i < this.mediaStreams.size(); i++) {
                if (this.mediaStreams.get(i).handlePlay(request, response)) {
                    handled = true;
                    // Bail if one of the streams fails to play
                    if (!response.getStatus().equals(RtspStatusCode.OK)) {
                        break;
                    }
                    if (i > 0) {
                        headerValue.append(',');
                    }
                    headerValue.append("url="+this.uri+"/"+STREAM_CONTROL_PREFIX+i);
                }
            }

            if (response.getStatus().equals(RtspStatusCode.OK)) {

                MessageHeader rtpInfo = new SimpleMessageHeader(RtspHeaderName.RTP_INFO, headerValue.toString());

                response.addHeader(rtpInfo);
            }

            return handled;
        }
        else {
            // Request URI is a stream control URI
            if (!isAggregateControlRequired()) {
                response.setStatus(RtspStatusCode.OnlyAggregateOperationAllowed);
                return true;
            }

            MediaStream mediaStream = this.mediaStreams.get(streamIndex);
            if (mediaStream == null) {
                RequestException.create(request.getProtocolVersion(),
                                        RtspStatusCode.BadRequest,
                                        "stream identified in PLAY request does not exist",
                                        log.getPrefix(),
                                        logger).setResponse(response);
                return true;
            }

            if (mediaStream.handlePlay(request, response)) {
                if (response.getStatus().equals(RtspStatusCode.OK)) {
                    MessageHeader rtpInfo = new SimpleMessageHeader(RtspHeaderName.RTP_INFO, 
                                                                    "url="+this.uri+"/"+STREAM_CONTROL_PREFIX+streamIndex);
                    response.addHeader(rtpInfo);
                }
                return true;
            }

            return false;
        }
    }

    /**
     * 
     * @param request
     * @param response
     * @return
     */
    private boolean handlePause(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handlePause", request, response));
        }

        if (!isPauseSupported()) {
            setMethodNotAllowed(request, response);
            return true;
        }

        int streamIndex;
        try {
            streamIndex = getStreamIndexFromControlUri(request);
        }
        catch (RequestException e) {
            e.setResponse(response);
            return true;
        }

        if (streamIndex == -1) {
            // Request URI is an aggregate control URI
            if (!isAggregateControlAllowed()) {
                response.setStatus(RtspStatusCode.AggregateOperationNotAllowed);
                return true;
            }

            boolean handled = false;
            for (int i=0; i < this.mediaStreams.size(); i++) {
                if (this.mediaStreams.get(i).handlePause(request, response)) {
                    handled = true;
                    // Bail if one of the streams fails to pause
                    if (!response.getStatus().equals(RtspStatusCode.OK)) {
                        break;
                    }
                }
            }

            return handled;
        }
        else {
            // Request URI is a stream control URI
            if (!isAggregateControlRequired()) {
                response.setStatus(RtspStatusCode.OnlyAggregateOperationAllowed);
                return true;
            }

            MediaStream mediaStream = this.mediaStreams.get(streamIndex);
            if (mediaStream == null) {
                RequestException.create(request.getProtocolVersion(),
                                        RtspStatusCode.BadRequest,
                                        "stream identified in PAUSE request does not exist",
                                        log.getPrefix(),
                                        logger).setResponse(response);
                return true;
            }

            return mediaStream.handlePause(request, response);
        }
    }

    private boolean handleRecord(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleRecord", request, response));
        }

        if (!isRecordSupported()) {
            setMethodNotAllowed(request, response);
            return true;
        }

        int streamIndex;
        try {
            streamIndex = getStreamIndexFromControlUri(request);
        }
        catch (RequestException e) {
            e.setResponse(response);
            return true;
        }

        if (streamIndex == -1) {
            // Request URI is an aggregate control URI
            if (!isAggregateControlAllowed()) {
                response.setStatus(RtspStatusCode.AggregateOperationNotAllowed);
                return true;
            }

            boolean handled = false;
            for (int i=0; i < this.mediaStreams.size(); i++) {
                if (this.mediaStreams.get(i).handleRecord(request, response)) {
                    handled = true;
                    // Bail if one of the streams fails to record
                    if (!response.getStatus().equals(RtspStatusCode.OK)) {
                        break;
                    }
                }
            }

            return handled;
        }
        else {
            // Request URI is a stream control URI
            if (!isAggregateControlRequired()) {
                response.setStatus(RtspStatusCode.OnlyAggregateOperationAllowed);
                return true;
            }

            MediaStream mediaStream = this.mediaStreams.get(streamIndex);
            if (mediaStream == null) {
                RequestException.create(request.getProtocolVersion(),
                                        RtspStatusCode.BadRequest,
                                        "stream identified in RECORD request does not exist",
                                        log.getPrefix(),
                                        logger).setResponse(response);
                return true;
            }

            return mediaStream.handleRecord(request, response);
        }
    }

    private boolean handleTeardown(final Request request, final Response response) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleTeardown", request, response));
        }

        int streamIndex;
        try {
            streamIndex = getStreamIndexFromControlUri(request);
        }
        catch (RequestException e) {
            e.setResponse(response);
            return true;
        }

        if (streamIndex == -1) {
            // Request URI is an aggregate control URI

            boolean handled = false;
            for (int i=0; i < this.mediaStreams.size(); i++) {
                if (this.mediaStreams.get(i).handleTeardown(request, response)) {
                    handled = true;
                    // Bail if one of the streams fails to teardown
                    if (!response.getStatus().equals(RtspStatusCode.OK)) {
                        break;
                    }
                }
            }

            if (handled && response.getStatus().equals(RtspStatusCode.OK)) {
                response.setHeader(new SimpleMessageHeader(RtspHeaderName.CONNECTION,"close"));
            }

            return handled;
        }
        else {
            // Request URI is a stream control URI
            if (!isAggregateControlRequired()) {
                response.setStatus(RtspStatusCode.OnlyAggregateOperationAllowed);
                return true;
            }

            MediaStream mediaStream = this.mediaStreams.get(streamIndex);
            if (mediaStream == null) {
                RequestException.create(request.getProtocolVersion(),
                                        RtspStatusCode.BadRequest,
                                        "stream identified in PAUSE request does not exist",
                                        log.getPrefix(),
                                        logger).setResponse(response);
                return true;
            }

            return mediaStream.handleTeardown(request, response);
        }
    }

    private boolean handleGetParameter(final Request request, final Response response) {

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
    private boolean handleSetParameter(final Request request, final Response response) {

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
            logger.finer(log.entry("close"));
        }

        for (int i=0; i < this.mediaStreams.size(); i++) {
            this.mediaStreams.get(i).close();
        }
    }

    protected void constructMediaStreams() throws SdpException {
        Vector<?> mediaDescriptions = this.sessionDescription.getMediaDescriptions(false);
        if (mediaDescriptions != null) {
            for (int i = 0; i < mediaDescriptions.size(); i++) {
                setMediaStream(i, doConstructMediaStream(i));
            }
        }
    }

    protected void setMediaStream(final int index, final MediaStream mediaStream) {
        if (index > this.mediaStreams.size() - 1) {
            this.mediaStreams.add(index, mediaStream);
        }
        else {
            this.mediaStreams.set(index, mediaStream);
        }
    }

    protected abstract MediaStream doConstructMediaStream(int index) throws SdpException;

    public void setInterleavedChannel(final int channelIndex, final OutputChannel<ByteBuffer> outputChannel) {
        if (this.reader == null) {
            this.reader = new InterleavedPacketReader();
        }
        
        this.reader.set(channelIndex, outputChannel);
    }

    public void closeInterleavedChannel(final int channelIndex) throws IOException, InterruptedException {
        if (this.reader != null) {
            this.reader.close(channelIndex);
        }
    }

    /**
     * 
     * @param request
     * @param response
     */
    protected void setMethodNotAllowed(final Request request, final Response response) {
        response.setStatus(RtspStatusCode.MethodNotAllowed);
        StringBuffer headerValue = new StringBuffer();
        headerValue.append("OPTIONS");
        if (isDescribeSupported()) headerValue.append(",DESCRIBE");
        headerValue.append(",SETUP,PLAY");
        if (isPauseSupported()) headerValue.append(",PAUSE");
        if (isRecordSupported()) headerValue.append(",RECORD");
        headerValue.append(",TEARDOWN");
        if (isGetParameterSupported()) headerValue.append(",GET_PARAMETER");
        if (isSetParameterSupported()) headerValue.append(",SET_PARAMETER");
        MessageHeader header = new SimpleMessageHeader(RtspHeaderName.ALLOW, headerValue.toString());
        response.setHeader(header);
    }


    /**
     * 
     * @param request
     * @param response
     */
    protected void setInvalidParameter(Request request, Response response){
        response.setStatus(RtspStatusCode.InvalidParameter);
    }

    /**
     * 
     * @param request
     * @return
     */
    protected static URI getPresentationUriFromControlUri(final Request request) {

        // Strip off trailing "/trackID=x" to get presentation URI.
        String controlUri = request.getRequestLine().getUri().toString();
        int lastSlashIndex = controlUri.lastIndexOf("/");
        if ((lastSlashIndex != -1) &&
            (lastSlashIndex < controlUri.length() - 1) &&
            (controlUri.startsWith(STREAM_CONTROL_PREFIX, lastSlashIndex + 1))) {
            return URI.create(controlUri.substring(0, lastSlashIndex));
        }
        else {
            return request.getRequestLine().getUri();
        }
    }

    /**
     * 
     * @param request
     * @return
     * @throws RequestException
     */
    protected int getStreamIndexFromControlUri(final Request request) throws RequestException {

        String controlUri = request.getRequestLine().getUri().toString();
        int lastSlashIndex = controlUri.lastIndexOf("/");
        if ((lastSlashIndex != -1) &&
            (lastSlashIndex < controlUri.length() - 1) &&
            (controlUri.startsWith(STREAM_CONTROL_PREFIX, lastSlashIndex + 1))) {
            String streamIdentifier = controlUri.substring(lastSlashIndex + STREAM_CONTROL_PREFIX.length() + 1);
            if (streamIdentifier.length() > 0) {
                try {
                    return Integer.parseInt(streamIdentifier);
                }
                catch (NumberFormatException e) {
                    throw RequestException.create(request.getProtocolVersion(),
                                                  RtspStatusCode.BadRequest,
                                                  "stream control URI in "+request.getRequestLine().getMethod().getName()+" request is invalid",
                                                  e,
                                                  log.getPrefix(),
                                                  logger);
                }
            }
        }

        // Indicate that the control URI does not carry a stream identifier
        return -1;
    }

}
