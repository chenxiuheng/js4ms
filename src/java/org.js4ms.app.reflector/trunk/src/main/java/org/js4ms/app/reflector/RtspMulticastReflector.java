/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: InputChannel.java (org.js4ms.channels)
 * 
 * Copyright (C) 2009-2012 Cisco Systems, Inc.
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

package org.js4ms.app.reflector;

import java.io.IOException;
import java.util.Properties;

import org.js4ms.service.protocol.http.server.handler.LoggingConfigurationHandler;
import org.js4ms.service.protocol.http.server.handler.LoggingOutputHandler;
import org.js4ms.service.protocol.rest.entity.StringEntity;
import org.js4ms.service.protocol.rest.handler.ResponseHandlerList;
import org.js4ms.service.protocol.rest.handler.TransactionDispatcher;
import org.js4ms.service.protocol.rest.handler.TransactionHandler;
import org.js4ms.service.protocol.rest.handler.TransactionProtocolResolver;
import org.js4ms.service.protocol.rest.handler.TransactionUriPathResolver;
import org.js4ms.service.protocol.rest.message.Request;
import org.js4ms.service.protocol.rest.message.Response;
import org.js4ms.service.protocol.rest.server.handlers.AddServerHeader;
import org.js4ms.service.protocol.rtsp.RtspStatusCodes;
import org.js4ms.service.protocol.rtsp.presentation.PresentationUriPathResolver;
import org.js4ms.service.protocol.rtsp.server.RtspService;
import org.js4ms.service.server.Connection;
import org.js4ms.service.server.ConnectionHandler;
import org.js4ms.service.server.ConnectionHandlerFactory;
import org.js4ms.service.server.ConnectionManager;
import org.js4ms.service.server.Server;
import org.js4ms.service.server.ServerFactory;
import org.js4ms.service.server.Service;
import org.js4ms.util.logging.Logging;

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
                            response.setStatus(RtspStatusCodes.OK);
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
                            response.setStatus(RtspStatusCodes.Forbidden);
                            response.setEntity(new StringEntity(RtspStatusCodes.Forbidden.toString()));
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
