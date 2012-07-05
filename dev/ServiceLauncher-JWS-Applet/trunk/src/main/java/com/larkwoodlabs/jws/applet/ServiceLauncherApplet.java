/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: ServiceLauncherApplet.java (com.larkwoodlabs.jws.applet)
 * 
 * Copyright © 2011-2012 Cisco Systems, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.larkwoodlabs.jws.applet;

import java.applet.Applet;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Logger;

import com.larkwoodlabs.service.launcher.jws.ServiceLauncher;
import com.larkwoodlabs.util.logging.Log;
import com.larkwoodlabs.util.logging.Logging;

/**
 * @author Greg Bumgardner (gbumgard)
 */
public class ServiceLauncherApplet extends Applet {

    private static final long serialVersionUID = 1L;

    private static final String SERVICE_JNLP_URL_PARAM = "ServiceJnlpUrl";
    private static final String SERVICE_PORT_PARAM = "ServicePort";
    private static final String USE_KEEP_ALIVE_PARAM = "UseKeepAlive";
    private static final String CONNECTION_RETRY_COUNT_PARAM = "ConnectionRetryCount";
    private static final String CONNECTION_RETRY_INTERVAL_PARAM = "ConnectionRetryInterval";
    private static final String SERVICE_PROPERTIES_PARAM = "ServiceProperties";
    private static final String LOGGING_PROPERTIES_URI_PARAM = "LoggingPropertiesUri";
    private static final String ON_ERROR_URL_PARAM = "OnErrorUrl";
    private static final String ON_CONNECT_URL_PARAM = "OnConnectUrl";
    private static final String ON_DISCONNECT_URL_PARAM = "OnDisconnectUrl";

    private static final String SERVICE_PORT_PROPERTY = "com.larkwoodlabs.service.socket.port";

    private static final int DEFAULT_SERVICE_PORT = 9999;
    private static final int DEFAULT_CONNECTION_RETRY_COUNT = 10;
    private static final int DEFAULT_CONNECTION_RETRY_INTERVAL = 1000;

    private static final String PARAMETER_INFO[][] = {
            { SERVICE_JNLP_URL_PARAM, "String", "The service .jnlp file URL (required)" },
            { SERVICE_PORT_PARAM, "Integer",
                    "The service port number. Passed to the service as property '" + SERVICE_PORT_PROPERTY + "'" },
            { USE_KEEP_ALIVE_PARAM, "Boolean", "Indicates whether the applet should open a keep-alive connection." },
            { CONNECTION_RETRY_COUNT_PARAM, "Integer",
                    "The number of attempts the applet should make to open a keep-alive connection to a service instance." },
            { CONNECTION_RETRY_INTERVAL_PARAM, "Integer", "The time delay to use between connection attempts (in milliseconds)." },
            { SERVICE_PROPERTIES_PARAM, "comma-delimited list of name=value pairs",
                    "Property values that will be passed to the service." },
            { ON_ERROR_URL_PARAM, "URL", "The URL that the applet will use to set the document location to should an error occur." },
            { ON_DISCONNECT_URL_PARAM, "URL",
                    "The URL that the applet will use to set the document location when the service connect is broken." } };

    public static final Logger logger = Logger.getLogger(ServiceLauncherApplet.class.getName());

    private final Log log = new Log(this);

    private String version;

    private ServiceLauncher launcher = null;

    /**
     * 
     */
    public ServiceLauncherApplet() {
    }

    @Override
    public String getAppletInfo() {
        return this.getClass().getSimpleName() + " Version " + (this.version != null ? this.version + "\n" : "<unknown>\n");
    }

    @Override
    public String[][] getParameterInfo() {
        return PARAMETER_INFO;
    }

    @Override
    public void init() {

        String loggingPropertiesUriParam = getParameter(LOGGING_PROPERTIES_URI_PARAM);

        if (loggingPropertiesUriParam != null) {
            try {
                Logging.configureLogging(new URI(loggingPropertiesUriParam));
            }
            catch (URISyntaxException e) {
            }
            catch (IOException e) {
            }
        }

        logVersionInfo();

        logger.fine(getAppletInfo());

        logger.finer(log.entry("init"));

        ServiceLauncher.Listener listener = null;

        String onConnectUrl = getParameter(ON_CONNECT_URL_PARAM);
        String onDisconnectUrl = getParameter(ON_DISCONNECT_URL_PARAM);
        if (onConnectUrl != null || onDisconnectUrl != null) {
            URL connectUrl = null;
            try {
                connectUrl = onConnectUrl != null ? new URL(onConnectUrl) : null;
            }
            catch (MalformedURLException e) {
                String message = "applet parameter '" + ON_CONNECT_URL_PARAM + "' is not a valid URL";
                logger.warning(log.msg(message));
                throw new IllegalArgumentException(message, e);
            }
            URL disconnectUrl = null;
            try {
                disconnectUrl = onDisconnectUrl != null ? new URL(onDisconnectUrl) : null;
            }
            catch (MalformedURLException e) {
                String message = "applet parameter '" + ON_DISCONNECT_URL_PARAM + "' is not a valid URL";
                logger.warning(log.msg(message));
                throw new IllegalArgumentException(message, e);
            }
            final URL onConnect = connectUrl;
            final URL onDisconnect = disconnectUrl;
            listener = new ServiceLauncher.Listener() {

                @Override
                public void onConnect() {
                    if (onConnect != null) {
                        getAppletContext().showDocument(onConnect);
                    }
                }

                @Override
                public void onDisconnect() {
                    if (onDisconnect != null) {
                        getAppletContext().showDocument(onDisconnect);
                    }
                }

            };

        }

        this.launcher = new ServiceLauncher(getServiceJnlpUrl(),
                                            getServicePort(),
                                            getUseKeepAlive(),
                                            getConnectionRetryCount(),
                                            getConnectionRetryInterval(),
                                            listener,
                                            getServiceProperties());

    }

