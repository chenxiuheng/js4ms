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
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Logging;

/**
 * An RTSP response message.
 * See [<a href="http://tools.ietf.org/html/rfc2326#page-22">RFC-2326, Section 7</a>].
 *
 * @author Gregory Bumgardner
 */
public final class Response extends RtspMessage {


    /*-- Member Variables ----------------------------------------------------*/

    private StatusLine statusLine;


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs an RTSP/1.0 response with a status code of {@link StatusCode#OK OK}.
     */
    public Response() {
        this(StatusCode.OK);
    }

    /**
     * Constructs an RTSP/1.0 response with the specified status code.
     * @param statusCode - A {@link StatusCode} value.
     */
    public Response(final StatusCode statusCode) {
        this.statusLine = new StatusLine(ProtocolVersion.RTSP_1_0, statusCode);
    }

    /**
     * Constructs a response message with the specified status line, headers, and entity.
     * @param statusLine - A representation of the first line in the response message
     *                     (consisting of the protocol version, status code and reason phrase).
     * @param headers - A collection of response message headers.
     * @param entity - The message entity or payload. May be null.
     */
    public Response(final StatusLine statusLine,
                    final LinkedHashMap<String,Header> headers,
                    final Entity entity) {
        super(headers, entity);
        this.statusLine = statusLine;
    }

    /**
     * Constructs a response message with the specified status line, headers, and entity.
     * @param handler - The handler that received this response.
     * @param statusLine - A representation of the first line in the response message
     *                     (consisting of the protocol version, status code and reason phrase).
     * @param headers - A collection of response message headers.
     * @param entity - The message entity or payload. May be null.
     */
    public Response(final ConnectionHandler handler,
                    final StatusLine statusLine,
                    final LinkedHashMap<String,Header> headers,
                    final Entity entity) {
        super(handler, headers, entity);
        this.statusLine = statusLine;
    }

    public void log(final Logger logger) {
        final String ObjectId = Logging.identify(this);
        logger.info(ObjectId + " " + this.statusLine.toString());
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
        return Type.Response;
    }

    /**
     * Returns the {@link ProtocolVersion} of this response.
     */
    @Override
    public ProtocolVersion getProtocolVersion() {
        return this.statusLine.getProtocolVersion();
    }

    /**
     * Gets the {@link StatusCode} for this response.
     */
    public StatusCode getStatusCode() {
        return this.statusLine.getStatusCode();
    }

    /**
     * Sets the {@link StatusCode} for this response.
     * @param statusCode - The new status code.
     */
    public void setStatusCode(final StatusCode statusCode) {
        this.statusLine.setStatusCode(statusCode);
    }

    /**
     * Gets the {@link StatusLine} for this response.
     */
    public StatusLine getStatusLine() {
        return this.statusLine;
    }

    /**
     * Sets the {@link StatusLine} for this response.
     * @param statusLine - The new status line.
     */
    public void setStatusLine(final StatusLine statusLine) {
        this.statusLine = statusLine;
    }
    
    /**
     * Sets the {@link StatusCode} and string entity for this response.
     * @param statusCode - The new status code.
     * @param message - A message that will be attached as the request payload.
     */
    public void setStatus(final StatusCode statusCode, final String message) {
        setStatusCode(statusCode);
        setEntity(new StringEntity(message));
    }
    
    /**
     * Sets the response status to {@link StatusCode#InternalServerError} and 
     * attaches a stack trace from the specified Throwable as the response entity.
     * @param t - A Throwable whose stack trace will be attached as the response payload.
     */
    public void setError(final Throwable t) {
        setStatusCode(StatusCode.InternalServerError);
        String entity = "";
        for (StackTraceElement frame : t.getStackTrace()) {
            entity += frame.toString() + "\n";
        }
        setEntity(new StringEntity(entity));
    }

    /**
     * Writes this response to the specified OutputStream.
     * Used to serialize the response for transmission.
     * @param outstream - The destination OutputStream for the response.
     * @throws IOException If an I/O occurs.
     */
    @Override
    protected void doWriteTo(OutputStream outstream) throws IOException {
        this.statusLine.writeTo(outstream);
        super.doWriteTo(outstream);
    }

}
