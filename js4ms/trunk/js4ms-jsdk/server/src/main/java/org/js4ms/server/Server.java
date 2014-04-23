package org.js4ms.server;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * Server.java [org.js4ms.jsdk:server]
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
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;



/**
 * Basic server application that accepts TCP connection requests and
 * delegates connection handling to a {@link Service} object that is
 * supplied to a server when it is constructed.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public class Server {

    /*-- Static Constants ----------------------------------------------------*/

    public static final int DEFAULT_SERVICE_PORT = 0;

    public static final int DEFAULT_SOCKET_BUFFER_SIZE = 8 * 1024;

    public static final boolean DEFAULT_STALE_CONNECTION_CHECK = false;

    public static final boolean DEFAULT_TCP_NODELAY = true;

    public static final int DEFAULT_KEEP_ALIVE_TIMEOUT = 10000;

    public static final int DEFAULT_MAX_CONNECTIONS = 16;

    public static final String SERVICE_PROPERTY_PREFIX = "org.js4ms.service.";

    public static final String SERVICE_NAME_PROPERTY = SERVICE_PROPERTY_PREFIX + ".name";

    public static final String SERVICE_MAX_CONNECTIONS_PROPERTY = SERVICE_PROPERTY_PREFIX + "maxconnections";

    public static final String SERVICE_SOCKET_ADDRESS_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.address";

    public static final String SERVICE_SOCKET_PORT_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.port";

    public static final String SERVICE_SOCKET_RECV_BUFFER_SIZE_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.recvbuffersize";

    public static final String SERVICE_SOCKET_SEND_BUFFER_SIZE_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.sendbuffersize";

    public static final String SERVICE_SOCKET_STALE_CONNECTION_CHECK_ENABLED_PROPERTY = SERVICE_PROPERTY_PREFIX
                                                                                        + "socket.staleconnectioncheck.enabled";

    public static final String SERVICE_SOCKET_TCP_NODELAY_ENABLED_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.nodelay.enabled";

    public static final String SERVICE_USE_KEEP_ALIVE_PROPERTY = SERVICE_PROPERTY_PREFIX + "keepalive.enabled";

    public static final String SERVICE_KEEP_ALIVE_TIMEOUT_PROPERTY = SERVICE_PROPERTY_PREFIX + "keepalive.timeout";

    public static final String SERVICE_CONSOLE_ENABLED_PROPERTY = SERVICE_PROPERTY_PREFIX + "console.enabled";

    public static final String SERVICE_CONSOLE_AUTOCLOSE_ENABLED_PROPERTY = SERVICE_PROPERTY_PREFIX + "console.autoclose.enabled";

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(Server.class.getName());

    private final static Log slog = new Log(Server.class);

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    private final Log log = new Log(this);

    private ServerSocket socket;

    private boolean isStarted = false;

    private Thread serverThread;

    private final ConnectionManager connectionManager;

    private final ConnectionHandlerFactory connectionHandlerFactory;

    private final Service service;

    private final ExecutorService handlerThreadPool;

    String serviceName = Server.class.getSimpleName();

    Properties properties;

    /*-- Member Functions  ----------------------------------------------------*/

    /**
     * @param properties
     *            Server configuration properties.
     * @param service
     *            The service provided by the server.
     * @param connectionHandlerFactory
     *            A factory object used to construct connection handlers.
     */
    public Server(final Properties properties,
                  final Service service,
                  final ConnectionHandlerFactory connectionHandlerFactory) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("<ctor>", properties, connectionHandlerFactory));
        }

        this.serviceName = properties.getProperty(SERVICE_NAME_PROPERTY);
        if (this.serviceName == null) {
            this.serviceName = getClass().getSimpleName();
        }

        this.properties = properties;

        this.connectionHandlerFactory = connectionHandlerFactory;

        this.handlerThreadPool = Executors.newCachedThreadPool();

        int maxConnections = DEFAULT_MAX_CONNECTIONS;
        String propertyValue = properties.getProperty(SERVICE_MAX_CONNECTIONS_PROPERTY);
        if (propertyValue != null) {
            try {
                maxConnections = Integer.parseInt(propertyValue);
            }
            catch (NumberFormatException e) {
                logger.warning(log.msg("invalid max connections property - " + e.getMessage()));
            }
        }

        this.connectionManager = new ConnectionManager(maxConnections);

        this.service = service;
    }

    /**
     * @return
     */
    public boolean start() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("start"));
        }

        if (!this.isStarted) {

            logger.info(log.msg("starting server..."));

            try {
                constructSocket();
            }
            catch (IOException e) {
                logger.severe(log.msg("cannot start server - attempt to construct socket failed"));
                return false;
            }

            this.serverThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    runServer();
                }
            }, this.serviceName + " server thread");

            this.service.start();

            this.isStarted = true;
            this.serverThread.start();

            logger.info(log.msg("server started"));

        }

        return true;
    }

    /**
     * @throws InterruptedException
     */
    public void stop() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("stop"));
        }

        if (this.isStarted) {

            logger.info(log.msg("stopping server..."));

            this.isStarted = false;

            // Set the interrupt state in the service thread
            this.serverThread.interrupt();

            // Interrupt the accept() call in the service thread
            if (!this.socket.isClosed()) {
                try {
                    this.socket.close();
                }
                catch (IOException e) {
                    logger.severe(log.msg("attempt to close server socket failed"));
                    e.printStackTrace();
                }
            }

            logger.fine(log.msg("closing connections..."));

            // Interrupt blocking reads in the handler threads
            this.connectionManager.closeConnections();

            logger.fine(log.msg("shutting down handler thread pool..."));

            // Give handler threads the chance to cleanup
            this.handlerThreadPool.shutdown();

            // Give the handler threads some time to exit.
            if (!this.handlerThreadPool.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                logger.fine(log.msg("one or more handlers could not be shutdown"));
            }

            // This call will interrupt handler threads if they were waiting elsewhere
            // (not blocking I/O).
            this.handlerThreadPool.shutdownNow();

            logger.fine(log.msg("all handler threads have stopped"));

            logger.fine(log.msg("stopping service..."));

            this.service.stop();

            logger.fine(log.msg("service stopped"));
        }
    }

    /**
     * @throws InterruptedException
     */
    public void join() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("join"));
        }

        if (this.isStarted) {

            boolean useKeepAlive = false;

            String propertyValue = this.properties.getProperty(SERVICE_USE_KEEP_ALIVE_PROPERTY);
            if (propertyValue != null) {
                useKeepAlive = Boolean.parseBoolean(propertyValue);
            }

            if (useKeepAlive) {

                int keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;

                propertyValue = this.properties.getProperty(SERVICE_KEEP_ALIVE_TIMEOUT_PROPERTY);
                if (propertyValue != null) {
                    try {
                        keepAliveTimeout = Integer.parseInt(propertyValue);
                    }
                    catch (NumberFormatException e) {
                        logger.warning(log.msg("'" + propertyValue + "' is not a valid " + SERVICE_KEEP_ALIVE_TIMEOUT_PROPERTY
                                               + " value"));
                    }
                }

                while (!this.socket.isClosed()) {

                    logger.info(log.msg("starting keep-alive timer..."));

                    try {
                        if (!this.connectionManager.waitForNewConnection(keepAliveTimeout)) {
                            logger.info(log.msg("keep-alive timer expired"));
                            stop();
                            break;
                        }
                    }
                    catch (InterruptedException e) {
                        logger.info(log.msg("keep-alive timer interrupted"));
                        throw e;
                    }

                    logger.info(log.msg("keep-alive timeout canceled"));

                    logger.fine(log.msg("waiting for all clients to disconnect..."));

                    try {
                        if (this.connectionManager.waitForNoConnections(Integer.MAX_VALUE)) {
                            logger.fine(log.msg("all clients have disconnected"));
                        }
                    }
                    catch (InterruptedException e) {
                        logger.info(log.msg("keep-alive wait interrupted"));
                        throw e;
                    }
                }
            }

            this.serverThread.join();
        }

    }

    /**
     * @return
     */
    public String getServiceName() {
        return this.serviceName;
    }

    /**
     * 
     */
    void runServer() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("runServer"));
        }

        while (!Thread.currentThread().isInterrupted() && !this.socket.isClosed()) {

            Socket clientSocket;
            try {

                this.connectionManager.waitForAvailableConnection();

                logger.fine(log.msg("listening for incoming client connections..."));

                clientSocket = this.socket.accept();

                logger.info(log.msg(clientSocket.getInetAddress().toString() + " connected"));

                setSocketProperties(clientSocket);

                Connection connection = new SocketConnection(clientSocket);

                this.connectionManager.addConnection(connection);

                this.handlerThreadPool.execute(this.connectionHandlerFactory.construct(this.connectionManager, connection,
                                                                                       this.service));

            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            catch (IOException e) {

                if (this.socket.isClosed()) {
                    // TODO: The pipes used to send logging messages to the console
                    // will be broken if the most recent thread to write to the pipe
                    // exits.
                    // This means that we can't log any messages on the way out.
                    logger.fine(log.msg("server socket closed"));
                }
                else {
                    e.printStackTrace();
                }
                break;
            }
        }

        // TODO: See note above - cannot send any log messages now
        logger.info(log.msg("exiting server thread"));

    }

    /**
     * @throws IOException
     */
    void constructSocket() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("constructSocket"));
        }

        String propertyValue;

        int serverPort = DEFAULT_SERVICE_PORT;

        propertyValue = this.properties.getProperty(SERVICE_SOCKET_PORT_PROPERTY);
        if (propertyValue != null) {
            try {
                serverPort = Short.parseShort(propertyValue);
            }
            catch (NumberFormatException e) {
                logger.warning(log.msg("'" + propertyValue + "' is not a valid " + SERVICE_SOCKET_PORT_PROPERTY + " value - "
                                       + e.getMessage()));
            }
        }

        InetSocketAddress serverSocketAddress;

        propertyValue = this.properties.getProperty(SERVICE_SOCKET_ADDRESS_PROPERTY);
        if (propertyValue != null) {
            try {
                serverSocketAddress = new InetSocketAddress(InetAddress.getByName(propertyValue), serverPort);
            }
            catch (UnknownHostException e) {
                logger.warning(log.msg("'" + propertyValue + "' is not a valid " + SERVICE_SOCKET_ADDRESS_PROPERTY + " value - "
                                       + e.getMessage()));
                serverSocketAddress = new InetSocketAddress(serverPort);
            }
        }
        else {
            serverSocketAddress = new InetSocketAddress(serverPort);
        }

        logger.fine(log.msg("binding server socket to " + serverSocketAddress.toString()));

        try {

            this.socket = new ServerSocket();

            // This method will throw a BindException if the port is already bound to a
            // socket.
            this.socket.bind(serverSocketAddress);

            logger.info(log.msg("server socket address is " + this.socket.getInetAddress().getHostAddress() + ":"
                                + this.socket.getLocalPort()));

            propertyValue = this.properties.getProperty(SERVICE_SOCKET_RECV_BUFFER_SIZE_PROPERTY);
            if (propertyValue != null) {
                try {
                    int bufferSize = Integer.parseInt(propertyValue);
                    this.socket.setReceiveBufferSize(bufferSize);
                }
                catch (NumberFormatException e) {
                    logger.warning(log.msg("cannot set recv buffer size property on socket - " + e.getMessage()));
                }
            }

        }
        catch (BindException e) {
            logger.warning(log.msg("port " + serverPort + " is currently in use - another server instance may be running"));
            throw e;
        }
        catch (IOException e) {
            logger.warning(log.msg("cannot construct server socket - " + e.getMessage()));
            throw e;
        }

    }

    /**
     * 
     * @return
     */
    public int getServerPort() {
        return this.socket != null ? this.socket.getLocalPort() : 0;
    }

    /**
     * 
     * @param clientSocket
     */
    public void setSocketProperties(Socket clientSocket) {

        String propertyValue = this.properties.getProperty(SERVICE_SOCKET_SEND_BUFFER_SIZE_PROPERTY);
        if (propertyValue != null) {
            try {
                int bufferSize = Integer.parseInt(propertyValue);
                clientSocket.setSendBufferSize(bufferSize);
            }
            catch (Exception e) {
                logger.warning(log.msg("cannot set send buffer size property on socket - " + e.getMessage()));
            }
        }
    }

    /**
     * @param properties
     * @param serverFactory
     * @throws InterruptedException
     */
    public static void runServer(Properties properties, ServerFactory serverFactory) throws InterruptedException {

        final Server server = serverFactory.construct(properties);

        Thread shutdownHook = new Thread() {

            @Override
            public void run() {
                try {
                    server.stop();
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        Runtime.getRuntime().addShutdownHook(shutdownHook);

        if (server.start()) {
            try {
                server.join();
            }
            catch (InterruptedException e) {
                logger.info(slog.msg("server join interrupted"));
                Thread.currentThread().interrupt();
                server.stop();
            }
        }

        LogManager.getLogManager().reset();

    }

}
