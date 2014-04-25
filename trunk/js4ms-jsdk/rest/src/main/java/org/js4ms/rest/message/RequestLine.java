package org.js4ms.rest.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * RequestLine.java [org.js4ms.jsdk:rest]
 * %%
 * Copyright (C) 2009 - 2014 Cisco Systems, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.js4ms.common.exception.ParseException;



/**
 * The first line of a request message consisting of a method, URI and protocol version. 
 *
 * @author Gregory Bumgardner
 */
public final class RequestLine extends StartLine {
    
    /*-- Static Constants ----------------------------------------------------*/

    /**
     * Regular expression used to parse the request line into a method, URI and protocol version.
     */
    public static Pattern pattern = Pattern.compile("([A-Z_]+)[ ]+([^ ]+) ([A-Z]+)/([0-9])\\.([0-9])");
    

    /*-- Member Variables ----------------------------------------------------*/

    Method method;

    URI uri;
    

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a request line instance from the first line of a request message.
     * @param bytes - A byte array containing a UTF-8 encoded string representing
     *                the first line of a request message.
     * @throws ParseException If the request line cannot be parsed due to a format error
     *         or presence of an unrecognized protocol version or method.
     */
    public static RequestLine parse(final byte[] bytes) throws ParseException {
        try {
            return parse(new String(bytes,"UTF8"));
        }
        catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }
    
    /**
     * Constructs a request line instance from the first line of a request message.
     * @param bytes - A string containing the first line of a request message.
     * @throws ParseException If the request line cannot be parsed due to a format error
     *         or presence of an unrecognized protocol version or method.
     */
    public static RequestLine parse(final String string) throws ParseException {
        Matcher matcher = pattern.matcher(string);
        if (!matcher.matches()) {
            throw new ParseException("invalid request line");
        }
        
        URI uri;
        try {
            uri = new URI(matcher.group(2));
        }
        catch (URISyntaxException e) {
            throw new ParseException("invalid URI specified in request");
        }

        try {
            return new RequestLine(new Method(matcher.group(1)),
                                   uri,
                                   new ProtocolVersion(new ProtocolName(matcher.group(3)),
                                                       Integer.parseInt(matcher.group(4)),
                                                       Integer.parseInt(matcher.group(5))));
        }
        catch (Exception e) {
            // Should not get here
            throw new ParseException(e);
        }
    }

    
    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a request line instance with the specified method, URI and protocol version.
     * @param protocolVersion - The response protocol version.
     * @param statusCode - The response status code.
     */
    public RequestLine(final Method method, final URI uri, final ProtocolVersion protocolVersion) {
        super(protocolVersion);
        this.method = method;
        this.uri = uri;
    }

    /**
     * Returns the request {@link Method} of this request line.
     */
    public Method getMethod() {
        return this.method;
    }

    /**
     * Returns the request URI of this request line.
     */
    public URI getUri() {
        return this.uri;
    }

    /**
     * Returns a string containing the serialized form of this request line (e.g. "DESCRIBE /movie.sdp RTSP/1.0").
     */
    @Override
    public String toString() {
        return this.method.getName() + " " + this.uri.toString() + " " + this.protocolVersion.toString();
    }

    /**
     * Writes this request line to the specified OutputStream.
     * Used to serialize the request line for transmission.
     * @param outstream - The destination OutputStream for the request.
     * @throws IOException If an I/O occurs.
     */
    @Override
    public void writeTo(final OutputStream outstream) throws IOException {
        outstream.write(this.method.getName().getBytes("UTF8"));
        outstream.write(' ');
        outstream.write(this.uri.toString().getBytes("UTF8"));
        outstream.write(' ');
        this.protocolVersion.writeTo(outstream);
        outstream.write('\r');
        outstream.write('\n');
    }
    
}
