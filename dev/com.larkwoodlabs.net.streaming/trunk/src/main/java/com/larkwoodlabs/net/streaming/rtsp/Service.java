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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Logging;

/**
 * Server-side RTSP protocol handler that coordinates each request-response message exchange
 * by dispatching incoming requests to the appropriate request handlers and sending the final
 * request response to the client via the originating connection handler.
 * The service object provides handlers for {@link Method#OPTIONS OPTIONS}
 * and {@link Method#DESCRIBE DESCRIBE} requests, it constructs {@link Session} objects
 * in response to {@link Method#SETUP SETUP} requests and it dispatches session-level 
 * requests to these Session objects.
 *
 * @author Gregory Bumgardner
 */
public class Service {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(ConnectionHandler.class.getName());

    /*-- Member Variables ----------------------------------------------------*/

    final String ObjectId = Logging.identify(this);
    
    final Server server;

    protected PresentationDescription lastPresentationDescription = null;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a service object for the specified {@link Server}.
     * @param server - The server that is responsible for instantiation of this object.
     *                 The server is used to register and retrieve active {@link Session} objects.
     */
    public Service(final Server server) {
        this.server = server;
    }

    /**
     * Services an RTSP request.
     * <p>
     * This method constructs a matching {@link Response} for each {@link Request}, attaches
     * or transfers some common headers to the new Response, passes the Request and Response to
     * a method which handles dispatching of the Request, and finally sends the updated
     * Response to the client via the {@link ConnectionHandler} that originated the Request.
     * @param request - The incoming RTSP request.
     * @throws Exception If an I/O error, thread interrupt or unrecoverable service exception occurs.
     */
    public void serviceRequest(Request request) throws Exception {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.serviceRequest", request));
        }
        
        ConnectionHandler handler = request.getConnectionHandler();

        Response response = new Response();
        response.setConnectionHandler(handler);

        response.addHeader(new Header(Header.SERVER, Server.getName()));

        Header header = request.getHeader(Header.CSEQ);
        if (header != null) {
            response.addHeader(new Header(Header.CSEQ, header.getValue()));
        }

        if (request.containsHeader(Header.REQUIRE)) {
            // Indicate that no special features are supported
            header = request.getHeader(Header.REQUIRE);
            response.setStatusCode(StatusCode.OptionNotSupported);
            response.setHeader(new Header(Header.UNSUPPORTED, header.getValue()));
            return;
        }

        try {
            handleRequest(request, response);
        }
        catch (RtspException e) {
            e.setResponse(response);
        }

