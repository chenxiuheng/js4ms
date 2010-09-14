package com.larkwoodlabs.net.messaging;

import java.util.LinkedHashMap;

/**
 * A request message.
 *
 * @author Gregory Bumgardner
 */
public class Request extends Message {

    protected Request(RequestLine requestLine) {
        super(requestLine);
    }

    /**
     * Protected constructor.
     * @param requestLine - A representation of the first line in the request message
     *                      (the message method, URI, and protocol version).
     * @param messageHeaders - A collection of request message headers.
     * @param entity - The message entity or payload. May be null.
     */
    Request(final RequestLine requestLine,
            final LinkedHashMap<String,Header> messageHeaders,
            final Entity entity) {
        super(requestLine, messageHeaders, entity);
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
