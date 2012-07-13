package com.larkwoodlabs.service.protocol.restful.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.logging.Logger;

import com.larkwoodlabs.service.protocol.restful.message.MessageHeader;


public interface Entity {

    public static final String CONTENT_BASE = "Content-Base";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String CONTENT_LANGUAGE = "Content-Language";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_LOCATION = "Content-Location";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String EXPIRES = "Expires";
    public static final String LAST_MODIFIED = "Last-Modified";

    InputStream getContent(final Codec codec) throws IOException;
    String getContentBase();
    String getContentDisposition();
    String getContentEncoding();
    String getContentLanguage();
    int getContentLength();
    String getContentLocation();
    String getContentType();

    Date getExpires();
    Date getLastModified();

    void setExpires(final Date date);
    void setLastModified(final Date date);

    boolean isEntityHeader(final MessageHeader header);

    /**
     * Writes this entity to the specified OutputStream.
     * @param outstream - The output stream that will receive the entity.
     * @throws IOException
     */
    public void writeTo(final OutputStream outstream, final Codec codec) throws IOException;

    public void consumeContent() throws IOException;

    public boolean isConsumed();

    public void ignoreContent();

    public void log(final Logger log);
}
