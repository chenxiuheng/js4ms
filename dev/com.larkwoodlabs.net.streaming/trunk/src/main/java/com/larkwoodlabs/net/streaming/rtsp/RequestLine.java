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

public final class RequestLine {

    public static final Pattern pattern = Pattern.compile("((?:ANNOUNCE)|(?:GET)|(?:POST)|(?:OPTIONS)|(?:DESCRIBE)|(?:SETUP)|(?:PLAY)|(?:PAUSE)|(?:RECORD)|(?:TEARDOWN)|(?:GET_PARAMETER)|(?:SET_PARAMETER)|(?:REDIRECT))[ ]+([^ ]+) ((?:HTTP)|(?:RTSP))/([0-9])\\.([0-9])");

    Method method;
    URI uri;
    ProtocolVersion protocolVersion;
    
    public static RequestLine parse(byte[] bytes) throws ParseException {
        try {
            return parse(new String(bytes,"UTF8"));
        }
        catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }
    
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

    public RequestLine(Method method, URI uri, ProtocolVersion protocolVersion) {
        this.method = method;
        this.uri = uri;
        this.protocolVersion = protocolVersion;
    }

    public Method getMethod() {
        return this.method;
    }
    
    public URI getUri() {
        return this.uri;
    }

    public ProtocolVersion getProtocolVersion() {
        return this.protocolVersion;
    }
    
    @Override
    public String toString() {
        return this.method.name() + " " + this.uri.toString() + " " + this.protocolVersion.toString();
    }

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
