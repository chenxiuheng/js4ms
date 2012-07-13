package com.larkwoodlabs.service.protocol.rtsp.server;

import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.service.protocol.rest.handler.ResponseHandlerList;
import com.larkwoodlabs.service.protocol.rest.handler.TransactionDispatcher;
import com.larkwoodlabs.service.protocol.rest.handler.TransactionHandlerList;
import com.larkwoodlabs.service.protocol.rest.handler.TransactionHeaderResolver;
import com.larkwoodlabs.service.protocol.rest.handler.TransactionProtocolResolver;
import com.larkwoodlabs.service.protocol.rest.handlers.AddDateHeader;
import com.larkwoodlabs.service.protocol.rest.message.MessageHeaderParser;
import com.larkwoodlabs.service.protocol.rest.message.ProtocolName;
import com.larkwoodlabs.service.protocol.rest.message.ProtocolVersion;
import com.larkwoodlabs.service.protocol.rtsp.handlers.TransferCSeqHeader;
import com.larkwoodlabs.service.protocol.rtsp.handlers.TransferSessionHeader;
import com.larkwoodlabs.service.protocol.rtsp.handlers.TransferTimestampHeader;
import com.larkwoodlabs.service.protocol.rtsp.handlers.VerifyRequireHeader;
import com.larkwoodlabs.service.protocol.rtsp.presentation.PresentationResolver;
import com.larkwoodlabs.service.protocol.text.server.AbstractService;
import com.larkwoodlabs.service.protocol.text.server.RequestParser;
import com.larkwoodlabs.service.protocol.text.server.handlers.VerifyAcceptEncodingHeader;


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
        this.parser = new RequestParser(new MessageHeaderParser(), this);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("RtspService", resolver));
        }

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
    public void log(Logger logger) {
    }
    
}
