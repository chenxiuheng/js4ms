package com.larkwoodlabs.applet;

import java.applet.Applet;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.service.ServiceLauncher;
import com.larkwoodlabs.util.logging.LogFormatter;

public class ServiceLauncherApplet extends Applet {

    private static final long serialVersionUID = 1L;

    static final String logPrefix = ServiceLauncherApplet.class.getSimpleName() + ": ";

    static final String JAVA_APPLICATION_LAUNCHER_PARAM = "JavaApplicationLauncher";
    static final String SERVICE_CLASS_PATH_PARAM = "ServiceClassPath";
    static final String SERVICE_CLASS_NAME_PARAM = "ServiceClassName";
    static final String SERVICE_PORT_PARAM = "ServicePort";
    static final String USE_KEEP_ALIVE_PARAM = "UseKeepAlive";
    static final String CONNECTION_RETRY_COUNT_PARAM = "ConnectionRetryCount";
    static final String CONNECTION_RETRY_INTERVAL_PARAM = "ConnectionRetryInterval";
    static final String SERVICE_PROPERTIES_PARAM = "ServiceProperties";

    static final String SERVICE_PORT_PROPERTY = "com.larkwoodlabs.service.socket.port";
    static final String SERVICE_KEEP_ALIVE_ENABLED_PROPERTY = "com.larkwoodlabs.service.keepalive.enabled";

    static final String DEFAULT_JAVA_APPLICATION_LAUNCHER = "java";
    static final int DEFAULT_SERVICE_PORT = 9999;
    static final int DEFAULT_CONNECTION_RETRY_COUNT = 10;
    static final int DEFAULT_CONNECTION_RETRY_INTERVAL = 1000;

    static final String PARAMETER_INFO[][] = { 
        {JAVA_APPLICATION_LAUNCHER_PARAM, "String", "The name of the program used to launch java applications (default is 'java')"},
        {SERVICE_CLASS_PATH_PARAM, "String", "The class path used to locate the service class (optional)"},
        {SERVICE_CLASS_NAME_PARAM, "String", "The name of the service class (required)"},
        {SERVICE_PORT_PARAM, "Integer", "The service port number. Passed to the service as property '"+SERVICE_PORT_PROPERTY+"'"},
        {USE_KEEP_ALIVE_PARAM, "Boolean", "Indicates whether the applet should open a keep-alive connection."},
        {CONNECTION_RETRY_COUNT_PARAM, "Integer", "The number of attempts the applet should make to open a keep-alive connection to a service instance."},
        {CONNECTION_RETRY_INTERVAL_PARAM, "Integer", "The time delay to use between connection attempts (in milliseconds)."},
        {SERVICE_PROPERTIES_PARAM,"comma-delimited list of name=value pairs", "Property values that will be passed to the service"}
    };

    String version;

    ServiceLauncher launcher;

    /**
     * 
     */
    public ServiceLauncherApplet() {
        logVersionInfo();
        System.out.println(getAppletInfo());

        Handler[] handlers = Logger.getLogger("").getHandlers();
        for ( int index = 0; index < handlers.length; index++ ) {
            // System.out.println("handler "+handlers[index].getClass().getName());
            handlers[index].setLevel(Level.FINER);
            handlers[index].setFormatter(new LogFormatter());
        }
        
        ServiceLauncher.logger.setLevel(Level.FINER);
    }

    /**
     * 
     */
    @Override
    public String getAppletInfo() {
        return this.getClass().getSimpleName() + " Version "+(this.version != null ? this.version + "\n" : "<unknown>\n");
    }

    /**
     * 
     */
    @Override
    public String[][] getParameterInfo() {
        return PARAMETER_INFO;
    }
    
    /**
     * 
     */
    @Override
    public void init() {

        System.out.println(logPrefix + "init");

        try {
            this.launcher = new ServiceLauncher(getJavaApplicationLauncher(),
                                                getServiceClassPath(),
                                                getServiceClassName(),
                                                getServicePort(),
                                                getUseKeepAlive(),
                                                getConnectionRetryCount(),
                                                getConnectionRetryInterval(),
                                                getServiceProperties());
        }
        catch (IllegalArgumentException e) {
            // TODO
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            // TODO
            e.printStackTrace();
        }

    }

