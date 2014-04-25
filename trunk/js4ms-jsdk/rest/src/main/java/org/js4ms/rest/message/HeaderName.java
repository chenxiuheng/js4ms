package org.js4ms.rest.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * HeaderName.java [org.js4ms.jsdk:rest]
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

public interface HeaderName {

    public static final String ACCEPT = "Accept"; // http(R), rtsp(R)
    public static final String ACCEPT_ENCODING = "Accept-Encoding"; // http(R), rtsp(R)
    public static final String ACCEPT_LANGUAGE = "Accept-Language"; // http(R), rtsp(R)
    public static final String ALLOW = "Allow"; // http(R,r), rtsp(r), sip
    public static final String AUTHORIZATION = "Authorization"; // http(R), rtsp(R), sip
    public static final String CACHE_CONTROL = "Cache-Control"; // http(R,r), rtsp(R,r)
    public static final String CONNECTION = "Connection"; // http(R), rtsp(R,r) 
    public static final String CSEQ = "CSeq"; // rtsp(R,r), sip
    public static final String DATE = "Date"; // http(R), rtsp(R,r), sip
    public static final String FROM = "From"; // http(R), rtsp(R), sip
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since"; // http(R), rtsp(R)
    public static final String MAX_FORWARDS = "Max-Forwards"; // http(R), sip
    public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate"; // http(r), rtsp(?), sip 
    public static final String PROXY_AUTHORIZATION = "Proxy-Authorization"; // http(R), sip
    public static final String PROXY_REQUIRE = "Proxy-Require"; // rtsp(R), sip
    public static final String RANGE = "Range"; // http(R), rtsp(r)
    public static final String REFERER = "Referer"; // http(R), rtsp(R)
    public static final String REQUIRE = "Require"; // rtsp(R), sip
    public static final String RETRY_AFTER = "Retry-After"; // http(r), rtsp(r), sip
    public static final String SERVER = "Server"; // http(r), rtsp(r), sip
    public static final String UNSUPPORTED = "Unsupported"; // rtsp(r), sip
    public static final String USER_AGENT = "User-Agent"; // http(R), rtsp(R), sip
    public static final String VIA = "Via"; // http(R), rtsp(R,r), sip
    public static final String WARNING = "Warning"; // http(R,r), sip
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate"; // http(r), rtsp(r), sip

}
