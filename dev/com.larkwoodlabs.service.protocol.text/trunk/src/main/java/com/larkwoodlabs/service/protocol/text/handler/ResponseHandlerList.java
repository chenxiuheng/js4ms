package com.larkwoodlabs.service.protocol.text.handler;

import java.util.Iterator;
import java.util.LinkedList;

import com.larkwoodlabs.service.protocol.text.message.Response;


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
    public void handleResponse(Response response) {
        for (ResponseHandler handler : this.handlers) {
            handler.handleResponse(response);
        }
    }

}
