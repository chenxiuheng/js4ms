package org.js4ms.rtsp.server;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * RtspTransactionHandler.java [org.js4ms.jsdk:rtsp]
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
import java.io.PushbackInputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.http.message.HttpHeaderName;
import org.js4ms.io.stream.Base64InputStream;
import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.entity.Entity;
import org.js4ms.rest.entity.StringEntity;
import org.js4ms.rest.handler.TransactionHandler;
import org.js4ms.rest.header.SimpleMessageHeader;
import org.js4ms.rest.message.MessageHeader;
import org.js4ms.rest.message.Method;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;
import org.js4ms.rest.server.SessionManager;
import org.js4ms.rtsp.message.RtspHeaderName;
import org.js4ms.rtsp.message.RtspMethod;
import org.js4ms.rtsp.message.RtspStatusCode;
import org.js4ms.rtsp.presentation.Presentation;
import org.js4ms.rtsp.presentation.PresentationResolver;
import org.js4ms.server.Connection;





/**
 * 
 * 
 *
 * @author gbumgard
 */
public class RtspTransactionHandler implements TransactionHandler {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(RtspTransactionHandler.class.getName());

    public final static String TUNNEL_SESSION_COOKIE_HEADER_NAME = "x-sessioncookie";
    public final static String TUNNEL_CONTENT_TYPE = "application/x-rtsp-tunnelled";

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final Log log = new Log(this);

    protected int sessionCount = 0;

    protected final PresentationResolver resolver;

    protected final Timer sessionTimer = new Timer("RTSP session timer");

    private final HashMap<String, Connection> outputConnections = new HashMap<String, Connection>();

    /**
     * 
     */
    protected final SessionManager sessionManager = new SessionManager();

    /*-- Member Functions  ----------------------------------------------------*/

