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

public final class StatusLine {

    public static final Pattern pattern = Pattern.compile("((?:HTTP)|(?:RTSP))/([0-9])\\.([0-9])[ ]+([0-9]+)[ ]+(.*)");
    ProtocolVersion protocolVersion;
    StatusCode statusCode;
    String reasonPhrase;
    
    public static StatusLine parse(byte[] bytes) throws ParseException {
        try {
            return parse(new String(bytes,"UTF8"));
        }
        catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

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

    public StatusLine(final ProtocolVersion protocolVersion, final StatusCode statusCode) {
        this.protocolVersion = protocolVersion;
        this.statusCode = statusCode;
        this.reasonPhrase = statusCode.getReasonPhrase();
    }
    
    protected StatusLine(final ProtocolVersion protocolVersion, final StatusCode statusCode, final String reasonPhrase) {
        this.protocolVersion = protocolVersion;
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public ProtocolVersion getProtocolVersion() {
        return this.protocolVersion;
    }
    
    public void setProtocolVersion(ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public StatusCode getStatusCode() {
        return this.statusCode;
    }
    
    public void setStatusCode(StatusCode statusCode) {
        setStatus(statusCode, statusCode.getReasonPhrase());
    }

    public void setStatus(StatusCode statusCode, String reasonPhrase) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public String getReasonPhrase() {
        return this.reasonPhrase;
    }

    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }

    @Override
    public String toString() {
        return this.protocolVersion.toString() + " " + this.statusCode.getCode() + " " + this.reasonPhrase;
    }

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