    @Override
    public void start() {

        logger.finer(log.entry("start"));

        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                try {
                    if (!launcher.start()) {
                        String onErrorUrl = getParameter(ON_ERROR_URL_PARAM);
                        if (onErrorUrl != null) {
                            try {
                                getAppletContext().showDocument(new URL(onErrorUrl));
                            }
                            catch (MalformedURLException e) {
                                String message = "applet parameter '" + ON_ERROR_URL_PARAM + "' is not a valid URL";
                                logger.warning(log.msg(message));
                                throw new IllegalArgumentException(message, e);
                            }
                        }
                    }
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }
        });
    }

    @Override
    public void stop() {

        logger.finer(log.entry("stop"));

        this.launcher.stop();

    }

    @Override
    public void destroy() {
        logger.finer(log.entry("destroy"));
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
                logger.fine(log.msg("Applet:   " + this.getClass().getName()));
                logger.fine(log.msg("Built-By: " + attr.getValue("Built-By")));
                logger.fine(log.msg("Vendor:   " + attr.getValue("Implementation-Vendor")));
                logger.fine(log.msg("Title:    " + attr.getValue("Implementation-Title")));
                this.version = attr.getValue("Implementation-Version");
                logger.fine(log.msg("Version:  " + this.version));
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
     * @return
     * @throws IllegalArgumentException
     */
    URI getServiceJnlpUrl() throws IllegalArgumentException {

        String propertyValue = getParameter(SERVICE_JNLP_URL_PARAM);
        if (propertyValue != null) {
            try {
                URI codebase = getCodeBase().toURI();
                logger.finer(log.msg("applet code base: " + codebase.toString()));
                URI jnlpUri = new URI(propertyValue);
                logger.finer(log.msg(".jnlp URL: " + jnlpUri.toString()));
                URI resolvedUri = codebase.resolve(jnlpUri);
                logger.finer(log.msg("resolved URL: " + resolvedUri));
                return resolvedUri;
            }
            catch (URISyntaxException e) {
                String message = "applet parameter '" + SERVICE_JNLP_URL_PARAM + "' is not a valid URL";
                logger.warning(log.msg(message));
                throw new IllegalArgumentException(message, e);
            }
        }
        else {
            String message = "required applet parameter '" + SERVICE_JNLP_URL_PARAM + "' is missing";
            logger.warning(log.msg(message));
            throw new IllegalArgumentException(message);
        }
    }

    /**
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
            catch (NumberFormatException e) {
                String message = "value of applet parameter '" + SERVICE_PORT_PARAM + "' is not an integer port number";
                logger.warning(log.msg(message));
                throw new IllegalArgumentException(message, e);
            }
        }
        return servicePort;
    }

    /**
     * @return
     */
    boolean getUseKeepAlive() {
        return Boolean.parseBoolean(getParameter(USE_KEEP_ALIVE_PARAM));
    }

    /**
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
            catch (NumberFormatException e) {
                String message = "value of applet parameter '" + CONNECTION_RETRY_COUNT_PARAM + "' is not an integer";
                logger.warning(log.msg(message));
                throw new IllegalArgumentException(message, e);
            }
        }

        return connectionRetryCount;
    }

    /**
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
            catch (NumberFormatException e) {
                String message = "value of applet parameter '" + CONNECTION_RETRY_INTERVAL_PARAM + "' is not an integer";
                logger.warning(log.msg(message));
                throw new IllegalArgumentException(message, e);
            }
        }

        return connectionRetryInterval;
    }

    /**
     * @return
     */
    Properties getServiceProperties() {

        Properties properties = new Properties();

        String propertiesParam = getParameter(SERVICE_PROPERTIES_PARAM);
        if (propertiesParam != null) {
            String[] propertyAssignments = propertiesParam.split(",");
            for (String propertyAssignment : propertyAssignments) {
                String[] pair = propertyAssignment.split("=");
                if (pair.length == 1) {
                    properties.setProperty(pair[0], "");
                }
                else if (pair.length == 2) {
                    properties.setProperty(pair[0], pair[1]);
                }
            }
        }

        return properties;
    }

}
