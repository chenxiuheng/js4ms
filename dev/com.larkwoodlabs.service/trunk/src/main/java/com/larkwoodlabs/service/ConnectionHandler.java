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

package com.larkwoodlabs.service;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Logging;


/**
 * Base class for objects that manage the serialization of messages over a {@link Connection}.
 * This class simply flushes bytes from the connection input stream.
 * 
 * @author Gregory Bumgardner
 */
public class ConnectionHandler implements Runnable {

    static public class Factory {
        public ConnectionHandler construct(Connection connection) {
            return new ConnectionHandler(connection);
        }
    }

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(ConnectionHandler.class.getName());

    /*-- Member Variables ----------------------------------------------------*/

    protected final String ObjectId = Logging.identify(this);

    protected final Object lock = new Object();

    protected Connection connection;
    
    protected boolean isRunning;
    
    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a handler for the specified connection.
     * @param context
     * @param connection
     */
    protected ConnectionHandler(final Connection connection) {
        this.connection = connection;
        this.isRunning = false;
    }
    
    /**
     * Closes this handler and associated connection.
     * @throws IOException
     */
    public void close() throws IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ConnectionHandler.close"));
        }

        this.isRunning = false;
        this.connection.close();
    }

    /**
     * Returns the connection managed by this handler.
     * @return
     */
    public Connection getConnection() {
        synchronized(this.lock) {
            return this.connection;
        }
    }
    
    /**
     * Sets the connection by this handler.
     * Used to switch connections.
     * @param connection
     */
    protected void setConnection(final Connection connection) {
        synchronized (this.lock) {
            this.connection = connection;
        }
    }
    
    /**
     * Continuously receives and forwards incoming bytes.
     */
    @Override
    public void run() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ConnectionHandler.run"));
        }

        this.isRunning = true;

        try {
            while (this.isRunning) {
                // Read and process one message from the input stream
                processMessage();
            }
        }
        catch (EOFException e) {
            // Connection was closed by peer or the input was shutdown
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " connection handler exiting for " + e.getClass().getName() + ": " + e.getMessage());
            }
        }
        catch (SocketException e) {
            // The connection socket was closed by another thread while this thread was waiting on I/O.
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " connection handler exiting for " + e.getClass().getName() + ": " + e.getMessage());
            }
        }
        catch (IOException e) {
            // IO exception occurred - most likely while attempting to send a message or data over a closed connection
            if (logger.isLoggable(Level.FINE)) {
                logger.warning(ObjectId + " connection handler aborted by " + e.getClass().getName() + ":" + e.getMessage());
            }
        }
        catch (InterruptedException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId + " connection handler thread was interrupted");
            }
        }
        catch (Exception e) {
            // An unexpected exception occurred
            if (logger.isLoggable(Level.FINE)) {
                logger.warning(ObjectId + " connection handler aborted by " + e.getClass().getName() + ":" + e.getMessage());
            }
            e.printStackTrace();
        }

        try {
            this.connection.close();
        }
        catch (IOException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(ObjectId +
                            " connection handler cannot close connection - " +
                            e.getClass().getName() + ":" + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    /**
     * Reads and dispatches messages and interleaved packets from the connection InputStream.
     * Typically overridden in derived classes. Default implementation simply flushes bytes
     * from the stream.
     * @throws Exception
     */
    public void processMessage() throws EOFException,
                                        SocketException,
                                        IOException,
                                        InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ConnectionHandler.processMessage"));
        }
        
        InputStream inputStream = this.connection.getInputStream();
        
        // Get first character in message 
        // Throws SocketException if the socket is closed by 
        // another thread while waiting in this call
        int c = inputStream.read();

        if (c == -1) {
            // Peer stopped sending data or input was shutdown
            throw new EOFException("connection stream returned EOF");
        }

    }
}