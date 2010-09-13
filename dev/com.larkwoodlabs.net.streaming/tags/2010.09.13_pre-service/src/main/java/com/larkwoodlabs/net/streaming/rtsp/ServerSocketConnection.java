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
import java.net.Socket;
import java.util.logging.Level;

import com.larkwoodlabs.util.logging.Logging;

/**
 * A server-side connection with a remote client.
 *
 * @author Gregory Bumgardner
 */
public class ServerSocketConnection extends SocketConnection {

    private final Server server;

    /**
     * Constructs a server-side socket connection for the specified server and socket.
     * @param server - The server responsible for constructing the connection and socket.
     * @param socket - The Socket created by a call to ServerSocket.accept().
     * @throws IOException If an I/O error occurs accessing the socket streams.
     */
    public ServerSocketConnection(Server server, Socket socket) throws IOException {
        super(socket);
        this.server = server;
    }

    /**
     * Closes this connection and removes it from active connection collection managed by the {@link Server}.
     */
    @Override
    public void close() throws IOException {
        
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ServerSocketConnection.close"));
        }

        try {
            super.close();
        }
        finally {
            this.server.removeConnection(this);
        }
    }
}
