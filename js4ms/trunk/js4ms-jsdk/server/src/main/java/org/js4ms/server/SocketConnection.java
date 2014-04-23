package org.js4ms.server;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * SocketConnection.java [org.js4ms.jsdk:server]
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


import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;

/**
 * A persistent client-server connection that uses the input and output streams
 * of a Socket object for communication with a remote host.
 * 
 * @author Gregory Bumgardner (gbumgard)
 */
public class SocketConnection
                extends Connection {

    /*-- Member Variables ----------------------------------------------------*/

    final Socket socket;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a connection that uses the specified socket. The socket input stream
     * is buffered.
     * 
     * @param socket
     *            The Socket object.
     *            In a server this object is constructed by a call to
     *            ServerSocket.accept().
     *            In a client this object is constructed when the client connects to a
     *            server.
     * @throws IOException
     *             If an I/O error occurs when accessing properties of the socket.
     */
    public SocketConnection(final Socket socket) throws IOException {
        super(new BufferedInputStream(socket.getInputStream()), socket.getOutputStream());

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("SocketConnection", socket));
        }

        this.socket = socket;
    }

    /**
     * Returns the host address of the connected client.
     * 
     * @return
     */
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) this.socket.getRemoteSocketAddress();
    }

    @Override
    public void shutdownInput() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("shutdownInput"));
        }

        if (!this.socket.isInputShutdown()) {
            this.socket.shutdownInput();
        }
    }

    @Override
    public void shutdownOutput() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("shutdownOutput"));
        }

        if (!this.socket.isOutputShutdown()) {
            this.socket.shutdownOutput();
        }
    }

    @Override
    public void close() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("close"));
        }

        try {
            socket.close();
        }
        catch (IOException e) {
            logger.warning(log.msg("cannot close socket connection"));
            e.printStackTrace();
            throw e;
        }
    }

}
