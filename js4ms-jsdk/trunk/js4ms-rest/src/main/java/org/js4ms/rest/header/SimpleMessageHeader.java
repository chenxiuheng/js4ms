package org.js4ms.rest.header;

import org.js4ms.rest.message.MessageHeader;
import org.js4ms.rest.message.MessageHeaderBase;


public class SimpleMessageHeader extends MessageHeaderBase {

    private String value;

    public SimpleMessageHeader(final String name) {
        super(name);
    }

    public SimpleMessageHeader(final String name, final String value) {
        super(name);
        this.value = value;
    }

    public SimpleMessageHeader(final SimpleMessageHeader header) {
        super(header);
        this.value = header.value;
    }

    @Override
    public Object clone() {
        return new SimpleMessageHeader(this);
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public void appendHeader(final MessageHeader header) {
        if (this.value.length() > 0) {
            this.value += "," + header.getValue();
        }
        else {
            this.value = header.getValue();
        }
    }

}
