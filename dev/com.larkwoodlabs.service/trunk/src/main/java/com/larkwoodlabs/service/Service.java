package com.larkwoodlabs.service;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.LogFormatter;
import com.larkwoodlabs.util.logging.Logging;

public class Service {

    /*-- Inner Classes--- ----------------------------------------------------*/    public interface Factory {
        Service construct(Properties properties);
    }

    /*-- Static Variables ----------------------------------------------------*/    public static final Logger logger = Logger.getLogger(Service.class.getName());

    public static final int     DEFAULT_SERVICE_PORT = 0;
    public static final int     DEFAULT_SOCKET_BUFFER_SIZE = 8 * 1024;
    public static final boolean DEFAULT_STALE_CONNECTION_CHECK = false;
    public static final boolean DEFAULT_TCP_NODELAY = true;
    public static final int     DEFAULT_KEEP_ALIVE_TIMEOUT = 10000;

    public static final String  SERVICE_PROPERTY_PREFIX = "com.larkwoodlabs.service.";
    public static final String  SERVICE_SOCKET_ADDRESS_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.address";
    public static final String  SERVICE_SOCKET_PORT_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.port";
    public static final String  SERVICE_SOCKET_BUFFER_SIZE_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.buffersize";
    public static final String  SERVICE_SOCKET_STALE_CONNECTION_CHECK_ENABLED_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.staleconnectioncheck.enabled";
    public static final String  SERVICE_SOCKET_TCP_NODELAY_ENABLED_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.nodelay.enabled";
    public static final String  SERVICE_USE_KEEP_ALIVE_PROPERTY = SERVICE_PROPERTY_PREFIX + "keepalive.enabled";
    public static final String  SERVICE_KEEP_ALIVE_TIMEOUT_PROPERTY = SERVICE_PROPERTY_PREFIX + "keepalive.timeout";
    public static final String  SERVICE_CONSOLE_ENABLED_PROPERTY = SERVICE_PROPERTY_PREFIX + "console.enabled";

    /*-- Member Variables ----------------------------------------------------*/    private final String ObjectId = Logging.identify(this);

    boolean useKeepAlive = false;

    ServerSocket socket;

    boolean isStarted = false;
    Thread serviceThread;

    HashMap<String,Connection> connections = new HashMap<String,Connection>();

    ConnectionHandler.Factory connectionHandlerFactory;

    private final ExecutorService threadPool;

    Object newConnection = new Object();
    Object noConnections = new Object();

    String serviceName = Service.class.getSimpleName();

    Properties properties;

    /*-- Member Functions  ----------------------------------------------------*/    /**     *      */    protected Service(String serviceName, Properties properties, ConnectionHandler.Factory connectionHandlerFactory) {

        this.serviceName = serviceName;
        this.properties = properties;

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.Service", connectionHandlerFactory));
        }

        this.connectionHandlerFactory = connectionHandlerFactory;

