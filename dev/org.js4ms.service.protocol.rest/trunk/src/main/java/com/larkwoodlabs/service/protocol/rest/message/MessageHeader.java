package com.larkwoodlabs.service.protocol.rest.message;

import java.io.IOException;
import java.io.OutputStream;

public interface MessageHeader extends Cloneable {

    public interface Factory {
        public String getHeaderName();
        public MessageHeader construct(final String value);
    }

    public Object clone();

    public String getName();

    public String getValue();

    public void setValue(final String value) throws IllegalArgumentException;

    public void appendHeader(final MessageHeader header) throws IllegalArgumentException;

    public String toString();

    /**
     * Writes the header to the specified OutputStream.
     * @param outstream - The destination OutputStream
     */
    public void writeTo(final OutputStream outstream) throws IOException;
}
