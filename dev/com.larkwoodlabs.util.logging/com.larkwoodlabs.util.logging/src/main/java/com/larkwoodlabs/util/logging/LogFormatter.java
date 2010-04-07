/*
 * Copyright � 2009-2010 Larkwood Labs Software.
 *
 * Licensed under the Larkwood Labs Software Source Code License, Version 1.0.
 * You may not use this file except in compliance with this License.
 *
 * You may view the Source Code License at
 * http://www.larkwoodlabs.com/source-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the license.
 */

package com.larkwoodlabs.util.logging;

import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
        
    private static final MessageFormat messageFormat = new MessageFormat("{0,date,HH:mm:ss.SSS} {1,number,00000000} {2} {3} {4} {5}\n");
    
    private long startTime = System.currentTimeMillis();

    public LogFormatter() {
        super();
    }
    
    @Override public String format(LogRecord record) {

        Object[] arguments = new Object[7];

        arguments[0] = new Date(record.getMillis());

        arguments[1] = record.getMillis() - this.startTime;

        String level = record.getLevel().toString() + "      ";
        arguments[2] = level.substring(0,6);

        String threadId = "00000" + Thread.currentThread().getId();
        arguments[3] = threadId.substring(threadId.length()-5);

        String loggerName = "                                        " + record.getLoggerName();
        
        arguments[4] = loggerName.substring(loggerName.length() - 40);
        
        arguments[5] = record.getMessage();

        return messageFormat.format(arguments);
    }   
}
