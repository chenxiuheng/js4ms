package org.js4ms.server;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ConnectionManager.java [org.js4ms.jsdk:server]
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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;



/**
 * Manages a collection of active {@link Connection} objects.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public class ConnectionManager {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * The {@link Logger} for {@link ConnectionManager} objects.
     */
    public static final Logger logger = Logger.getLogger(ConnectionManager.class.getName());

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * Helper object used to construct log messages.
     */
    protected final Log log = new Log(this);

    /**
     * 
     */
    private int maxConnections;

    /**
     * 
     */
    private HashMap<String, Connection> connections = new HashMap<String, Connection>();

    /**
     * Object used to notify a waiting thread that a new connection has been added to the
     * collection.
     */
    Object onNewConnection = new Object();

    /**
     * Object used to notify a waiting thread that the last remaining connection
     * in the collection has been removed.
     */
    Object onNoConnections = new Object();

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     * @param maxConnections
     */
    public ConnectionManager(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * @throws InterruptedException
     */
    public void waitForAvailableConnection() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("waitForAvailableConnection"));
        }

        synchronized (this.connections) {
            if (this.connections.size() >= maxConnections) {
                logger.fine(log.msg("maximum number of connections reached - waiting for existing connection to close..."));
                this.connections.wait();
            }
        }
    }

    /**
     * @param milliseconds
     * @return
     * @throws InterruptedException
     */
    public boolean waitForNewConnection(int milliseconds) throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("waitForNewConnection", milliseconds));
        }

        synchronized (this.onNewConnection) {
            this.onNewConnection.wait(milliseconds);
            synchronized (this.connections) {
                if (this.connections.size() == 0) {
                    return false;
                }
                // Timeout has occurred
                return true;
            }
        }
    }

    /**
     * @param milliseconds
     * @return
     * @throws InterruptedException
     */
    public boolean waitForNoConnections(int milliseconds) throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("waitForNoConnections", milliseconds));
        }

        synchronized (this.onNoConnections) {
            this.onNoConnections.wait(milliseconds);
            synchronized (this.connections) {
                if (this.connections.size() == 0) {
                    return true;
                }
                // Timeout has occurred
                return false;
            }
        }
    }

    /**
     * @param connection
     */
    public void addConnection(Connection connection) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("addConnection", connection));
        }

        synchronized (this.connections) {
            this.connections.put(connection.getIdentifier(), connection);
            synchronized (this.onNewConnection) {
                this.onNewConnection.notifyAll();
            }
        }
    }

    /**
     * @param connection
     */
    public void removeConnection(Connection connection) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("removeConnection", connection));
        }

        synchronized (this.connections) {
            if (this.connections.remove(connection.getIdentifier()) != null) {
                if (connections.size() < maxConnections) {
                    this.connections.notifyAll();
                }
                if (this.connections.size() == 0) {
                    synchronized (this.onNoConnections) {
                        this.onNoConnections.notifyAll();
                    }
                }
            }
        }
    }

    /**
     * @param oldName
     * @param newName
     */
    public void renameConnection(String oldName, String newName) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("renameConnection", oldName, newName));
        }

        Connection connection;
        synchronized (this.connections) {
            if ((connection = this.connections.remove(oldName)) != null) {
                connection.setIdentifier(newName);
                this.connections.put(newName, connection);
            }
        }
    }

    /**
     * 
     */
    public void closeConnections() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("closeConnections"));
        }

        // Close all connections but do not remove them - the handlers will do that
        synchronized (this.connections) {
            for (Connection connection : this.connections.values()) {
                try {
                    connection.close();
                }
                catch (IOException e) {
                    // TODO log message
                    e.printStackTrace();
                }
            }
        }
    }

}
