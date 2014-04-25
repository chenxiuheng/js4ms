package org.js4ms.launcher.jws;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ServiceLauncher.java [org.js4ms.jsdk:launcher]
 * %%
 * Copyright (C) 2009 - 2014 Cisco Systems, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;



/**
 * Launches a Java Web Start application in a new process. 
 * 
 *
 * @author gbumgard@cisco.com
 */
public class ServiceLauncher {

    public interface Listener {
        void onExit(int exitCode);
        void onConnect();
        void onDisconnect();
    }

    /*-- Static Constants  ----------------------------------------------------*/

    /**
     * 
     */
    public static final String DEFAULT_SERVICE_PORT_PROPERTY = "org.js4ms.service.socket.port";
    public static final String DEFAULT_SERVICE_KEEP_ALIVE_ENABLED_PROPERTY = "org.js4ms.service.keepalive.enabled";

    public static final int DEFAULT_CONNECTION_RETRY_COUNT = 10;
    public static final int DEFAULT_CONNECTION_RETRY_INTERVAL = 1000;


    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(ServiceLauncher.class.getName());


    /*-- Member Variables  ----------------------------------------------------*/

    /**
     * 
     */
    private final Log log = new Log(this);

    private Properties serviceProperties;

    private URI serviceJnlpUri;
    private int servicePort;
    private boolean useKeepAlive;
    private int retryCount;
    private int retryInterval;

    private final Listener listener;

    private Socket socket;

    private boolean isConnected = false;

    private Process process;

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
    public ServiceLauncher(final URI serviceJnlpUri,
                           final int servicePort,
                           final boolean useKeepAlive,
                           final int retryCount,
                           final int retryInterval,
                           final Listener listener,
                           final Properties serviceProperties) {

        this.serviceProperties = serviceProperties;
        this.serviceJnlpUri = serviceJnlpUri;
        this.servicePort = servicePort;
        this.useKeepAlive = useKeepAlive;
        this.retryCount = retryCount;
        this.retryInterval = retryInterval;
        this.listener = listener;
    }

    public boolean start() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("start"));
        }

        if (!isServiceRunning()) {
            return launchProcess();
        }
        return true;
    }

    public void stop() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("stop"));
        }

        if (this.useKeepAlive) {
            disconnect();
        }
    }

    public boolean isServiceRunning() throws InterruptedException {
        if (this.process != null) {
            try {
                this.process.exitValue();
                return false;
            }
            catch (IllegalThreadStateException e) {
                return isServiceListening();
            }
        }
        return isServiceListening();
    }

    public boolean isServiceListening() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("isServiceListening"));
        }

        if (connect(1,0)) {
            if (!this.useKeepAlive) {
                disconnect();
            }
            return true;
        }
        return false;
    }

    private boolean launchProcess() throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("launchProcess"));
        }

        ArrayList<String> parameters = new ArrayList<String>();

        parameters.add(System.getProperty("java.home")+File.separator + "bin" + File.separator + "javaws");

        //parameters.add("-J-Djavaws."+DEFAULT_SERVICE_PORT_PROPERTY+"="+this.servicePort);

        parameters.add("-wait");

        if (this.useKeepAlive) {
            // parameters.add("-J-Djavaws."+DEFAULT_SERVICE_KEEP_ALIVE_ENABLED_PROPERTY+"=true");
        }

        Set<Entry<Object, Object>> entries = this.serviceProperties.entrySet();

        for (Entry<Object,Object> entry : entries) {
            // parameters.add("-J-Djavaws."+(String)entry.getKey()+"="+(String)entry.getValue());
            parameters.add("-J-D"+(String)entry.getKey()+"="+(String)entry.getValue());
        }

        parameters.add(this.serviceJnlpUri.toString());

        try {

            String[] commandLineParams = parameters.toArray(new String[parameters.size()]);

            String commandLine = "";
            for (String s : commandLineParams) {
                commandLine += s+" ";
            }

            if (commandLine.contains("Xaltjvm")) {
                logger.warning(log.msg("launch aborted because web start command line contains possible javaws exploit"));
                throw new IllegalArgumentException("command line contains possible JWS exploit");
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(log.msg("attempting to launch web start service using:"));
                logger.fine(log.msg(commandLine));
            }


            ProcessBuilder builder = new ProcessBuilder(commandLineParams);
            this.process = builder.start();
            if (this.listener != null) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            int result = ServiceLauncher.this.process.waitFor();
                            // If the launcher killed javaws, the process member will be set to null
                            if (ServiceLauncher.this.process != null) {
                                ServiceLauncher.this.listener.onExit(result);
                            }
                        }
                        catch (InterruptedException e) {
                            // The waitFor() call was interrupted - interrupt again
                            Thread.currentThread().interrupt();
                        }
                    }
                };
                thread.setDaemon(true);
                thread.start();
            }

            logger.fine(log.msg("service launched"));
        }
        catch (IOException e) {
            logger.severe(log.msg("launch failed with exception: "+e.getMessage()));
            return false;
        }

        if (this.useKeepAlive) {
            return connect(this.retryCount, this.retryInterval);
        }

        return true;
    }

    private boolean connect(final int retries, final int retryInterval) throws InterruptedException {

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
                if (this.listener != null) {
                    this.listener.onConnect();
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            try {
                                ServiceLauncher.this.socket.getInputStream().read();
                            }
                            catch (IOException e) {
                            }
                            if (ServiceLauncher.this.isConnected) {
                                ServiceLauncher.this.isConnected = false;
                                ServiceLauncher.this.listener.onDisconnect();
                            }
                        }
                    };
                    thread.setDaemon(true);
                    thread.start();
                }
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

    private void disconnect() {

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
