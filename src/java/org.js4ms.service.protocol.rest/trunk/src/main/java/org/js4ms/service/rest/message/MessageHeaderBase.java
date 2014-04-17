package org.js4ms.service.rest.message;

import java.io.IOException;
import java.io.OutputStream;


public abstract class MessageHeaderBase implements MessageHeader, Cloneable {

    private final String name;
    
    protected MessageHeaderBase(final String name) {
        this.name = name;
    }

    protected MessageHeaderBase(final MessageHeader header) {
        this.name = header.getName();
    }

    public abstract Object clone();

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return getName() + ": " + getValue();
    }

    @Override
    public void writeTo(final OutputStream outstream) throws IOException {
        outstream.write(toString().getBytes("UTF8"));
        outstream.write('\r');
        outstream.write('\n');
    }
}
