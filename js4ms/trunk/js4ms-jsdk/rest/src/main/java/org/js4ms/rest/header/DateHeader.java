package org.js4ms.rest.header;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * DateHeader.java [org.js4ms.jsdk:rest]
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

import java.util.Date;

import org.js4ms.common.exception.ParseException;
import org.js4ms.rest.message.FormattedMessageHeader;
import org.js4ms.rest.message.MessageHeader;
import org.js4ms.rest.util.DateUtil;





public class DateHeader extends FormattedMessageHeader {

    private Date date;

    public DateHeader(final String name, final Date value) {
        super(name);
        this.date = value;
    }

    public DateHeader(final DateHeader header) {
        super(header);
        this.date = header.date;
    }

    @Override
    public void appendHeader(MessageHeader header) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void parse(String value) throws ParseException {
        try {
            this.date = DateUtil.toDate(value);
        }
        catch (java.text.ParseException e) {
            throw new ParseException(e);
        }
    }

    @Override
    protected String format() {
        return DateUtil.DATE_FORMAT_RFC_1123.format(this.date);
    }

    @Override
    public Object clone() {
        return new DateHeader(this);
    }

}
