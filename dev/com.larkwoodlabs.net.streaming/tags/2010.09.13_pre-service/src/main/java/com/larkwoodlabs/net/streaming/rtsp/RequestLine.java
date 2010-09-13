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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.larkwoodlabs.common.exceptions.ParseException;

/**
 * The first line of an RTSP request consisting of a method, URI and protocol version. 
 *
 * @author Gregory Bumgardner
 */
public final class RequestLine {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * Regular expression used to parse the request line into a method, URI and protocol version.
     */
    public static final Pattern pattern = Pattern.compile("((?:ANNOUNCE)|(?:GET)|(?:POST)|(?:OPTIONS)|(?:DESCRIBE)|(?:SETUP)|(?:PLAY)|(?:PAUSE)|(?:RECORD)|(?:TEARDOWN)|(?:GET_PARAMETER)|(?:SET_PARAMETER)|(?:REDIRECT))[ ]+([^ ]+) ((?:HTTP)|(?:RTSP))/([0-9])\\.([0-9])");


    /*-- Member Variables ----------------------------------------------------*/

    Method method;
    URI uri;
    ProtocolVersion protocolVersion;


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a request line instance from the first line of an RTSP request.
     * @param bytes - A byte array containing a UTF-8 encoded string representing the first line of an RTSP request.
     * @throws ParseException If the request line cannot be parsed due to a format error
     *         or presence of an unrecognized protocol version or method.
     */
    public static RequestLine parse(byte[] bytes) throws ParseException {
        try {
            return parse(new String(bytes,"UTF8"));
        }
        catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }
    
    /**
     * Constructs a request line instance from the first line of an RTSP request.
     * @param bytes - A string containing the first line of an RTSP request.
     * @throws ParseException If the request line cannot be parsed due to a format error
     *         or presence of an unrecognized protocol version or method.
     */
    public static RequestLine parse(String string) throws ParseException {
        Matcher matcher = pattern.matcher(string);
        if (!matcher.matches()) {
            throw new ParseException("invalid request line");
        }
        
        URI uri;
        String requestUri = matcher.group(2);
        if (requestUri.equals("*")) {
            uri = null;
        }
        else {
            try {
                uri = new URI(matcher.group(2));
            }
            catch (URISyntaxException e) {
                throw new ParseException("invalid URI specified in request");
            }
        }

        try {
            return new RequestLine(Method.valueOf(matcher.group(1)),
                                   uri,
                                   new ProtocolVersion(Protocol.valueOf(matcher.group(3)),
                                                       Integer.parseInt(matcher.group(4)),
                                                       Integer.parseInt(matcher.group(5))));
        }
        catch (Exception e) {
            // Should not get here
            throw new ParseException(e);
        }
    }

    /**
     * Constructs a request line from a method, URI and protocol version.
     * @param method - The RTSP method (type of request).
     * @param uri - The resource or control URI target for the request.
     * @param protocolVersion - The protocol version for the request.
     */
    public RequestLine(Method method, URI uri, ProtocolVersion protocolVersion) {
        this.method = method;
        this.uri = uri;
        this.protocolVersion = protocolVersion;
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
     * Returns the request {@link ProtocolVersion} of this request line.
     * @return
     */
    public ProtocolVersion getProtocolVersion() {
        return this.protocolVersion;
    }
    
    /**
     * Returns a string containing the serialized form of this request line (e.g. "DESCRIBE /movie.sdp RTSP/1.0").
     */
    @Override
    public String toString() {
        return this.method.name() + " " + this.uri.toString() + " " + this.protocolVersion.toString();
    }

    /**
     * Writes this request line to the specified OutputStream.
     * Used to serialize the request line for transmission.
     * @param outstream - The destination OutputStream for the request.
     * @throws IOException If an I/O occurs.
     */
    public void writeTo(OutputStream outstream) throws IOException {
        outstream.write(this.method.name().getBytes("UTF8"));
        outstream.write(' ');
        outstream.write(this.uri.toString().getBytes("UTF8"));
        outstream.write(' ');
        this.protocolVersion.write(outstream);
        outstream.write('\r');
        outstream.write('\n');
    }
}