    /**
     * 
     * @param presentationFactory
     */
    public RtspTransactionHandler(final PresentationResolver resolver) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("RtspTransactionHandler", resolver));
        }

        this.resolver = resolver;
    }

    public void terminate() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("close"));
        }

        this.sessionTimer.cancel();

        this.sessionManager.terminateSessions();
    }

    @Override
    public boolean handleTransaction(Request request, Response response) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleTransaction", request, response));
        }

        return doHandleTransaction(request, response);
    }

    public boolean doHandleTransaction(Request request, Response response) throws IOException {

        Method method = request.getRequestLine().getMethod();

        if (request.containsHeader(RtspHeaderName.SESSION)) {
            // Lookup Session
            String identifier = request.getHeader(RtspHeaderName.SESSION).getValue();
            RtspSession session = (RtspSession)this.sessionManager.getSession(identifier);
            if (session == null) {
                String message = "rejected "+method.getName()+" request because the session '"+identifier+"' does not exist";
                logger.info(log.msg(message));
                response.setStatus(RtspStatusCode.SessionNotFound);
                response.setEntity(new StringEntity(message));
                return true;
            }
            else {
                return session.handleTransaction(request, response);
            }
        }
        else {
            if (method.equals(RtspMethod.GET)) {
                return handleGet(request, response);
            }
            else if (method.equals(RtspMethod.POST)) {
                return handlePost(request, response);
            }
            else if (method.equals(RtspMethod.OPTIONS)) {
                return handleOptions(request, response);
            }
            else if (method.equals(RtspMethod.ANNOUNCE)) {
                return handleAnnounce(request, response);
            }
            else if (method.equals(RtspMethod.DESCRIBE)) {
                return handleDescribe(request, response);
            }
            else if (method.equals(RtspMethod.SETUP)) {
                return handleSetup(request, response);
            }
            else {
                return setMethodNotAllowed(request, response);
            }
        }
    }

    protected boolean isAnnounceImplemented() {
        return false;
    }

    protected boolean isAnnounceSupported(Request request) {
        return false;
    }

    protected boolean isDescribeImplemented() {
        return false;
    }

    protected boolean isDescribeSupported(Request request) {
        return false;
    }

    protected boolean isParameterValid(Request request) {
        return false;
    }

    /**
     * See http://developer.apple.com/quicktime/icefloe/dispatch028.html
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    protected boolean handleGet(Request request, Response response) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleGet", request, response));
        }

        MessageHeader header = request.getHeader(RtspHeaderName.ACCEPT);
        if (header == null || !header.getValue().equals(TUNNEL_CONTENT_TYPE)) {
            RequestException.create(request.getProtocolVersion(),
                                    RtspStatusCode.MethodNotAllowed,
                                    "HTTP GET request must specify acceptable content type is "+TUNNEL_CONTENT_TYPE).setResponse(response);
        }

        header = request.getHeader(TUNNEL_SESSION_COOKIE_HEADER_NAME);
        if (header == null) {
            RequestException.create(request.getProtocolVersion(),
                                    RtspStatusCode.BadRequest,
                                    "HTTP tunneling request is missing x-sessioncookie header").setResponse(response);
            return true;
        }

        String sessionCookie = header.getValue();

        Connection outputConnection = request.getConnection();

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.msg("received HTTP GET tunneling request session-cookie="+sessionCookie+" connection="+Logging.identify(outputConnection)));
        }

        this.outputConnections.put(sessionCookie, outputConnection);

        // Add headers required by HTTP tunneling protocol
        response.setHeader(new SimpleMessageHeader(Entity.CONTENT_TYPE, TUNNEL_CONTENT_TYPE));
        response.setHeader(new SimpleMessageHeader(HttpHeaderName.CACHE_CONTROL,"no-store"));
        response.setHeader(new SimpleMessageHeader(HttpHeaderName.PRAGMA,"no-cache"));
        response.setHeader(new SimpleMessageHeader(RtspHeaderName.CONNECTION,"close"));
        response.setStatus(RtspStatusCode.OK);
        return true;
    }

    /**
     * See http://developer.apple.com/quicktime/icefloe/dispatch028.html
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    protected boolean handlePost(Request request, Response response) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handlePost", request, response));
        }

        MessageHeader header = request.getHeader(Entity.CONTENT_TYPE);
        if (header == null || !header.getValue().equals(TUNNEL_CONTENT_TYPE)) {
            RequestException.create(request.getProtocolVersion(),
                                    RtspStatusCode.MethodNotAllowed,
                                    "HTTP POST request must specify content type as "+TUNNEL_CONTENT_TYPE).setResponse(response);
        }

        header = request.getHeader(TUNNEL_SESSION_COOKIE_HEADER_NAME);
        if (header == null) {
            RequestException.create(request.getProtocolVersion(),
                                    RtspStatusCode.BadRequest,
                                    "HTTP tunneling request is missing x-sessioncookie header").setResponse(response);
            return true;
        }

        String sessionCookie = header.getValue();

        Connection connection = request.getConnection();
        Connection outputConnection = this.outputConnections.get(sessionCookie);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.msg("received HTTP tunnel POST request session-cookie="+sessionCookie+" input-connection="+Logging.identify(connection)+" output-connection="+Logging.identify(outputConnection)));
        }

        // Replace input stream with Base-64 decoder filter
        connection.setInputStream(new PushbackInputStream(new Base64InputStream(connection.getInputStream())));
        connection.setOutputStream(outputConnection.getOutputStream());

        // Prevent request entity created by the POST from being flushed
        // as it represents the tunneled message stream
        request.getEntity().ignoreContent();

        // Prevent a response from being sent since this would close the tunnel.
        response.isSent(true);

        return true;
    }

    protected boolean handleAnnounce(Request request, Response response) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleAnnounce", request, response));
        }

        // Construct recordable presentation with SDP description sent by client
        setMethodNotAllowed(request, response);
        return true;
    }

    protected boolean handleOptions(Request request, Response response) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleOptions", request, response));
        }

        URI requestUri = request.getRequestLine().getUri();
        if (requestUri.getPath().length() == 0 || requestUri.getPath().equals("*")) {
            MessageHeader header = new SimpleMessageHeader(RtspHeaderName.PUBLIC,
                                                           "GET,POST,DESCRIBE,SETUP,PLAY,PAUSE,RECORD,TEARDOWN,GET_PARAMETER,SET_PARAMETER");
            response.setHeader(header);
            response.setStatus(RtspStatusCode.OK);
            return true;
        }
        else {
            return dispatchTransaction(request, response);
        }
    }

    /**
     * Handles DESCRIBE requests.<p>
     * Request Headers:
     * <ul>
     *   <li>[Accept] - Is there a presentation description for resource URI that matches an acceptable MIME type?
     *   <li>[Accept-Encoding]
     *   What content encodings are acceptable and in what order? "*", identity, format (e.g. gzip).
     *   Does original content encoding match an acceptable encoding in list?
     *   Can the content be encoded into an encoding in the list?
     *   Order encodings by q-value.
     *   Step through list until an encoding is found for which there is a matching encoder.
     *   Bind the selected encoder to the response/entity.
     *   <li>[Accept-Language]
     *   <li>[Authorization]
     *   <li>[Bandwidth]
     *   <li>[Blocksize]
     *   <li>Connection
     *   <li>CSeq
     *   <li>[Date]
     *   <li>[From]
     *   <li>[If-Modified-Since]
     *   <li>[Pragma] impl specific
     *   <li>Proxy-Require
     *   <li>[Referer] - Ignore
     *   <li>Require - Sent to query for supported options. Default: Respond with Unsupported
     *   <li>Session
     *   <li>Timestamp - Must echo in response
     *   <li>[User-Agent]
     *   <li>[Via]
     *   <li>[WWW-Authenticate]
     * </ul>
     * Response Headers:
     * <ul>
     *   <li>[Allow]
     *   <li>Connection
     *   <li>CSeq
     *   <li>[Date]
     *   <li>[ETag] - Used with If-Match on SETUP
     *   <li>[Public]
     *   <li>[Retry-After]
     *   <li>[Server]
     *   <li>Session
     *   <li>[Unsupported] - Sent in response to Require header
     *   <li>[Vary] - List of request header fields used to negotiate the response or "*" if something other than headers were used.
     *   <li>[Via]
     * </ul>
     * Response Entity Headers:
     * <ul>
     *   <li>[Content-Base]
     *   <li>Content-Encoding - What decoder must be used to decode the content? No header required for identity (no encoding).
     *   <li>Content-Language
     *   <li>Content-Length
     *   <li>[Content-Location]
     *   <li>Content-Type
     *   <li>[Expires]
     *   <li>[Last-Modified]
     * </ul>
     * @param request
     * @param response
     * @throws IOException
     * @throws RequestException 
     */
    protected boolean handleDescribe(Request request, Response response) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleDescribe", request, response));
        }

        return dispatchTransaction(request, response);
    }

    /**
     * Handles SETUP requests.<p>
     * Request Headers:
     * <ul>
     *   <li>[Accept-Language]
     *   <li>[Authorization]
     *   <li>[Bandwidth]
     *   <li>[Blocksize]
     *   <li>[Cache-Control]
     *   <li>[Conference]
     *   <li>Connection
     *   <li>CSeq
     *   <li>[Date]
     *   <li>[From]
     *   <li>[If-Modified-Since]
     *   <li>[Pragma] impl specific
     *   <li>Proxy-Require
     *   <li>[Referer] - Ignore
     *   <li>Require - Sent to query for supported options. Default: Respond with Unsupported
     *   <li>Timestamp - Must echo in response
     *   <li>[User-Agent]
     *   <li>[Via]
     *   <li>[WWW-Authenticate]
     * </ul>
     * Response Headers:
     * <ul>
     *   <li>[Allow]
     *   <li>Connection
     *   <li>CSeq
     *   <li>[Date]
     *   <li>[ETag] - Used with If-Match on SETUP
     *   <li>[Public]
     *   <li>[Retry-After]
     *   <li>[Server]
     *   <li>Session
     *   <li>[Unsupported] - Sent in response to Require header
     *   <li>[Vary] - List of request header fields used to negotiate the response or "*" if something other than headers were used.
     *   <li>[Via]
     * </ul>
     * Response Entity Headers:
     * <ul>
     *   <li>[Content-Base]
     *   <li>Content-Language
     *   <li>Content-Length
     *   <li>[Content-Location]
     *   <li>Content-Type
     * </ul>
     * @param request
     * @param response
     * @throws IOException
     * @throws RequestException 
     */
    protected boolean handleSetup(Request request, Response response) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleSetup", request, response));
        }

        // Add the session header here since presentation setup method will
        // send response directly when interleaved transport is used.
        String sessionId = getNewSessionId();
        if (!response.containsHeader(RtspHeaderName.SESSION)) {
            response.setHeader(new SimpleMessageHeader(RtspHeaderName.SESSION, sessionId + ";timeout=" + (RtspSession.getSessionTimeout() / 1000)));
        }

        Presentation presentation = null;
        try {
            presentation = this.resolver.getPresentation(request);
        }
        catch (RequestException e) {
            e.setResponse(response);
            return true;
        }

        if (presentation == null) {
            response.setStatus(RtspStatusCode.NotFound);
            return true;
        }

        if (presentation.handleTransaction(request, response)) {
            if (response.getStatus().equals(RtspStatusCode.OK)) {
                new RtspSession(sessionId, presentation, this.sessionManager, this.sessionTimer);
            }
            else {
                // We failed to create a session
                response.removeHeader(RtspHeaderName.SESSION);
            }
            return true;
        }

        // We failed to create a session - remove the header
        response.removeHeader(RtspHeaderName.SESSION);
        return false;
    }

    /**
     * 
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    protected boolean dispatchTransaction(final Request request, final Response response) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("dispatchTransaction", request, response));
        }

        Presentation presentation = null;
        try {
            presentation = this.resolver.getPresentation(request);
        }
        catch (RequestException e) {
            e.setResponse(response);
            return true;
        }
        if (presentation == null) {
            response.setStatus(RtspStatusCode.NotFound);
            return true;
        }
        return presentation.handleTransaction(request, response);
    }

    /**
     * 
     * @return
     */
    protected String getNewSessionId() {
        return String.valueOf(System.currentTimeMillis())+"-"+(++sessionCount);
    }

    protected boolean setMethodNotAllowed(final Request request, final Response response) {
        response.setStatus(RtspStatusCode.MethodNotAllowed);
        MessageHeader header = new SimpleMessageHeader("Allow","OPTIONS,DESCRIBE,SETUP");
        response.setHeader(header);
        return true;
    }
}
