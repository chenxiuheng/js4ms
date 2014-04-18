
import java.applet.Applet;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import com.larkwoodlabs.net.streaming.rtsp.Server;
import com.larkwoodlabs.util.logging.LogFormatter;
import com.larkwoodlabs.util.logging.Logging;


public final class AmtProxyApplet extends Applet {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtProxyApplet.class.getName());

    private static final long serialVersionUID = -320790566981694462L;


    /*-- Member Variables ----------------------------------------------------*/

    private final String ObjectId = Logging.identify(this);

    /**
     * Static server instance - created for first applet constructed in JVM instance.
     * Destroyed when JVM shuts down. Allows multiple pages to that share JVM instances to share proxy.
     */
    private Server server = null;
    
    private Thread thread = null;;

    private Socket socket = null;

    private JSObject window = null;
    
    private boolean isJavaScriptEnabled = false;
    private String onReadyFuncName = null;
    private String onErrorFuncName = null;
    private String onFailoverFuncName = null;
    
    private boolean isStopped = false;
    
    /*-- Member Functions ----------------------------------------------------*/

    public AmtProxyApplet() {
        setLoggerLevels();
    }

    public String getAppletInfo() {
        return "AMT Proxy Server Applet, Version 0.0.1.x, Copyright © 2010, Greg Bumgardner, All rights reserved.";
    }
    
    public String[][] getParameterInfo() {
        String pinfo[][] = {
                {"onready",     "string",   "name of onready callback function"},
                {"onerror",     "string",   "name of onerror callback function"},
                {"onfailover",  "string",   "name of onfailover callback function"}
            };
        return pinfo;
    }

    public String getLoggerLevels() {
        String result = "{";
        Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                Logger logger = Logger.getLogger(name);
                if (logger != null) {
                    Level level = logger.getLevel();
                    if (level != null) {
                        result += '\'' + name + "':'" + level.getName() + (names.hasMoreElements() ? "'," : '\'');
                    }
                }
            }
        }
        result += "}";
        return result;
    }

    public void setLoggerLevel(String name, String level) {
        if (name != null && level != null) {
            Logger logger = Logger.getLogger(name);
            if (logger != null) {
                try {
                    logger.setLevel(Level.parse(level));
                }
                catch (Exception e) {
                }
            }
        }
                                            
    }

    /**
     * 
     */
    @Override
    public void init() {

        logger.finer(Logging.entering(ObjectId, "AmtProxyApplet.init"));

        try {
            this.window = JSObject.getWindow(this);
            this.isJavaScriptEnabled = true;
        }
        catch (JSException e) {
            logger.finer(ObjectId + " unable to get applet host window - " + e.getClass().getName() + ": "+e.getMessage());
        }

        if (this.isJavaScriptEnabled) {
            this.onReadyFuncName = getParameter("onReady");
            logger.finer(ObjectId + " onReady=" + this.onReadyFuncName);
            this.onErrorFuncName = getParameter("onError");
            logger.finer(ObjectId + " onReady=" + this.onReadyFuncName);
            this.onFailoverFuncName = getParameter("onFailover");
            logger.finer(ObjectId + " onFailover=" + this.onFailoverFuncName);
        }
        
        try {
            this.server = Server.create("");
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
    }
    
    @Override
    public void start() {

        logger.finer(Logging.entering(ObjectId, "AmtProxyApplet.start"));

        if (this.server == null) {
            fireOnError("AMT Proxy applet failed to start");
            return;
        }

        try {
            // If we are already the primary, then a page reload occurred and the
            // the server has already been initialized but has been stopped.
            if (!startServer()) {
                this.socket = new Socket();
                this.thread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            while (!Thread.currentThread().isInterrupted()) {
                                waitForServer();
                                if (isStopped) break;
                                if (startServer()) {
                                    fireOnFailover();
                                    break;
                                }
                                else {
                                    fireOnFailover();
                                }
                            }
                        }
                        catch (IOException e) {
                            // We're done
                            e.printStackTrace();
                        }
                    }
                });
                this.thread.setDaemon(true);
                this.thread.start();
            }

            fireOnReady();

        }
        catch (IOException e) {
            // Unexpected IO exception thrown
            String message = "proxy start failed - " + e.getClass().getName() + ":" + e.getMessage();
            logger.severe(ObjectId + " " + message);
            e.printStackTrace();
            fireOnError(message);
        }

    }

    @Override
    public void stop() {
        
        logger.finer(Logging.entering(ObjectId, "AmtProxyApplet.stop"));

        if (this.server == null) {
            return;
        }

        this.isStopped = true;
        
        if (this.socket != null) {
            try {
                this.socket.close();
                this.thread.interrupt();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        this.server.stop();
    }

    @Override
    public void destroy() {
        logger.finer(Logging.entering(ObjectId, "AmtProxyApplet.destroy"));
    }
    
    private boolean startServer() throws IOException {

        logger.finer(Logging.entering(ObjectId, "AmtProxyApplet.startAsPrimary"));

        try {
            this.server.start();
            setLoggerLevels();
            logger.info(ObjectId + " applet started proxy server instance");
            return true;
         }
         catch (BindException e) {
             // The port is already in use - another proxy applet is already running.
             // Create persistent connection to the proxy and start the server when the connection fails
             logger.info(ObjectId + " applet cannot start proxy server instance because one is already running");
             return false;
         }
    }

    private void waitForServer() throws IOException {

        logger.finer(Logging.entering(ObjectId, "AmtProxyApplet.waitForPrimary"));

        socket.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),8054));
        try {

            logger.finer(ObjectId + " waiting for preexisting proxy server to shutdown");

            int c = 0;
            while (c != -1) {
                // Blocks until primary server disconnects or socket is closed
                c = socket.getInputStream().read();
            }
            // OK
        }
        catch (SocketException e) {
            // OK
        }
    }
    
    private void fireOnReady() {

        if (this.isJavaScriptEnabled && this.onReadyFuncName != null) {
            new Thread(new Runnable() {
                public void run() {
                    logger.finer(ObjectId + " calling function "+onReadyFuncName);
                    try {
                        window.call(onReadyFuncName, new String[]{getParameter("id")});
                    }
                    catch(JSException e) {
                        logger.finer(ObjectId + " unable to call function "+onReadyFuncName+" - " + e.getClass().getName() + ": "+e.getMessage());
                    }
                }
            }).start();
        }
    }

    private void fireOnError(final String message) {

        this.showStatus(message);

        if (this.isJavaScriptEnabled && this.onErrorFuncName != null) {
            new Thread(new Runnable() {
                public void run() {
                    logger.finer(ObjectId + " calling function "+onErrorFuncName);
                    try {
                        window.call(onErrorFuncName, new String[]{getParameter("id"), message});
                    }
                    catch(JSException e) {
                        logger.finer(ObjectId + " unable to call function "+onErrorFuncName+" - " + e.getClass().getName() + ": "+e.getMessage());
                    }
                }
            }).start();
        }
    }

    private void fireOnFailover() {

        if (this.isJavaScriptEnabled && this.onFailoverFuncName != null) {
            logger.finer(ObjectId + " calling function "+onFailoverFuncName);
            try {
                window.call(onFailoverFuncName, new String[]{getParameter("id")});
            }
            catch(JSException e) {
                logger.finer(ObjectId + " unable to call function "+onFailoverFuncName+" - " + e.getClass().getName() + ": "+e.getMessage());
            }
        }
    }
    
    private static void setLoggerLevels() {
        
        Handler[] handlers = Logger.getLogger("").getHandlers();
        for ( int index = 0; index < handlers.length; index++ ) {
            System.out.println("handler "+handlers[index].getClass().getName());
            handlers[index].setLevel(Level.FINER);
            handlers[index].setFormatter(new LogFormatter());
        }
        
        logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.streaming.rtsp.Server.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.streaming.rtsp.ConnectionHandler.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.streaming.rtsp.ServerTunnelConnection.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.streaming.rtsp.Session.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.amt.AmtInterface.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.amt.ChannelMembershipManager.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.amt.InterfaceMembershipManager.logger.setLevel(Level.FINER);
    }

    
}
