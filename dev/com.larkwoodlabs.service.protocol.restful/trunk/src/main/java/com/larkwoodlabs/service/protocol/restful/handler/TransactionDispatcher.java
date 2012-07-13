package com.larkwoodlabs.service.protocol.restful.handler;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.restful.RequestException;
import com.larkwoodlabs.service.protocol.restful.message.Request;
import com.larkwoodlabs.service.protocol.restful.message.Response;

public class TransactionDispatcher implements TransactionHandler {

    final TransactionHandlerResolver resolver;

    public TransactionDispatcher(final TransactionHandlerResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean handleTransaction(Request request, Response response) throws IOException {
        TransactionHandler handler;
        try {
            handler = this.resolver.getHandler(request);
        }
        catch (RequestException e) {
            e.setResponse(response);
            return true;
        }
        if (handler != null) {
            return handler.handleTransaction(request, response);
        }
        return false;
    }

}
