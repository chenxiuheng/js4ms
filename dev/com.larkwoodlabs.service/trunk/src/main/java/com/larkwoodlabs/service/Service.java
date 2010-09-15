package com.larkwoodlabs.service;

import java.io.ByteArrayInputStream;import java.io.ByteArrayOutputStream;import java.io.FileInputStream;import java.io.FileNotFoundException;import java.io.IOException;
import java.io.InputStream;import java.lang.reflect.InvocationTargetException;import java.net.BindException;
import java.net.ConnectException;import java.net.HttpURLConnection;import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;import java.net.URISyntaxException;import java.net.URLDecoder;import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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
    public static final int     DEFAULT_KEEP_ALIVE_TIMEOUT = 10000;    public static final int     DEFAULT_MAX_CONNECTIONS = 16;

    public static final String  SERVICE_PROPERTY_PREFIX = "com.larkwoodlabs.service.";    public static final String  SERVICE_MAX_CONNECTIONS_PROPERTY = SERVICE_PROPERTY_PREFIX + "maxconnections";
    public static final String  SERVICE_SOCKET_ADDRESS_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.address";
    public static final String  SERVICE_SOCKET_PORT_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.port";
    public static final String  SERVICE_SOCKET_RECV_BUFFER_SIZE_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.recvbuffersize";
    public static final String  SERVICE_SOCKET_SEND_BUFFER_SIZE_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.sendbuffersize";    public static final String  SERVICE_SOCKET_STALE_CONNECTION_CHECK_ENABLED_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.staleconnectioncheck.enabled";
    public static final String  SERVICE_SOCKET_TCP_NODELAY_ENABLED_PROPERTY = SERVICE_PROPERTY_PREFIX + "socket.nodelay.enabled";
    public static final String  SERVICE_USE_KEEP_ALIVE_PROPERTY = SERVICE_PROPERTY_PREFIX + "keepalive.enabled";
    public static final String  SERVICE_KEEP_ALIVE_TIMEOUT_PROPERTY = SERVICE_PROPERTY_PREFIX + "keepalive.timeout";
    public static final String  SERVICE_CONSOLE_ENABLED_PROPERTY = SERVICE_PROPERTY_PREFIX + "console.enabled";
    public static final String  SERVICE_CONSOLE_AUTOCLOSE_ENABLED_PROPERTY = SERVICE_PROPERTY_PREFIX + "console.autoclose.enabled";
    final static String LOGGING_PROPERTIES_URL_PROPERTY = "com.larkwoodlabs.logging.properties.url";    /*-- Member Variables ----------------------------------------------------*/    private final String ObjectId = Logging.identify(this);

    boolean useKeepAlive = false;

    ServerSocket socket;

    boolean isStarted = false;
    Thread serviceThread;
    int maxConnections = DEFAULT_MAX_CONNECTIONS;    HashMap<String,Connection> connections = new HashMap<String,Connection>();

    ConnectionHandler.Factory connectionHandlerFactory;

    private final ExecutorService handlerThreadPool;

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

        this.handlerThreadPool = Executors.newCachedThreadPool();
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
    /**     * @throws InterruptedException      *      */
    public void stop() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.stop"));
        }

        if (this.isStarted) {

            logger.fine(ObjectId + " stopping service...");

            this.isStarted = false;

            // Set the interrupt state in the service thread
            this.serviceThread.interrupt();            // Interrupt the accept() call in the service thread
            if (!this.socket.isClosed()) {
                try {
                    this.socket.close();
                }
                catch (IOException e) {
                    logger.severe(ObjectId + " attempt to close service socket failed");
                    e.printStackTrace();
                }
            }
            logger.fine(ObjectId + " closing connections...");            // Interrupt blocking reads in the handler threads            closeConnections();            logger.fine(ObjectId + " shutting down handler thread pool...");            // Give handler threads the chance to cleanup            this.handlerThreadPool.shutdown();            // Give the handler threads some time to exit.            if (!this.handlerThreadPool.awaitTermination(1000, TimeUnit.MILLISECONDS)) {                logger.fine(ObjectId + " one or more handlers could not be shutdown");            }            // This call will interrupt handler threads if they were waiting elsewhere (not blocking I/O).
            this.handlerThreadPool.shutdownNow();            logger.fine(ObjectId + " all handler threads have stopped");        }
    }
    /**     *      * @throws InterruptedException     */
    public void join() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.join"));
        }

        if (this.isStarted) {

            boolean useKeepAlive = false;

            String propertyValue = (String)getProperty(SERVICE_USE_KEEP_ALIVE_PROPERTY);
            if (propertyValue != null) {
                useKeepAlive = Boolean.parseBoolean(propertyValue);
            }

            if (useKeepAlive) {

                int keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;

                propertyValue = (String)getProperty(SERVICE_KEEP_ALIVE_TIMEOUT_PROPERTY);
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

                    logger.fine(ObjectId + " waiting for all clients to disconnect...");

                    try {
                        synchronized(noConnections) {
                            noConnections.wait();
                        }
                        logger.fine(ObjectId + " all clients have disconnected");
                    }
                    catch (InterruptedException e) {
                        logger.info(ObjectId+" keep-alive wait interrupted");
                        throw e;
                    }
                }
            }
            this.serviceThread.join();        }

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
                setSocketProperties(clientSocket);
                Connection connection = new SocketConnection(clientSocket);
                if (addConnection(connection)) {                    this.handlerThreadPool.execute(this.connectionHandlerFactory.construct(this, connection));                }                else {                    logger.fine(ObjectId + " cannot add connection - max connections exceeded");                    connection.close();                }
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

    /**     *      * @param connection     * @return      */    boolean addConnection(Connection connection) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.addConnection", connection));
        }
        synchronized (this.connections) {            if (this.connections.size() <= this.maxConnections) {
                this.connections.put(connection.getIdentifier(),connection);
        
                logger.fine(ObjectId + " added connection "+connection.getIdentifier());                synchronized (this.newConnection) {
                    this.newConnection.notifyAll();
                }                return true;            }            else {                logger.fine(ObjectId + " cannot add connection "+connection.getIdentifier() + " max connections exceeded");                return false;            }        }
    }
    /**     *      * @param connection     */    Connection getConnection(String identifier) {        if (logger.isLoggable(Level.FINER)) {            logger.finer(Logging.entering(ObjectId, "Service.getConnection", identifier));        }        synchronized (this.connections) {            return this.connections.get(identifier);        }    }    /**     *      * @param connection     */    boolean renameConnection(Connection connection, String newIdentifier) {        if (logger.isLoggable(Level.FINER)) {            logger.finer(Logging.entering(ObjectId, "Service.getConnection", newIdentifier));        }        String oldIdentifier = connection.getIdentifier();        synchronized (this.connections) {            if (this.connections.containsKey(oldIdentifier)) {                this.connections.remove(oldIdentifier);                logger.fine(ObjectId + " renaming connection "+oldIdentifier+" to "+connection.getIdentifier());                connection.setIdentifier(newIdentifier);                addConnection(connection);            }        }        return false;    }    /**     *      * @param identifier     */
    void removeConnection(Connection connection) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.removeConnection", connection));
        }

        synchronized (this.connections) {            if (this.connections.remove(connection.getIdentifier()) != null) {
                logger.fine(ObjectId + " removed connection "+connection.getIdentifier());                if (this.connections.size() == 0) {                    synchronized (this.noConnections) {                        this.noConnections.notifyAll();                    }                }            }        }
    }
    /**     *      */    void closeConnections() {        if (logger.isLoggable(Level.FINER)) {            logger.finer(Logging.entering(ObjectId, "Service.closeConnections"));        }        synchronized (this.connections) {            Set<Map.Entry<String,Connection>> entries = this.connections.entrySet();            for (Map.Entry<String, Connection> entry : entries) {                Connection connection = entry.getValue();                try {                    // This will wake up the handler thread if it is still waiting in a read                    // The handler thread will remove the connection                    connection.close();                }                catch (IOException e) {                    logger.warning(ObjectId + " cannot close connection '"+connection.getIdentifier() + " - "+e.getClass().getSimpleName()+": "+e.getMessage());                    e.printStackTrace();                }            }        }    }
    /**     *      * @throws IOException     */    void constructSocket() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Service.constructSocket"));
        }

        String propertyValue;

        int servicePort = DEFAULT_SERVICE_PORT;

        propertyValue = (String)getProperty(SERVICE_SOCKET_PORT_PROPERTY);
        if (propertyValue != null) {
            try {
                servicePort = Short.parseShort(propertyValue);
            }
            catch (NumberFormatException e) {
                logger.warning(ObjectId + " '"+propertyValue+"' is not a valid "+SERVICE_SOCKET_PORT_PROPERTY+" value - "+e.getMessage());
            }
        }

        InetSocketAddress serviceSocketAddress;

        propertyValue = (String)getProperty(SERVICE_SOCKET_ADDRESS_PROPERTY);
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

            propertyValue = (String)getProperty(SERVICE_SOCKET_RECV_BUFFER_SIZE_PROPERTY);            if (propertyValue != null) {                try {                    int bufferSize = Integer.parseInt(propertyValue);                    this.socket.setReceiveBufferSize(bufferSize);                }                catch (NumberFormatException e) {                    logger.warning(ObjectId+" cannot set recv buffer size property on socket - "+e.getMessage());                }            }        }
        catch(BindException e) {
            logger.warning(ObjectId+" port "+servicePort+ " is currently in use - another service instance may be running");
            throw e;
        }
        catch (IOException e) {
            logger.warning(ObjectId+" cannot construct service socket - "+e.getMessage());
            throw e;
        }
        this.maxConnections = DEFAULT_MAX_CONNECTIONS;        propertyValue = (String)getProperty(SERVICE_MAX_CONNECTIONS_PROPERTY);        if (propertyValue != null) {            try {                this.maxConnections = Integer.parseInt(propertyValue);            }            catch (NumberFormatException e) {                logger.warning(ObjectId+" invalid max connections property - "+e.getMessage());            }        }    }
    public void setSocketProperties(Socket clientSocket) {        String propertyValue = (String)getProperty(SERVICE_SOCKET_SEND_BUFFER_SIZE_PROPERTY);        if (propertyValue != null) {            try {                int bufferSize = Integer.parseInt(propertyValue);                clientSocket.setSendBufferSize(bufferSize);            }            catch (Exception e) {                logger.warning(ObjectId+" cannot set send buffer size property on socket - "+e.getMessage());            }        }    }    public static void configureLogging(Properties loggingProperties) {        String handlers = loggingProperties.getProperty("handlers");        if (handlers != null) {            //loggingProperties.remove("handlers");        }        // Iterate over configuration properties to locate logger entries and attempt        // to use the logger name to load a class to force static logger initialization.        // We must do this before loading the configuration into the LogManager so that loggers will        // be registered before the LogManager applies anyt level settings contained in the configuration.        Set<Map.Entry<Object,Object>> entries = loggingProperties.entrySet();        for (Map.Entry<Object,Object> entry : entries) {            String key = (String)entry.getKey();            if (key.endsWith(".level")) {                // Remove the .level part                String loggerName = key.substring(0,key.length()-6);                if (loggerName.length() == 0) {                    // Must be the root logger - skip to next                    continue;                }                try {                    // Try to load class to force static logger instantiation                    Class.forName(loggerName, true, Thread.currentThread().getContextClassLoader());                }                catch (ClassNotFoundException e) {                    System.out.println("cannot initialize logger '"+loggerName+"' - class not found");                    continue;                }            }        }        // Write the properties into a string so they can be loaded by the log manager        try {            ByteArrayOutputStream os = new ByteArrayOutputStream();            loggingProperties.store(os,"Logging Properties");            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());            LogManager.getLogManager().readConfiguration(is);        }        catch (Exception e) {            System.out.println("cannot configure logging - " + e.getMessage());            e.printStackTrace();        }    }    /**     *      */
    public static void configureLogging() {
        String loggingPropertiesUrl = System.getProperty(LOGGING_PROPERTIES_URL_PROPERTY);        if (loggingPropertiesUrl != null) {            URI uri = null;            try {                uri = new URI(loggingPropertiesUrl);            }            catch (URISyntaxException e) {               System.out.println("the value assigned to the '"+LOGGING_PROPERTIES_URL_PROPERTY+"' is not a valid URL.");                e.printStackTrace();            }            String path = uri.getPath();            if (path != null) {                if (uri.getScheme().equals("http")) {                    try {                        HttpURLConnection urlConnection = ((HttpURLConnection)uri.toURL().openConnection());                        if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {                            int contentLength = urlConnection.getContentLength();                            if (contentLength == -1) {                                System.out.println("the logging configuration fetched from '" + uri.toString() + "' is empty");                                return;                            }                            else {                                Properties properties = new Properties();                                properties.load(urlConnection.getInputStream());                                configureLogging(properties);                            }                        }                        else {                            System.out.println("cannot fetch logging configuration from '" + uri.toString() + "' - server returned " +                                               urlConnection.getResponseCode() + " " +                                                urlConnection.getResponseMessage() );                        }                    }                    catch (ConnectException e) {                        System.out.println("cannot fetch logging configuration '" + uri.toString() + "' - " + e.getMessage());                    }                    catch (IOException e) {                        System.out.println("cannot fetch logging configuration '" + uri.toString() + "' - " + e.getMessage());                        e.printStackTrace();                    }                }                else if (uri.getScheme().equals("file")) {                    try {                        InputStream inputStream = new FileInputStream(URLDecoder.decode(uri.getSchemeSpecificPart(),"UTF8"));                        Properties properties = new Properties();                        properties.load(inputStream);                        configureLogging(properties);                    }                    catch (FileNotFoundException e) {                        System.out.println("cannot read logging configuration '" + uri.toString() + "' - file not found");                    }                    catch (IOException e) {                        System.out.println("cannot read logging configuration '" + uri.toString() + "' - " + e.getMessage());                        e.printStackTrace();                    }                }            }        }
    }

    /**     *      * @param properties     * @param serviceFactory     * @throws InterruptedException      */    public static void runService(Properties properties, Service.Factory serviceFactory) throws InterruptedException {
        Object console = null;        Class<?> consoleClass = null;        if (Boolean.parseBoolean(properties.getProperty(SERVICE_CONSOLE_ENABLED_PROPERTY))) {            try {                System.out.println("attempting to load Console class");                consoleClass = Class.forName("com.larkwoodlabs.service.Console");                console = consoleClass.getConstructor(String.class).newInstance("Test Service");                 // new Console("Test Service");
            }            catch (Exception e) {                System.out.println("cannot load Console class - "+e.getMessage());                // TODO Auto-generated catch block                e.printStackTrace();            }        }        configureLogging();        final Service service = serviceFactory.construct(properties);

        Thread shutdownHook = new Thread() {
            @Override
            public void run() {
                try {                    service.stop();                }                catch (InterruptedException e) {                    Thread.currentThread().interrupt();                }
            }
        };

        Runtime.getRuntime().addShutdownHook(shutdownHook);

        if (service.start()) {
            try {
                service.join();            }
            catch (InterruptedException e) {
                logger.info("[ static ] service join interrupted");
                Thread.currentThread().interrupt();
                service.stop();
            }
        }

        if (console != null) {            if (Boolean.parseBoolean(properties.getProperty(SERVICE_CONSOLE_AUTOCLOSE_ENABLED_PROPERTY))) {                logger.info("[ static ] closing console window...");                try {                    consoleClass.getMethod("exit").invoke(console);                }                catch (Exception e) {                    System.out.println("cannot close Console class - "+e.getMessage());                    e.printStackTrace();                }            }            else {                try {
                    logger.info("[ static ] Waiting for console window to close...");
                    try {                        consoleClass.getMethod("waitForClose").invoke(console);                    }                    catch (InvocationTargetException e) {                        // TODO Auto-generated catch block                        if (e.getCause() instanceof InterruptedException) {                            throw (InterruptedException)e.getCause();                        }                        e.printStackTrace();                    }                    catch (Exception e) {                        System.out.println("cannot close Console class - "+e.getMessage());                        e.printStackTrace();                    }                    //((Console)console).waitForClose();
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }            }
        }

        //Runtime.getRuntime().removeShutdownHook(shutdownHook);

    }

    /**
     * Entry point used to test minimal service interaction with keep-alive clients.
     * @param args
     */
    public static void main(String[] args) {
        
        try {            runService(System.getProperties(), new Factory() {

                @Override
                public Service construct(Properties properties) {
                    return new Service("Test Service", properties, new ConnectionHandler.Factory());
                }

            });        }        catch (InterruptedException e) {            Thread.currentThread().interrupt();        }
    }
}
