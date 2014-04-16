package org.js4ms.service.protocol.rest.handler.header;

import java.util.Date;

import org.js4ms.service.protocol.rest.MessageHeaders;
import org.js4ms.service.protocol.rest.RequestException;
import org.js4ms.service.protocol.rest.handler.RequestHandler;
import org.js4ms.service.protocol.rest.handler.ResponseHandler;
import org.js4ms.service.protocol.rest.header.DateHeader;
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
        message.setHeader(new DateHeader(MessageHeaders.DATE, new Date()));;
    }
}
