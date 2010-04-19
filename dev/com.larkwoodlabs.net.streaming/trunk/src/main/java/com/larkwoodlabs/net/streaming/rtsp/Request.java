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
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Logging;

/**
 * An RTSP request message.
 * See [<a href="http://tools.ietf.org/html/rfc2326#page-20">RFC-2326, Section 6</a>].
 *
 * @author Gregory Bumgardner
 */
public final class Request extends RtspMessage {

    /*-- Member Variables ----------------------------------------------------*/

    private RequestLine requestLine;


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs an RTSP request message for the specified method and URI reference.
     * @param method - The type of RTSP request.
     * @param uri - A resource or control URI.
     */
    public Request(final Method method, final URI uri) {
        this.requestLine = new RequestLine(method, uri, ProtocolVersion.RTSP_1_0);
    }
    
    /**
     * Private constructor.
     * @param requestLine - A representation of the first line in the request message
     *                      (the message method, URI, and protocol version).
     * @param headers - A collection of request message headers.
     * @param entity - The message entity or payload. May be null.
     */
    Request(final RequestLine requestLine,
            final LinkedHashMap<String,Header> headers,
            final Entity entity) {
        super(headers, entity);
        this.requestLine = requestLine;
    }

    /**
     * Private constructor.
     * @param handler - The connection handler that received this request.
     * @param requestLine - A representation of the first line in the request message
     *                      (the message method, URI, and protocol version).
     * @param headers - A collection of request message headers.
     * @param entity - The request entity or message payload. May be null.
     */
    Request(final ConnectionHandler handler,
            final RequestLine requestLine,
            final LinkedHashMap<String,Header> headers,
            final Entity entity) {
        super(handler, headers, entity);
        this.requestLine = requestLine;
    }

    public void log(final Logger logger) {
        final String ObjectId = Logging.identify(this);
        logger.info(ObjectId + " " + this.requestLine.toString());
        for (Header header : this.headers.values()) {
            logger.info(ObjectId + " " + header.toString());
        }
        if (this.entity != null) {
            logger.info(ObjectId + " ----> Entity");
            this.entity.log(logger);
            logger.info(ObjectId + " <---- Entity");
        }
    }

    /**
     * Returns the message {@link RtspMessage.Type Type}.
     */
    @Override
    public Type getType() {
        return Type.Request;
    }

    /**
     * Returns the {@link ProtocolVersion} of this request.
     */
    @Override
    public ProtocolVersion getProtocolVersion() {
        return this.requestLine.getProtocolVersion();
    }

    /**
     * Returns a representation of the first line, or message header, for this request.
     */
    public RequestLine getRequestLine() {
        return this.requestLine;
    }

    /**
     * Sets the first line, or message header, for this request.
     */
    public void setRequestLine(RequestLine requestLine) {
        this.requestLine = requestLine;
    }
   
    /**
     * Writes this request to the specified OutputStream.
     * Used to serialize the request for transmission.
     * @param outstream - The destination OutputStream for the request.
     * @throws IOException If an I/O occurs.
     */
    @Override
    protected void doWriteTo(OutputStream outstream) throws IOException {
        this.requestLine.writeTo(outstream);
        super.doWriteTo(outstream);
    }
    
}
