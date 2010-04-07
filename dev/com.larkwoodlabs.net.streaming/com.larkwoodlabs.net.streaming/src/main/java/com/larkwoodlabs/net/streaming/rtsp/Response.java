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

public final class Response extends RtspMessage {


    /*-- Member Variables ----------------------------------------------------*/

    private StatusLine statusLine;


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     */
    public Response() {
        this(StatusCode.OK);
    }

    public Response(final StatusCode statusCode) {
        this.statusLine = new StatusLine(ProtocolVersion.RTSP_1_0, statusCode);
    }

    public Response(final StatusLine statusLine,
                    final LinkedHashMap<String,Header> headers,
                    final Entity entity) {
        super(headers, entity);
        this.statusLine = statusLine;
    }

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

    @Override
    public Type getType() {
        return Type.Response;
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        return this.statusLine.getProtocolVersion();
    }

    public StatusCode getStatusCode() {
        return this.statusLine.getStatusCode();
    }

    public void setStatusCode(final StatusCode statusCode) {
        this.statusLine.setStatusCode(statusCode);
    }

    public StatusLine getStatusLine() {
        return this.statusLine;
    }

    public void setStatusLine(final StatusLine statusLine) {
        this.statusLine = statusLine;
    }
    
    public void setStatus(final StatusCode statusCode, final String message) {
        setStatusCode(statusCode);
        setEntity(new StringEntity(message));
    }
    
    public void setError(final Throwable t) {
        setStatusCode(StatusCode.InternalServerError);
        String entity = "";
        for (StackTraceElement frame : t.getStackTrace()) {
            entity += frame.toString() + "\n";
        }
        setEntity(new StringEntity(entity));
    }

    @Override
    protected void doWriteTo(OutputStream outstream) throws IOException {
        this.statusLine.writeTo(outstream);
        super.doWriteTo(outstream);
    }

}
