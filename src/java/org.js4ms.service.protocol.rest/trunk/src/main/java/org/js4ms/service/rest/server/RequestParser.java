package org.js4ms.service.rest.server;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.js4ms.common.exception.ParseException;
import org.js4ms.server.Connection;
import org.js4ms.service.rest.common.RequestException;
import org.js4ms.service.rest.entity.Entity;
import org.js4ms.service.rest.handler.RequestHandler;
import org.js4ms.service.rest.message.Message;
import org.js4ms.service.rest.message.MessageHeader;
import org.js4ms.service.rest.message.MessageHeaderParser;
import org.js4ms.service.rest.message.MessageParser;
import org.js4ms.service.rest.message.Request;
import org.js4ms.service.rest.message.RequestLine;
import org.js4ms.service.rest.message.StartLine;




public class RequestParser extends MessageParser {


    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    final RequestHandler handler;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * @param headerParser - A message header parser to use during message parsing.
     * @param connection
     * @param handler
     */
    public RequestParser(final MessageHeaderParser headerParser, final RequestHandler handler) {
        super(headerParser);
        this.handler = handler;
    }

    @Override
    protected StartLine doParseStartLine(final String line) throws ParseException {
        return RequestLine.parse(line);
    }

    @Override
    protected Message doConstructMessage(Connection connection,
                                         StartLine startLine,
                                         LinkedHashMap<String, MessageHeader> headers,
                                         Entity entity) {
        return new Request(connection, (RequestLine)startLine, headers, entity);
    }

    @Override
    protected void doHandleMessage(Message message) throws RequestException, IOException {
        this.handler.handleRequest((Request)message);
    }

}
