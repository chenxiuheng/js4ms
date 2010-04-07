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
 * 
 *
 *
 * @author Gregory Bumgardner
 */
public abstract class RtspMessage {

    /*-- Inner Classes -------------------------------------------------------*/

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
     * 
     * @param headers
     * @param entity
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

    protected RtspMessage() {
        this.headers = new LinkedHashMap<String,Header>();
        this.entity = null;
    }
    
    public abstract Type getType();
    
    public abstract ProtocolVersion getProtocolVersion();
    
    public ConnectionHandler getConnectionHandler() {
        return this.handler;
    }

    public void setConnectionHandler(ConnectionHandler handler) {
        this.handler = handler;
    }

    public boolean isSent() {
        return this.isSent;
    }

    public void isSent(boolean isSent) {
        this.isSent = isSent;
    }

    /**
     * 
     * @return
     */
    public Iterator<String> getHeaderNames() {
        return this.headers.keySet().iterator();
    }
    
    /**
     * 
     * @param name
     * @return
     */
    public boolean containsHeader(final String name) {
        return this.headers.containsKey(name.toLowerCase());
    }
    
    /**
     * 
     * @param name
     * @return
     */
    public Header getHeader(final String name) {
        return this.headers.get(name.toLowerCase());
    }
    
    /**
     * Adds a header to this message.
     * If a header with the same name is already attached to this message,
     * that header is replaced.
     * @param header
     */
    public void setHeader(final Header header) {
        this.headers.put(header.getName().toLowerCase(),header);
    }

    /**
     * Adds a header to this message.
     * If a header with the same name is already attached to this message,
     * the value carried by the new header is appended to the value of
     * the existing header.
     * @param header
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

    public void removeHeader(final String name) {
        this.headers.remove(name.toLowerCase());
    }

    public void removeHeaders() {
        this.headers.clear();
    }

    /**
     * 
     * @return
     */
    public Entity getEntity() {
        return this.entity;
    }
    
    /**
     * 
     * @param entity
     */
    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public void consumeContent() throws IOException {
        if (this.entity != null && !this.entity.isConsumed()) {
            this.entity.consumeContent();
        }
    }

    public final void writeTo(OutputStream outstream) throws IOException {
        doWriteTo(outstream);
        this.isSent = true;
    }

    /**
     * 
     * @param outstream
     * @throws IOException
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
