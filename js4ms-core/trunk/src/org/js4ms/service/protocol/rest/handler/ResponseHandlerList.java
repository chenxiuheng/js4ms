package org.js4ms.service.protocol.rest.handler;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.js4ms.service.protocol.rest.message.Response;




public class ResponseHandlerList implements ResponseHandler {

    private final LinkedList<ResponseHandler> handlers = new LinkedList<ResponseHandler>();

    public ResponseHandlerList() {
    }

    public void addHandler(ResponseHandler handler) {
        this.handlers.add(handler);
    }

    public void removeHandler(ResponseHandler handler) {
        this.handlers.remove(handler);
    }

    public void removeHandler(Class<?> handlerClass) {
        Iterator<ResponseHandler> iter = this.handlers.iterator();
        while (iter.hasNext()) {
            if (iter.next().getClass().equals(handlerClass)) {
                iter.remove();
            }
        }
    }

    @Override
    public void handleResponse(Response response) throws IOException {
        for (ResponseHandler handler : this.handlers) {
            handler.handleResponse(response);
        }
    }

}
