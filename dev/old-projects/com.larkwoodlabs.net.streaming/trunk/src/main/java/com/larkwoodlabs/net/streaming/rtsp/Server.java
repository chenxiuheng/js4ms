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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.LogFormatter;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An RTSP server.
 * This class waits for incoming client connection requests and delegates the handling of
 * each new connection to a {@link ServerConnectionHandler}.
 *
 * @author Gregory Bumgardner
 */
public final class Server implements Runnable {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(Server.class.getName());

    public static final String  DEFAULT_ADDRESS_BINDING = "0.0.0.0";
    public static final int     DEFAULT_PORT_BINDING = 8054;
    public static final int     DEFAULT_SO_TIMEOUT = 0;
    public static final int     DEFAULT_SOCKET_BUFFER_SIZE = 8 * 1024;
    public static final boolean DEFAULT_STALE_CONNECTION_CHECK = false;
    public static final boolean DEFAULT_TCP_NODELAY = true;
    public static final String  SERVER_VERSION = "0.1.0"; // Replace with package version
    public static final String  SERVER_NAME = Server.class.getName() + " " + SERVER_VERSION;
    
    public static final int     CLIENT_REMOVAL_DELAY = 30000;

    /*-- Static Functions ----------------------------------------------------*/


    /**
     * Returns a default set of server configuration properties.
     */
    public static Properties getDefaultConfiguration() {

        Properties configuration = new Properties();

        // Set default values
        configuration.setProperty("ADDRESS_BINDING", String.valueOf(DEFAULT_ADDRESS_BINDING));
        configuration.setProperty("PORT_BINDING", String.valueOf(DEFAULT_PORT_BINDING));
        configuration.setProperty("SO_TIMEOUT", String.valueOf(DEFAULT_SO_TIMEOUT));
        configuration.setProperty("SOCKET_BUFFER_SIZE", String.valueOf(DEFAULT_SOCKET_BUFFER_SIZE));
        configuration.setProperty("STALE_CONNECTION_CHECK", String.valueOf(DEFAULT_STALE_CONNECTION_CHECK));
        configuration.setProperty("TCP_NODELAY", String.valueOf(DEFAULT_TCP_NODELAY));
        
        return configuration;
    }

