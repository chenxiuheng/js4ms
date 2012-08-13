package org.js4ms.service.protocol.rest.handler;

import java.io.IOException;

import org.js4ms.service.protocol.rest.message.Response;


public interface ResponseHandler {
    void handleResponse(Response response) throws IOException;
}