        if (!response.isSent()) {
            handler.sendResponse(response);
        }

    }
    
    /**
     * Services an RTSP response.
     * This method currently does nothing.
     * @param response - The incoming RTSP response.
     */
    public void serviceResponse(Response response) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.serviceResponse", response));
        }
    }

    /**
     * Dispatches a {@link Request} and its matching {@link Response} to the appropriate
     * request handler.
     * @param request - The incoming RTSP request.
     * @param response - The outgoing RTSP response.
     * @throws Exception - If an I/O error, thread interrupt or unrecoverable service exception occurs.
     */
    void handleRequest(Request request, Response response) throws Exception {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.handleRequest", request));
        }

        Method method = request.getRequestLine().getMethod();

        switch (method) {
        case OPTIONS:
            if (request.containsHeader(Header.SESSION)) {
                handleSessionRequest(request, response);
            }
            else {
                handleOptions(request, response);
            }
            break;
        case DESCRIBE:
            if (request.containsHeader(Header.SESSION)) {
                handleSessionRequest(request, response);
            }
            else {
                handleDescribe(request, response);
            }
            break;

        case SETUP:
        case PLAY:
        case PAUSE:
        case TEARDOWN:
            handleSessionRequest(request, response);
            break;
        }
        
    }

    /**
     * Handles an RTSP {@link Method#OPTIONS OPTIONS} request.
     * The request may optionally identify a specific presentation, in which case, the
     * presentation is retrieved and the response is constructed according to the properties
     * of the presentation.
     * @param request - The incoming RTSP request.
     * @param response - The outgoing RTSP response.
     * @throws IOException If an I/O error occurs.
     * @throws RtspException If the request URI is malformed, the presentation description
     *                       identified by the request URI cannot be retrieved or the
     *                       request cannot be satisfied for the given presentation description. 
     */
    void handleOptions(Request request, Response response) throws IOException, RtspException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.handleOptions", request));
        }

        URI uri = request.getRequestLine().getUri();
        String query = null;
        if (uri != null) {
            query = uri.getQuery();
        }

        if (query != null) {

            URI presentationUri;
            try {
                presentationUri = new URI(query);
            }
            catch(URISyntaxException e) {
                throw RtspException.create(StatusCode.BadRequest, e.getMessage(), ObjectId, logger);
            }

            PresentationDescription description = getPresentationDescription(presentationUri);

            response.setHeader(new Header("Public","DESCRIBE, SETUP, TEARDOWN, PLAY" + (description.isPauseAllowed() ? ", PAUSE" : "")));
        }
        else {
            response.setHeader(new Header("Public","DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE"));
        }
    }

    /**
     * Handles an RTSP {@link Method#DESCRIBE DESCRIBE} request.
     * Currently limited to presentations described using the Session Description Protocol (SDP).
     * The request URI is used to fetch the "server-side" presentation description.
     * This description is modified to appear as if it originated at the proxy server,
     * and is sent to the client by attaching the description as an entity in the outgoing response.
     * @param request - The incoming RTSP request.
     * @param response - The outgoing RTSP response.
     * @throws IOException If an I/O error occurs.
     * @throws RtspException If the request URI is malformed, the presentation description
     *                       identified by the request URI cannot be retrieved or the
     *                       request cannot be satisfied for the given presentation description. 
     */
    void handleDescribe(Request request, Response response) throws IOException, RtspException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.handleDescribe", request));
        }

        URI presentationUri = request.getRequestLine().getUri();

        if (presentationUri == null) {
            response.setStatusCode(StatusCode.BadRequest);
            response.setEntity(new StringEntity("invalid URI specified in request"));
        }
        else {
            try {
                presentationUri = new URI(request.getRequestLine().getUri().getQuery());
            }
            catch(URISyntaxException e) {
                response.setStatusCode(StatusCode.BadRequest);
                response.setEntity(new StringEntity(e.getMessage()));
                return;
            }

            PresentationDescription description = getPresentationDescription(presentationUri);
            String sdp = description.describe(MimeType.application.sdp);

            // TODO: get this from HTTP server for SDP file?
            String date = Header.DATE_FORMAT_RFC_1123.format(new Date());
            response.addHeader(new Header(Header.DATE, date));
            response.addHeader(new Header(Header.EXPIRES, date));
            
            response.addHeader(Header.CACHE_CONTROL_IS_NO_CACHE);
            response.addHeader(new Header(Header.CONTENT_BASE, presentationUri.toString()+"/"));
            response.setEntity(new StringEntity(sdp,
                                                MimeType.application.sdp,
                                                StringEntity.UTF_8,
                                                false /* no charset= in Content-Type*/));
        }
    }

    /**
     * Handles session-specific RTSP requests for
     * {@link Method#SETUP SETUP},
     * {@link Method#TEARDOWN TEARDOWN},
     * {@link Method#PLAY PLAY},
     * {@link Method#PAUSE PAUSE},
     * {@link Method#OPTIONS OPTIONS}, and
     * {@link Method#DESCRIBE DESCRIBE}.
     * Any RTSP request message that carries a <code>Session</code> header is treated as a session-specific request.
     * A {@link Session} is created when a {@link Method#SETUP SETUP} request is received that does
     * not carry a <code>Session</code> header.
     * @param request - The incoming RTSP request.
     * @param response - The outgoing RTSP response.
     * @throws Exception An IOException, IllegalArgumentException, InterruptedException or {@link RtspException}.
     * @throws IOException If an I/O error occurs.
     * @throws IllegalArgumentException - The request URI is malformed.
     * @throws InterruptedException If a thread interrupt occurs.
     * @throws RtspException If the presentation description identified by the request URI 
     *                       cannot be retrieved or the request cannot be satisfied for the
     *                       given presentation description. 
     */
    void handleSessionRequest(Request request, Response response) throws Exception {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.dispatchSessionRequest", request));
        }
        
        Header sessionHeader = request.getHeader(Header.SESSION);
        Session session;
        if (sessionHeader == null) {

            // The client should be using the control URI inserted 
            // into the SDP sent as the value of the Content-Base header
            // If the scheme is still rtsp: then the client ignored both.
            URI requestUri = request.getRequestLine().getUri();
            String setupUri;
            if (requestUri.getScheme().equals("rtsp")) {
                setupUri = requestUri.getQuery();
            }
            else {
                setupUri = requestUri.toString();
            }
    
            // Strip control suffix, trackID from end of URI
            URI presentationUri = URI.create(setupUri.substring(0, setupUri.lastIndexOf("/")));
    
            PresentationDescription description = getPresentationDescription(presentationUri);
            
            session = new Session(this.server, description);
            
            this.server.addSession(session);
        }
        else {
            session = this.server.getSession(sessionHeader.getValue());
            if (session == null) {
                response.setStatusCode(StatusCode.SessionNotFound);
                return;
            }
        }
        
        try {
            session.handleRequest(request, response);
        }
        catch(Exception e) {
            this.server.removeSession(session.getSessionId());
            session.close();
            throw e;
        }

    }

    /**
     * Retrieves the presentation description identified by the specified URI.
     * The presentation is retrieved from the presentation description cache if present,
     * otherwise it is retrieved from the host specified in the URI.
     * @param uri - The presentation description URI (e.g. a URL that references an SDP file).
     * @throws RtspException If the presentation description cannot be retrieved or 
     *                       the description specifies features or media transports that
     *                       are not supported by this server.
     */
    PresentationDescription getPresentationDescription(URI uri) throws RtspException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.getPresentationDescription", uri.toString()));
        }

        PresentationDescription presentationDescription;
        if (this.lastPresentationDescription != null &&
            this.lastPresentationDescription.getUri().equals(uri)) {
            presentationDescription = this.lastPresentationDescription;
        }
        else {
            presentationDescription = PresentationDescription.construct(uri);
        }
        
        return presentationDescription;
    }

}
