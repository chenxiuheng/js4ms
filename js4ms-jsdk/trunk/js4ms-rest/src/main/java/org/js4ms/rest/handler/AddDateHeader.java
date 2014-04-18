package org.js4ms.rest.handler;

import java.util.Date;

import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.header.DateHeader;
import org.js4ms.rest.message.HeaderName;
import org.js4ms.rest.message.Message;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;



public class AddDateHeader implements RequestHandler, ResponseHandler {

    public AddDateHeader() {
        
    }

    @Override
    public void handleRequest(Request request) throws RequestException {
        setHeader(request);
    }

    @Override
    public void handleResponse(Response response) {
        setHeader(response);
    }

    void setHeader(Message message) {
        message.setHeader(new DateHeader(HeaderName.DATE, new Date()));;
    }
}
