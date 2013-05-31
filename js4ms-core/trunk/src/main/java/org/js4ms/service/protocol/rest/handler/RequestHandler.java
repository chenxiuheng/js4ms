package org.js4ms.service.protocol.rest.handler;

import java.io.IOException;

import org.js4ms.service.protocol.rest.RequestException;
import org.js4ms.service.protocol.rest.message.Request;



public interface RequestHandler {
    void handleRequest(Request request) throws RequestException, IOException;
}
