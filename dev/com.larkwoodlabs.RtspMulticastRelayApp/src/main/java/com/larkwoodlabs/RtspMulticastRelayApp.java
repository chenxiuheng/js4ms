package com.larkwoodlabs;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jnlp.ServiceManager;
import javax.jnlp.SingleInstanceListener;
import javax.jnlp.SingleInstanceService;
import javax.jnlp.UnavailableServiceException;

import com.larkwoodlabs.net.streaming.rtsp.Server;
import com.larkwoodlabs.util.logging.LogFormatter;
import com.larkwoodlabs.util.logging.Logging;


public class RtspMulticastRelayApp implements SingleInstanceListener {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(RtspMulticastRelayApp.class.getName());
    
    private final String ObjectId = Logging.identify(this);
    
    private Server server;

    RtspMulticastRelayApp() {
        
    }

    void run(final String[] args) throws IOException, InterruptedException {

        logger.info(ObjectId + " starting relay...");

        try {
            this.server = Server.create(args.length > 0 ? args[0] : "");
        }
        catch (IOException e) {
            logger.severe(ObjectId + " relay initialization failed - " + e.getClass().getName() + ":" + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        Thread shutdownHook = new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        };
        
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        try {
            this.server.start();
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new Error(e);
        }

        logger.info(ObjectId + " relay started");

        this.server.waitForShutdown();

        Runtime.getRuntime().removeShutdownHook(shutdownHook);

        logger.info(ObjectId + " relay stopped");
    }

    void shutdown() {

        logger.info(ObjectId + " stopping relay...");

        this.server.stop();

    }

    /**
     * 
     * @param args
     */
    public static void main(final String[] args) {

        boolean isWebStart = false;

        SingleInstanceService singleInstanceService = null;
        try {
            singleInstanceService = (SingleInstanceService)ServiceManager.lookup("javax.jnlp.SingleInstanceService");
            isWebStart = true;
        }
        catch(UnavailableServiceException e) {
            isWebStart = false;
            //System.exit(-1);
        }        

        setLoggerLevels();

        String className = RtspMulticastRelayApp.class.getSimpleName() + ".class";
        String classPath = RtspMulticastRelayApp.class.getResource(className).toString();
        if (classPath.indexOf("!") != -1) {
            String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
            Manifest manifest;
            try {
                manifest = new Manifest(new URL(manifestPath).openStream());
                Attributes attr = manifest.getMainAttributes();
                logger.info("[  static  ] Built-By: "+attr.getValue("Built-By"));
                logger.info("[  static  ] Vendor: "+attr.getValue("Implementation-Vendor"));
                logger.info("[  static  ] Title: "+attr.getValue("Implementation-Title"));
                logger.info("[  static  ] Version: "+attr.getValue("Implementation-Version"));
            }
            catch (MalformedURLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        
        RtspMulticastRelayApp relay = new RtspMulticastRelayApp();

        try {
            if (isWebStart) {
                logger.info("[ static ] registering as singleton application");
                singleInstanceService.addSingleInstanceListener((SingleInstanceListener)relay);
            }
        
            relay.run(args);
        }
        catch (InterruptedException e) {
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (isWebStart) {
                logger.info("[static ] deregistering as singleton application");
                singleInstanceService.removeSingleInstanceListener((SingleInstanceListener)relay);
            }
        }
    }

    @Override
    public void newActivation(final String[] args) {
        logger.info(ObjectId + " ignoring attempt to start new proxy instance");
    }

    private static void setLoggerLevels() {
        
        Handler[] handlers = Logger.getLogger("").getHandlers();
        for ( int index = 0; index < handlers.length; index++ ) {
            handlers[index].setLevel(Level.FINER);
            handlers[index].setFormatter(new LogFormatter());
        }
        
        logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.streaming.rtsp.Server.logger.setLevel(Level.FINE);
        com.larkwoodlabs.net.streaming.rtsp.ConnectionHandler.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.streaming.rtsp.Connection.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.streaming.rtsp.Session.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.amt.AmtInterface.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.amt.ChannelMembershipManager.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.amt.InterfaceMembershipManager.logger.setLevel(Level.FINER);
    }
}
