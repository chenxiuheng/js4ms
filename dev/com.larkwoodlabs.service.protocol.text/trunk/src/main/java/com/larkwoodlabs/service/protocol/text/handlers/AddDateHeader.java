package com.larkwoodlabs.service.protocol.text.handlers;

import java.util.Date;

import com.larkwoodlabs.service.protocol.text.MessageHeaders;
import com.larkwoodlabs.service.protocol.text.RequestException;
import com.larkwoodlabs.service.protocol.text.handler.ResponseHandler;
import com.larkwoodlabs.service.protocol.text.headers.DateHeader;
import com.larkwoodlabs.service.protocol.text.message.Message;
import com.larkwoodlabs.service.protocol.text.message.Request;
import com.larkwoodlabs.service.protocol.text.message.Response;
import com.larkwoodlabs.service.protocol.text.handler.RequestHandler;

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
