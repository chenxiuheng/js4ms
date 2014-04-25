package org.js4ms.rtsp.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * RtspHeaderName.java [org.js4ms.jsdk:rtsp]
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

import org.js4ms.rest.message.HeaderName;

public interface RtspHeaderName extends HeaderName {
    public static final String BANDWIDTH = "Bandwidth"; // rtsp(R)
    public static final String BLOCKSIZE = "Blocksize"; // rtsp(R)
    public static final String CONFERENCE = "Conference"; // rtsp(R)
    public static final String CONTENT_BASE = "Content-Base"; // rtsp(e)
    public static final String PUBLIC = "Public"; // rtsp(r)
    public static final String RTP_INFO = "RTP-Info"; // rtsp(r)
    public static final String SCALE = "Scale"; // rtsp(R->r)
    public static final String SESSION = "Session"; // rtsp(R->r)
    public static final String SPEED = "Speed"; // rtsp(R->r)
    public static final String TIMESTAMP = "Timestamp"; // rtsp(R->r)
    public static final String TRANSPORT = "Transport"; // rtsp(R->r)
    public static final String X_SESSIONCOOKIE = "x-sessioncookie";
}
