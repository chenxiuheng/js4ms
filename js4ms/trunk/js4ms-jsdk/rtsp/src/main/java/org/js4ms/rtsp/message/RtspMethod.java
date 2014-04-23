package org.js4ms.rtsp.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * RtspMethod.java [org.js4ms.jsdk:rtsp]
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

import org.js4ms.rest.message.Method;

public interface RtspMethod {

    public static final Method GET = new Method("GET");
    public static final Method POST = new Method("POST");
    public static final Method ANNOUNCE = new Method("ANNOUNCE");
    public static final Method OPTIONS = new Method("OPTIONS");
    public static final Method DESCRIBE = new Method("DESCRIBE");
    public static final Method SETUP = new Method("SETUP");
    public static final Method PLAY = new Method("PLAY");
    public static final Method PAUSE = new Method("PAUSE");
    public static final Method RECORD = new Method("RECORD");
    public static final Method TEARDOWN = new Method("TEARDOWN");
    public static final Method GET_PARAMETER = new Method("GET_PARAMETER");
    public static final Method SET_PARAMETER = new Method("SET_PARAMETER");
    public static final Method REDIRECT = new Method("REDIRECT");

}
