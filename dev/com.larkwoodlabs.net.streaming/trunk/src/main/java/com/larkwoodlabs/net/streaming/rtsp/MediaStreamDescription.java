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

import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import com.larkwoodlabs.net.amt.SourceFilter;
import com.larkwoodlabs.util.logging.Logging;

/**
 * Describes a single RTSP media stream.
 *
 * @author Gregory Bumgardner
 */
public abstract class MediaStreamDescription {
    
 
    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(MediaStreamDescription.class.getName());

    
    /*-- Member Variables ----------------------------------------------------*/

    protected final String ObjectId = Logging.identify(this);

    int streamIndex;

    TransportDescription.Protocol protocol = TransportDescription.Protocol.RTP;
    TransportDescription.Profile profile = TransportDescription.Profile.AVP;
    TransportDescription.Transport transport = TransportDescription.Transport.UDP;
    TransportDescription.Distribution distribution = TransportDescription.Distribution.multicast;
    
    Vector<SourceFilter> filters = null;
    
    int firstSourcePort;
    int sourcePortCount;
    
    
    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs media stream description with a default
     * protocol of {@link TransportDescription.Protocol#RTP RTP},
     * profile of {@link TransportDescription.Profile#AVP AVP},
     * transport of {@link TransportDescription.Transport#UDP UDP}, and
     * distribution of {@link TransportDescription.Distribution#multicast multicast}.
     */
    protected MediaStreamDescription() {
    }

    /**
     * Returns the stream index associated with the media stream.
     * @return
     */
    public int getStreamIndex() {
        return this.streamIndex;
    }
    
    /**
     * Returns the value of the {@link TransportDescription.Protocol} attribute.
     */
    public TransportDescription.Protocol getProtocol() {
        return this.protocol;
    }

    /**
     * Returns the value of the {@link TransportDescription.Profile} attribute.
     */
    public TransportDescription.Profile getProfile() {
        return this.profile;
    }

    /**
     * Returns the value of the {@link TransportDescription.Transport} attribute.
     */
    public TransportDescription.Transport getTransport() {
        return this.transport;
    }

    /**
     * Returns the value of the {@link TransportDescription.Distribution} attribute.
     */
    public TransportDescription.Distribution getDistribution() {
        return this.distribution;
    }

    /**
     * Returns the size of {@link SourceFilter} collection associated with this media stream description.
     */
    public int getFilterCount() {
        return this.filters.size();
    }

    /**
     * Returns an iterator for the {@link SourceFilter} collection associated with this media stream description.
     */
    public Iterator<SourceFilter> getFilterIterator() {
        return this.filters.iterator();
    }
    
    /**
     * Returns the {@link SourceFilter} collection associated with this media stream description.
     */
    public Vector<SourceFilter> getFilters() {
        return this.filters;
    }
    
    /**
     * Returns the first port number to use when constructing media streams from this description.
     */
    public int getFirstSourcePort() {
        return this.firstSourcePort;
    }
    
    /**
     * Returns the port count.
     */
    public int getSourcePortCount() {
        return this.sourcePortCount;
    }
    
}
