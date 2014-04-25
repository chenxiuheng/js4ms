package org.js4ms.common.exception;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ParseException.java [org.js4ms.jsdk:common]
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


/**
 * @author Greg Bumgardner (gbumgard)
 */
public class ParseException
                extends Exception {

    private static final long serialVersionUID = -8100180238203347845L;

    /**
     * 
     */
    public ParseException() {
        super();
    }

    /**
     * @param message
     */
    public ParseException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param throwable
     */
    public ParseException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * @param throwable
     */
    public ParseException(Throwable throwable) {
        super(throwable);
    }
}
