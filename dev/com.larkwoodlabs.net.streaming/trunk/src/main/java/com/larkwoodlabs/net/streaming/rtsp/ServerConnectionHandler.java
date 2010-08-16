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
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.logging.Level;

import com.larkwoodlabs.util.logging.Logging;

/**
 * Handles server-side processing of RTSP messages received on the {@link Connection} created
 * when an external client opens a connection to the RTSP {@link Server}.
 *
 * @author Gregory Bumgardner
 */
public class ServerConnectionHandler extends ConnectionHandler {


    /*-- Member Variables ----------------------------------------------------*/

    protected final Server server;
    protected final Object lock = new Object();
    protected final Connection inputConnection;
    protected final Service service;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a connection handler for the specified {@link Server} and server-side {@link Connection}.
     * @param serverConnection - The server-side connection, typically a {@link ServerSocketConnection}.
     */
    public ServerConnectionHandler(final Server server, final Connection serverConnection) {
        super(serverConnection);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ServerConnectionHandler.ServerConnectionHandler", server, serverConnection));
        }

        this.server = server;
        // Cache the original so the tunnel Client->Server side of the tunnel can be constructed more than once.
        this.inputConnection = serverConnection;
        this.service = new Service(server);
    }
    
    /**
     * Handles a single RTSP or HTTP request.
     * @param request - The request message.
     */
    @Override
    public void handleRequest(final Request request) throws Exception {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ServerConnectionHandler.handleRequest", request));
        }

        switch (request.getRequestLine().getMethod()) {
        case GET:
            handleGet(request);
            break;
        case POST:
            handlePost(request);
            break;
        case OPTIONS:
        case DESCRIBE:
        case SETUP:
        case PLAY:
        case PAUSE:
        case TEARDOWN:
            this.service.serviceRequest(request);
            break;
        }
    }
    
    /**
     * Handles an HTTP {@link Method#GET GET} request.
     * A GET request message that carries an <code>x_rtsp_tunnelled</code> header is
     * used by clients to construct the server-to-client side of an HTTP tunnel.
     * The tunnel request must also carry an <code>x-sessioncookie</code> header that
     * identifies the connection to it can be matched with a subsequent HTTP POST request.
     * A GET request message that targets certain predefined URL paths are used to 
     * request other types of server response.
     * @param request - An HTTP {@link Method#GET GET} request message. 
     * @throws IOException If an I/O error occurs while handling the request.
     */
    void handleGet(final Request request) throws IOException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ServerConnectionHandler.handleGet", request));
        }

        Header accept = request.getHeader(Header.ACCEPT);
        if (accept == null || !accept.getValue().contains(MimeType.application.x_rtsp_tunnelled)) {

            if (logger.isLoggable(Level.FINER)) {
                logger.finer(ObjectId + " responding to GET request");
            }

            Response response = new Response(new StatusLine(ProtocolVersion.HTTP_1_1, StatusCode.OK), null, null);
            response.addHeader(new Header(Header.SERVER, Server.getName()));
            response.addHeader(Header.CACHE_CONTROL_IS_NO_CACHE);

            URI uri = request.getRequestLine().getUri();
            String path = uri.getPath();
            if (path.equals("/echo")) {
                handleEchoRequest(request, response);
            }
            else if (path.equals("/status")) {
                handleStatusRequest(request, response);
            }
            else if (path.equals("/action")) {
                handleActionRequest(request, response);
            }
            else {
                response.setStatusCode(StatusCode.Forbidden);
                response.setHeader(new Header(Header.CONTENT_LENGTH,"0"));
                response.addHeader(Header.CONNECTION_IS_CLOSE);
                sendResponse(response);
            }
        }
        else if (accept.getValue().contains(MimeType.application.x_rtsp_tunnelled)) {
            // Setup Server-Client tunnel
            Header sessionCookie = request.getHeader(Header.X_SESSIONCOOKIE);
            if (sessionCookie != null) {

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(ObjectId + " constructing Server->Client HTTP tunnel for session=" + sessionCookie.getValue());
                }

                // Register this output stream with the server so it can be retrieved in subsequent POST request
                this.server.addOutputConnection(sessionCookie.getValue(), this.connection);

                String date = Header.DATE_FORMAT_RFC_1123.format(new Date());
                
                // Send HTTP response for GET followed by RTSP response with SDP
                
                Response response = new Response(new StatusLine(ProtocolVersion.HTTP_1_0, StatusCode.OK), null, null);
                response.addHeader(new Header(Header.SERVER, Server.getName()));
                response.addHeader(new Header(Header.DATE, date));
                response.addHeader(Header.CONNECTION_IS_CLOSE);
                response.addHeader(Header.CACHE_CONTROL_IS_NO_STORE);
                response.addHeader(Header.PRAGMA_IS_NO_CACHE);
                response.addHeader(new Header(Header.CONTENT_TYPE, MimeType.application.x_rtsp_tunnelled));
                sendResponse(response);
                //this.connection.shutdownInput();
            }
            else {
                Response response = new Response(new StatusLine(ProtocolVersion.HTTP_1_0, StatusCode.NotImplemented), null, null);
                response.setEntity(new StringEntity("tunneling GET request must include an x_sessioncookie header."));
                sendResponse(response);
            }
        }
    }
    
    /**
     * Handles an HTTP {@link Method#GET GET} message that requests the server
     * to echo the first query string parameter in the request URL back to the
     * client in the response.
     * Typically used to check whether the RTSP server is running.
     * @param request - An HTTP GET request with a URL containing the path "<code>/echo</code>" followed by a query string.
     * @param response - An HTTP response whose entity will be set to the requested echo string.
     * @throws IOException 
     */
    void handleEchoRequest(Request request, Response response) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ServerConnectionHandler.handleEchoRequest", request, response));
        }

        URI uri = request.getRequestLine().getUri();
        String query = uri.getQuery();
        if (query != null && query.length() > 0) {
            String[] parameters = query.split("&");
            if (parameters.length > 0) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(ObjectId + " responding to echo request response='" + parameters[0] + "'");
                }
                response.setEntity(new StringEntity(parameters[0]));
            }
            else {
                response.setStatusCode(StatusCode.NoContent);
                response.addHeader(Header.CONNECTION_IS_CLOSE);
            }
        }
        else {
            response.setHeader(new Header(Header.CONTENT_LENGTH,"0"));
        }

        sendResponse(response);
    }

    /**
     * Handles an HTTP {@link Method#GET GET} message that requests the server
     * to transmit status information back to the client in the response.
     * If the request carries the query string parameter "<code>callback</code>",
     * the response is formatted as a JavaScript call to the function named in the
     * callback parameter with a parameter consisting of a JSON message describing
     * the current status.
     * @param request - An HTTP GET request with a URL containing the path "<code>/status</code>"
     *                  followed by an optional query string containing a <code>callback</code> parameter.
     * @param response - An HTTP response whose entity is set according to the requested
     *                   format for the status response.
     * @throws IOException
     */
    void handleStatusRequest(Request request, Response response) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ServerConnectionHandler.handleStatusRequest", request, response));
        }

        URI uri = request.getRequestLine().getUri();
        String query = uri.getQuery();
        if (query == null || query.length() == 0) {
            response.setStatus(StatusCode.BadRequest,"query string for status request cannot be empty");
            return;
        }

        // TODO: Add support for simple HTML response, perhaps generated from embedded resource files.
        
        String callbackFuncName = null;
        String[] parameters = query.split("&");
        for (String parameter : parameters) {
            String[] pair = parameter.split("=");
            if (pair.length == 2) {
                if (pair[0].toLowerCase().equals("callback")) {
                    callbackFuncName = pair[1];
                    break;
                }
            }
        }

        if (callbackFuncName == null) {
            response.setStatus(StatusCode.BadRequest,"query string for update request must specify callback function");
            return;
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(ObjectId + " responding to streaming update request; callback-function=" + callbackFuncName);
        }

        response.addHeader(new Header(Header.CONTENT_TYPE, MimeType.text.javascript));
        
        response.setEntity(new StringEntity(callbackFuncName+"({'time':'"+(new Date().getTime()) + "'});\n"));

        sendResponse(response);
    }

    public void handleActionRequest(Request request, Response response) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ServerConnectionHandler.handleActionRequest", request, response));
        }

        URI uri = request.getRequestLine().getUri();
        String query = uri.getQuery();
        if (query == null || query.length() == 0) {
            response.setStatus(StatusCode.BadRequest,"query string for action request cannot be empty");
            return;
        }

        String[] parameters = query.split("&");
        for (String parameter : parameters) {

            String action = null;
            String[] arguments = null;

            if (parameter.indexOf("=") != -1) {
                String[] pair = parameter.split("=");
                action = pair[0];
                if (pair.length == 2) {
                    arguments = pair[1].split(",");
                }
            }
            else {
                action = parameter;
            }

            if (action.equals("shutdown")) {
                response.setEntity(new StringEntity(new Date().toString()+": shutting down RTSP relay..."));
                sendResponse(response);
                this.server.stop();
            }
            else {
                response.setStatus(StatusCode.BadRequest,"unrecognized action '"+action+"'");
                sendResponse(response);
            }
            break;
        }
    }

    /**
     * Handles an HTTP {@link Method#POST POST} request.
     * A POST request message that carries an <code>x_rtsp_tunnelled</code> header is
     * used by clients to construct the client-to-server side of an HTTP tunnel.
     * The tunnel request must also carry an <code>x-sessioncookie</code> header that
     * identifies the server-to-client connection constructed in response to an HTTP GET request.
     * @param request - An HTTP {@link Method#GET GET} request message. 
     * @throws IOException If an I/O error occurs while handling the request.
     */
    void handlePost(final Request request) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ServerConnectionHandler.handlePost", request));
        }

        /*
         * x-sessioncookie: <cookie>
         * Content-Type: application/x-rtsp-tunnelled
         */
        Header sessionCookie = request.getHeader(Header.X_SESSIONCOOKIE);
        Header contentType = request.getHeader(Header.CONTENT_TYPE);
        if (sessionCookie != null &&
            contentType != null &&
            contentType.getValue().contains(MimeType.application.x_rtsp_tunnelled)) {
            // POST is requesting Client->Server tunnel creation.
            // Get the Server->Client connection that carries the same session cookie value
            Connection outputConnection = this.server.getOutputConnection(sessionCookie.getValue());
            if (outputConnection != null) {

                // Replace the current connection with the tunnel connection.
                // Future requests, responses and interleaved RTP/RTCP packets
                // will be sent over this connection.

                //this.connection.shutdownOutput(); // This does not work as it kills both streams.
                reconnect(new ServerTunnelConnection(this.server,
                                                     sessionCookie.getValue(),
                                                     this.inputConnection,
                                                     outputConnection));
            }
            else {
                Response response = new Response();
                response.setStatus(StatusCode.SessionNotFound, "tunnel session identified in POST request does not exist");
                sendResponse(response);
            }
        }
        else {
            Response response = new Response();
            response.setStatus(StatusCode.NotImplemented, "POST can only be used to establish an HTTP tunnel");
            sendResponse(response);
        }
    }
    
    @Override
    public void handleResponse(final Response response) {
        this.service.serviceResponse(response);
        
    }

    @Override
    public void handlePacket(final int channel, final ByteBuffer packet) {
    }
    
}
