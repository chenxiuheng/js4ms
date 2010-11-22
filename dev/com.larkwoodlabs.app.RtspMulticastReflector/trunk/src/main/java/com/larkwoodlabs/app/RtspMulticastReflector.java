package com.larkwoodlabs.app;

import java.io.IOException;
import java.util.Properties;

import com.larkwoodlabs.service.Connection;
import com.larkwoodlabs.service.ConnectionHandler;
import com.larkwoodlabs.service.ConnectionHandlerFactory;
import com.larkwoodlabs.service.ConnectionManager;
import com.larkwoodlabs.service.Server;
import com.larkwoodlabs.service.ServerFactory;
import com.larkwoodlabs.service.Service;
import com.larkwoodlabs.service.protocol.http.server.handlers.LoggingConfigurationHandler;
import com.larkwoodlabs.service.protocol.http.server.handlers.LoggingOutputHandler;
import com.larkwoodlabs.service.protocol.rtsp.RtspStatusCodes;
import com.larkwoodlabs.service.protocol.rtsp.presentation.PresentationUriPathResolver;
import com.larkwoodlabs.service.protocol.rtsp.reflector.MulticastReflectorFactory;
import com.larkwoodlabs.service.protocol.rtsp.server.RtspService;
import com.larkwoodlabs.service.protocol.text.entity.StringEntity;
import com.larkwoodlabs.service.protocol.text.handler.ResponseHandlerList;
import com.larkwoodlabs.service.protocol.text.message.Request;
import com.larkwoodlabs.service.protocol.text.message.Response;
import com.larkwoodlabs.service.protocol.text.handler.TransactionDispatcher;
import com.larkwoodlabs.service.protocol.text.handler.TransactionHandler;
import com.larkwoodlabs.service.protocol.text.handler.TransactionProtocolResolver;
import com.larkwoodlabs.service.protocol.text.handler.TransactionUriPathResolver;
import com.larkwoodlabs.service.protocol.text.server.handlers.AddServerHeader;
import com.larkwoodlabs.util.logging.Logging;

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

                    final Server server = new Server(properties,
                                                     service,
                                                     new ConnectionHandlerFactory() {
                                                         @Override
                                                         public ConnectionHandler construct(ConnectionManager manager,
                                                                                            Connection connection,
                                                                                            Service service) {
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
