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

public final class Request extends RtspMessage {


    /*-- Member Variables ----------------------------------------------------*/

    private RequestLine requestLine;


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     * @param method
     * @param uri
     */
    public Request(final Method method, final URI uri) {
        this.requestLine = new RequestLine(method, uri, ProtocolVersion.RTSP_1_0);
    }
    
    Request(final RequestLine requestLine,
            final LinkedHashMap<String,Header> headers,
            final Entity entity) {
        super(headers, entity);
        this.requestLine = requestLine;
    }

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

    @Override
    public Type getType() {
        return Type.Request;
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        return this.requestLine.getProtocolVersion();
    }

    public RequestLine getRequestLine() {
        return this.requestLine;
    }

    public void setRequestLine(RequestLine requestLine) {
        this.requestLine = requestLine;
    }
   
    @Override
    protected void doWriteTo(OutputStream outstream) throws IOException {
        this.requestLine.writeTo(outstream);
        super.doWriteTo(outstream);
    }
    
}
