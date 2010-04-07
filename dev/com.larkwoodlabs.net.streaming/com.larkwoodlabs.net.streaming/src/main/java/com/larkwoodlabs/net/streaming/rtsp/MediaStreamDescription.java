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

public abstract class MediaStreamDescription {
    
    /*-- Inner Classes -------------------------------------------------------*/

    /*-- Static Constants ----------------------------------------------------*/

 
    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(MediaStreamDescription.class.getName());

    /*-- Static Functions ----------------------------------------------------*/

    
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

    protected MediaStreamDescription() {
    }

    public int getStreamIndex() {
        return this.streamIndex;
    }
    
    public TransportDescription.Protocol getProtocol() {
        return this.protocol;
    }

    public TransportDescription.Profile getProfile() {
        return this.profile;
    }

    public TransportDescription.Transport getTransport() {
        return this.transport;
    }

    public TransportDescription.Distribution getDistribution() {
        return this.distribution;
    }

    public int getFilterCount() {
        return this.filters.size();
    }

    public Iterator<SourceFilter> getFilterIterator() {
        return this.filters.iterator();
    }
    
    public Vector<SourceFilter> getFilters() {
        return this.filters;
    }
    
    public int getFirstSourcePort() {
        return this.firstSourcePort;
    }
    
    public int getSourcePortCount() {
        return this.sourcePortCount;
    }
    
}
