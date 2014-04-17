package org.js4ms.rest.handler;

import java.io.IOException;

import org.js4ms.rest.message.Response;



public interface ResponseHandler {
    void handleResponse(Response response) throws IOException;
}
