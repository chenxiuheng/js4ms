package org.js4ms.rtsp.presentation;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * TransportPreferences.java [org.js4ms.jsdk:rtsp]
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


import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Logging;
import org.js4ms.rest.common.RequestException;




/**
 * Constructs a list of {@link TransportDescription} instances initialized from the transport specifiers 
 * and parameters contained in an RTSP Transport header.
 * This list is used to guide stream construction in response to a client setup request.
 * 
 * @author Gregory Bumgardner
 */
public final class TransportPreferences {

    /*-- Member Variables ----------------------------------------------------*/

    private LinkedList<TransportDescription> descriptions = new LinkedList<TransportDescription>();
    
    final String objectId = Logging.identify(this);
    final String logPrefix = objectId + " ";
    
    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Converts a comma-separated list of transport descriptions from an RTSP Transport header
     * into a list of {@link TransportDescription} instances
     * @throws UnknownHostException 
     * @throws RtspException 
     */
    public TransportPreferences(String header) throws RequestException, UnknownHostException {
        add(header);
    }

    public void log(Logger logger) {
        logger.info(this.logPrefix + "----> Transport Preferences");
        for (TransportDescription description : this.descriptions) {
            description.log(logger);
        }
        logger.info(this.logPrefix + "<---- Transport Preferences");
    }

    public Iterator<TransportDescription> getIterator() {
        return this.descriptions.iterator();
    }

    public void add(String header) throws RequestException, UnknownHostException {
        String[] descriptions = header.split(",");
        for (String description : descriptions) {
            this.descriptions.add(new TransportDescription(description));
        }
    }
}
