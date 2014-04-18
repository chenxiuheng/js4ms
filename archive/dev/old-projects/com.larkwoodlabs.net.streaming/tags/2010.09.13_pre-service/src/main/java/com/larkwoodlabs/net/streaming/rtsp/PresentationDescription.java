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

/**
 * Describes a single media presentation consisting of one or more media streams (audio, video, data).
 * A presentation description is identified by a URI and typically possesses an external representation
 * whose format is identified by a MIME type (e.g. <code>application/sdp</code>).
 *
 * @author Gregory Bumgardner
 */
public abstract class PresentationDescription {

    /*-- Inner Classes -------------------------------------------------------*/
    
    /**
     * Factory for presentation description objects.
     */
    public static class Factory {

        /*-- Inner Classes -------------------------------------------------------*/

        /**
         * Base class for concrete description factory classes.
         */
        public static abstract class AbstractFactory {
            public abstract boolean constructs(URI uri) throws RtspException;
            public abstract PresentationDescription construct(URI uri) throws RtspException;
        }

        /*-- Member Variables ----------------------------------------------------*/
        
        LinkedList<AbstractFactory> factories = new LinkedList<AbstractFactory>();
        
        final String ObjectId = Logging.identify(this);
        
        /*-- Member Functions ----------------------------------------------------*/

        /**
         * Constructs a presentation description given a URI.
         * This method iterates over the collection of registered factories
         * until it finds a factory that indicates that it can construct
         * a description based on the specified URI.
         */
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
    
    /**
     * Constructs a presentation description that is identified by the specified URI.
     */
    protected PresentationDescription(URI uri) {
        this.uri = uri;
    }
    
    /**
     * Returns the URI used to identify this presentation description.
     */
    public final URI getUri() {
        return this.uri;
    }

    /**
     * Returns a string use to construct a stream control URI (e.g. "trackID").
     */
    public abstract String getStreamControlIdentifier();

    /**
     * Returns the time and date that the presentation description was created
     * or last modified, if available. 
     */
    public abstract Date getDateLastModified();
    
    /**
     * Indicates whether the presentation can produce a description in the 
     * format identified by the specified MIME type. 
     * @param mimeType - the format requested by the client (e.g. <code>application/sdp</code>).
     */
    public abstract boolean isSupported(String mimeType);
    
    /**
     * Returns the MIME type of the presentation description (e.g. <code>application/sdp</code>).
     */
    public abstract String getMimeType();

    /**
     * Returns a string containing a serialized representation of the
     * proxy presentation description suitable for transmission to an RTSP client
     * in response to a DESCRIBE request.
     * @param mimeType - The format requested by the client (e.g. <code>application/sdp</code>).
     * @throws RtspException If a suitable client-side description cannot be generated for this presentation description.
     */
    public abstract String describe(String mimeType) throws RtspException;
    
    /**
     * Returns <code>true</code> if a presentation constructed from this
     * presentation description can be paused, and <code>false</code> if not.
     * Live presentations and presentations distributed via multicast generally
     * cannot be paused, though the RTSP server may still accept a pause request
     * (the streams are not actually paused - the stream delivery is stopped). 
     * @return
     */
    public abstract boolean isPauseAllowed();
    
    /**
     * Returns the media stream description for the stream identified by the specified index.
     * @param streamIndex - A numerical index that identifies a stream.
     *                      The stream index is typically assigned in the order that each stream
     *                      definition appears within the external presentation description.
     * @throws RtspException If there is no stream description associated with the specified index.
     */
    public MediaStreamDescription getStreamDescription(int streamIndex) throws RtspException {
        MediaStreamDescription streamDescription = this.streamDescriptions.elementAt(streamIndex);
        if (streamDescription == null) {
            throw RtspException.create(StatusCode.BadRequest,"SETUP request references a non-existent stream", ObjectId, logger);
        }
        return streamDescription;
    }
}
