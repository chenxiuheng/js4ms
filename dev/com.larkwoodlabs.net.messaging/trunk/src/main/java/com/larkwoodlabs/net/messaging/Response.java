package com.larkwoodlabs.net.messaging;

import java.util.LinkedHashMap;

public class Response extends Message {

    protected Response(StatusLine statusLine) {
        super(statusLine);
    }

    /**
     * Constructs a response message with the specified status line, headers, and entity.
     * @param statusLine - A representation of the first line in the response message
     *                     (consisting of the protocol version, status code and reason phrase).
     * @param headers - A collection of response message headers.
     * @param entity - The message entity or payload. May be null.
     */
    public Response(final StatusLine statusLine,
                    final LinkedHashMap<String,Header> messageHeaders,
                    final Entity entity) {
        super(statusLine, messageHeaders, entity);
    }
}
