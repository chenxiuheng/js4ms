package org.js4ms.rest.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * StatusLine.java [org.js4ms.jsdk:rest]
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.js4ms.common.exception.ParseException;



public final class StatusLine extends StartLine {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * Regular expression used to parse the response line into a protocol, version, status code and reason phrase.
     */
    public static Pattern pattern = Pattern.compile("([A-Z]+)/([0-9])\\.([0-9])[ ]+([0-9]+)[ ]+(.*)");

    
    /*-- Member Variables ----------------------------------------------------*/

    /**
     * Constructs a status line instance from the first line of a response message.
     * @param bytes - A byte array containing a UTF-8 encoded string representing
     *                the first line of a response message.
     * @throws ParseException If the status line cannot be parsed due to a format error
     *         or presence of an unrecognized protocol version.
     */
    public static StatusLine parse(final byte[] bytes) throws ParseException {
        try {
            return parse(new String(bytes,"UTF8"));
        }
        catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }
    
    /**
     * Constructs a status line instance from the first line of a response message.
     * @param bytes - A string containing the first line of a response message.
     * @throws ParseException If the status line cannot be parsed due to a format error
     *         or presence of an unrecognized protocol version.
     */
    public static StatusLine parse(final String string) throws ParseException {
        Matcher matcher = pattern.matcher(string);
        if (!matcher.matches()) {
            throw new ParseException("invalid status line");
        }
        
        try {
            return new StatusLine(new ProtocolVersion(new ProtocolName(matcher.group(1)),
                                                      Integer.parseInt(matcher.group(2)),
                                                      Integer.parseInt(matcher.group(3))),
                                  new Status(Integer.parseInt(matcher.group(4)),matcher.group(5)));
        }
        catch (Exception e) {
            // Should not get here
            throw new ParseException(e);
        }
    }

    /*-- Member Variables ----------------------------------------------------*/

    Status status;
    

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a status line instance with the specified protocol version and status value.
     * @param protocolVersion - The protocol version.
     * @param statusCode - The status code and reason phrase.
     */
    public StatusLine(final ProtocolVersion protocolVersion, final Status status) {
        super(protocolVersion);
        this.status = status;
    }

    /**
     * Gets the {@link Status} property of this status line.
     */
    public Status getStatus() {
        return this.status;
    }

    /**
     * Sets the {@link Status} property of this status line.
     * @param status - The new status value.
     */
    public void setStatus(final Status status) {
        this.status = status;
    }

    /**
     * Returns a string containing the serialized form of this request line (e.g. "DESCRIBE /movie.sdp RTSP/1.0").
     */
    @Override
    public String toString() {
        return this.protocolVersion.toString() + " " + this.status.toString();
    }

    /**
     * Writes this status line to the specified OutputStream.
     * Used to serialize the status line for transmission.
     * @param outstream - The destination OutputStream for the response.
     * @throws IOException If an I/O occurs.
     */
    @Override
    public void writeTo(final OutputStream outstream) throws IOException {
        this.protocolVersion.writeTo(outstream);
        outstream.write(' ');
        this.status.writeTo(outstream);
        outstream.write('\r');
        outstream.write('\n');
    }
}
