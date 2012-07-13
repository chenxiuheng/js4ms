package com.larkwoodlabs.service.protocol.restful.handlers;

import java.util.Date;

import com.larkwoodlabs.service.protocol.restful.MessageHeaders;
import com.larkwoodlabs.service.protocol.restful.RequestException;
import com.larkwoodlabs.service.protocol.restful.handler.RequestHandler;
import com.larkwoodlabs.service.protocol.restful.handler.ResponseHandler;
import com.larkwoodlabs.service.protocol.restful.headers.DateHeader;
import com.larkwoodlabs.service.protocol.restful.message.Message;
import com.larkwoodlabs.service.protocol.restful.message.Request;
import com.larkwoodlabs.service.protocol.restful.message.Response;

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
