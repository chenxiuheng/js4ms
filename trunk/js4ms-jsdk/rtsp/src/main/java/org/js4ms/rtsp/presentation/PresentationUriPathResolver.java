package org.js4ms.rtsp.presentation;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * PresentationUriPathResolver.java [org.js4ms.jsdk:rtsp]
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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;
import org.js4ms.rest.common.RequestException;
import org.js4ms.rest.message.Request;



/**
 * Examples:
 * <pre>
 * *
 * /*
 * /foo/*
 * /foo/bar
 * </pre>
 * 
 * @author gbumgard
 */
public final class PresentationUriPathResolver implements PresentationResolver {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(PresentationUriPathResolver.class.getName());


    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final Log log = new Log(this);

    /**
     * 
     */
    final HashMap<String,PresentationResolver> resolvers = new HashMap<String, PresentationResolver>();

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     */
    public PresentationUriPathResolver() {
    }

    /**
     * Registers a handler resolver for the specified URI pattern.
     * @param pattern - A String containing an absolute URI or portion thereof.
     * @param resolver
     */
    public void put(final String pattern, final PresentationResolver resolver) {
        this.resolvers.put(pattern, resolver);
    }

    public void remove(final String pattern) {
        this.resolvers.remove(pattern);
    }

    @Override
    public Presentation getPresentation(final Request request) throws RequestException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("getPresentation", request));
        }

        String path = request.getRequestLine().getUri().getPath();

        String bestMatchPattern = "";
        PresentationResolver bestMatchResolver = null;
        for (Map.Entry<String,PresentationResolver> entry : this.resolvers.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.equals(path)) {
                bestMatchPattern = pattern;
                bestMatchResolver = entry.getValue();
                break;
            }
            else {
                if (pattern.equals("*") ||
                    (pattern.startsWith("*") && path.endsWith(pattern.substring(1))) ||
                    (pattern.endsWith("*") && path.startsWith(pattern.substring(0,pattern.length()-1)))) {
                    if (pattern.length() > bestMatchPattern.length()) { 
                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer(log.msg("found candidate presentation resolver for URI path '"+path+"' using pattern '"+pattern+"'"));
                        }
                        bestMatchPattern = pattern;
                        bestMatchResolver = entry.getValue();
                    }
                }
            }
        }
        if (bestMatchResolver != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(log.msg("found presentation resolver for URI path '"+path+"' using pattern '"+bestMatchPattern+"'"));
            }
            return bestMatchResolver.getPresentation(request);
        }

        return null;
    }

}
