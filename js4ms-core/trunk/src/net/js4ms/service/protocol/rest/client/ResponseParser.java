package net.js4ms.service.protocol.rest.client;

import java.io.IOException;
import java.util.LinkedHashMap;

import net.js4ms.common.exceptions.ParseException;
import net.js4ms.service.Connection;
import net.js4ms.service.protocol.rest.RequestException;
import net.js4ms.service.protocol.rest.entity.Entity;
import net.js4ms.service.protocol.rest.handler.ResponseHandler;
import net.js4ms.service.protocol.rest.message.Message;
import net.js4ms.service.protocol.rest.message.MessageHeader;
import net.js4ms.service.protocol.rest.message.MessageHeaderParser;
import net.js4ms.service.protocol.rest.message.MessageParser;
import net.js4ms.service.protocol.rest.message.Response;
import net.js4ms.service.protocol.rest.message.StartLine;
import net.js4ms.service.protocol.rest.message.StatusLine;



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
