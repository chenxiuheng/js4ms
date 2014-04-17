package org.js4ms.service.rest.handler;

import java.util.Date;

import org.js4ms.service.protocol.rest.common.RequestException;
import org.js4ms.service.protocol.rest.header.DateHeader;
import org.js4ms.service.protocol.rest.message.HeaderName;
import org.js4ms.service.protocol.rest.message.Message;
import org.js4ms.service.protocol.rest.message.Request;
import org.js4ms.service.protocol.rest.message.Response;



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
