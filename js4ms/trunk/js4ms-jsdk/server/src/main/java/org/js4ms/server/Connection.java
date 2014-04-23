package org.js4ms.server;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * Connection.java [org.js4ms.jsdk:server]
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;



/**
 * An abstract base class for persistent client-server connections. 
 *
 * @author Greg Bumgardner (gbumgard)
 */
public abstract class Connection  {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * The {@link Logger} for {@link Connection} objects.
     */
    public static final Logger logger = Logger.getLogger(Connection.class.getName());

    /**
     * Global index used to create unique identifier for new connections.
     */
    static int connectionIndex;

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * Helper object used to construct log messages.
     */
    protected final Log log = new Log(this);

    /**
     * Unique identifier used to retrieve connection instances managed by a {@link ConnectionManager}.
     */
    protected String identifier;

    /**
     * The InputStream used to read data from the connection.
     */
    protected PushbackInputStream inputStream;

    /**
     * The OutputStream used to send data over the connection.
     */
    protected OutputStream outputStream;
    

    /*-- Member Functions ----------------------------------------------------*/

    protected Connection() {
        
    }

    /**
     * Protected constructor used by derived classes to specify the input stream, output stream,
     * and client host address for the connection.
     * @param identifier - A String containing an identifier that is unique within
     *                     the context of the associated Server instance.
     * @param inputStream - The stream that will be used to receive data from the client.
     * @param outputStream - The stream that will be used to send data to the client.
     */
    protected Connection(final String identifier, final InputStream inputStream, final OutputStream outputStream) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("Connection", identifier, inputStream, outputStream));
        }

        this.inputStream = new PushbackInputStream(inputStream);
        this.outputStream = outputStream;
        this.identifier = identifier;
    }

    protected Connection(final InputStream inputStream, final OutputStream outputStream) {
        this("#"+String.valueOf(++connectionIndex), inputStream, outputStream);
    }

    /**
     * Gets the connection identifier.
     * @return
     */
    public final String getIdentifier() {
        return this.identifier;
    }

    /**
     * Sets the connection identifier. The identifier must be unique within
     * the context of a {@link Server} instance.
     * This method should not be used while the connection
     * is registered with a {@link ConnectionManager}
     * @param identifier - A String containing a unique identifier.
     */
    public final void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public abstract InetSocketAddress getRemoteAddress();

    /**
     * Returns an input stream that can be used to receive data from the client.
     * @return
     */
    public final PushbackInputStream getInputStream() {
        return this.inputStream;
    }

    /**
     * Sets the input stream used to receive data.
     * Typically used to bind this connection to the input stream of another connection
     * for tunneling purposes.
     * @param inputStream
     */
    public final void setInputStream(final PushbackInputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Returns an output stream that can be used to send data to the client.
     * @return
     */
    public final OutputStream getOutputStream() {
        return this.outputStream;
    }

    /**
     * Sets the output stream used to send data.
     * Typically used to bind this connection to the output stream of another connection
     * for tunneling purposes.
     * @param inputStream
     */
    public final void setOutputStream(final OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * Disables the connection InputStream.
     * Data received from connection peer is silently discarded.
     * Used to provide non-abortive shutdown of connection.
     * The InputStream will return EOF (-1) if an attempt is made to read
     * from the stream after this method has been called.
     * @throws IOException
     */
    public abstract void shutdownInput() throws IOException;

    /**
     * Disables the connection OutputStream. The connection peer is
     * notified that no more data will be sent (FIN).
     * Used to provide non-abortive shutdown of connection;
     * after calling this method, call shutdownInput() or read the InputStream
     * until EOF and then call close().
     * The OutputStream will thrown an IOException if an attempt is made to 
     * write to the stream after this method has been called.
     * @throws IOException
     */
    public abstract void shutdownOutput() throws IOException;

    /**
     * Closes the connection.
     * The connection with the peer is aborted and any threads waiting
     * to read from the connection will throw a SocketException.
     * @throws IOException
     */
    public abstract void close() throws IOException;

}
