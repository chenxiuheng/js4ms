package com.larkwoodlabs.service.protocol.text.message;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.larkwoodlabs.service.Connection;
import com.larkwoodlabs.service.protocol.text.entity.Codec;
import com.larkwoodlabs.service.protocol.text.entity.CodecManager;
import com.larkwoodlabs.service.protocol.text.entity.Entity;
import com.larkwoodlabs.service.protocol.text.entity.RawEntity;
import com.larkwoodlabs.util.logging.Log;


public abstract class Message {

    /*-- Member Variables ----------------------------------------------------*/

    protected final Log log = new Log(this);

    protected Connection connection;

    protected final LinkedHashMap<String,Header> headers;

    protected Entity entity = null;

    protected StartLine startLine = null;

    protected boolean isSent = false;


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a message with no headers and no entity.
     */
    protected Message(final Connection connection,
                      final StartLine startLine) {
        this(connection, startLine, new LinkedHashMap<String,Header>(), null);
    }

    /**
     * Constructs a message from the specified collection of message headers and entity.
     * @param headers - A collection of message headers. May be <code>null</code>.
     * @param entity - A message entity (the payload). May be <code>null</code>.
     */
    protected Message(final Connection connection,
                      final StartLine startLine, 
                      final LinkedHashMap<String,Header> headers,
                      final Entity entity) {
        this.connection = connection;
        this.startLine = startLine;
        this.headers = headers;
        this.entity = entity;
    }

    /**
     * 
     * @return
     */
    public Connection getConnection() {
        return this.connection;
    }

    /**
     * 
     * @return
     */
    public void setConnection(final Connection connection) {
        this.connection = connection;
    }

    /**
     * Indicates whether this message has been "sent".
     * This flag is initially set to <code>false</code> and is set to <code>true</code>
     * when the {@link #writeTo(OutputStream)} method is called.
     */
    public boolean isSent() {
        return this.isSent;
    }

    /**
     * Used to mark whether this message has been "sent".
     * @param isSent - The new value for the "is-sent" property.
     */
    public void isSent(boolean isSent) {
        this.isSent = isSent;
    }

    public ProtocolVersion getProtocolVersion() {
        return this.startLine.getProtocolVersion();
    }
 
    /**
     * Returns an iterator for a collection of names that identify the
     * {@link Header} objects currently attached to this message.
     */
    public Iterator<String> getHeaderNames() {
        return this.headers.keySet().iterator();
    }
    
    /**
     * Indicates whether a header with the specified name is currently attached to this messsage.
     * @param name - The name of a message header. Header names are case-insensitive.
     */
    public boolean containsHeader(final String name) {
        return this.headers.containsKey(name.toLowerCase());
    }
    
    /**
     * Returns the {@link Header} identified by the specified name if a header
     * with that name is currently attached to this message.
     * @param name - The name of a message header. Header names are case-insensitive.
     */
    public Header getHeader(final String name) {
        return this.headers.get(name.toLowerCase());
    }
    
    /**
     * Adds a header to this message.
     * If a header with the same name is already attached to this message,
     * that header is replaced.
     * @param header - The header to be set.
     */
    public void setHeader(final Header header) {
        this.headers.put(header.getName().toLowerCase(),header);
    }

    /**
     * Adds a header to this message.
     * If a header with the same name is already attached to this message,
     * the value carried by the new header is appended to the value of
     * the existing header (see {@link Header#appendValue(String)}).
     * @param header - The header to be added.
     */
    public void addHeader(final Header header) {
        Header current = this.headers.get(header.getName().toLowerCase());
        if (current != null) {
           current.appendValue(header.getValue());
        }
        else {
            this.headers.put(header.getName().toLowerCase(),header);
        }
    }

    /**
     * Removes a header from this message.
     * @param name - The name of a message header. Header names are case-insensitive.
     */
    public Header removeHeader(final String name) {
        return this.headers.remove(name.toLowerCase());
    }

    /**
     * Removes all headers from this message.
     */
    public void removeHeaders() {
        this.headers.clear();
    }

    /**
     * Gets the entity attached to this message or <code>null</code> if no entity is attached.
     */
    public Entity getEntity() {
        return this.entity;
    }
    
    /**
     * Sets or clears the entity for this message.
     * @param entity - The entity to attach to the message. May be <code>null</code>.
     */
    public void setEntity(Entity entity) {
        this.entity = entity;
        
    }

    /**
     * Consumes the entity attached to this message (if any).
     * See {@link RawEntity#consumeContent()}.
     * @throws IOException
     */
    public void consumeContent() throws IOException {
        if (this.entity != null && !this.entity.isConsumed()) {
            this.entity.consumeContent();
        }
    }

    public void log(Logger logger) {
        logger.finer(log.msg("+ logging [" + getClass().getSimpleName() + "]"));
        logger.finer(log.msg(this.startLine.toString()));
        for (Map.Entry<String, Header> entry : this.headers.entrySet()) {
            logger.finer(log.msg(entry.getValue().toString()));
        }
        if (this.entity != null) {
            this.entity.log(logger);
        }
    }

    /**
     * 
     * @throws IOException
     */
    public void send() throws IOException {
        writeTo(this.connection.getOutputStream());
    }

    /**
     * Writes this message to the specified OutputStream.
     * This method sets the "is-sent" property of the message to <code>true</code>.
     * @param outstream - The destination OutputStream for the message.
     * @throws IOException If an I/O occurs.
     */
    public final void writeTo(OutputStream outstream) throws IOException {

        // Must be synchronized to prevent simultaneous writes when data interleaving is used.
        synchronized (outstream) {
            this.startLine.writeTo(outstream);
    
            if (this.entity != null) {
    
                for (Header header : this.headers.values()) {
                    // Write all message headers except entity headers
                    if (!this.entity.isEntityHeader(header)) {
                        header.writeTo(outstream);
                    }
                }
    
                Codec codec;
                Header header = getHeader(Entity.CONTENT_ENCODING);
                if (header != null) {
                    codec = CodecManager.getManager().getCodec(header.getValue());
                }
                else {
                    codec = CodecManager.getManager().getCodec(this.entity.getContentEncoding());
                }
                this.entity.writeTo(outstream, codec);
            }
            else {
                for (Header header : this.headers.values()) {
                    header.writeTo(outstream);
                }
                outstream.write('\r');
                outstream.write('\n');
            }
    
            outstream.flush();
        }
        this.isSent = true;
    }

}
