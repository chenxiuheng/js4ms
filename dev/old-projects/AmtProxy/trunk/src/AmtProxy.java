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


public class AmtProxy implements SingleInstanceListener {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(Server.class.getName());
    
    private final String ObjectId = Logging.identify(this);
    
    private Server server;

    AmtProxy() {
        
    }

    void start(final String[] args) throws IOException {

        logger.info("AMT proxy started");

        try {
            this.server = Server.create(args.length > 0 ? args[0] : "");
        }
        catch (IOException e) {
            logger.severe("AMT proxy initialization failed - " + e.getClass().getName() + ":" + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        });

        try {
            this.server.start();
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new Error(e);
        }

    }

    void shutdown() {

        logger.info("stopping AMT proxy");

        this.server.stop();

        logger.info("AMT proxy stopped");
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
            System.exit(-1);
        }        

        String className = AmtProxy.class.getSimpleName() + ".class";
        String classPath = AmtProxy.class.getResource(className).toString();
        String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
        Manifest manifest;
        try {
            manifest = new Manifest(new URL(manifestPath).openStream());
            Attributes attr = manifest.getMainAttributes();
            System.out.println("Built-By: "+attr.getValue("Built-By"));
            System.out.println("Vendor: "+attr.getValue("Implementation-Vendor"));
            System.out.println("Title: "+attr.getValue("Implementation-Title"));
            System.out.println("Version: "+attr.getValue("Implementation-Version"));
        }
        catch (MalformedURLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        setLoggerLevels();
        
        AmtProxy proxy = new AmtProxy();

        if (isWebStart) {
            singleInstanceService.addSingleInstanceListener((SingleInstanceListener)proxy);
        }
        
        try {
            proxy.start(args);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
            return;
        }

        if (isWebStart) {
            singleInstanceService.removeSingleInstanceListener((SingleInstanceListener)proxy);
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
        com.larkwoodlabs.net.streaming.rtsp.Server.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.streaming.rtsp.ConnectionHandler.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.streaming.rtsp.ServerTunnelConnection.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.streaming.rtsp.Session.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.amt.AmtInterface.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.amt.ChannelMembershipManager.logger.setLevel(Level.FINER);
        com.larkwoodlabs.net.amt.InterfaceMembershipManager.logger.setLevel(Level.FINER);
    }
}