        this.threadPool = Executors.newCachedThreadPool(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                //t.setDaemon(true);
                return t;
            }
        });
    }
    /**     *      * @return     */
    public boolean start() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.start"));
        }

        if (!this.isStarted) {

            try {
                constructSocket();
            }
            catch (IOException e) {
                logger.severe(ObjectId + " cannot start service - attempt to construct socket failed");
                return false;
            }

            this.serviceThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    runService();
                }
            }, this.serviceName + " service thread");

            this.isStarted = true;
            this.serviceThread.start();
        }

        return true;
    }
    /**     *      */
    public void stop() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.stop"));
        }

        if (this.isStarted) {

            logger.fine(ObjectId + " stopping service...");

            this.isStarted = false;

            // Set the interrupt state in the service thread
            this.serviceThread.interrupt();

            // Interrupt the accept() call in the service thread
            if (!this.socket.isClosed()) {
                try {
                    this.socket.close();
                }
                catch (IOException e) {
                    logger.severe(ObjectId + " attempt to close service socket failed");
                    e.printStackTrace();
                }
            }

            // Closing connections should cause handler threads to exit
            closeConnections();

            // This call will attempt to interrupt the threads
            this.threadPool.shutdownNow();

        }
    }
    /**     *      * @throws InterruptedException     */
    public void join() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.join"));
        }

        if (this.isStarted) {

            boolean useKeepAlive = false;

            String propertyValue = getProperty(SERVICE_USE_KEEP_ALIVE_PROPERTY).toString();
            if (propertyValue != null) {
                useKeepAlive = Boolean.parseBoolean(propertyValue);
            }

            if (useKeepAlive) {

                int keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;

                propertyValue = getProperty(SERVICE_KEEP_ALIVE_TIMEOUT_PROPERTY).toString();
                if (propertyValue != null) {
                    try {
                        keepAliveTimeout = Short.parseShort(propertyValue);
                    }
                    catch (NumberFormatException e) {
                        logger.warning(ObjectId + " '"+propertyValue+"' is not a valid "+SERVICE_KEEP_ALIVE_TIMEOUT_PROPERTY+" value");
                    }
                }

                while (!this.socket.isClosed()) {

                    synchronized(newConnection) {

                        logger.info(ObjectId+" starting keep-alive timer...");

                        try {
                            newConnection.wait(keepAliveTimeout);
                        }
                        catch (InterruptedException e) {
                            logger.info(ObjectId+" keep-alive timer interrupted");
                            throw e;
                        }

                        if (this.connections.size() == 0) {

                            logger.info(ObjectId+" keep-alive timer has expired");

                            stop();

                            break;
                        }

                        logger.info(ObjectId+" keep-alive timeout canceled");
                    }

                    logger.fine(ObjectId + " waiting for all keep-alive clients to disconnect...");

                    try {
                        synchronized(noConnections) {
                            noConnections.wait();
                        }
                        logger.fine(ObjectId + " all keep-alive clients have disconnected");
                    }
                    catch (InterruptedException e) {
                        logger.info(ObjectId+" keep-alive wait interrupted");
                        throw e;
                    }
                }
            }

            logger.info(ObjectId+" waiting for service thread to stop...");

            try {
                this.serviceThread.join();
                logger.info(ObjectId+" service thread stopped");
            }
            catch (InterruptedException e) {
                logger.info(ObjectId+" service thread join interrupted");
                throw e;
            }

        }

    }
    /**     *      * @return     */
    public String getServiceName() {
        return this.serviceName;
    }
    /**     *      * @param factory     */
    public void setConnectionHandlerFactory(ConnectionHandler.Factory factory) {

        this.connectionHandlerFactory = factory;
    }
    /**     *      * @param key     * @return     */
    public Object getProperty(String key) {
        return this.properties.getProperty(key);
    }
    /**     *      * @param key     * @param value     */
    public void setProperty(String key, Object value) {
        this.properties.put(key, value);
    }
    /**     *      */
    void runService() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.runService"));
        }

        logger.info(ObjectId + " entering service thread");

        while (!Thread.currentThread().isInterrupted() && !this.socket.isClosed()) {

            Socket clientSocket;
            try {

                logger.fine(ObjectId + " listening for incoming client connections...");
                clientSocket = this.socket.accept();
                logger.fine(ObjectId + " accepted connection from "+clientSocket.getInetAddress());

                Connection connection = new SocketConnection(this, clientSocket);
                addConnection(connection);

                ConnectionHandler handler = this.connectionHandlerFactory.construct(connection);
                //new Thread(handler).start();

                this.threadPool.execute(handler);

            }
            catch (IOException e) {

                if (this.socket.isClosed()) {
                    // TODO: The pipes used to send logging messages to the console
                    // will be broken if the most recent thread to write to the pipe exits.
                    // This means that we can't log any messages on the way out.
                    //logger.fine(ObjectId + " service socket closed");
                }
                else {
                    e.printStackTrace();
                }
                break;
            }
        }

        // TODO: See note above - cannot send any log messages now
        //logger.info(ObjectId + " exiting service thread");

    }

    /**     *      * @param connection     */    void addConnection(Connection connection) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.addConnection", connection));
        }

        this.connections.put(connection.getIdentifier(),connection);

        synchronized (this.newConnection) {
            this.newConnection.notifyAll();
        }
    }
    /**     *      * @param identifier     */
    void removeConnection(String identifier) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.removeConnection", identifier));
        }

        this.connections.remove(identifier);
        if (this.connections.size() == 0) {
            synchronized (this.noConnections) {
                this.noConnections.notifyAll();
            }
        }
    }
    /**     *      */
    void closeConnections() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.closeConnections"));
        }

        Set<Map.Entry<String,Connection>> connections = this.connections.entrySet();

        for (Map.Entry<String,Connection> entry : connections) {
            try {
                entry.getValue().close(false);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        connections.clear();

        synchronized (this.noConnections) {
            this.noConnections.notifyAll();
        }
    }

    /**     *      * @throws IOException     */    void constructSocket() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.constructSocket"));
        }

        String propertyValue;

        int servicePort = DEFAULT_SERVICE_PORT;

        propertyValue = getProperty(SERVICE_SOCKET_PORT_PROPERTY).toString();
        if (propertyValue != null) {
            try {
                servicePort = Short.parseShort(propertyValue);
            }
            catch (NumberFormatException e) {
                logger.warning(ObjectId + " '"+propertyValue+"' is not a valid "+SERVICE_SOCKET_PORT_PROPERTY+" value - "+e.getMessage());
            }
        }

        InetSocketAddress serviceSocketAddress;

        propertyValue = getProperty(SERVICE_SOCKET_ADDRESS_PROPERTY).toString();
        if (propertyValue != null) {
            try {
                serviceSocketAddress = new InetSocketAddress(InetAddress.getByName(propertyValue),servicePort);
            }
            catch (UnknownHostException e) {
                logger.warning(ObjectId + " '"+propertyValue+"' is not a valid "+SERVICE_SOCKET_ADDRESS_PROPERTY+" value - "+e.getMessage());
                serviceSocketAddress = new InetSocketAddress(servicePort);
            }
        }
        else {
            serviceSocketAddress = new InetSocketAddress(servicePort);
        }

        if (logger.isLoggable(Level.INFO)) {
            logger.info(ObjectId + " binding service socket to "+serviceSocketAddress.toString());
        }

        try {

            this.socket = new ServerSocket();

            // This method will throw a BindException if the port is already bound to a socket.
            this.socket.bind(serviceSocketAddress);

            logger.info(ObjectId+" socket address "+this.socket.getInetAddress().getHostAddress()+":"+this.socket.getLocalPort());

        }
        catch(BindException e) {
            logger.warning(ObjectId+" port "+servicePort+ " is currently in use - another service instance may be running");
            throw e;
        }
        catch (IOException e) {
            logger.warning(ObjectId+" cannot construct service socket - "+e.getMessage());
            throw e;
        }
    }
    /**     *      */
    public static void setLoggerLevels() {

        final String LOGGER_PROPERTY_PREFIX = "com.larkwoodlabs.logger.";
        final int prefixLength = LOGGER_PROPERTY_PREFIX.length();

        Handler[] handlers = Logger.getLogger("").getHandlers();
        for ( int index = 0; index < handlers.length; index++ ) {
            // System.out.println("handler "+handlers[index].getClass().getName());
            handlers[index].setLevel(Level.FINER);
            handlers[index].setFormatter(new LogFormatter());
        }

        Properties properties = System.getProperties();
        Set<Map.Entry<Object,Object>> entries = properties.entrySet();

        LogManager manager = LogManager.getLogManager();

        logger.setLevel(Level.WARNING);

        for (Map.Entry<Object,Object> entry : entries) {
            String key = entry.getKey().toString();
            if (key.startsWith(LOGGER_PROPERTY_PREFIX)) {
                String loggerName = key.substring(prefixLength);
                if (loggerName.length() == 0) {
                    logger.warning(" [static]   property '"+key+"' does not include a logger name");
                }

                Level loggerLevel = Level.WARNING;
                boolean loadClass = false;
                String className = null;

                // Determine the name of the class that must be loaded force instantiation of the logger
                String value = entry.getValue().toString();
                String[] fields = value.split(";");

                if (fields.length == 0) {
                    // Invalid property value
                    logger.warning(" [static]  logger options missing from property '"+key+"'");
                    continue;
                }

                if (fields.length >= 1) {
                    try {
                        loggerLevel = Level.parse(fields[0]);
                    }
                    catch (IllegalArgumentException e) {
                        logger.warning(" [static]  cannot initialize logger '"+loggerName+"' because '"+fields[0]+"' does not map to a recognized log level value");
                        continue;
                    }
                }

                if (fields.length >= 2) {
                    loadClass = Boolean.parseBoolean(fields[1]);
                }

                if (fields.length >= 3) {
                    className = fields[2];
                }
                else if (loadClass) {
                    className = loggerName;
                }

                try{
                    if (loadClass) {
                        // Load class to force static logger instantiation
                        Class.forName(className);
                    }
                }
                catch (ClassNotFoundException e) {
                    Service.logger.warning(" [static]  cannot initialize logger '"+loggerName+"' - class '"+className+"' not found");
                    continue;
                }

                Logger logger = manager.getLogger(loggerName);
                if (logger != null) {
                    logger.setLevel(loggerLevel);
                }
                else {
                    Service.logger.warning(" [static]  cannot initialize logger '"+loggerName+"' - no such logger exists");
                    continue;
                }
            }
        }
    }

    /**     *      * @param properties     * @param serviceFactory     */    public static void startService(Properties properties, Service.Factory serviceFactory) {

        Console console = null;
        if (Boolean.parseBoolean(properties.getProperty(SERVICE_CONSOLE_ENABLED_PROPERTY))) {
            console = new Console("Test Service");
        }

        setLoggerLevels();

        final Service service = serviceFactory.construct(properties);

        Thread shutdownHook = new Thread() {
            @Override
            public void run() {
                service.stop();
            }
        };

        Runtime.getRuntime().addShutdownHook(shutdownHook);

        if (service.start()) {
            try {
                service.join();
            }
            catch (InterruptedException e) {
                logger.info(" [static]  service join interrupted");
                Thread.currentThread().interrupt();
                service.stop();
            }
        }

        if (console != null) {
            try {
                logger.info(" [static]  Waiting for console window to close...");
                console.waitForClose();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Runtime.getRuntime().removeShutdownHook(shutdownHook);

    }

    /**
     * Entry point used to test minimal service interaction with keep-alive clients.
     * @param args
     */
    public static void main(String[] args) {

        startService(System.getProperties(), new Factory() {

            @Override
            public Service construct(Properties properties) {
                return new Service("Test Service", properties, new ConnectionHandler.Factory());
            }

        });
    }
}
