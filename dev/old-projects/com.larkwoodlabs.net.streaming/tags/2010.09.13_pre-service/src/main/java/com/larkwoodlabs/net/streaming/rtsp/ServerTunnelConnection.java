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
import java.util.logging.Level;

import com.larkwoodlabs.io.Base64InputStream;
import com.larkwoodlabs.util.logging.Logging;

/**
 * A server-side connection with a remote client that uses 
 * separate connections for input and output.<p>
 * A server tunnel connection is constructed in response to an
 * HTTP tunneling request from an RTSP client. The output connection
 * is the connection that received an open-ended HTTP GET request and the input
 * connection is the connection that received a matching HTTP POST request.
 * The GET and POST requests are matched using the identifier specified in
 * an <code>x-sessioncookie</code> message header.
 * See <a href="http://developer.apple.com/quicktime/icefloe/dispatch028.html">Tunnelling RTSP and RTP through HTTP<a>.
 *
 * @author Gregory Bumgardner
 */
public final class ServerTunnelConnection extends Connection {
    

    /*-- Member Variables ----------------------------------------------------*/

    private final Server server;
    
    private final String sessionCookie;
    
    private final Connection inputConnection;
    
    private final Connection outputConnection;
    
    
    /*-- Member Functions ----------------------------------------------------*/

    /**
     * @param server - The server responsible for constructing this connection.
     * @param sessionCookie - An tunnel connection identifier typically specified
     *                        by a client in an <code>x-sessioncookie</code> header. 
     * @param inputConnection - The connection used to receive messages from a client.
     * @param outputStream - The connection used to send messages to a client.
     */
    public ServerTunnelConnection(Server server,
                                  final String sessionCookie,
                                  final Connection inputConnection,
                                  final Connection outputConnection) {
        super(inputConnection.getRemoteAddress(), new Base64InputStream(inputConnection.getInputStream()), outputConnection.getOutputStream());

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ServerTunnelConnection.ServerTunnelConnection", server, sessionCookie, inputConnection, outputConnection));
        }

        this.server = server;
        this.sessionCookie = sessionCookie;
        this.inputConnection = inputConnection;
        this.outputConnection = outputConnection;
    }
    
    @Override
    public void shutdownInput() throws IOException {
        this.inputConnection.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        this.outputConnection.shutdownOutput();
    }

    /**
     * Closes the input and output connections and removes the connection identified
     * by the session cookie from the collection of active tunnel connections managed
     * by the {@link Server}.
     * @throws IOException If an I/O error occurs while closing the connection.
     */
    @Override
    public void close() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ServerTunnelConnection.close"));
        }

        this.inputConnection.close();
        this.outputConnection.close();
        
        this.server.removeOutputConnection(this.sessionCookie);
    }

}
