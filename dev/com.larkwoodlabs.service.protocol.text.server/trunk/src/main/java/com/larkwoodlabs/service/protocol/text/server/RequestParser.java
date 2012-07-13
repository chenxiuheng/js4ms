package com.larkwoodlabs.service.protocol.text.server;

import java.io.IOException;
import java.util.LinkedHashMap;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.service.Connection;
import com.larkwoodlabs.service.protocol.restful.RequestException;
import com.larkwoodlabs.service.protocol.restful.entity.Entity;
import com.larkwoodlabs.service.protocol.restful.handler.RequestHandler;
import com.larkwoodlabs.service.protocol.restful.message.Message;
import com.larkwoodlabs.service.protocol.restful.message.MessageHeader;
import com.larkwoodlabs.service.protocol.restful.message.MessageHeaderParser;
import com.larkwoodlabs.service.protocol.restful.message.MessageParser;
import com.larkwoodlabs.service.protocol.restful.message.Request;
import com.larkwoodlabs.service.protocol.restful.message.RequestLine;
import com.larkwoodlabs.service.protocol.restful.message.StartLine;

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
