package com.larkwoodlabs.service.protocol.text.message;

import java.util.LinkedHashMap;

import com.larkwoodlabs.service.Connection;
import com.larkwoodlabs.service.protocol.text.entity.Entity;

public class Response extends Message {

    /**
     * @param connection - The connection on which the message is to be sent.
     * @param statusLine
     */
    public Response(Connection connection,
                    StatusLine statusLine) {
        super(connection, statusLine);
    }

    /**
     * Constructs a response message with the specified status line, headers, and entity.
     * @param connection - The connection on which the message is to be sent.
     * @param statusLine - A representation of the first line in the response message
     *                     (consisting of the protocol version, status code and reason phrase).
     * @param headers - A collection of response message headers.
     * @param entity - The message entity or payload. May be null.
     */
    public Response(final Connection connection,
                    final StatusLine statusLine,
                    final LinkedHashMap<String,MessageHeader> messageHeaders,
                    final Entity entity) {
        super(connection, statusLine, messageHeaders, entity);
    }

    /**
     * Returns a representation of the first line, or message header, for this response.
     */
    public StatusLine getStatusLine() {
        return (StatusLine)this.startLine;
    }

    /**
     * Sets the first line, or message header, for this response.
     */
    public void setStatusLine(StatusLine statusLine) {
        this.startLine = statusLine;
    }

    /**
     * Returns status code of by this response.
     */
    public Status getStatus() {
        return getStatusLine().getStatus();
    }

    /**
     * Sets the status code of this response.
     */
    public void setStatus(Status status) {
        getStatusLine().setStatus(status);
    }
}
