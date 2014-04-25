package org.js4ms.rest.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * FormattedMessageHeader.java [org.js4ms.jsdk:rest]
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

import org.js4ms.common.exception.ParseException;

public abstract class FormattedMessageHeader extends MessageHeaderBase {

    protected FormattedMessageHeader(final String name) {
        super(name);
    }

    protected FormattedMessageHeader(final String name, final String value) throws IllegalArgumentException {
        super(name);
        setValue(value);
    }

    protected FormattedMessageHeader(final FormattedMessageHeader header) {
        super(header);
    }

    @Override
    public String getValue() {
        return format();
    }

    @Override
    public void setValue(final String value) throws IllegalArgumentException {
        try {
            parse(value);
        }
        catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected abstract void parse(final String value) throws ParseException;

    protected abstract String format();
}
