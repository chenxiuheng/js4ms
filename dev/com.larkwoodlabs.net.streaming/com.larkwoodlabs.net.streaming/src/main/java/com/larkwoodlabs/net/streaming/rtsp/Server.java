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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Logging;


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

    public static Server create(final Properties configuration) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(" [static] ", "Server.create", configuration));
        }

        return new Server(configuration);
    }

    public static String getName() {
        return SERVER_NAME;
    }

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
    

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     */
    protected Server(final Properties configuration) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "Server.Server"));
        }

        this.configuration = configuration;
        
        this.taskTimer = new Timer(this.getClass().getName()+" timer");

        this.threadPool = Executors.newCachedThreadPool(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });

    }
    
    public void bind() throws IOException {
        this.serverSocket = new ServerSocket(Integer.parseInt(this.configuration.getProperty("PORT_BINDING")));
    }

    public void start() throws IOException {

        if (this.serverSocket == null) {
            bind();
        }
        
        execute(this);
    }

    /**
     * 
     */
    public void run() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "RtspServer.run"));
        }

        while (!Thread.currentThread().isInterrupted()) {

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
                    logger.info(ObjectId + " server socket closed");
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

            if (!Thread.currentThread().isInterrupted()) {

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(ObjectId +
                                " incoming connection from " +
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
            logger.fine(ObjectId + " closing open client connections...");
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

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(ObjectId + " exiting Server thread");
        }
    }

    public void stop() {
        try {
            if (this.serverSocket != null) {
                this.serverSocket.close();
                this.serverSocket = null;
            }
            this.threadPool.shutdownNow();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void execute(Runnable runnable) {
        this.threadPool.execute(runnable);
    }

    void schedule(TimerTask timerTask, long delay) {
        this.taskTimer.schedule(timerTask, delay);
    }

    void schedule(TimerTask timerTask, long delay, long period) {
        this.taskTimer.schedule(timerTask, delay);
    }

    boolean containsOutputConnection(String sessionCookie) {
        synchronized (this.outputConnections) {
            return this.outputConnections.containsKey(sessionCookie);
        }
    }

    Connection getOutputConnection(String sessionCookie) {
        synchronized (this.outputConnections) {
            return this.outputConnections.get(sessionCookie);
        }
    }
    
    void addOutputConnection(String sessionCookie, Connection connection) {
        synchronized (this.outputConnections) {
            this.outputConnections.put(sessionCookie, connection);
        }
    }

    Connection removeOutputConnection(String sessionCookie) {
        synchronized (this.outputConnections) {
            return this.outputConnections.remove(sessionCookie);
        }
    }

    void removeConnection(Connection connection) {
        this.connections.remove(connection);
    }

    void addSession(Session session) {
        synchronized (this.sessions) {
            this.sessions.put(session.getSessionId(), session);
        }
    }

    boolean containsSession(String sessionId) {
        synchronized (this.sessions) {
            return this.sessions.containsKey(sessionId);
        }
    }
    
    Session getSession(String sessionId) {
        synchronized (this.sessions) {
            return this.sessions.get(sessionId);
        }
    }
    
    void removeSession(String sessionId) {
        synchronized (this.sessions) {
            this.sessions.remove(sessionId);
        }
    }
    
}
