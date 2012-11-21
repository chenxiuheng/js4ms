package net.js4ms.service.protocol.http;

import net.js4ms.service.protocol.rest.MessageHeaders;

public interface HttpMessageHeaders extends MessageHeaders {

    public static final String ACCEPT_CHARSET = "Accept-Charset"; // http(R)
    public static final String ACCEPT_RANGES = "Accept-Ranges"; // http(r)
    public static final String AGE = "Age"; // http(r)
    public static final String CONTENT_MD5 = "Content-MD5"; // http(r)
    public static final String CONTENT_RANGE = "Content-Range"; // http(r)
    public static final String COOKIE = "Cookie"; // http(R) 
    public static final String ETAG = "ETag"; // http(r)
    public static final String EXPECT = "Expect"; // http(R)
    public static final String HOST = "Host"; // http(R)
    public static final String IF_MATCH = "If-Match"; // http(R)
    public static final String IF_NONE_MATCH = "If-None-Match"; // http(R)
    public static final String IF_RANGE = "If-Range"; // http(R)
    public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since"; // http(R)
    public static final String LOCATION = "Location"; // http(r)
    public static final String PRAGMA = "Pragma"; // http(R,r)
    public static final String REFRESH = "Refresh"; // http(r)
    public static final String SET_COOKIE = "Set-Cookie"; // http(r)
    public static final String TE = "TE"; // http(R)
    public static final String TRAILER = "Trailer"; // http(r)
    public static final String TRANSFER_ENCODING = "Transfer-Encoding"; // http(r)
    public static final String UPGRADE = "Upgrade"; // http(R)
    public static final String VARY = "Vary"; // http(r)

}
