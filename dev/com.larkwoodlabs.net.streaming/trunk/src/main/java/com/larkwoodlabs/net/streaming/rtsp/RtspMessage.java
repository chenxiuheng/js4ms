/*
 * Copyright © 2009-2010 Larkwood Labs Software.
 *
 * Licensed under the Larkwood Labs Software Source Code License, Version 1.0.
 * You may not use this file except in compliance with this License.
 *
 * You may view the Source Code License at
 * http://www.larkwoodlabs.com/source-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the license.
 */

package com.larkwoodlabs.net.streaming.rtsp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Base class for RTSP request and response messages.
 *
 * @author Gregory Bumgardner
 */
public abstract class RtspMessage {

    /*-- Inner Classes -------------------------------------------------------*/

    /**
     * An enumeration of RTSP message types.
     */
    enum Type {
        Unknown,
        Request,
        Response
    }


    /*-- Member Variables ----------------------------------------------------*/

    final LinkedHashMap<String,Header> headers;

    protected Entity entity;

    protected ConnectionHandler handler = null;
    
    protected boolean isSent = false;
    

    /*-- Member Functions ----------------------------------------------------*/
    
    /**
     * Constructs an RTSP message from the specified collection of message headers and entity.
     * @param headers - A collection of message headers. May be <code>null</code>.
     * @param entity - A message entity (the payload). May be <code>null</code>.
     */
    protected RtspMessage(final LinkedHashMap<String,Header> headers, final Entity entity) {
        if (headers == null) {
            this.headers = new LinkedHashMap<String,Header>();
        }
        else {
            this.headers = headers;
        }
        this.entity = entity;
    }
    
    /**
     * Constructs an RTSP message from the specified collection of message headers and entity.
     * @param handler - The connection handler that received this message.
     * @param headers - A collection of message headers. May be <code>null</code>.
     * @param entity - A message entity (the payload). May be <code>null</code>.
     */
    protected RtspMessage(final ConnectionHandler handler,
                          final LinkedHashMap<String,Header> headers,
                          final Entity entity) {
        this.handler = handler;
        if (headers == null) {
            this.headers = new LinkedHashMap<String,Header>();
        }
        else {
            this.headers = headers;
        }
        this.entity = entity;
    }

    /**
     * Constructs an RTSP message with no headers and no entity.
     */
    protected RtspMessage() {
        this.headers = new LinkedHashMap<String,Header>();
        this.entity = null;
    }
    
    /**
     * Returns the message {@link Type}.
     */
    public abstract Type getType();

    /**
     * Gets the {@link ProtocolVersion} of this message.
     * @return
     */
    public abstract ProtocolVersion getProtocolVersion();
    
    /**
     * Gets the {@link ConnectionHandler} that received this message, or <code>null</code> if not set.
     */
    public ConnectionHandler getConnectionHandler() {
        return this.handler;
    }

    /**
     * Sets the {@link ConnectionHandler} that received this message.
     * @param handler - The new connection handler.
     */
    public void setConnectionHandler(ConnectionHandler handler) {
        this.handler = handler;
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
    public void removeHeader(final String name) {
        this.headers.remove(name.toLowerCase());
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
     * See {@link Entity#consumeContent()}.
     * @throws IOException
     */
    public void consumeContent() throws IOException {
        if (this.entity != null && !this.entity.isConsumed()) {
            this.entity.consumeContent();
        }
    }

    /**
     * Writes this message to the specified OutputStream.
     * This method sets the "is-sent" property of the message to <code>true</code>.
     * @param outstream - The destination OutputStream for the message.
     * @throws IOException If an I/O occurs.
     */
    public final void writeTo(OutputStream outstream) throws IOException {
        doWriteTo(outstream);
        this.isSent = true;
    }

    /**
     * Writes this message to the specified OutputStream.
     * Override in derived class to handle message specific output (the first line).
     * Derived class implementations must call this method after writing the
     * first line so any headers or entity attached to the message can be written
     * to the stream.
     * @param outstream - The destination OutputStream for the message.
     * @throws IOException If an I/O occurs.
     */
    protected void doWriteTo(OutputStream outstream) throws IOException {

        if (this.entity != null) {

            String contentType = this.entity.getContentType();
            if (contentType != null) {
                Header header = new Header(Header.CONTENT_TYPE, contentType);
                setHeader(header);
            }

            String contentEncoding = this.entity.getContentEncoding();
            if (contentEncoding != null) {
                Header header = new Header(Header.CONTENT_ENCODING, contentEncoding);
                setHeader(header);
            }

            int contentLength = this.entity.getContentLength();
            if (contentLength >= 0) {
                Header header = new Header(Header.CONTENT_LENGTH, String.valueOf(contentLength));
                setHeader(header);
            }

        }

        for (Map.Entry<String, Header> entry : this.headers.entrySet()) {
            entry.getValue().writeTo(outstream);
        }

        outstream.write('\r');
        outstream.write('\n');
        
        if (this.entity != null) {
            this.entity.writeTo(outstream);
        }

        outstream.flush();
    } 
}
