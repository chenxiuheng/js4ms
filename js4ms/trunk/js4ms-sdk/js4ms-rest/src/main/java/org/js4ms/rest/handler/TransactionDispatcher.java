package org.js4ms.rest.handler;

import java.io.IOException;

import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;



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
