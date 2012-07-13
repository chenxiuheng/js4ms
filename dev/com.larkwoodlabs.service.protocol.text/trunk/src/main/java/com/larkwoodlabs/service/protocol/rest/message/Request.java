package com.larkwoodlabs.service.protocol.rest.message;

import java.net.URI;
import java.util.LinkedHashMap;

import com.larkwoodlabs.service.Connection;
import com.larkwoodlabs.service.protocol.rest.entity.Entity;

/**
 * A request message.
 *
 * @author Gregory Bumgardner
 */
public class Request extends Message {

    
    /**
     * Constructs a request message for the specified method and URI reference.
     * @param connection - The connection from which the message was received.
     * @param method - The request method.
     * @param uri - A resource or control URI.
     */
    public Request(final Connection connection,
                   final ProtocolVersion protocolVersion,
                   final Method method,
                   final URI uri) {
        this(connection, new RequestLine(method, uri, protocolVersion));
    }

    /**
     * Constructs a request message with the specified request line.
     * @param connection - The connection from which the message was received.
     * @param requestLine - The request line.
     */
    public Request(final Connection connection,
                   final RequestLine requestLine) {
        super(connection, requestLine);
    }

    /**
     * Protected constructor.
     * @param connection - The connection f which the message was received.
     * @param requestLine - A representation of the first line in the request message
     *                      (the message method, URI, and protocol version).
     * @param messageHeaders - A collection of request message headers.
     * @param entity - The message entity or payload. May be null.
     */
    public Request(final Connection connection,
                   final RequestLine requestLine,
                   final LinkedHashMap<String,MessageHeader> messageHeaders,
                   final Entity entity) {
        super(connection, requestLine, messageHeaders, entity);
    }

    /**
     * Returns a representation of the first line, or message header, for this request.
     */
    public RequestLine getRequestLine() {
        return (RequestLine)this.startLine;
    }

    /**
     * Sets the first line, or message header, for this request.
     */
    public void setRequestLine(RequestLine requestLine) {
        this.startLine = requestLine;
    }

}