    /**
     * 
     */
    @Override
    public void start() {

        System.out.println(logPrefix + "start");

        try {
            this.launcher.start();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 
     */
    @Override
    public void stop() {

        System.out.println(logPrefix + "stop");

        this.launcher.stop();
    }

    /**
     * 
     */
    @Override
    public void destroy() {

        System.out.println(logPrefix + "destroy");

    }
 
    /**
     * 
     */
    void logVersionInfo() {
        
        String className = this.getClass().getSimpleName() + ".class";
        String classPath = this.getClass().getResource(className).toString();
        if (classPath.indexOf("!") != -1) {
            String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
            Manifest manifest;
            try {
                manifest = new Manifest(new URL(manifestPath).openStream());
                Attributes attr = manifest.getMainAttributes();
                System.out.println("Applet:   "+this.getClass().getName());
                System.out.println("Built-By: "+attr.getValue("Built-By"));
                System.out.println("Vendor:   "+attr.getValue("Implementation-Vendor"));
                System.out.println("Title:    "+attr.getValue("Implementation-Title"));
                this.version = attr.getValue("Implementation-Version");
                System.out.println("Version:  "+this.version);
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
    }

    /**
     * 
     * @return
     */
    String getJavaApplicationLauncher() {
        String javaVmName = DEFAULT_JAVA_APPLICATION_LAUNCHER;
    
        String propertyValue = getParameter(JAVA_APPLICATION_LAUNCHER_PARAM);
        if (propertyValue != null) {
            javaVmName = propertyValue;
        }

        return javaVmName;
    }

    /**
     * 
     * @return
     * @throws IllegalArgumentException
     * @throws ClassNotFoundException 
     */
    String getServiceClassPath() throws IllegalArgumentException, ClassNotFoundException {

        String propertyValue = getParameter(SERVICE_CLASS_PATH_PARAM);
        if (propertyValue != null) {
            return propertyValue;
        }
        else {
            // Attempt to determine class location using class loader
            String serviceClassName = getServiceClassName();

            System.out.println(logPrefix + "attempting to load "+serviceClassName);

            Class<?> serviceClass;

            try {
                serviceClass = Class.forName(serviceClassName);
            }
            catch (ClassNotFoundException e) {
                System.out.println(logPrefix + "cannot load class "+serviceClassName);
                throw e;
            }

            System.out.println(logPrefix + serviceClassName + " successfully loaded");

            System.out.println(logPrefix + " attempting to locate .jar file containing " + serviceClassName);

            String jarFilePath;

            try {
                jarFilePath = new File(serviceClass.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            }
            catch (URISyntaxException e) {
                System.out.println(logPrefix + " cannot locate jar file for "+ serviceClassName + " - " + e.getMessage());
                throw new ClassNotFoundException("cannot locate jar file for "+ serviceClassName);
            }

            System.out.println(logPrefix + serviceClassName + " found in " + jarFilePath);

            return jarFilePath;
        }

    }

    /**
     * 
     * @return
     * @throws IllegalArgumentException
     */
    String getServiceClassName() throws IllegalArgumentException {

        String propertyValue = getParameter(SERVICE_CLASS_NAME_PARAM);
        if (propertyValue != null) {
            return propertyValue;
        }
        else {
            String message = "required applet parameter '"+SERVICE_CLASS_NAME_PARAM+"' is missing";
            System.out.println(logPrefix + " " + message);
            throw new IllegalArgumentException();
        }

    }

    /**
     * 
     * @return
     * @throws IllegalArgumentException
     */
    int getServicePort() throws IllegalArgumentException {

        int servicePort = DEFAULT_SERVICE_PORT;

        String propertyValue = getParameter(SERVICE_PORT_PARAM);
        if (propertyValue != null) {
            try {
                servicePort = Short.parseShort(propertyValue);
            }
            catch(NumberFormatException e) {
                String message = "value of applet parameter '"+SERVICE_PORT_PARAM+"' is not an integer port number";
                System.out.println(logPrefix + " " + message );
                throw new IllegalArgumentException(message, e);
            }
        }
        return servicePort;
    }

    /**
     * 
     * @return
     */
    boolean getUseKeepAlive() {
        return Boolean.parseBoolean(getParameter(USE_KEEP_ALIVE_PARAM));
    }

    /**
     * 
     * @return
     * @throws IllegalArgumentException
     */
    int getConnectionRetryCount() throws IllegalArgumentException {

        int connectionRetryCount = DEFAULT_CONNECTION_RETRY_COUNT;

        String propertyValue = getParameter(CONNECTION_RETRY_COUNT_PARAM);
        if (propertyValue != null) {
            try {
                connectionRetryCount = Short.parseShort(propertyValue);
                if (connectionRetryCount < 0) connectionRetryCount = 0;
            }
            catch(NumberFormatException e) {
                String message = "value of applet parameter '"+CONNECTION_RETRY_COUNT_PARAM+"' is not an integer";
                System.out.println(logPrefix + " " + message);
                throw new IllegalArgumentException(message, e);
            }
        }

        return connectionRetryCount;
    }

    /**
     * 
     * @return
     * @throws IllegalArgumentException
     */
    int getConnectionRetryInterval() throws IllegalArgumentException {

        int connectionRetryInterval = DEFAULT_CONNECTION_RETRY_INTERVAL;

        String propertyValue = getParameter(CONNECTION_RETRY_INTERVAL_PARAM);
        if (propertyValue != null) {
            try {
                connectionRetryInterval = Short.parseShort(propertyValue);
                if (connectionRetryInterval < 0) connectionRetryInterval = 0;
            }
            catch(NumberFormatException e) {
                String message = "value of applet parameter '"+CONNECTION_RETRY_INTERVAL_PARAM+"' is not an integer";
                System.out.println(logPrefix + " " + message);
                throw new IllegalArgumentException(message, e);
            }
        }

        return connectionRetryInterval;
    }

    /**
     * 
     * @return
     */
    Properties getServiceProperties() {

        Properties properties = new Properties();

        properties.put(SERVICE_PORT_PROPERTY, String.valueOf(getServicePort()));

        properties.put(SERVICE_KEEP_ALIVE_ENABLED_PROPERTY, String.valueOf(getUseKeepAlive()));

        String propertiesParam = getParameter(SERVICE_PROPERTIES_PARAM);
        if (propertiesParam != null) {
            String[] propertyAssignments = propertiesParam.split(",");
            for (String propertyAssignment : propertyAssignments) {
                String[] pair = propertyAssignment.split("=");
                if (pair.length == 1) {
                    properties.setProperty(pair[0],"");
                }
                else if (pair.length == 2) {
                    properties.setProperty(pair[0],pair[1]);
                }
            }
        }

        return properties;
    }

}
