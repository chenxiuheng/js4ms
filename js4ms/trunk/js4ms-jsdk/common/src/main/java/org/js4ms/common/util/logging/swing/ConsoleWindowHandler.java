package org.js4ms.common.util.logging.swing;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ConsoleWindowHandler.java [org.js4ms.jsdk:common]
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
