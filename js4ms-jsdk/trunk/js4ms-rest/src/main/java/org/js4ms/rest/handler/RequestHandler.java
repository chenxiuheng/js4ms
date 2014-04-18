package org.js4ms.rest.handler;

import java.io.IOException;

import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.message.Request;



public interface RequestHandler {
    void handleRequest(Request request) throws RequestException, IOException;
}
