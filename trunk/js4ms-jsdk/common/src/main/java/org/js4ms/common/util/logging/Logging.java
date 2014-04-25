package org.js4ms.common.util.logging;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * Logging.java [org.js4ms.jsdk:common]
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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public final class Logging {

    public static final String LOGGING_PROPERTIES_URL_PROPERTY = "org.js4ms.logging.properties.url";

    public static final int methodStackTraceLevel = determineStackTraceLevel();

    static int determineStackTraceLevel() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].getMethodName().equals("determineStackTraceLevel")) {
                return i + 1;
            }
        }
        return 0;
    }

    public static String entering(final String objectId, final String methodName) {
        return objectId + " entering " + methodName + "()";
    }

    public static String entering(final String objectId, final String methodName, final Object... args) {
        return objectId + " entering " + methodName + Logging.args(args);
    }

    public static String exiting(final String objectId, final String methodName) {
        return objectId + " exiting " + methodName;
    }

    public static String exiting(final String objectId, final String methodName, final Object result) {
        return objectId + " exiting " + methodName + " returns " + result;
    }

    public static String entry(final Object object, final Object... args) {
        String info = Thread.currentThread().getStackTrace()[methodStackTraceLevel].getMethodName() + "(";
        for (int index = 0; index < args.length; index++) {
            info += args[index];
            if (index < args.length - 1) info += ", ";
        }
        info += ")";
        return info;
    }

    public static String exit(final Object object, final Object result) {
        return identify(object) + " " + Thread.currentThread().getStackTrace()[methodStackTraceLevel].getMethodName() + "->"
               + result;
    }

    public static String identify(final Object object) {
        String id = "00000000" + Integer.toHexString(object.hashCode());
        return "[" + id.substring(id.length() - 8) + "]";
    }

    public static String method() {
        return Thread.currentThread().getStackTrace()[methodStackTraceLevel].getMethodName();
    }

    public static String address(final SocketAddress address) {
        return Logging.address(((InetSocketAddress) address).getAddress().getAddress()) + ":"
               + ((InetSocketAddress) address).getPort();
    }

    public static String address(final InetSocketAddress address) {
        return Logging.address(address.getAddress()) + ":" + address.getPort();
    }

    public static String address(final InetAddress address) {
        return Logging.address(address.getAddress());
    }

    public static String address(final byte[] address) {
        String result = "";
        if (address == null) {
            return null;
        }
        if (address.length == 4) {
            // IPv4 Address
            for (int i = 0; i < address.length;) {
                result += address[i++] & 0xFF;
                if (i < address.length) result += ".";
            }
        }
        else {
            // IPv6 Address
            for (int i = 0; i < address.length;) {
                result += String.format("%02X", address[i++] & 0xFF);
                if (i < address.length) result += ":";
            }
        }
        return result;
    }

    public static String mac(final byte[] mac) {
        String result = "";
        for (int i = 0; i < mac.length; i++) {
            result += String.format("%02X", mac[i++]);
        }
        return result;
    }

    public static Object[] argArray(Object... objects) {
        return objects;
    }

    public static String arg(final Object object) {
        if (object == null) return "null";
        Class<?> objectClass = object.getClass();
        if (objectClass.isPrimitive() || object instanceof String) {
            return object.toString();
        }
        else {
            String name = object.getClass().getSimpleName();
            if (name.length() == 0) {
                name = ((Class<?>) object.getClass().getSuperclass()).getSimpleName();
            }

            return name + Logging.identify(object);
        }
    }

    public static String args(final Object... objects) {
        int length = objects.length;
        String result = "(";
        if (length > 0) {
            result += Logging.arg(objects[0]);
            for (int i = 1; i < length; i++) {
                result += ", " + Logging.arg(objects[i]);
            }
        }
        return result + ")";
    }

    /**
     * @throws IOException
     */
    public static void configureLogging() throws IOException {

        String loggingPropertiesUrl = System.getProperty(LOGGING_PROPERTIES_URL_PROPERTY);

        if (loggingPropertiesUrl != null) {
            try {
                configureLogging(new URI(loggingPropertiesUrl));
            }
            catch (URISyntaxException e) {
                System.out.println("the value assigned to the '" + LOGGING_PROPERTIES_URL_PROPERTY + "' is not a valid URL.");
            }
        }
    }

    /**
     * @throws IOException
     */
    public static void configureLogging(final URI uri) throws IOException {

        String path = uri.getPath();

        if (path != null) {

            if (uri.getScheme().equals("http")) {

                try {

                    HttpURLConnection urlConnection = ((HttpURLConnection) uri.toURL().openConnection());

                    if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                        int contentLength = urlConnection.getContentLength();

                        if (contentLength == -1) {
                            System.out.println("the logging configuration fetched from '" + uri.toString() + "' is empty");
                            return;
                        }
                        else {

                            Properties properties = new Properties();

                            properties.load(urlConnection.getInputStream());

                            configureLogging(properties);
                        }
                    }
                    else {
                        System.out.println("cannot fetch logging configuration from '" + uri.toString() + "' - server returned " +
                                           urlConnection.getResponseCode() + " " +
                                           urlConnection.getResponseMessage());
                    }
                }
                catch (ConnectException e) {
                    System.out.println("cannot fetch logging configuration '" + uri.toString() + "' - " + e.getMessage());
                    throw e;
                }
                catch (IOException e) {
                    System.out.println("cannot fetch logging configuration '" + uri.toString() + "' - " + e.getMessage());
                    throw e;
                }
            }
            else if (uri.getScheme().equals("file")) {

                try {
                    InputStream inputStream = new FileInputStream(URLDecoder.decode(uri.getSchemeSpecificPart(), "UTF8"));

                    Properties properties = new Properties();
                    properties.load(inputStream);
                    configureLogging(properties);
                }
                catch (FileNotFoundException e) {
                    System.out.println("cannot read logging configuration '" + uri.toString() + "' - file not found");
                    throw e;
                }
                catch (IOException e) {
                    System.out.println("cannot read logging configuration '" + uri.toString() + "' - " + e.getMessage());
                    throw e;
                }
            }

        }

    }

    public static void configureLogging(Properties loggingProperties) {

        Set<Map.Entry<Object, Object>> entries = loggingProperties.entrySet();

        /*
         * This no longer seems to be necessary.
         * // Iterate over configuration properties to locate logger entries and attempt
         * // to use the logger name to load a class to force static logger
         * initialization.
         * // We must do this before loading the configuration into the LogManager so that
         * loggers will
         * // be registered before the LogManager applies any level settings contained in
         * the configuration.
         * 
         * 
         * for (Map.Entry<Object,Object> entry : entries) {
         * String key = (String)entry.getKey();
         * if (key.endsWith(".level") && !key.startsWith("java.util.logging")) {
         * 
         * // Remove the .level part
         * String loggerName = key.substring(0,key.length()-6);
         * 
         * if (loggerName.length() == 0) {
         * // Must be the root logger - skip to next
         * continue;
         * }
         * 
         * try {
         * // Try to load class to force static logger instantiation
         * Class.forName(loggerName, true,
         * Thread.currentThread().getContextClassLoader());
         * }
         * catch (ClassNotFoundException e) {
         * //System.out.println("cannot initialize logger '"+loggerName+"' - class not found"
         * );
         * continue;
         * }
         * }
         * }
         */

        // Write the properties into a string so they can be loaded by the log manager
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            loggingProperties.store(os, "Logging Properties");
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            LogManager.getLogManager().readConfiguration(is);
        }
        catch (Exception e) {
            System.out.println("cannot configure logging - " + e.getMessage());
            e.printStackTrace();
        }

        // Workaround for class loader issue for custom handlers/formatters
        // Java logging framework only uses system class loader to load handlers,
        // formatters etc.
        // Custom handlers/formatters packaged in an application/applet jar will not be
        // loaded.
        // We can't doing anything about handlers specified in a logging configuration but
        // we
        // can instantiate formatters and attach them to handlers as specified in the
        // configuration properties.

        // Create and set formatters that LogManager failed to load.
        for (Map.Entry<Object, Object> entry : entries) {
            String key = (String) entry.getKey();
            if (key.endsWith(".formatter")) {

                String formatterClassName = (String) entry.getValue();

                Formatter formatter = null;
                try {
                    Class<?> cls = Class.forName(formatterClassName, true, Thread.currentThread().getContextClassLoader());
                    try {
                        formatter = (Formatter) cls.newInstance();
                    }
                    catch (Exception e) {
                        System.out.println("cannot instantiate formatter class '" + formatterClassName + "': " + e.getMessage());
                        break;
                    }
                }
                catch (ClassNotFoundException e) {
                    System.out.println("formatter class '" + formatterClassName + "' not found");
                    continue;
                }

                String handlerClassName = key.substring(0, key.lastIndexOf('.'));

                Enumeration<String> iter = LogManager.getLogManager().getLoggerNames();
                while (iter.hasMoreElements()) {
                    String loggerName = iter.nextElement();
                    Handler[] handlers = Logger.getLogger(loggerName).getHandlers();
                    for (int index = 0; index < handlers.length; index++) {
                        Handler handler = handlers[index];
                        if (handlerClassName.equals(handler.getClass().getName())) {
                            Formatter currentFormatter = handler.getFormatter();
                            if (currentFormatter != null) {
                                if (!formatterClassName.equals(currentFormatter.getClass().getName())) {
                                    // Reset the formatter for this handler instance
                                    handler.setFormatter(formatter);
                                }
                            }
                        }
                    }
                }
            }
        }

    }

}
