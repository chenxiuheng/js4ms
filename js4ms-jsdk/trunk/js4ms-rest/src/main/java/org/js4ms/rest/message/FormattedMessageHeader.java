package org.js4ms.rest.message;

import org.js4ms.common.exception.ParseException;

public abstract class FormattedMessageHeader extends MessageHeaderBase {

    protected FormattedMessageHeader(final String name) {
        super(name);
    }

    protected FormattedMessageHeader(final String name, final String value) throws IllegalArgumentException {
        super(name);
        setValue(value);
    }

    protected FormattedMessageHeader(final FormattedMessageHeader header) {
        super(header);
    }

    @Override
    public String getValue() {
        return format();
    }

    @Override
    public void setValue(final String value) throws IllegalArgumentException {
        try {
            parse(value);
        }
        catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected abstract void parse(final String value) throws ParseException;

    protected abstract String format();
}
