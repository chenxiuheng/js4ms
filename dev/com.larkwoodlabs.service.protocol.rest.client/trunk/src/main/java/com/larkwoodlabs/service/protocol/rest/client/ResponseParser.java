package com.larkwoodlabs.service.protocol.rest.client;

import java.io.IOException;
import java.util.LinkedHashMap;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.service.Connection;
import com.larkwoodlabs.service.protocol.rest.RequestException;
import com.larkwoodlabs.service.protocol.rest.entity.Entity;
import com.larkwoodlabs.service.protocol.rest.handler.ResponseHandler;
import com.larkwoodlabs.service.protocol.rest.message.Message;
import com.larkwoodlabs.service.protocol.rest.message.MessageHeader;
import com.larkwoodlabs.service.protocol.rest.message.MessageHeaderParser;
import com.larkwoodlabs.service.protocol.rest.message.MessageParser;
import com.larkwoodlabs.service.protocol.rest.message.Response;
import com.larkwoodlabs.service.protocol.rest.message.StartLine;
import com.larkwoodlabs.service.protocol.rest.message.StatusLine;

public class ResponseParser extends MessageParser {

    /*-- Member Variables ----------------------------------------------------*/

    final ResponseHandler handler;
    
    public ResponseParser(final MessageHeaderParser headerParser,
                          final ResponseHandler handler) {
        super(headerParser);
        this.handler = handler;
    }

    @Override
    protected StartLine doParseStartLine(final String line) throws ParseException {
        return StatusLine.parse(line);
    }

    @Override
    protected Message doConstructMessage(Connection connection,
                                         StartLine startLine,
                                         LinkedHashMap<String, MessageHeader> headers,
                                         Entity entity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void doHandleMessage(Message message) throws RequestException, IOException {
        this.handler.handleResponse((Response)message);
    }


}