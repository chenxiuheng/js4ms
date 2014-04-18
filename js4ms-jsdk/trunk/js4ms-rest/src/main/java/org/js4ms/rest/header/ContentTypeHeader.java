package org.js4ms.rest.header;

import org.js4ms.rest.entity.Entity;
import org.js4ms.rest.entity.MediaType;
import org.js4ms.rest.message.FormattedMessageHeader;
import org.js4ms.rest.message.MessageHeader;

public class ContentTypeHeader
                extends FormattedMessageHeader {

    private MediaType mediaType;

    public ContentTypeHeader(final MediaType mediaType) {
        super(Entity.CONTENT_TYPE);
        this.mediaType = mediaType;
    }

    public ContentTypeHeader(final ContentTypeHeader header) {
        super(header);
        this.mediaType = header.mediaType;
    }

    @Override
    public void appendHeader(MessageHeader header) throws IllegalArgumentException {
        // TODO Auto-generated method stub

    }

    @Override
    protected void parse(String value) {
        this.mediaType = MediaType.parse(value);
    }

    @Override
    protected String format() {
        return this.mediaType.toString();
    }

    @Override
    public Object clone() {
        return new ContentTypeHeader(this);
    }

}
