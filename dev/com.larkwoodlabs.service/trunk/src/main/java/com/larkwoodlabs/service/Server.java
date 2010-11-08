package com.larkwoodlabs.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Log;
public class Server {

    /*-- Static Constants ----------------------------------------------------*/
    public static final int     DEFAULT_SOCKET_BUFFER_SIZE = 8 * 1024;
    public static final boolean DEFAULT_STALE_CONNECTION_CHECK = false;
    public static final boolean DEFAULT_TCP_NODELAY = true;
    public static final int     DEFAULT_KEEP_ALIVE_TIMEOUT = 10000;

    public static final String  SERVICE_PROPERTY_PREFIX = "com.larkwoodlabs.service.";
    public static final String  SERVICE_SOCKET_ADDRESS_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.address";
    public static final String  SERVICE_SOCKET_PORT_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.port";
    public static final String  SERVICE_SOCKET_RECV_BUFFER_SIZE_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.recvbuffersize";
    public static final String  SERVICE_SOCKET_SEND_BUFFER_SIZE_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.sendbuffersize";
    public static final String  SERVICE_SOCKET_TCP_NODELAY_ENABLED_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.nodelay.enabled";
    public static final String  SERVICE_USE_KEEP_ALIVE_PROPERTY = SERVICE_PROPERTY_PREFIX + "keepalive.enabled";
    public static final String  SERVICE_KEEP_ALIVE_TIMEOUT_PROPERTY = SERVICE_PROPERTY_PREFIX + "keepalive.timeout";
    public static final String  SERVICE_CONSOLE_ENABLED_PROPERTY = SERVICE_PROPERTY_PREFIX + "console.enabled";
    public static final String  SERVICE_CONSOLE_AUTOCLOSE_ENABLED_PROPERTY = SERVICE_PROPERTY_PREFIX + "console.autoclose.enabled";
    final static String LOGGING_PROPERTIES_URL_PROPERTY = "com.larkwoodlabs.logging.properties.url";
    private ServerSocket socket;

    private boolean isStarted = false;
    private Thread serverThread;

    private final ConnectionHandlerFactory connectionHandlerFactory;


    String serviceName = Server.class.getSimpleName();

    Properties properties;





        this.connectionHandlerFactory = connectionHandlerFactory;

        this.handlerThreadPool = Executors.newCachedThreadPool();
    }

    public boolean start() {

        if (logger.isLoggable(Level.FINER)) {
        if (!this.isStarted) {

            logger.info(log.msg("starting server..."));
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
            this.serverThread.start();


        return true;
    }

    public void stop() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
        if (this.isStarted) {

            logger.info(log.msg("stopping server..."));

            this.isStarted = false;

            // Set the interrupt state in the service thread
            this.serverThread.interrupt();
            if (!this.socket.isClosed()) {
                try {
                    this.socket.close();
                }
                catch (IOException e) {
                    logger.severe(log.msg("attempt to close server socket failed"));
                    e.printStackTrace();
                }
            }

            this.handlerThreadPool.shutdownNow();
    }

    public void join() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
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
                        keepAliveTimeout = Short.parseShort(propertyValue);
                    }
                    catch (NumberFormatException e) {
                        logger.warning(log.msg("'"+propertyValue+"' is not a valid "+SERVICE_KEEP_ALIVE_TIMEOUT_PROPERTY+" value"));
                    }
                }

                while (!this.socket.isClosed()) {

                    logger.info(log.msg("starting keep-alive timer..."));

                    try {
                        if (!this.connectionManager.waitForNewConnection(keepAliveTimeout)) {
                    }
                    catch (InterruptedException e) {
                        logger.info(log.msg("keep-alive timer interrupted"));
                        throw e;
                    }

                    logger.info(log.msg("keep-alive timeout canceled"));

                    logger.fine(log.msg("waiting for all clients to disconnect..."));

                    try {
                    }
                    catch (InterruptedException e) {
                        logger.info(log.msg("keep-alive wait interrupted"));
                        throw e;
                    }
                }
            }


    }

    public String getServiceName() {
        return this.serviceName;
    }

    void runServer() {

        if (logger.isLoggable(Level.FINER)) {

            Socket clientSocket;
            try {

                clientSocket = this.socket.accept();


                Connection connection = new SocketConnection(clientSocket);

            }
            catch (InterruptedException e) {

                if (this.socket.isClosed()) {
                    // TODO: The pipes used to send logging messages to the console
                    // will be broken if the most recent thread to write to the pipe exits.
                    // This means that we can't log any messages on the way out.
                    logger.fine(log.msg("server socket closed"));
                }
                else {
                    e.printStackTrace();
                }
                break;
        }

        // TODO: See note above - cannot send any log messages now
        logger.info(log.msg("exiting server thread"));

    }



        if (logger.isLoggable(Level.FINER)) {
        String propertyValue;

        int serverPort = DEFAULT_SERVICE_PORT;

        propertyValue = this.properties.getProperty(SERVICE_SOCKET_PORT_PROPERTY);
        if (propertyValue != null) {
            try {
                serverPort = Short.parseShort(propertyValue);
            }
            catch (NumberFormatException e) {
                logger.warning(log.msg("'"+propertyValue+"' is not a valid "+SERVICE_SOCKET_PORT_PROPERTY+" value - "+e.getMessage()));
            }
        }

        InetSocketAddress serverSocketAddress;

        propertyValue = this.properties.getProperty(SERVICE_SOCKET_ADDRESS_PROPERTY);
        if (propertyValue != null) {
            try {
                serverSocketAddress = new InetSocketAddress(InetAddress.getByName(propertyValue),serverPort);
            }
            catch (UnknownHostException e) {
                logger.warning(log.msg("'"+propertyValue+"' is not a valid "+SERVICE_SOCKET_ADDRESS_PROPERTY+" value - "+e.getMessage()));
                serverSocketAddress = new InetSocketAddress(serverPort);
            }
        }
        else {
            serverSocketAddress = new InetSocketAddress(serverPort);
        }

        logger.fine(log.msg("binding server socket to "+serverSocketAddress.toString()));

        try {

            this.socket = new ServerSocket();

            // This method will throw a BindException if the port is already bound to a socket.
            this.socket.bind(serverSocketAddress);

            logger.info(log.msg("server socket address is "+this.socket.getInetAddress().getHostAddress()+":"+this.socket.getLocalPort()));

            propertyValue = this.properties.getProperty(SERVICE_SOCKET_RECV_BUFFER_SIZE_PROPERTY);
        catch(BindException e) {
            logger.warning(log.msg("port "+serverPort+ " is currently in use - another server instance may be running"));
            throw e;
        }
        catch (IOException e) {
            logger.warning(log.msg("cannot construct server socket - "+e.getMessage()));
            throw e;
        }


    public static void configureLogging() {



    /**


        Thread shutdownHook = new Thread() {
            @Override
            public void run() {
                try {
            }
        };

        Runtime.getRuntime().addShutdownHook(shutdownHook);

        if (server.start()) {
            try {
                server.join();
            catch (InterruptedException e) {
                logger.info(slog.msg("server join interrupted"));
                Thread.currentThread().interrupt();
                server.stop();
            }
        }


    }

    /**
     * Entry point used to test minimal server interaction with keep-alive clients.
     * @param args
     */
    public static void main(String[] args) {


                @Override
                public Server construct(Properties properties) {
                    return new Server(properties,
                }

            });
    }
}