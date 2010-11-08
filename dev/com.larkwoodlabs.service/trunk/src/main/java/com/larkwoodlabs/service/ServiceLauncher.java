package com.larkwoodlabs.service;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Log;

/**
 * 
 * 
 *
 * @author gbumgard@cisco.com
 */
public class ServiceLauncher {

    /*-- Static Constants  ----------------------------------------------------*/

    /**
     * 
     */
    public static final String JAVA_VM_NAME_PARAM = "JavaVmName";
    public static final String SERVICE_CLASS_NAME_PARAM = "ServiceClassName";
    public static final String SERVICE_PORT_PARAM = "ServicePort";
    public static final String USE_KEEP_ALIVE_PARAM = "UseKeepAlive";
    public static final String CONNECTION_RETRY_COUNT_PARAM = "ConnectionRetryCount";
    public static final String CONNECTION_RETRY_INTERVAL_PARAM = "ConnectionRetryInterval";

    static final String SERVICE_PORT_PROPERTY = "com.larkwoodlabs.service.socket.port";
    static final String SERVICE_KEEP_ALIVE_ENABLED_PROPERTY = "com.larkwoodlabs.service.keepalive.enabled";

    static final int DEFAULT_CONNECTION_RETRY_COUNT = 10;
    static final int DEFAULT_CONNECTION_RETRY_INTERVAL = 1000;


    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(Server.class.getName());


    /*-- Member Variables  ----------------------------------------------------*/

    /**
     * 
     */
    public final Log log = new Log(this);

    private Properties serviceProperties;

    private String javaApplicationLauncher;
    private String serviceClassPath;
    private String serviceClassName;
    private int servicePort;
    private boolean useKeepAlive;
    private int retryCount;
    private int retryInterval;

    Socket socket;

    boolean isConnected = false;


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     * @param javaApplicationLauncher
     * @param serviceClassPath
     * @param serviceClassName
     * @param servicePort
     * @param useKeepAlive
     * @param retryCount
     * @param retryInterval
     * @param serviceProperties
     */
    public ServiceLauncher(String javaApplicationLauncher,
                           String serviceClassPath,
                           String serviceClassName,
                           int servicePort,
                           boolean useKeepAlive,
                           int retryCount,
                           int retryInterval,
                           Properties serviceProperties) {

        this.javaApplicationLauncher = javaApplicationLauncher;
        this.serviceProperties = serviceProperties;
        this.serviceClassPath = serviceClassPath;
        this.serviceClassName = serviceClassName;
        this.servicePort = servicePort;
        this.useKeepAlive = useKeepAlive;
        this.retryCount = retryCount;
        this.retryInterval = retryInterval;
    }

    public void start() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("start"));
        }

        if (!isServiceStarted()) {
            launchProcess();
        }
    }

    public void stop() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("stop"));
        }

        if (this.useKeepAlive) {
            disconnect();
        }
    }

    boolean isServiceStarted() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("isServiceStarted"));
        }

        if (this.servicePort != -1) {
            if (connect(1,0)) {
                if (!this.useKeepAlive) {
                    disconnect();
                }
                return true;
            }
        }
        return false;
    }

    void launchProcess() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("launchProcess"));
        }

        ArrayList<String> parameters = new ArrayList<String>();

        parameters.add(System.getProperty("java.home")+File.separator + "bin" + File.separator + this.javaApplicationLauncher);

        parameters.add("-D"+SERVICE_PORT_PROPERTY+"="+this.servicePort);

        if (this.useKeepAlive) {
            parameters.add("-D"+SERVICE_KEEP_ALIVE_ENABLED_PROPERTY+"=true");
        }

        Set<Entry<Object, Object>> entries = this.serviceProperties.entrySet();

        for (Entry<Object,Object> entry : entries) {
            parameters.add("-D"+(String)entry.getKey()+"="+(String)entry.getValue());
        }

        parameters.add("-classpath");
        parameters.add(this.serviceClassPath);

        parameters.add(this.serviceClassName);

        try {

            String[] commandLine = parameters.toArray(new String[parameters.size()]);

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(log.msg("attempting to launch service using:"));
                String message = "";
                for (String s : commandLine) {
                    message += s+" ";
                }
                logger.fine(log.msg(message));
            }

            ProcessBuilder builder = new ProcessBuilder(commandLine);
            builder.start();

            logger.fine(log.msg("service launched"));
        }
        catch (IOException e) {
            logger.severe(log.msg("launch failed with exception:"));
            e.printStackTrace();
            return;
        }

        if (this.useKeepAlive) {
            connect(this.retryCount, this.retryInterval);
        }

    }

    boolean connect(int retries, int retryInterval) throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("connect", retries, retryInterval));
        }

        if (this.isConnected) return true;

        for (int i=0; i < retries; i++) {
            logger.fine(log.msg("attempting to establish connection on port "+this.servicePort));

            try {
                this.socket = new Socket();
                InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), this.servicePort);
                logger.fine(log.msg("attempt "+(i+1)+" to connect running service at "+address.getAddress().getHostAddress()+":"+address.getPort()));
                this.socket.connect(address);
                logger.fine(log.msg("connected to running service instance"));
                this.isConnected = true;
                this.socket.getInputStream();
                return true;
            }
            catch(ConnectException e) {
                logger.fine(log.msg("cannot connect to port "+this.servicePort+" - " + e.getMessage()));
                Thread.sleep(retryInterval);
            }
            catch (UnknownHostException e) {
                logger.warning(log.msg("cannot connect on port "+this.servicePort+" - " + e.getMessage()));
                break;
            }
            catch (IOException e) {
                logger.warning(log.msg("cannot connect on port "+this.servicePort+" - " + e.getMessage()));
                e.printStackTrace();
                break;
            }
        }
        return false;
    }

    void disconnect() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("disconnect"));
        }


        if (this.isConnected) {

            logger.fine(log.msg("closing service connection..."));

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
