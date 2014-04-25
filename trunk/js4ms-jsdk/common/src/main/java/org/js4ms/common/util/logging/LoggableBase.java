package org.js4ms.common.util.logging;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * LoggableBase.java [org.js4ms.jsdk:common]
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
 * Abstract base class for classes that support logging.
 * 
 * @author Gregory Bumgardner
 */
public abstract class LoggableBase
                implements Loggable {

    protected static final String STATIC = "[ static ]";

    public final String ClassId = this.getClass().getName();

    public final Log log = new Log(this);

    /**
     * Logs the internal state of this object using the logger returned by
     * {@link #getLogger()}.
     */
    public final void log(Level level) {
        log(getLogger(), level);
    }

    @Override
    public void log(final Logger logger, final Level level) {
        logger.log(level,this.log.msg(" + logging [" + ClassId + "]"));
    }

}