    /**
     * Constructs a Server instance that is initialized using the properties
     * contained in the specified property file.
     * @param propertiesFileName - A file containing server configuration properties.
     * @throws IOException If an I/O error occurs when accessing the properties file.
     */
    public static Server create(final String propertiesFileName) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(" [static] ", "Server.create", propertiesFileName));
        }

        Properties configuration = getDefaultConfiguration();

        if (propertiesFileName.length() > 0) {
            FileInputStream in = new FileInputStream(propertiesFileName);
            configuration.load(in);
            in.close();
        }
        
        return new Server(configuration);
    }

    /**
     * Constructs a Server instance that is initialized using the specified properties.
     * @param configuration - A set of server configuration properties.
     */
    public static Server create(final Properties configuration) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(" [static] ", "Server.create", configuration));
        }

        return new Server(configuration);
    }

    /**
     * Returns the server name that will be sent in RTSP responses.
     */
    public static String getName() {
        return SERVER_NAME;
    }

    /**
     * Returns the server version that will be sent in RTSP responses.
     */
    public static String getVersion() {
        return SERVER_VERSION;
    }


    /*-- Member Variables ----------------------------------------------------*/

    private final String ObjectId = Logging.identify(this);
    
    private ServerSocket serverSocket = null;

    private final HashSet<Connection> connections = new HashSet<Connection>();
    
    private final HashMap<String, Connection> outputConnections = new HashMap<String, Connection>();
    
    private final HashMap<String, Session> sessions = new HashMap<String, Session>();

    private final Timer taskTimer;
    
    private final ExecutorService threadPool;
    
    private final Properties configuration;

    private Object lock = new Object();
    
    private Object notifier = new Object();
    
    private boolean isRunning = false;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a server using the specified configuration properties.
     * @param configuration - A set of configuration properties.
     */
    protected Server(final Properties configuration) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Server.Server"));
        }

        this.configuration = configuration;
        
        this.taskTimer = new Timer(this.getClass().getName()+" timer");

        this.threadPool = Executors.newCachedThreadPool(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                //t.setDaemon(true);
                return t;
            }
        });

    }
    
    /**
     * Constructs a new ServerSocket instance for this server.
     * Callers can use exceptions thrown by this method to detect when a
     * server socket has already been bound to the configured port.
     * @throws IOException If an I/O error occurs during ServerSocket construction.
     */
    public void bind() throws IOException {
        this.serverSocket = new ServerSocket(Integer.parseInt(this.configuration.getProperty("PORT_BINDING")));
    }

    /**
     * Starts the server.
     * @throws IOException If an I/O error occurs.
     */
    public void start() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Server.start"));
        }

        synchronized(this.lock) {

            if (!this.isRunning) {
        
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(ObjectId + " starting server");
                }
        
                this.isRunning = true;

                if (this.serverSocket == null) {
                    bind();
                }
                
                execute(this);
            }
        }
    }

    /**
     * Executes the main server processing loop.
     */
    @Override
    public void run() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Server.run"));
        }

        while (this.isRunning) {

            Socket socket;
            try {
                // Wait for client to connect
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(ObjectId + " waiting to accept client connection...");
                }

                socket = this.serverSocket.accept();
            }
            catch (IOException e) {
                if (this.serverSocket == null || this.serverSocket.isClosed()) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(ObjectId + " server socket closed");
                    }
                }
                else {
                    logger.warning(ObjectId + " cannot accept client connection - " + e.getClass().getName() + ":" + e.getMessage());
                }
                break;
            }
            catch (Exception e) {
                logger.warning(ObjectId + " cannot accept client connection - " + e.getClass().getName() + ":" + e.getMessage());
                e.printStackTrace();
                break;
            }

            if (this.isRunning) {

                if (logger.isLoggable(Level.INFO)) {
                    logger.info(ObjectId +
                                " accepting incoming connection from " +
                                Logging.address(socket.getInetAddress()));
                }
                
                try {
                    ServerSocketConnection connection = new ServerSocketConnection(this, socket);
                    synchronized (this.connections) {
                        this.connections.add(connection);
                    }
                    execute(new ServerConnectionHandler(this, connection));
                }
                catch(IOException e) {
                    // Treated as a non-fatal exception.
                    // May have been thrown because the socket was closed by the peer
                    // before the connection could be constructed
                    logger.warning(ObjectId + " cannot construct client connection - " + e.getClass().getName() + ":" + e.getMessage());
                }
            }
        }

        if (this.serverSocket != null && !this.serverSocket.isClosed()) {
            try {
                this.serverSocket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(ObjectId + " canceling timer tasks...");
        }

        this.taskTimer.cancel();

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(ObjectId + " closing open client sessions...");
        }
        
        synchronized (this.sessions) {

            // Copy to avoid concurrent modification
            HashSet<Session> sessions = new HashSet<Session>(this.sessions.values());
            
            for (Session session : sessions) {
                try {
                    session.close();
                }
                catch (InterruptedException e) {
                    // Keep going!
                }
            }
            this.sessions.clear();
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(ObjectId + " closing open client connections...");
        }
        
        synchronized (this.connections) {

            // Copy to avoid concurrent modification
            HashSet<Connection> connections = new HashSet<Connection>(this.connections);

            for (Connection connection : connections) {
                try {
                    connection.close();
                }
                catch (IOException e) {
                    // Keep going...
                    e.printStackTrace();
                }
            }
            this.connections.clear();
        }
        
        if (logger.isLoggable(Level.INFO)) {
            logger.info(ObjectId + " server stopped");
        }
        
        synchronized (this.notifier) {
            this.notifier.notifyAll();
        }
    }

    /**
     * Stops the server.
     */
    public void stop() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Server.stop"));
        }
        
        synchronized (this.lock) {
    
            if (this.isRunning) {
                
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(ObjectId + " stopping server...");
                }
                
                this.isRunning = false;
        
                if (this.serverSocket != null) {
                    try {
                        this.serverSocket.close();
                    }
                    catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    this.serverSocket = null;
                }
        
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(ObjectId + " shutting down thread pool...");
                }
        
                this.threadPool.shutdownNow();
            }
        }
    }
    
    /**
     * Waits for the server to shutdown.
     * @throws InterruptedException 
     */
    public void waitForShutdown() throws InterruptedException {
        synchronized (this.notifier) {
            this.notifier.wait();
        }
    }

    /**
     * Executes a runnable using a thread in the server thread pool.
     * @param runnable - The runnable to be executed.
     */
    void execute(Runnable runnable) {
        this.threadPool.execute(runnable);
    }

    /**
     * Schedules a TimerTask for execution in the server Timer thread.
     * @param timerTask - The TimerTask to be scheduled for execution.
     * @param delay - The task execution delay in milliseconds.
     */
    void schedule(TimerTask timerTask, long delay) {
        this.taskTimer.schedule(timerTask, delay);
    }

    /**
     * Schedules a TimerTask for repeated execution in the server Timer thread.
     * @param timerTask - The TimerTask to be scheduled for execution.
     * @param delay - The task execution delay in milliseconds.
     * @param period - The task execution period in milliseconds.
     */
    void schedule(TimerTask timerTask, long delay, long period) {
        this.taskTimer.schedule(timerTask, delay);
    }

    /**
     * Indicates whether a tunnel connection with the specified session cookie identifier exists.
     * @param sessionCookie - An HTTP tunnel connection identifier as specified
     *                        in an RTSP <code>x-sessioncookie</code> header. 
     */
    boolean containsOutputConnection(String sessionCookie) {
        synchronized (this.outputConnections) {
            return this.outputConnections.containsKey(sessionCookie);
        }
    }

    /**
     * Returns the tunnel connection identified by the specified session cookie identifer.
     * @param sessionCookie - An HTTP tunnel connection identifier as specified
     *                        in an RTSP <code>x-sessioncookie</code> header.
     */
    Connection getOutputConnection(String sessionCookie) {
        synchronized (this.outputConnections) {
            return this.outputConnections.get(sessionCookie);
        }
    }
    
    /**
     * Adds the specified tunnel connection to the internal collection of tunnel connections.
     * @param sessionCookie - An HTTP tunnel connection identifier as specified
     *                        in an RTSP <code>x-sessioncookie</code> header.
     * @param connection - The tunnel connection.
     */
    void addOutputConnection(String sessionCookie, Connection connection) {
        synchronized (this.outputConnections) {
            this.outputConnections.put(sessionCookie, connection);
        }
    }

    /**
     * Removes the tunnel connection identified by the specified session cookie identifer.
     * @param sessionCookie - An HTTP tunnel connection identifier as specified
     *                        in an RTSP <code>x-sessioncookie</code> header.
     */
    Connection removeOutputConnection(String sessionCookie) {
        synchronized (this.outputConnections) {
            return this.outputConnections.remove(sessionCookie);
        }
    }

    /**
     * Removes the specified connection from the internal collection of client connections.
     * @param connection - The {@link Connection} to be removed.
     */
    void removeConnection(Connection connection) {
        this.connections.remove(connection);
    }

    /**
     * Adds a new RTSP session to the internal collection of active RTSP sessions.
     * @param session - A new RTSP {@link Session}.
     */
    void addSession(Session session) {
        synchronized (this.sessions) {
            this.sessions.put(session.getSessionId(), session);
        }
    }

    /**
     * Indicates whether an RTSP session with the specified session identifier is contained
     * in the collection of active RTSP sessions.
     * @param sessionId - The session identifier as specified in an RTSP <code>Session</code> header.
     */
    boolean containsSession(String sessionId) {
        synchronized (this.sessions) {
            return this.sessions.containsKey(sessionId);
        }
    }
    
    /**
     * Returns the RTSP session identified by the specified session identifier.
     * @param sessionId - The session identifier as specified in an RTSP <code>Session</code> header.
     */
    Session getSession(String sessionId) {
        synchronized (this.sessions) {
            return this.sessions.get(sessionId);
        }
    }
    
    /**
     * Removes the RTSP session identified by the specified session identifier
     * from the internal collection of active sessions.
     * @param sessionId - The session identifier as specified in an RTSP <code>Session</code> header.
     */
    void removeSession(String sessionId) {
        synchronized (this.sessions) {
            this.sessions.remove(sessionId);
        }
    }
    
    public static void main(final String[] args) {
        
        Handler[] handlers = Logger.getLogger("").getHandlers();
        for ( int index = 0; index < handlers.length; index++ ) {
            handlers[index].setLevel(Level.FINER);
            handlers[index].setFormatter(new LogFormatter());
        }
        
        logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.streaming.rtsp.Server.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.streaming.rtsp.ConnectionHandler.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.streaming.rtsp.Connection.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.streaming.rtsp.Session.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.amt.AmtInterface.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.amt.ChannelMembershipManager.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.amt.InterfaceMembershipManager.logger.setLevel(Level.FINER);
        
        try {
            final Server server = Server.create(args.length > 0 ? args[0] : "");

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    server.stop();
                }
            });
            
            server.start();
        }
        catch (IOException e) {
            logger.severe("server initialization failed - " + e.getClass().getName() + ":" + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }

    }
}
