package org.js4ms.server;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ConnectionHandler.java [org.js4ms.jsdk:server]
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


import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;



/**
 * Base class for objects that manage the serialization of messages over a
 * {@link Connection}.
 * This class simply flushes bytes from the connection input stream.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public class ConnectionHandler
                implements Runnable {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(ConnectionHandler.class.getName());

    /*-- Member Variables ----------------------------------------------------*/

    protected final Log log = new Log(this);

    protected final Object lock = new Object();

    protected ConnectionManager manager;

    protected Connection connection;

    protected Service service;

    protected boolean isRunning;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a handler for the specified connection.
     * 
     * @param context
     * @param connection
     */
    public ConnectionHandler(final ConnectionManager manager, final Connection connection, final Service service) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("ConnectionHandler", manager, connection, service));
        }

        this.manager = manager;
        this.connection = connection;
        this.service = service;
    }

    /**
     * Returns the connection managed by this handler.
     * 
     * @return
     */
    public Connection getConnection() {
        synchronized (this.lock) {
            return this.connection;
        }
    }

    /**
     * Sets the connection managed by this handler.
     * Used to switch connections, primarily for tunneling.
     * If the previous connection is no longer needed it should be closed.
     * 
     * @param connection
     */
    protected void setConnection(final Connection connection) {
        synchronized (this.lock) {
            this.connection = connection;
        }
    }

    /**
     * @return
     */
    public ConnectionManager getConnectionManager() {
        return this.manager;
    }

    /**
     * Continuously receives and forwards incoming bytes.
     */
    @Override
    public void run() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("run"));
        }

        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Read and process incoming bytes
                this.service.service(connection);
            }
        }
        catch (EOFException e) {
            // Connection was closed by peer or the input was shutdown
            logger.fine(log.msg("connection handler exiting for " + e.getClass().getName() + ": " + e.getMessage()));
        }
        catch (SocketException e) {
            // The connection socket was closed by another thread while this thread was
            // waiting on I/O.
            logger.fine(log.msg("connection handler exiting for " + e.getClass().getName() + ": " + e.getMessage()));
        }
        catch (IOException e) {
            // IO exception occurred - most likely while attempting to send a message or
            // data over a closed connection
            logger.warning(log.msg("connection handler aborted by " + e.getClass().getName() + ":" + e.getMessage()));
        }
        catch (InterruptedException e) {
            logger.fine(log.msg("connection handler thread was interrupted"));
            // Continue on to attempt to close the connection but set the flag to
            // interrupt any subsequent waits
            Thread.currentThread().interrupt();
        }
        catch (Exception e) {
            // An unexpected exception occurred
            logger.warning(log.msg("connection handler aborted by " + e.getClass().getName() + ":" + e.getMessage()));
            e.printStackTrace();
        }

        try {
            this.connection.close();
        }
        catch (IOException e) {
            logger.fine(log.msg("connection handler cannot close connection - " + e.getClass().getName() + ":" + e.getMessage()));
            e.printStackTrace();
        }

        logger.info(log.msg(connection.getRemoteAddress().getAddress().toString() + " disconnected"));

        this.manager.removeConnection(connection);

    }

}
