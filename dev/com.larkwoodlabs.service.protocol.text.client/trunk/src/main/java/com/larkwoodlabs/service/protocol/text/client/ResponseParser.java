package com.larkwoodlabs.service.protocol.text.client;

import java.io.IOException;
import java.util.LinkedHashMap;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.service.Connection;
import com.larkwoodlabs.service.protocol.text.RequestException;
import com.larkwoodlabs.service.protocol.text.entity.Entity;
import com.larkwoodlabs.service.protocol.text.handler.ResponseHandler;
import com.larkwoodlabs.service.protocol.text.message.Header;
import com.larkwoodlabs.service.protocol.text.message.Message;
import com.larkwoodlabs.service.protocol.text.message.MessageParser;
import com.larkwoodlabs.service.protocol.text.message.Response;
import com.larkwoodlabs.service.protocol.text.message.StartLine;
import com.larkwoodlabs.service.protocol.text.message.StatusLine;

public class ResponseParser extends MessageParser {

    /*-- Member Variables ----------------------------------------------------*/

    final ResponseHandler handler;
    
    public ResponseParser(final ResponseHandler handler) {
        super();
        this.handler = handler;
    }

    @Override
    protected StartLine doParseStartLine(final String line) throws ParseException {
        return StatusLine.parse(line);
    }

    @Override
    protected Message doConstructMessage(Connection connection,
                                         StartLine startLine,
                                         LinkedHashMap<String, Header> headers,
                                         Entity entity) {
        return new Response(connection, (StatusLine)startLine, headers, entity);
    }

    @Override
    protected void doHandleMessage(Message message) throws RequestException, IOException {
        this.handler.handleResponse((Response)message);
    }


}
