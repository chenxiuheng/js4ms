package com.larkwoodlabs.service.protocol.restful.handler;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import com.larkwoodlabs.service.protocol.restful.RequestException;
import com.larkwoodlabs.service.protocol.restful.message.Request;


public class RequestHandlerList implements RequestHandler {

    private final LinkedList<RequestHandler> handlers = new LinkedList<RequestHandler>();

    public RequestHandlerList() {
    }

    public void addHandler(RequestHandler handler) {
        this.handlers.add(handler);
    }

    public void removeHandler(RequestHandler handler) {
        this.handlers.remove(handler);
    }

    public void removeHandler(Class<?> handlerClass) {
        Iterator<RequestHandler> iter = this.handlers.iterator();
        while (iter.hasNext()) {
            if (iter.next().getClass().equals(handlerClass)) {
                iter.remove();
            }
        }
    }

    @Override
    public void handleRequest(Request request) throws RequestException, IOException {
        for (RequestHandler handler : this.handlers) {
            handler.handleRequest(request);
        }
    }

}
