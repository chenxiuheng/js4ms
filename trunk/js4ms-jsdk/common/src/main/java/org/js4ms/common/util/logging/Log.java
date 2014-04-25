package org.js4ms.common.util.logging;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * Log.java [org.js4ms.jsdk:common]
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

public final class Log {

    final String classPrefix;
    final String objectId;
    final String prefix;

    public Log(final Object object) {
        if (object instanceof Class) {
            this.classPrefix = ((Class<?>)object).getSimpleName() + ".";
            this.objectId = "[ static ]";
        }
        else {
            this.classPrefix = object.getClass().getSimpleName() + ".";
            this.objectId = Logging.identify(object);
        }
        this.prefix = this.objectId + " ";
    }

    public Log(final Object object, final Class<?> clazz) {
        this.classPrefix = clazz.getSimpleName() + ".";
        this.objectId = Logging.identify(object);
        this.prefix = this.objectId + " ";
    }

    public final String entry(final String methodName, final Object ...args) {
        return Logging.entering(this.objectId, this.classPrefix+methodName, args);
    }

    public final String entry(final String methodName) {
        return Logging.entering(this.objectId, this.classPrefix+methodName);
    }

    public final String exit(final String methodName) {
        return Logging.exiting(this.objectId, this.classPrefix+methodName);
    }

    public final String exit(final String methodName, final Object result) {
        return Logging.exiting(this.objectId, this.classPrefix+methodName, result);
    }

    public final String msg(String message) {
        return this.prefix + message;
    }

    public final String getPrefix() {
        return this.prefix;
    }

}
