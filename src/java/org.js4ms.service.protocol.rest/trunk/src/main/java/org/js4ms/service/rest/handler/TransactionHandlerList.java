package org.js4ms.service.rest.handler;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.js4ms.service.protocol.rest.message.Request;
import org.js4ms.service.protocol.rest.message.Response;



public class TransactionHandlerList implements TransactionHandler {

    private final LinkedList<TransactionHandler> handlers = new LinkedList<TransactionHandler>();

    public TransactionHandlerList() {
    }

    public void addHandler(TransactionHandler handler) {
        this.handlers.add(handler);
    }

    public void removeHandler(TransactionHandler handler) {
        this.handlers.remove(handler);
    }

    public void removeHandler(Class<?> handlerClass) {
        Iterator<TransactionHandler> iter = this.handlers.iterator();
        while (iter.hasNext()) {
            if (iter.next().getClass().equals(handlerClass)) {
                iter.remove();
            }
        }
    }

    @Override
    public boolean handleTransaction(final Request request, final Response response) throws IOException {
        for (TransactionHandler handler : this.handlers) {
            if (handler.handleTransaction(request, response)) return true;
        }
        return false;
    }

}
