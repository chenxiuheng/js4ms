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
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.larkwoodlabs.common.exceptions.ParseException;

public class Header {

    public static final String ACCEPT = "Accept";
    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    public static final String ACCEPT_LANGUAGE = "Accept-Language";
    public static final String ALLOW = "Allow";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BANDWIDTH = "Bandwidth";
    public static final String BLOCKSIZE = "Bandwidth";
    public static final String CACHE_CONTROL = "Cache-Control";
    public static final String CONFERENCE = "Conference";
    public static final String CONNECTION = "Connection";
    public static final String CONTENT_BASE = "Content-Base";
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String CONTENT_LANGUAGE = "Content-Language";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_LOCATION = "Content-Location";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CSEQ = "CSeq";
    public static final String DATE = "Date";
    public static final String EXPIRES = "Expires";
    public static final String FROM = "From";
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    public static final String LAST_MODIFIED = "Last-Modified";
    public static final String PRAGMA = "Pragma";
    public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate"; 
    public static final String PROXY_REQUIRE = "Proxy-Require";
    public static final String PUBLIC = "Public";
    public static final String RANGE = "Range";
    public static final String REFERER = "Referer";
    public static final String REQUIRE = "Require";
    public static final String RETRY_AFTER = "Retry-After";
    public static final String RTP_INFO = "RTP-Info";
    public static final String SCALE = "Scale";
    public static final String SESSION = "Session";
    public static final String SERVER = "Server";
    public static final String SPEED = "Speed";
    public static final String TRANSPORT = "Transport";
    public static final String UNSUPPORTED = "Unsupported";
    public static final String USER_AGENT = "User-Agent";
    public static final String VIA = "Via";
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    public static final String X_SESSIONCOOKIE = "x-sessioncookie";

    public static final Pattern pattern = Pattern.compile("([[0-9][a-z][A-Z][-_]]+):[ ]*(.*)");

    // Commonly used headers

    public static final Header CACHE_CONTROL_IS_NO_CACHE = new Header(Header.CACHE_CONTROL,"no-cache");
    public static final Header CONNECTION_IS_CLOSE = new Header(Header.CONNECTION,"close");
    public static final Header CACHE_CONTROL_IS_NO_STORE = new Header(Header.CACHE_CONTROL, "no-store");
    public static final Header PRAGMA_IS_NO_CACHE = new Header(Header.PRAGMA, "no-cache");
    public static final Header CONTENT_TYPE_IS_JSON = new Header(Header.CONTENT_TYPE, MimeType.application.json);
 
    protected final String name;
    protected String value;

    public static final SimpleDateFormat DATE_FORMAT_RFC_1123;
    
    static {
        DATE_FORMAT_RFC_1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        DATE_FORMAT_RFC_1123.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static final Header parse(final String string) throws ParseException {
        Matcher matcher = pattern.matcher(string);
        if (!matcher.matches()) {
            throw new ParseException("invalid header");
        }
        return new Header(matcher.group(1),matcher.group(2).trim());
    }

    public Header(final String name) {
        this.name = name;
        this.value = "";
    }

    public Header(final String name, final String value) {
        this.name = name;
        this.value = value;
    }
    
    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
    
    public void appendFragment(final String value) {
        this.value += value;
    }

    public void appendValue(final String value) {
        if (this.value.length() > 0) {
            this.value += "," + value;
        }
        else {
            this.value = value;
        }
    }

    @Override
    public String toString() {
        return this.name + ": " + this.value;
    }

    public void writeTo(final OutputStream outstream) throws IOException {
        outstream.write(this.name.getBytes("UTF8"));
        outstream.write(':');
        outstream.write(' ');
        outstream.write(this.value.getBytes("UTF8"));
        outstream.write('\r');
        outstream.write('\n');
    }
}
