package org.js4ms.rtsp.server;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * RtspService.java [org.js4ms.jsdk:rtsp]
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

import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.rest.handler.AddDateHeader;
import org.js4ms.rest.handler.ResponseHandlerList;
import org.js4ms.rest.handler.TransactionDispatcher;
import org.js4ms.rest.handler.TransactionHandlerList;
import org.js4ms.rest.handler.TransactionHeaderResolver;
import org.js4ms.rest.handler.TransactionProtocolResolver;
import org.js4ms.rest.handler.VerifyAcceptEncodingHeader;
import org.js4ms.rest.message.MessageHeaderParser;
import org.js4ms.rest.message.ProtocolName;
import org.js4ms.rest.message.ProtocolVersion;
import org.js4ms.rest.server.AbstractService;
import org.js4ms.rest.server.RequestParser;
import org.js4ms.rtsp.handler.TransferCSeqHeader;
import org.js4ms.rtsp.handler.TransferSessionHeader;
import org.js4ms.rtsp.handler.TransferTimestampHeader;
import org.js4ms.rtsp.handler.VerifyRequireHeader;
import org.js4ms.rtsp.presentation.PresentationResolver;




/**
 * An RTSP service implementation.
 * The RTSP service can be used to exchange interleaved RTP/RTCP packets over a persistent TCP connection.
 * When sending or receiving interleaved packets, the connection, connection handler and service
 * must persist for the duration of the RTSP session that requires interleaving.
 * The session is initiated on the connection as a result of a SETUP request. Any RTP/RTCP streams transmitted
 * following the SETUP request are interleaved with RTSP messages on the same connection.
 * All interleaved data packets received on the connection are forwarded to data handlers registered
 * by sessions started on the connection.
 *
 * @author gbumgard
 */
public class RtspService extends AbstractService {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(RtspService.class.getName());

    public final static ProtocolVersion RTSP_PROTOCOL_VERSION = new ProtocolVersion(new ProtocolName("RTSP"), 1, 0);

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    private final RequestParser parser;

    private final Timer timer = new Timer("RTSP Service Timer");
    
    private final RtspTransactionHandler rtspHandler;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     * @param protocol
     */
    public RtspService(final PresentationResolver resolver) {
        super(RTSP_PROTOCOL_VERSION);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("RtspService", resolver));
        }

        this.parser = new RequestParser(new MessageHeaderParser(), this);

        TransactionHandlerList transactionHandlers = getTransactionHandlers();

        transactionHandlers.addHandler(new TransferTimestampHeader());
        transactionHandlers.addHandler(new TransferCSeqHeader());
        transactionHandlers.addHandler(new TransferSessionHeader());
        transactionHandlers.addHandler(new VerifyRequireHeader());
        transactionHandlers.addHandler(new VerifyAcceptEncodingHeader());

        this.rtspHandler = new RtspTransactionHandler(resolver);

        TransactionProtocolResolver protocolResolver = new TransactionProtocolResolver();
        protocolResolver.put("RTSP", rtspHandler);

        TransactionHeaderResolver tunnelResolver = new TransactionHeaderResolver(RtspTransactionHandler.TUNNEL_SESSION_COOKIE_HEADER_NAME);
        tunnelResolver.put(".*", rtspHandler);

        protocolResolver.put("HTTP", tunnelResolver);

        TransactionDispatcher dispatcher = new TransactionDispatcher(protocolResolver);

        transactionHandlers.addHandler(dispatcher);

        ResponseHandlerList responseHandlers = getResponseHandlers();
        responseHandlers.addHandler(new AddDateHeader());
    }

    @Override
    public void start() {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("start"));
        }
    }

    @Override
    public void stop() {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("stop"));
        }
        this.timer.cancel();
        this.rtspHandler.terminate();
    }

    @Override
    protected final RequestParser getRequestParser() {
        return this.parser;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void log(final Logger logger, final Level level) {
    }
    
}
