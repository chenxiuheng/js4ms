/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: ServiceLauncherApplet.java (org.js4ms.jws.applet)
 * 
 * Copyright ?? 2011-2012 Cisco Systems, Inc.
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

package org.js4ms.jws.launcher;

import java.applet.Applet;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;
import org.js4ms.common.util.logging.Logging;
import org.js4ms.launcher.jws.ServiceLauncher;

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
    private static final String LOGGING_PROPERTIES_URL_PARAM = "LoggingPropertiesUrl";
    private static final String ON_ERROR_URL_PARAM = "OnErrorUrl";
    private static final String ON_TERMINATION_URL_PARAM = "OnTerminationUrl";
    private static final String ON_CONNECT_URL_PARAM = "OnConnectUrl";
    private static final String ON_DISCONNECT_URL_PARAM = "OnDisconnectUrl";

    private static final int DEFAULT_SERVICE_PORT = 9999;
    private static final int DEFAULT_CONNECTION_RETRY_COUNT = 10;
    private static final int DEFAULT_CONNECTION_RETRY_INTERVAL = 1000;

    private static final String PARAMETER_INFO[][] = {
            { SERVICE_JNLP_URL_PARAM,
              "String",
              "The Java Web Start service .jnlp file URL (required)" },
            { USE_KEEP_ALIVE_PARAM,
              "Boolean",
              "Indicates whether the applet should open a keep-alive connection."  },
            { SERVICE_PORT_PARAM,
              "Integer",
              "The keep-alive service port. This is the TCP port on which the applet will attempt to establish a connection to the service" },
            { CONNECTION_RETRY_COUNT_PARAM,
              "Integer",
              "The number of attempts the applet should make to open a keep-alive connection to a service instance." },
            { CONNECTION_RETRY_INTERVAL_PARAM,
              "Integer",
              "The time delay to use between connection attempts (in milliseconds)." },
            { SERVICE_PROPERTIES_PARAM,
              "comma-delimited list of name=value pairs",
              "Property values that will be passed to the service." },
            { LOGGING_PROPERTIES_URL_PARAM,
              "URL",
              "A URL that identifies a source of java.logging properties for the applet" },
            { ON_ERROR_URL_PARAM,
              "URL",
              "The URL that the applet will use to set the document location to should an error occur." },
            { ON_TERMINATION_URL_PARAM,
              "URL",
              "The URL that the applet will use to set the document location when the service terminates." },
            { ON_CONNECT_URL_PARAM,
              "URL",
              "The URL that the applet will use to set the document location when the applet successfully opens a keep-alive connection to the service." },
            { ON_DISCONNECT_URL_PARAM,
              "URL",
              "The URL that the applet will use to set the document location when the service has stopped accepting connections." } };

    public static final Logger logger = Logger.getLogger(ServiceLauncherApplet.class.getName());

    private final Log log = new Log(this);

    private String version;

    private ServiceLauncher.Listener listener = null;

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
    
    public String getParameterInfoJson() {
    	String result = "[";
    	for (int index = 0; index < PARAMETER_INFO.length; index++) {
    		String[] entry = PARAMETER_INFO[index];
    		if (index > 0) result += ',';
    		result += "{ \"name\":\""+entry[0]+"\",";
    		result += "\"type\":\""+entry[1]+"\",";
    		result += "\"description\":\""+entry[2]+"\" }";
    	}
    	result += ']';
    	return result;
    }

    @Override
    public void init() {

    	URI loggingPropertiesUri = getLoggingPropertiesUrl();

        if (loggingPropertiesUri != null) {
            try {
                Logging.configureLogging(loggingPropertiesUri);
            }
            catch (MalformedURLException e) {
            }
            catch (IOException e) {
            }
        }

        logVersionInfo();

        logger.finer(log.entry("init"));

        String onTerminationUrl = getParameter(ON_TERMINATION_URL_PARAM);
        String onConnectUrl = getParameter(ON_CONNECT_URL_PARAM);
        String onDisconnectUrl = getParameter(ON_DISCONNECT_URL_PARAM);
        if (onTerminationUrl != null || onConnectUrl != null || onDisconnectUrl != null) {
            URL terminationUrl = null;
            try {
                terminationUrl = onTerminationUrl != null ? constructDocumentUrl(onTerminationUrl) : null;
            }
            catch (MalformedURLException e) {
                String message = "applet parameter '" + ON_TERMINATION_URL_PARAM + "' value '"+terminationUrl+"'is not a valid URL";
                logger.warning(log.msg(message));
                throw new IllegalArgumentException(message, e);
            }
            URL connectUrl = null;
            try {
                connectUrl = onConnectUrl != null ? constructDocumentUrl(onConnectUrl) : null;
            }
            catch (MalformedURLException e) {
                String message = "applet parameter '" + ON_CONNECT_URL_PARAM + "' value '"+connectUrl+"' is not a valid URL";
                logger.warning(log.msg(message));
                throw new IllegalArgumentException(message, e);
            }
            URL disconnectUrl = null;
            try {
                disconnectUrl = onDisconnectUrl != null ? constructDocumentUrl(onDisconnectUrl) : null;
            }
            catch (MalformedURLException e) {
                String message = "applet parameter '" + ON_DISCONNECT_URL_PARAM + "' value '"+disconnectUrl+"'is not a valid URL";
                logger.warning(log.msg(message));
                throw new IllegalArgumentException(message, e);
            }
            final URL onTermination = terminationUrl;
            final URL onConnect = connectUrl;
            final URL onDisconnect = disconnectUrl;
            this.listener = new ServiceLauncher.Listener() {

                @Override
                public void onConnect() {
                    logger.finer(log.entry("onConnect"));
                    if (onConnect != null) {
                        getAppletContext().showDocument(onConnect);
                    }
                }

                @Override
                public void onDisconnect() {
                    logger.finer(log.entry("onDisconnect"));
                    if (onDisconnect != null) {
                        getAppletContext().showDocument(onDisconnect);
                    }
                }

                @Override
                public void onExit(int exitCode) {
                    logger.finer(log.entry("onExit"));
                    if (onTermination != null) {
                        getAppletContext().showDocument(onTermination);
                    }
                }

            };

        }

        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                try {
                    ServiceLauncherApplet.this.launcher = new ServiceLauncher(
                                                        getServiceJnlpUrl(),
                                                        getServicePort(),
                                                        getUseKeepAlive(),
                                                        getConnectionRetryCount(),
                                                        getConnectionRetryInterval(),
                                                        ServiceLauncherApplet.this.listener,
                                                        getServiceProperties());
                }
                catch (Exception e) {
                    throwJSException(e.getClass().getName() + " - " + e.getMessage());
                }
                return null;
            }
        });

    }

    @Override
    public void start() {

        logger.finer(log.entry("start"));

        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                try {
                    if (ServiceLauncherApplet.this.launcher != null && !ServiceLauncherApplet.this.launcher.start()) {
                        String onErrorUrl = getParameter(ON_ERROR_URL_PARAM);
                        if (onErrorUrl != null) {
                            try {
                                getAppletContext().showDocument(constructDocumentUrl(onErrorUrl));
                            }
                            catch (MalformedURLException e) {
                                String message = "applet parameter '" + ON_ERROR_URL_PARAM + "' value '" + onErrorUrl + "' is not a valid URL";
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

    URL constructDocumentUrl(final String documentUrl) throws MalformedURLException {
        // Construct dummy stream handler - required workaround when URL is a "javascript:" expression
        final URLStreamHandler streamHandler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u)
                throws IOException {
                return null;
            }
        };

        return new URL(null,documentUrl,streamHandler);
    }

    void throwJSException(final String description) {
        
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                String expression = "javascript:throw new Error('"+description.replace("'", "\\'")+"')";
                
                try {
                    getAppletContext().showDocument(constructDocumentUrl(expression));
                }
                catch (MalformedURLException e) {
                    String message = "the expression (" + expression + ") cannot be converted into a valid URL";
                    logger.warning(log.msg(message));
                }
                return null;
            }
        });
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
                logger.fine(log.msg("Main-Class: " + attr.getValue("Main-Class")));
                logger.fine(log.msg("Built-By:   " + attr.getValue("Built-By")));
                logger.fine(log.msg("Vendor:     " + attr.getValue("Implementation-Vendor")));
                logger.fine(log.msg("Title:      " + attr.getValue("Implementation-Title")));
                this.version = attr.getValue("Implementation-Version");
                logger.fine(log.msg("Version:    " + this.version));
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

            // Trim spaces and quotes - quotes may be required when running in the applet viewer.
            propertyValue = propertyValue.trim( );
            if (propertyValue.startsWith( "\"" ) && propertyValue.endsWith( "\"" ) ) {
              propertyValue = propertyValue.substring( 1, propertyValue.length( ) - 1 );
            }
            else if (propertyValue.startsWith( "'" ) && propertyValue.endsWith( "'" ) ) {
                propertyValue = propertyValue.substring( 1, propertyValue.length( ) - 1 );
            }

            try {
                URI codebase = getCodeBase().toURI();
                logger.finer(log.msg("applet code base: " + codebase.toString()));
                URI jnlpUri = new URI(propertyValue);
                logger.finer(log.msg(".jnlp URL: " + jnlpUri.toString()));
                URI resolvedUri = codebase.resolve(jnlpUri);
                logger.finer(log.msg("resolved URL: " + resolvedUri));
                String scheme = resolvedUri.getScheme();

                // Check for existence of JNLP description so we can report an error in the applet process
                // and avoid having javaws display an exception popup should the it not exist.
                if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
                    try {
                        HttpURLConnection connection = (HttpURLConnection)resolvedUri.toURL().openConnection();
                        connection.connect();
                        int code = connection.getResponseCode();
                        connection.disconnect();
                        if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_NOT_MODIFIED) {
                            String message = "JNLP description '"+resolvedUri.toString() + "' does not exist.";
                            logger.warning(log.msg(message));
                            throw new IllegalArgumentException(message);
                        }
                    }
                    catch (MalformedURLException e) {
                        String message = "applet parameter '" + SERVICE_JNLP_URL_PARAM + "' is not a valid URL.";
                        logger.warning(log.msg(message));
                        throw new IllegalArgumentException(message, e);
                    }
                    catch (IOException e) {
                        String message = "connection attempt for JNLP URL '"+resolvedUri.toString() + "' failed.";
                        logger.warning(log.msg(message));
                        throw new IllegalArgumentException(message, e);
                    }
                }
                else if (scheme.equalsIgnoreCase("file")) {
                    File file = new File(resolvedUri);
                    if (!file.exists()) {
                        String message = "JNLP description '"+resolvedUri.toString() + "' does not exist.";
                        logger.warning(log.msg(message));
                        throw new IllegalArgumentException(message);
                    }
                }
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
    URI getLoggingPropertiesUrl() throws IllegalArgumentException {

        String propertyValue = getParameter(LOGGING_PROPERTIES_URL_PARAM);
        if (propertyValue != null) {

            // Trim spaces and quotes - quotes may be required when running in the applet viewer.
            propertyValue = propertyValue.trim( );
            if (propertyValue.startsWith( "\"" ) && propertyValue.endsWith( "\"" ) ) {
              propertyValue = propertyValue.substring( 1, propertyValue.length( ) - 1 );
            }
            else if (propertyValue.startsWith( "'" ) && propertyValue.endsWith( "'" ) ) {
                propertyValue = propertyValue.substring( 1, propertyValue.length( ) - 1 );
            }

            try {
                URI codebase = getCodeBase().toURI();
                logger.finer(log.msg("applet code base: " + codebase.toString()));
                URI propertiesUri = new URI(propertyValue);
                logger.finer(log.msg("properties URI: " + propertiesUri.toString()));
                URI resolvedUri = codebase.resolve(propertiesUri);
                logger.finer(log.msg("resolved URI: " + resolvedUri));
                String scheme = resolvedUri.getScheme();

                // Check for existence of properties file so we can report an error in the applet process
                if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
                    try {
                        HttpURLConnection connection = (HttpURLConnection)resolvedUri.toURL().openConnection();
                        connection.connect();
                        int code = connection.getResponseCode();
                        connection.disconnect();
                        if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_NOT_MODIFIED) {
                            String message = "logging properties '"+resolvedUri.toString() + "' does not exist.";
                            logger.warning(log.msg(message));
                            throw new IllegalArgumentException(message);
                        }
                    }
                    catch (MalformedURLException e) {
                        String message = "applet parameter '" + LOGGING_PROPERTIES_URL_PARAM + "' is not a valid URL.";
                        logger.warning(log.msg(message));
                        throw new IllegalArgumentException(message, e);
                    }
                    catch (IOException e) {
                        String message = "connection attempt for logging properties URL '"+resolvedUri.toString() + "' failed.";
                        logger.warning(log.msg(message));
                        throw new IllegalArgumentException(message, e);
                    }
                }
                else if (scheme.equalsIgnoreCase("file")) {
                    File file = new File(resolvedUri);
                    if (!file.exists()) {
                        String message = "logging properties '"+resolvedUri.toString() + "' does not exist.";
                        logger.warning(log.msg(message));
                        throw new IllegalArgumentException(message);
                    }
                }
                return resolvedUri;
            }
            catch (URISyntaxException e) {
                String message = "applet parameter '" + LOGGING_PROPERTIES_URL_PARAM + "' is not a valid URL";
                logger.warning(log.msg(message));
                throw new IllegalArgumentException(message, e);
            }
        }
        else {
        	return null;
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
