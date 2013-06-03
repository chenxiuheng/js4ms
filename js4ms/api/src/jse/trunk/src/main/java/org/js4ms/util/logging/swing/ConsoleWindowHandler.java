package org.js4ms.util.logging.swing;

import java.nio.charset.Charset;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;
import java.util.logging.LogManager;

public class ConsoleWindowHandler extends StreamHandler {

    private Console console;

    public ConsoleWindowHandler() {
        super();

        LogManager logManager = LogManager.getLogManager();
        String title = logManager.getProperty("org.js4ms.util.logging.swing.ConsoleWindowHandler.title");
        if (title == null) title = "Log Messages";

        String encoding = getEncoding();
        if (encoding == null) {
            encoding = Charset.defaultCharset().displayName();
        }

        this.console = new Console(title,
                                   encoding,
                                   Boolean.parseBoolean(logManager.getProperty("org.js4ms.util.logging.swing.ConsoleWindowHandler.waitforclose")));
    }

    @Override
    public void publish(LogRecord record) {
        System.out.print(getFormatter().format(record));
    }

    @Override
    public void close() {
        super.close();
        this.console.close();
    }

}
