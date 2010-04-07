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

public final class ProtocolVersion {
    
    public static final Pattern pattern = Pattern.compile("(?:(HTTP)|(RTSP))/([0-9])\\.([0-9])");
    
    public static final ProtocolVersion RTSP_1_0 = new ProtocolVersion(Protocol.RTSP, 1, 0);
    public static final ProtocolVersion HTTP_1_0 = new ProtocolVersion(Protocol.HTTP, 1, 0);
    public static final ProtocolVersion HTTP_1_1 = new ProtocolVersion(Protocol.HTTP, 1, 1);

    private Protocol protocol;
    private int majorVersion;
    private int minorVersion;
    private String representation;
    
    public ProtocolVersion(final Protocol protocol, final int majorVersion, final int minorVersion) {
        this.protocol = protocol;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.representation = protocol.name()+"/"+majorVersion+"."+minorVersion;
    }

    public static ProtocolVersion parse(final byte[] bytes) throws ParseException {
        try {
            return parse(new String(bytes,"UTF8"));
        }
        catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public static ProtocolVersion parse(final String string) throws ParseException {
        Matcher matcher = pattern.matcher(string);
        if (!matcher.matches()) {
            throw new ParseException("invalid protocol version");
        }

        try {
            return new ProtocolVersion(Protocol.valueOf(matcher.group(0)),
                                       Integer.parseInt(matcher.group(1)),
                                       Integer.parseInt(matcher.group(2)));
        }
        catch (Exception e) {
            // Should not get here
            throw new ParseException(e);
        }
    }
    
    public Protocol getProtocol() {
        return this.protocol;
    }

    public int getMajorVersion() {
        return this.majorVersion;
    }
    
    public int getMinorVersion() {
        return this.minorVersion;
    }

    @Override
    public String toString() {
        return this.protocol.name() + "/" + this.majorVersion + "." + this.minorVersion;
    }

    public void write(OutputStream outstream) throws IOException {
        outstream.write(this.representation.getBytes("UTF8"));
    }
}
