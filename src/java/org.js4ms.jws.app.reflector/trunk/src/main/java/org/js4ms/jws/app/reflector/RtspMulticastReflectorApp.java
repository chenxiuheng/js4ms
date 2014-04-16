package org.js4ms.jws.app.reflector;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Logger;

import javax.jnlp.ServiceManager;
import javax.jnlp.SingleInstanceListener;
import javax.jnlp.SingleInstanceService;
import javax.jnlp.UnavailableServiceException;

import org.js4ms.util.logging.swing.Console;
import org.js4ms.app.reflector.RtspMulticastReflector;
import org.js4ms.common.util.logging.Log;
import org.js4ms.common.util.logging.Logging;


public class RtspMulticastReflectorApp implements SingleInstanceListener {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(RtspMulticastReflectorApp.class.getName());

    public static final String  JAVAWS_PROPERTY_PREFIX = "javaws.org.js4ms.";

    public static final String  SERVICE_PROPERTY_PREFIX = "org.js4ms.service.";

    public static final String  CONSOLE_ENABLED_PROPERTY = SERVICE_PROPERTY_PREFIX + ".console.enabled";
    public static final String  CONSOLE_CLOSE_PROPERTY = SERVICE_PROPERTY_PREFIX + ".console.close";

    static final Log slog = new Log(RtspMulticastReflectorApp.class);

    /*-- Member Variables ----------------------------------------------------*/

    final Log log = new Log(this);


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     */
    public RtspMulticastReflectorApp() {
        
    }

    /**
     * 
     */
    @Override
    public void newActivation(String[] arg0) {
        // TODO: if service port is new then start another server?
    }

    /**
     * Stores system properties whose names values start with "javaws." as
     * new properties under names created by stripping the leading "javaws" prefix.
     * This allows one to pass recognized properties on the javaws command-line.
     */
    public static void transferJWSProperties() {
        Enumeration<?> iter = System.getProperties().propertyNames();
        int prefixLength = "javaws.".length();
        while (iter.hasMoreElements()) {
            String name = (String)iter.nextElement();
            if (name.startsWith(JAVAWS_PROPERTY_PREFIX)) {
                String propertyName = name.substring(prefixLength);
                System.setProperty(propertyName, System.getProperty(name));
            }
        }
    }

    public static void main(final String[] args) {

        transferJWSProperties();

        if (Boolean.parseBoolean(System.getProperty(CONSOLE_ENABLED_PROPERTY))) {
            new Console("RTSP Multicast Reflector", Boolean.parseBoolean(System.getProperty(CONSOLE_ENABLED_PROPERTY)));
        }

        try {
            Logging.configureLogging();
        }
        catch (IOException e) {
        }

        boolean isWebStart = false;

        SingleInstanceService singleInstanceService = null;

        try {
            singleInstanceService = (SingleInstanceService)ServiceManager.lookup("javax.jnlp.SingleInstanceService");
            isWebStart = true;
        }
        catch(UnavailableServiceException e) {
            isWebStart = false;
        }        

        // Log some information about this application
        String className = RtspMulticastReflectorApp.class.getSimpleName() + ".class";
        String classPath = RtspMulticastReflectorApp.class.getResource(className).toString();
        if (classPath.indexOf("!") != -1) {
            String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
            Manifest manifest;
            try {
                manifest = new Manifest(new URL(manifestPath).openStream());
                Attributes attr = manifest.getMainAttributes();
                logger.info(slog.msg("Built-By: "+attr.getValue("Built-By")));
                logger.info(slog.msg("Vendor: "+attr.getValue("Implementation-Vendor")));
                logger.info(slog.msg("Title: "+attr.getValue("Implementation-Title")));
                logger.info(slog.msg("Version: "+attr.getValue("Implementation-Version")));
            }
            catch (Exception e) {
                logger.info(slog.msg("No manifest found"));
            }
        }

        RtspMulticastReflectorApp app = new RtspMulticastReflectorApp();

        try {

            if (isWebStart) {
                logger.info(slog.msg("registering as singleton application"));
                singleInstanceService.addSingleInstanceListener((SingleInstanceListener)app);
            }

            // Start the RTSP reflector
            RtspMulticastReflector.main(args);

        }
        finally {
            if (isWebStart) {
                logger.info(slog.msg("deregistering as singleton application"));
                singleInstanceService.removeSingleInstanceListener((SingleInstanceListener)app);
            }
        }

        // Force exit to close console.
        System.exit(0);
    }

}
