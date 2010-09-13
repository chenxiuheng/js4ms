package com.larkwoodlabs.applet;

import java.applet.Applet;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ServiceLauncherApplet extends Applet {

    private static final long serialVersionUID = 1L;

    static final String logPrefix = ServiceLauncherApplet.class.getSimpleName() + ": ";
    
    static final String SERVICE_CLASS_NAME_PARAM = "ServiceClassName";
    static final String SERVICE_PORT_PARAM = "ServicePort";
    static final String USE_KEEP_ALIVE_PARAM = "UseKeepAlive";
    static final String KEEP_ALIVE_PORT_PARAM = "KeepAlivePort";
    static final String CONNECTION_RETRY_COUNT_PARAM = "ConnectionRetryCount";
    static final String CONNECTION_RETRY_INTERVAL_PARAM = "ConnectionRetryInterval";
    static final String SERVICE_PROPERTIES_PARAM = "ServiceProperties";

    static final String SERVICE_PORT_PROPERTY = "com.larkwoodlabs.service.socket.port";
    static final String SERVICE_KEEP_ALIVE_ENABLED_PROPERTY = "com.larkwoodlabs.service.keepalive.enabled";
    static final String SERVICE_KEEP_ALIVE_PORT_PROPERTY = "com.larkwoodlabs.service.keepalive.port";

    static final int DEFAULT_CONNECTION_RETRY_COUNT = 10;
    static final int DEFAULT_CONNECTION_RETRY_INTERVAL = 1000;

    static final String PARAMETER_INFO[][] = { 
        {SERVICE_CLASS_NAME_PARAM, "String", "The name of the service class (required)"},
        {SERVICE_PORT_PARAM, "Integer", "The service port number. Passed to the service as property '"+SERVICE_PORT_PROPERTY+"'"},
        {USE_KEEP_ALIVE_PARAM, "Boolean", "Indicates whether the applet should open a keep-alive connection."},
        {KEEP_ALIVE_PORT_PARAM, "Integer", "The port number to use when opening a 'keep-alive' connection. "+
                                           "Passed to the service as the property value '"+SERVICE_KEEP_ALIVE_PORT_PROPERTY+"'. " +
                                           "Default is value of '"+SERVICE_PORT_PROPERTY+"'."},
        {CONNECTION_RETRY_COUNT_PARAM, "Integer", "The number of attempts the applet should take to open a keep-alive connection to a service instance."},
        {CONNECTION_RETRY_INTERVAL_PARAM, "Integer", "The time delay to use between connection attempts (in milliseconds)."},
        {SERVICE_PROPERTIES_PARAM,"comma-delimited list of name=value pairs", "Property values that will be passed to the service"}
    };

    String version;

    boolean useKeepAlive = false;
    int servicePort;
    int keepAlivePort;
    
    Socket socket;
    boolean isConnected = false;
    
    public ServiceLauncherApplet() {
        logVersionInfo();
        System.out.println(getAppletInfo());
    }

    @Override
    public String getAppletInfo() {
        return this.getClass().getSimpleName() + " Version "+(this.version != null ? this.version + "\n" : "<unknown>\n");
    }
    
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
        
        applyProperties();

    }
    
    @Override
    public void start() {

        System.out.println(logPrefix + "start");

        try {
            if (!isServiceStarted()) {
                launchProcess();
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        
        System.out.println(logPrefix + "stop");

        if (this.useKeepAlive) {
            disconnect();
        }
    }

    @Override
    public void destroy() {

        System.out.println(logPrefix + "destroy");

    }
 
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

    void applyProperties() {

        this.servicePort = -1;

        String servicePortParam = getParameter(SERVICE_PORT_PARAM);
        if (servicePortParam != null) {
            try {
                this.servicePort = Short.parseShort(servicePortParam);
            }
            catch(NumberFormatException e) {
                System.out.println(logPrefix + "value of applet parameter '"+SERVICE_PORT_PARAM+"' is not a valid port number!");
            }
        }

        this.keepAlivePort = -1;

        String keepAlivePortParam = getParameter(KEEP_ALIVE_PORT_PARAM);
        if (keepAlivePortParam != null) {
            try {
                this.keepAlivePort = Short.parseShort(keepAlivePortParam);
            }
            catch(NumberFormatException e) {
                System.out.println(logPrefix + "value of applet parameter '"+KEEP_ALIVE_PORT_PARAM+"' is not a valid port number!");
            }
        }

        if (this.keepAlivePort == -1 && this.servicePort != -1) {
            this.keepAlivePort = this.servicePort;
        }

        String useKeepAliveParam = getParameter(USE_KEEP_ALIVE_PARAM);
        this.useKeepAlive = Boolean.parseBoolean(useKeepAliveParam) && this.keepAlivePort != -1;

    }

    public void launchProcess() throws InterruptedException {
       
        String className = getParameter(SERVICE_CLASS_NAME_PARAM);
        
        if (className == null) {
            System.out.println(logPrefix + "applet parameter " + SERVICE_CLASS_NAME_PARAM + " not defined!");
            return;
        }
        
        System.out.println(logPrefix + "attempting to load "+className);

        Class<?> processClass;

        try {
            processClass = Class.forName(className);
        }
        catch (ClassNotFoundException e1) {
            System.out.println(logPrefix + "cannot load class "+className);
            return;
        }
        
        System.out.println(logPrefix + className + " successfully loaded");

        System.out.println(logPrefix+"attempting to locate .jar file containing "+className);

        String jarFilePath;
        
        try {
            jarFilePath = new File(processClass.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        }
        catch (URISyntaxException e) {
            System.out.println(logPrefix+"cannot locate jar file for "+className + " - " + e.getMessage());
            return;
        }

        System.out.println(logPrefix+className+" found in "+jarFilePath);

        ArrayList<String> parameters = new ArrayList<String>();
        parameters.add(System.getProperty("java.home")+File.separator+"bin"+File.separator+"java");
        
        if (this.servicePort != -1) {
            parameters.add("-D"+SERVICE_PORT_PROPERTY+"="+this.servicePort);
        }
        
        if (this.useKeepAlive) {
            parameters.add("-D"+SERVICE_KEEP_ALIVE_ENABLED_PROPERTY+"=true");
            
        }

        if (this.keepAlivePort != -1) {
            parameters.add("-D"+SERVICE_KEEP_ALIVE_PORT_PROPERTY+"="+this.keepAlivePort);
        }

        
        String propertiesParam = getParameter(SERVICE_PROPERTIES_PARAM);
        if (propertiesParam != null) {
            String[] propertyAssignments = propertiesParam.split(",");
            for (String propertyAssignment : propertyAssignments) {
                String[] pair = propertyAssignment.split("=");
                if (pair.length == 1) {
                    parameters.add("-D"+pair[0]);
                }
                else if (pair.length == 2) {
                    parameters.add("-D"+pair[0]+"="+pair[1]);
                }
            }
        }
        
        parameters.add("-jar");
        parameters.add(jarFilePath);

        try {
            String[] commandLine = parameters.toArray(new String[parameters.size()]);

            System.out.println(logPrefix+"attempting to launch service using:");
            String message = "";
            for (String s : commandLine) {
                message += s+" ";
            }
            System.out.println(logPrefix+message);
            
            ProcessBuilder builder = new ProcessBuilder(commandLine);
            builder.start();
            System.out.println(logPrefix+"service launched");
        }
        catch (IOException e) {
            System.out.println(logPrefix+"launch failed with exception:");
            e.printStackTrace();
            return;
        }

        if (this.useKeepAlive) {

            int retryCount = DEFAULT_CONNECTION_RETRY_COUNT;

            String propertyValue = getParameter(CONNECTION_RETRY_COUNT_PARAM);
            if (propertyValue != null) {
                try {
                    retryCount = Short.parseShort(propertyValue);
                    if (retryCount < 0) retryCount = 0;
                }
                catch(NumberFormatException e) {
                    System.out.println(logPrefix + "value of applet parameter '"+CONNECTION_RETRY_COUNT_PARAM+"' is not a valid integer!");
                }
            }

            int retryInterval = DEFAULT_CONNECTION_RETRY_INTERVAL;

            propertyValue = getParameter(CONNECTION_RETRY_INTERVAL_PARAM);
            if (propertyValue != null) {
                try {
                    retryInterval = Short.parseShort(propertyValue);
                    if (retryInterval < 0) retryInterval = 0;
                }
                catch(NumberFormatException e) {
                    System.out.println(logPrefix + "value of applet parameter '"+CONNECTION_RETRY_INTERVAL_PARAM+"' is not a valid integer!");
                }
            }

            connect(retryCount, retryInterval);
        }

    }
    
    boolean isServiceStarted() throws InterruptedException {

        System.out.println(logPrefix+"check for running service instance");

        if (this.keepAlivePort != -1) {
            if (connect(1, 0)) {
                if (!this.useKeepAlive) {
                    disconnect();
                }
                return true;
            }
        }
        return false;
    }

    boolean connect(int retries, int retryInterval) throws InterruptedException {

        if (this.isConnected) return true;

        for (int i=0; i < retries; i++) {
            System.out.println(logPrefix+"attempting to establish connection on port "+this.keepAlivePort);
    
            try {
                this.socket = new Socket();
                InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(),this.keepAlivePort);
                System.out.println(logPrefix + "attempt "+(i+1)+" to connect running service at "+address.getHostName()+":"+address.getPort());
                this.socket.connect(address);
                System.out.println(logPrefix + "connected to running service instance");
                this.isConnected = true;
                this.socket.getInputStream();
                return true;
            }
            catch(ConnectException e) {
                System.out.println(logPrefix+"cannot connect to port "+this.keepAlivePort+" - " + e.getMessage());
                Thread.sleep(retryInterval);
            }
            catch (UnknownHostException e) {
                System.out.println(logPrefix + "cannot connect on port "+this.keepAlivePort+" - " + e.getMessage());
                break;
            }
            catch (IOException e) {
                System.out.println(logPrefix + "cannot connect on port "+this.keepAlivePort+" - " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }
        return false;
    }
    
    void disconnect() {

        if (this.isConnected) {

            System.out.println(logPrefix+"closing service connection...");

            this.isConnected = false;

            try {
                this.socket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
}
