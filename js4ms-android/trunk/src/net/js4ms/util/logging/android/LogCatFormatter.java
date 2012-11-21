package net.js4ms.util.logging.android;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogCatFormatter extends Formatter {
    
    // private static final MessageFormat messageFormat = new MessageFormat("{0,date,HH:mm:ss.SSS} {1,number,00000000} {2} {3} {4} {5}\n");
    // private static final MessageFormat messageFormat = new MessageFormat("{0} {1}\n");
    
    // private long startTime = System.currentTimeMillis();

    public LogCatFormatter() {
        super();
    }
    
    @Override
    public String format(LogRecord record) {

        //Object[] arguments = new Object[7];
        // Object[] arguments = new Object[2];
        // arguments[0] = record.getMessage();

        //return messageFormat.format(arguments);
        return record.getMessage();
    }   
}