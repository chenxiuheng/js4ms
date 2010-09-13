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
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.larkwoodlabs.common.exceptions.ParseException;

/**
 * The first line of an RTSP response consisting of a protocol version, status code and reason phrase. 
 *
 * @author Gregory Bumgardner
 */
public final class StatusLine {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * Regular expression used to parse the response line into a protocol version, status code and reason phrase.
     */
    public static final Pattern pattern = Pattern.compile("((?:HTTP)|(?:RTSP))/([0-9])\\.([0-9])[ ]+([0-9]+)[ ]+(.*)");

    
    /*-- Member Variables ----------------------------------------------------*/

    ProtocolVersion protocolVersion;
    StatusCode statusCode;
    String reasonPhrase;
    

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a status line instance from the first line of an RTSP response.
     * @param bytes - A byte array containing a UTF-8 encoded string representing the first line of an RTSP response.
     * @throws ParseException If the response line cannot be parsed due to a format error
     *         or presence of an unrecognized protocol version or method.
     */
    public static StatusLine parse(byte[] bytes) throws ParseException {
        try {
            return parse(new String(bytes,"UTF8"));
        }
        catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    /**
     * Constructs a status line instance from the first line of an RTSP response.
     * @param string - A string containing the first line of an RTSP response.
     * @throws ParseException If the response line cannot be parsed due to a format error
     *         or presence of an unrecognized protocol version or method.
     */
    public static StatusLine parse(String string) throws ParseException {
        Matcher matcher = pattern.matcher(string);
        if (!matcher.matches()) {
            throw new ParseException("invalid RTSP status line");
        }
        
        StatusCode statusCode;
        try {
            statusCode = StatusCode.valueOf(matcher.group(4));
        }
        catch (IllegalArgumentException e) {
            statusCode = StatusCode.Unrecognized;
        }

        try {
            return new StatusLine(new ProtocolVersion(Protocol.valueOf(matcher.group(1)),
                                                      Integer.parseInt(matcher.group(2)),
                                                      Integer.parseInt(matcher.group(3))),
                                  statusCode,
                                  matcher.group(5).trim());
        }
        catch (Exception e) {
            // Should not get here
            throw new ParseException(e);
        }
    }

    /**
     * Constructs a status line instance from a protocol version and status code.
     * The reason phrase is set using the value returned by {@link StatusCode#getReasonPhrase()}.
     * @param protocolVersion - The response protocol version.
     * @param statusCode - The response status code.
     */
    public StatusLine(final ProtocolVersion protocolVersion, final StatusCode statusCode) {
        this.protocolVersion = protocolVersion;
        this.statusCode = statusCode;
        this.reasonPhrase = statusCode.getReasonPhrase();
    }

    /**
     * Constructs a status line instance from a protocol version, status code and reason phrase.
     * @param protocolVersion - The response protocol version.
     * @param statusCode - The response status code.
     * @param reasonPhrase - The response reason phrase.
     */
    protected StatusLine(final ProtocolVersion protocolVersion, final StatusCode statusCode, final String reasonPhrase) {
        this.protocolVersion = protocolVersion;
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    /**
     * Gets the response {@link ProtocolVersion} of this status line.
     * @return
     */
    public ProtocolVersion getProtocolVersion() {
        return this.protocolVersion;
    }
    
    /**
     * Sets the response {@link ProtocolVersion} of this status line.
     * @param protocolVersion - The new protocol version.
     */
    public void setProtocolVersion(ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    /**
     * Gets the response {@link StatusCode} of this status line.
     */
    public StatusCode getStatusCode() {
        return this.statusCode;
    }

    /**
     * Sets the response {@link StatusCode} and reason phrase of this status line.
     * The reason phrase is set using the value returned by {@link StatusCode#getReasonPhrase()}.
     * @param statusCode - The new status code.
     */
    public void setStatusCode(StatusCode statusCode) {
        setStatus(statusCode, statusCode.getReasonPhrase());
    }

    /**
     * Sets the response {@link StatusCode} and reason phrase of this status line.
     * @param statusCode - The new status code.
     * @param reasonPhrase - The new reason phrase.
     */
    public void setStatus(StatusCode statusCode, String reasonPhrase) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    /**
     * Gets the response reason phrase of this status line.
     */
    public String getReasonPhrase() {
        return this.reasonPhrase;
    }

    /**
     * Sets the response reason phrase of this status line.
     * @param reasonPhrase - The new reason phrase.
     */
    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }

    /**
     * Returns a string containing the serialized form of this status line (e.g. "RTSP/1.0 200 OK").
     */
    @Override
    public String toString() {
        return this.protocolVersion.toString() + " " + this.statusCode.getCode() + " " + this.reasonPhrase;
    }

    /**
     * Writes this status line to the specified OutputStream.
     * Used to serialize the status line for transmission.
     * @param outstream - The destination OutputStream for the response.
     * @throws IOException If an I/O occurs.
     */
    public void writeTo(OutputStream outstream) throws IOException {
        this.protocolVersion.write(outstream);
        outstream.write(' ');
        outstream.write(String.valueOf(this.statusCode.getCode()).getBytes("UTF8"));
        outstream.write(' ');
        outstream.write(this.reasonPhrase.getBytes("UTF8"));
        outstream.write('\r');
        outstream.write('\n');
    }
}
