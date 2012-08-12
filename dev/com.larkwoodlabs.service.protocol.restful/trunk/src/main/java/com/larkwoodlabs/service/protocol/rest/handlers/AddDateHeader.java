package com.larkwoodlabs.service.protocol.rest.handlers;

import java.util.Date;

import com.larkwoodlabs.service.protocol.rest.MessageHeaders;
import com.larkwoodlabs.service.protocol.rest.RequestException;
import com.larkwoodlabs.service.protocol.rest.handler.RequestHandler;
import com.larkwoodlabs.service.protocol.rest.handler.ResponseHandler;
import com.larkwoodlabs.service.protocol.rest.headers.DateHeader;
import com.larkwoodlabs.service.protocol.rest.message.Message;
import com.larkwoodlabs.service.protocol.rest.message.Request;
import com.larkwoodlabs.service.protocol.rest.message.Response;

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
