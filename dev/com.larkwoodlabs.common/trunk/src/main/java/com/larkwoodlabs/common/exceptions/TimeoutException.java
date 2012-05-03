/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: TimeoutException.java (com.larkwoodlabs.common)
 * 
 * Copyright � 2009 Cisco Systems, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.larkwoodlabs.common.exceptions;

import java.io.IOException;

/**
 * @author Greg Bumgardner (gbumgard)
 */
public class TimeoutException
                extends IOException {

    private static final long serialVersionUID = -8396512668584669976L;

    /**
     * 
     */
    public TimeoutException() {
        super("operation has timed out");
    }

    /**
     * @param message
     */
    public TimeoutException(String message) {
        super(message);
    }
}
