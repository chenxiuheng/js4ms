package org.js4ms.service.rest.handler;

import java.io.IOException;

import org.js4ms.service.rest.common.RequestException;
import org.js4ms.service.rest.message.Request;



public interface RequestHandler {
    void handleRequest(Request request) throws RequestException, IOException;
}
