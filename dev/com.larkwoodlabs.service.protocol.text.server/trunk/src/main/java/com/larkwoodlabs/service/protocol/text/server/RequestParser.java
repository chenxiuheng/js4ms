package com.larkwoodlabs.service.protocol.text.server;

import java.io.IOException;
import java.util.LinkedHashMap;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.service.Connection;
import com.larkwoodlabs.service.protocol.text.RequestException;
import com.larkwoodlabs.service.protocol.text.entity.Entity;
import com.larkwoodlabs.service.protocol.text.handler.RequestHandler;
import com.larkwoodlabs.service.protocol.text.message.Header;
import com.larkwoodlabs.service.protocol.text.message.Message;
import com.larkwoodlabs.service.protocol.text.message.MessageParser;
import com.larkwoodlabs.service.protocol.text.message.Request;
import com.larkwoodlabs.service.protocol.text.message.RequestLine;
import com.larkwoodlabs.service.protocol.text.message.StartLine;

public class RequestParser extends MessageParser {


    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    final RequestHandler handler;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     * @param connection
     * @param handler
     */
    public RequestParser(final RequestHandler handler) {
        super();
        this.handler = handler;
    }

    @Override
    protected StartLine doParseStartLine(final String line) throws ParseException {
        return RequestLine.parse(line);
    }

    @Override
    protected Message doConstructMessage(Connection connection,
                                         StartLine startLine,
                                         LinkedHashMap<String, Header> headers,
                                         Entity entity) {
        return new Request(connection, (RequestLine)startLine, headers, entity);
    }

    @Override
    protected void doHandleMessage(Message message) throws RequestException, IOException {
        this.handler.handleRequest((Request)message);
    }

}