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
import java.util.LinkedList;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Logging;


/**
 * Constructs a list of {@link TransportDescription} instances initialized from the transport specifiers 
 * and parameters contained in an RTSP Transport header.
 * This list is used by the {@link MediaStream.Factory} to guide stream construction in response
 * to a client setup request.
 * 
 * @author Gregory Bumgardner
 */
public final class TransportPreferences {

    /*-- Member Variables ----------------------------------------------------*/

    private LinkedList<TransportDescription> descriptions = new LinkedList<TransportDescription>();
    
    final String ObjectId = Logging.identify(this);
    
    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Converts a comma-separated list of transport descriptions from an RTSP Transport header
     * into a list of {@link TransportDescription} instances
     * @throws RtspException 
     */
    public TransportPreferences(String header) throws RtspException {
        add(header);
    }
    
    public void log(Logger logger) {
        logger.info(ObjectId + " ----> Transport Preferences");
        for (TransportDescription description : this.descriptions) {
            description.log(logger);
        }
        logger.info(ObjectId + " <---- Transport Preferences");
    }

    Iterator<TransportDescription> getIterator() {
        return this.descriptions.iterator();
    }
    
    public void add(String header) throws RtspException {
        String[] descriptions = header.split(",");
        for (String description : descriptions) {
            this.descriptions.add(new TransportDescription(description));
        }
    }
}
