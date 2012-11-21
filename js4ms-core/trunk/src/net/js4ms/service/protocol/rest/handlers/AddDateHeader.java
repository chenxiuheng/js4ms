package net.js4ms.service.protocol.rest.handlers;

import java.util.Date;

import net.js4ms.service.protocol.rest.MessageHeaders;
import net.js4ms.service.protocol.rest.RequestException;
import net.js4ms.service.protocol.rest.handler.RequestHandler;
import net.js4ms.service.protocol.rest.handler.ResponseHandler;
import net.js4ms.service.protocol.rest.headers.DateHeader;
import net.js4ms.service.protocol.rest.message.Message;
import net.js4ms.service.protocol.rest.message.Request;
import net.js4ms.service.protocol.rest.message.Response;


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
