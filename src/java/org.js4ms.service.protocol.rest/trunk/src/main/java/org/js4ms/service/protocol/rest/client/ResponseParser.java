package org.js4ms.service.protocol.rest.client;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.js4ms.exceptions.ParseException;
import org.js4ms.service.protocol.rest.RequestException;
import org.js4ms.service.protocol.rest.entity.Entity;
import org.js4ms.service.protocol.rest.handler.ResponseHandler;
import org.js4ms.service.protocol.rest.message.Message;
import org.js4ms.service.protocol.rest.message.MessageHeader;
import org.js4ms.service.protocol.rest.message.MessageHeaderParser;
import org.js4ms.service.protocol.rest.message.MessageParser;
import org.js4ms.service.protocol.rest.message.Response;
import org.js4ms.service.protocol.rest.message.StartLine;
import org.js4ms.service.protocol.rest.message.StatusLine;
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
