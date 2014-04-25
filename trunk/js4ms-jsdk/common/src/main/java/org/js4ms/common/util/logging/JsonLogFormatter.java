package org.js4ms.common.util.logging;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * JsonLogFormatter.java [org.js4ms.jsdk:common]
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

import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class JsonLogFormatter extends Formatter {

    private static final MessageFormat messageFormat = new MessageFormat(
                                          "'{'\"date\": {0,date,yyyy-MM-dd'T'HH:mm:ss.SSSZ}, " +
                                          "\"millis\": {1,number,0}, " + 
                                          "\"sequence\": {2,number,0}, "+
                                          "\"logger\": \"{3}\", "+
                                          "\"level\": \"{4}\", "+
                                          "\"class\": \"{5}\", " +
                                          "\"method\": \"{6}\", " +
                                          "\"thread\": {7,number,0}, "+
                                          "\"message\": \"{8}\"'}'");

    private final String callbackFunction;

    private long startTime = 0;

    //YYYY-MM-DDThh:mm:ss.sTZD
    public JsonLogFormatter() {
        this.callbackFunction = null;
    }

    public JsonLogFormatter(final String callbackFunction) {
        this.callbackFunction = callbackFunction;
    }

    @Override
    public String format(LogRecord record) {

        if (startTime == 0) {
            this.startTime = record.getMillis();
        }

        Object[] arguments = new Object[9];

        arguments[0] = new Date(record.getMillis());
        arguments[1] = record.getMillis() - this.startTime;
        arguments[2] = record.getSequenceNumber();
        arguments[3] = record.getLoggerName();
        arguments[4] = record.getLevel();
        arguments[5] = record.getSourceClassName();
        arguments[6] = record.getSourceMethodName();
        arguments[7] = record.getThreadID();
        arguments[8] = record.getMessage();

        StringBuffer buffer = new StringBuffer();

        if (this.callbackFunction != null) {
            buffer.append(callbackFunction);
            buffer.append('(');
        }
        buffer.append(messageFormat.format(arguments));
        if (this.callbackFunction != null) {
            buffer.append(')');
        }
        buffer.append('\n');

        return buffer.toString();
    }

}
