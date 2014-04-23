package org.js4ms.reflector;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * RtspMulticastReflector.java [org.js4ms.jsdk:reflector]
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

import java.io.IOException;
import java.util.Properties;

import org.js4ms.common.util.logging.Logging;
import org.js4ms.http.handler.LoggingConfigurationHandler;
import org.js4ms.http.handler.LoggingOutputHandler;
import org.js4ms.rest.entity.StringEntity;
import org.js4ms.rest.handler.AddServerHeader;
import org.js4ms.rest.handler.ResponseHandlerList;
import org.js4ms.rest.handler.TransactionDispatcher;
import org.js4ms.rest.handler.TransactionHandler;
import org.js4ms.rest.handler.TransactionProtocolResolver;
import org.js4ms.rest.handler.TransactionUriPathResolver;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;
import org.js4ms.rtsp.message.RtspStatusCode;
import org.js4ms.rtsp.presentation.PresentationUriPathResolver;
import org.js4ms.rtsp.server.RtspService;
import org.js4ms.server.Connection;
import org.js4ms.server.ConnectionHandler;
import org.js4ms.server.ConnectionHandlerFactory;
import org.js4ms.server.ConnectionManager;
import org.js4ms.server.Server;
import org.js4ms.server.ServerFactory;
import org.js4ms.server.Service;

/**
 * @author Greg Bumgardner (gbumgard)
 */
public class RtspMulticastReflector {

    /**
     * @param args
     */
    public static void main(String[] args) {

        try {
            Logging.configureLogging();
        }
        catch (IOException e) {
        }

        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "");

        try {
            Server.runServer(System.getProperties(), new ServerFactory() {

                @Override
                public Server construct(Properties properties) {

                    PresentationUriPathResolver reflectorResolver = new PresentationUriPathResolver();
                    reflectorResolver.put("/reflect", new MulticastReflectorFactory());

                    final RtspService service = new RtspService(reflectorResolver);

                    ResponseHandlerList decorators = service.getResponseHandlers();
                    decorators.addHandler(new AddServerHeader("RTSP Multicast Reflector"));

                    final Server server = new Server(properties, service, new ConnectionHandlerFactory() {
                        @Override
                        public ConnectionHandler construct(ConnectionManager manager, Connection connection, Service service) {
                            return new ConnectionHandler(manager, connection, service);
                        }
                    });

                    // Add HTTP resource that can be used to shutdown the server

                    TransactionUriPathResolver adminResolver = new TransactionUriPathResolver();
                    adminResolver.put("/shutdown", new TransactionHandler() {
                        @Override
                        public boolean handleTransaction(Request request, Response response) throws IOException {
                            response.setStatus(RtspStatusCode.OK);
                            response.setEntity(new StringEntity("stopping RTSP Multicast reflector..."));
                            new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    try {
                                        server.stop();
                                    }
                                    catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                }

                            }).start();
                            return true;
                        }
                    });

                    adminResolver.put("/log", new LoggingOutputHandler());

                    adminResolver.put("/loggers", new LoggingConfigurationHandler());

                    adminResolver.put("/*", new TransactionHandler() {
                        @Override
                        public boolean handleTransaction(Request request, Response response) throws IOException {
                            response.setStatus(RtspStatusCode.Forbidden);
                            response.setEntity(new StringEntity(RtspStatusCode.Forbidden.toString()));
                            response.send();
                            return true;
                        }
                    });

                    TransactionProtocolResolver protocolResolver = new TransactionProtocolResolver();
                    protocolResolver.put("HTTP", adminResolver);

                    service.getTransactionHandlers().addHandler(new TransactionDispatcher(protocolResolver));

                    return server;
                }

            });
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
