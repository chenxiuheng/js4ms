package org.js4ms.common.util.logging;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * LogFormatter.java [org.js4ms.jsdk:common]
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

public class LogFormatter
                extends Formatter {

    private static final MessageFormat messageFormat = new MessageFormat("{0,date,HH:mm:ss.SSS} {1,number,00000000} {2} {3} {4} {5}\n");

    private long startTime = 0;

    public LogFormatter() {
        super();
    }

    @Override
    public String format(LogRecord record) {

        if (startTime == 0) {
            this.startTime = record.getMillis();
        }

        Object[] arguments = new Object[7];

        arguments[0] = new Date(record.getMillis());

        arguments[1] = record.getMillis() - this.startTime;

        String level = record.getLevel().toString() + "      ";
        arguments[2] = level.substring(0, 6);

        String threadId = "00000" + Thread.currentThread().getId();
        arguments[3] = threadId.substring(threadId.length() - 5);

        String loggerName = "                                        " + record.getLoggerName();

        arguments[4] = loggerName.substring(loggerName.length() - 40);

        arguments[5] = record.getMessage();

        return messageFormat.format(arguments);
    }
}
