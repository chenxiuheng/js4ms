package com.larkwoodlabs.service.protocol.rest.headers;

import com.larkwoodlabs.service.protocol.rest.entity.Entity;
import com.larkwoodlabs.service.protocol.rest.entity.MediaType;
import com.larkwoodlabs.service.protocol.rest.message.FormattedMessageHeader;
import com.larkwoodlabs.service.protocol.rest.message.MessageHeader;

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
