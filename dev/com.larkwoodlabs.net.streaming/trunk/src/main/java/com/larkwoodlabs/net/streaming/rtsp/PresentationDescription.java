/*
 * Copyright © 2009-2010 Larkwood Labs Software.
 *
 * Licensed under the Larkwood Labs Software Source Code License, Version 1.0.
 * You may not use this file except in compliance with this License.
 *
 * You may view the Source Code License at
 * http://www.larkwoodlabs.com/source-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the license.
 */

package com.larkwoodlabs.net.streaming.rtsp;

import java.net.URI;
import java.util.Date;
import java.util.LinkedList;
import java.util.Vector;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Logging;

public abstract class PresentationDescription {

    /*-- Inner Classes -------------------------------------------------------*/
    
    public static class Factory {

        /*-- Inner Classes -------------------------------------------------------*/

        public static abstract class AbstractFactory {
            public abstract boolean constructs(URI uri) throws RtspException;
            public abstract PresentationDescription construct(URI uri) throws RtspException;
        }

        /*-- Member Variables ----------------------------------------------------*/
        
        LinkedList<AbstractFactory> factories = new LinkedList<AbstractFactory>();
        
        final String ObjectId = Logging.identify(this);
        
        /*-- Member Functions ----------------------------------------------------*/

        PresentationDescription construct(URI uri) throws RtspException {
            for (AbstractFactory factory : factories) {
                if (factory.constructs(uri)) {
                    return factory.construct(uri);
                }
            }
            throw RtspException.create(StatusCode.BadRequest, "cannot determine presentation type from specified URI", ObjectId, logger);
        }

        public void add(AbstractFactory concreteFactory) {
            this.factories.add(concreteFactory);
        }

    }

    
    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(PresentationDescription.class.getName());


    /*-- Static Functions ----------------------------------------------------*/
    
    public final static Factory getDefaultFactory() {
        Factory factory = new Factory();
        factory.add(new SDPPresentationDescription.Factory());
        return factory;
    }

    public final static PresentationDescription construct(URI uri) throws RtspException {
        return construct(uri, getDefaultFactory());
    }

    public final static PresentationDescription construct(URI uri, Factory factory) throws RtspException {
        return factory.construct(uri);
    }
 
    /*-- Member Variables ----------------------------------------------------*/
    
    protected URI uri;
    
    protected Vector<MediaStreamDescription> streamDescriptions = new Vector<MediaStreamDescription>();

    protected final String ObjectId = Logging.identify(this);


    /*-- Member Functions ----------------------------------------------------*/
    
    protected PresentationDescription(URI uri) {
        this.uri = uri;
    }
    
    public final URI getUri() {
        return this.uri;
    }

    public abstract String getStreamControlIdentifier();

    public abstract Date getDateLastModified();
    
    /**
     * Indicates whether the presentation can produce a description in the 
     * format identified by the specified MIME type. 
     * @param mimeType - the format requested by the client (e.g. application/sdp).
     */
    public abstract boolean isSupported(String mimeType);
    
    public abstract String getMimeType();

    /**
     * Returns presentation description as a string to be included in the
     * server response to an RTSP client DESCRIBE request.
     * @param mimeType - the format requested by the client (e.g. application/sdp).
     * @throws RtspException 
     */
    public abstract String describe(String mimeType) throws RtspException;
    
    
    public abstract boolean isPauseAllowed();
    
    public MediaStreamDescription getStreamDescription(int streamIndex) throws RtspException {
        MediaStreamDescription streamDescription = this.streamDescriptions.elementAt(streamIndex);
        if (streamDescription == null) {
            throw RtspException.create(StatusCode.BadRequest,"SETUP request references a non-existent stream", ObjectId, logger);
        }
        return streamDescription;
    }
}
