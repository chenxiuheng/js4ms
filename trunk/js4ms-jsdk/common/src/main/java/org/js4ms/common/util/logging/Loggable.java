package org.js4ms.common.util.logging;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * Loggable.java [org.js4ms.jsdk:common]
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


import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interface exposed by objects whose internal state can be logged.
 *
 * @author Gregory Bumgardner
 */
public interface Loggable {

    /**
     * Returns the Logger instance used by this object to generate log messages.
     */
    public Logger getLogger();

    /**
     * Logs internal state of object using specified logger for output.
     * @param logger The logger object to use when generating log messages.
     */
    public void log(Logger logger, Level level);

}
