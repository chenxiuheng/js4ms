package org.js4ms.service.protocol.rest.server;

import java.io.IOException;
import java.util.logging.Level;

import org.js4ms.common.exceptions.ParseException;
import org.js4ms.service.Connection;
import org.js4ms.service.Service;
import org.js4ms.service.protocol.rest.MessageException;
import org.js4ms.service.protocol.rest.RequestException;
import org.js4ms.service.protocol.rest.StatusCodes;
import org.js4ms.service.protocol.rest.handler.RequestHandler;
import org.js4ms.service.protocol.rest.handler.RequestHandlerList;
import org.js4ms.service.protocol.rest.handler.ResponseHandlerList;
import org.js4ms.service.protocol.rest.handler.TransactionHandlerList;
import org.js4ms.service.protocol.rest.message.ProtocolVersion;
import org.js4ms.service.protocol.rest.message.Request;
import org.js4ms.service.protocol.rest.message.Response;
import org.js4ms.service.protocol.rest.message.StatusLine;

import com.larkwoodlabs.util.logging.Log;
import com.larkwoodlabs.util.logging.Loggable;

public abstract class AbstractService implements Service, RequestHandler, Loggable {

    protected final Log log = new Log(this);
    protected final ProtocolVersion protocolVersion;
    protected final RequestHandlerList requestHandlers = new RequestHandlerList();
    protected final TransactionHandlerList transactionHandlers = new TransactionHandlerList();
    protected final ResponseHandlerList responseHandlers = new ResponseHandlerList();


    protected AbstractService(final ProtocolVersion protocolVersion) {

        if (getLogger().isLoggable(Level.FINER)) {
            getLogger().finer(log.entry("<ctor>", protocolVersion));
        }

        this.protocolVersion = protocolVersion;
    }

    /**
     * Constructs a request parser for the service.
     * This method allows a derived service class implementations
     * to construct a parser during instantiation.
     * @return
     */
    protected abstract RequestParser getRequestParser();

    @Override
    public void service(final Connection connection) throws IOException {

        if (getLogger().isLoggable(Level.FINER)) {
            getLogger().finer(log.entry("service", connection));
        }

        try {
            // Parse the next message
            getRequestParser().parse(connection);
        }
        catch (MessageException e) {
            if (getLogger().isLoggable(Level.FINE)) {
                getLogger().finer(log.msg("request parsing failed with exception "+e.getClass().getSimpleName()+": "+e.getMessage()));
            }
            // The parsing operation failed after the start line
            // Send a response and close the connection.
            sendResponse(new Response(connection, new StatusLine(e.getProtocolVersion(), StatusCodes.BadRequest)));
            connection.close();
        }
        catch (ParseException e) {
            if (getLogger().isLoggable(Level.FINE)) {
                getLogger().finer(log.msg("request parsing failed with exception "+e.getClass().getSimpleName()+": "+e.getMessage()));
            }
            // The parsing operation failed at the start line - give up since message framing is lost
            connection.close();
        }
    }

    /**
     * Method called by the request parser when a message is received.
     * This method is called without reading the entity body that might be carried by the message.
     * If the entity body is not consumed by a request processor or transaction handler, it will
     * be consumed and discarded by this method to preserve message framing.
     */
    public void handleRequest(final Request request) throws RequestException, IOException {

        if (getLogger().isLoggable(Level.FINER)) {
            getLogger().finer(log.entry("handleRequest", request));
        }

        if (getLogger().isLoggable(Level.FINE)) {
            getLogger().fine(log.msg("received request:"));
            request.log(getLogger());
        }

        try {
            // Pre-process request (i.e. check request validity and server support)
            this.requestHandlers.handleRequest(request);
    
            // Create the response that will be sent back to the client.
            // The response is created with an InternalServerError status to indicate
            // that no transaction handler responded to the request.

            Response response = new Response(request.getConnection(),
                                             new StatusLine(request.getRequestLine().getProtocolVersion(), StatusCodes.InternalServerError));

            // Decorate response (i.e. add common headers such as Date and Server).
            this.responseHandlers.handleResponse(response);

            // Act upon the request to produce a response
            if (this.transactionHandlers.handleTransaction(request, response)) {
    
                // Send the response back to the client
                if (!response.isSent()) {
                    sendResponse(response);
                }
            }

        }
        finally {
            // Read the rest of the request body if we have not already done so (to preserve framing)
            if (request.getEntity() != null && !request.getEntity().isConsumed()) {
                request.getEntity().consumeContent();
            }
        }

    }

    void sendResponse(Response response) throws IOException {

        if (getLogger().isLoggable(Level.FINER)) {
            getLogger().finer(log.entry("sendResponse", response));
        }

        if (getLogger().isLoggable(Level.FINE)) {
            getLogger().fine(log.msg("sending response:"));
            response.log(getLogger());
        }

        response.send();

    }

    public final RequestHandlerList getRequestHandlers() {
        return this.requestHandlers;
    }

    public final TransactionHandlerList getTransactionHandlers() {
        return this.transactionHandlers;
    }

    public final ResponseHandlerList getResponseHandlers() {
        return this.responseHandlers;
    }
}
