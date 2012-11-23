package org.js4ms.util.logging.android;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import android.util.Log;

public class LogCatHandler extends Handler {

    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void flush() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void publish(LogRecord record) {

        if (!isLoggable(record)) return;
        
        Level loggerLevel = record.getLevel();

        int logcatLevel = Log.INFO;
        if (loggerLevel == Level.SEVERE) logcatLevel = Log.ERROR;
        else if (loggerLevel == Level.WARNING) logcatLevel = Log.WARN;
        else if (loggerLevel == Level.INFO) logcatLevel = Log.INFO;
        else if (loggerLevel == Level.FINE) logcatLevel = Log.DEBUG;
        else if (loggerLevel == Level.FINER) logcatLevel = Log.VERBOSE;
        else if (loggerLevel == Level.FINEST) logcatLevel = Log.VERBOSE;

        // Output the formatted data to the file
        Log.println(logcatLevel, record.getLoggerName(), getFormatter().format(record));
    }
}