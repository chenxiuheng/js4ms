package org.js4ms.server;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * Service.java [org.js4ms.jsdk:server]
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

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;

/**
 * Interface exposed by objects that service messages received on a {@link Connection}.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public interface Service {

    /**
     * Starts the service.
     * Used to initialize the service, start threads or timers and acquire resources.
     */
    void start();

    /**
     * Stops the service.
     * Used to terminate threads and timers and releases resources.
     */
    void stop();

    /**
     * Reads and processes a single message or stream of messages.
     * 
     * @param connection
     * @throws IOException
     */
    void service(Connection connection) throws EOFException,
                                       SocketException,
                                       IOException,
                                       InterruptedException;

}
