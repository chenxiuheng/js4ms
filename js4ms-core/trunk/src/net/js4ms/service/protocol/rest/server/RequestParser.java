package net.js4ms.service.protocol.rest.server;

import java.io.IOException;
import java.util.LinkedHashMap;

import net.js4ms.common.exceptions.ParseException;
import net.js4ms.service.Connection;
import net.js4ms.service.protocol.rest.RequestException;
import net.js4ms.service.protocol.rest.entity.Entity;
import net.js4ms.service.protocol.rest.handler.RequestHandler;
import net.js4ms.service.protocol.rest.message.Message;
import net.js4ms.service.protocol.rest.message.MessageHeader;
import net.js4ms.service.protocol.rest.message.MessageHeaderParser;
import net.js4ms.service.protocol.rest.message.MessageParser;
import net.js4ms.service.protocol.rest.message.Request;
import net.js4ms.service.protocol.rest.message.RequestLine;
import net.js4ms.service.protocol.rest.message.StartLine;



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
