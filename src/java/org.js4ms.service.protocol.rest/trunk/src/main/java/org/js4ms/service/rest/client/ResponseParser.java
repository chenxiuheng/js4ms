package org.js4ms.service.rest.client;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.js4ms.common.exception.ParseException;
import org.js4ms.service.rest.common.RequestException;
import org.js4ms.service.rest.entity.Entity;
import org.js4ms.service.rest.handler.ResponseHandler;
import org.js4ms.service.rest.message.Message;
import org.js4ms.service.rest.message.MessageHeader;
import org.js4ms.service.rest.message.MessageHeaderParser;
import org.js4ms.service.rest.message.MessageParser;
import org.js4ms.service.rest.message.Response;
import org.js4ms.service.rest.message.StartLine;
import org.js4ms.service.rest.message.StatusLine;
import org.js4ms.service.server.Connection;




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
