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

package com.larkwoodlabs.net.amt;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.logging.Logger;

import com.larkwoodlabs.net.Precondition;
import com.larkwoodlabs.util.logging.Logging;


final public class SourceFilter {

    /*-- Static Variables ---------------------------------------------------*/

    /**
     * MODE_IS_INCLUDE - indicates that the interface has a filter mode of
     * INCLUDE for the specified multicast address. The Source Address [i]
     * fields in this Group Record contain the interface's source list for
     * the specified multicast address, if it is non-empty.
     */
    public static final byte MODE_IS_INCLUDE = 1;

    /**
     * MODE_IS_EXCLUDE - indicates that the interface has a filter mode of
     * EXCLUDE for the specified multicast address. The Source Address [i]
     * fields in this Group Record contain the interface's source list for
     * the specified multicast address, if it is non-empty.
     */
    public static final byte MODE_IS_EXCLUDE = 2;

    /*-- Inner Classes ------------------------------------------------------*/

    public enum Mode {

        INCLUDE(MODE_IS_INCLUDE),
        EXCLUDE(MODE_IS_EXCLUDE);
        
        int value;
        Mode(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return this.value;
        }
    }
    

    /*-- Member Variables ---------------------------------------------------*/

    private final InetAddress groupAddress;
    private SourceFilter.Mode mode;
    private boolean isSSM;
    
    private HashSet<InetAddress> sources = new HashSet<InetAddress>();
    

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs a filter with a mode of INCLUDE and an empty source list.
     * @param groupAddress
     */
    public SourceFilter(final InetAddress groupAddress) {
        this.mode = Mode.INCLUDE;
        this.groupAddress = groupAddress;
        this.isSSM = Precondition.isSSMMulticastAddress(groupAddress);
    }

    /**
     * 
     * @param logger
     */
    public void log(final Logger logger) {
        logger.info(" : group-address=" + Logging.address(groupAddress));
        logger.info(" : filter-mode=" + (this.mode == Mode.INCLUDE ? "INCLUDE" : "EXCLUDE"));
        logger.info(" : ----> sources");
        for (InetAddress address : this.sources){
            logger.info(" : " + Logging.address(address));
        }
        logger.info(" : <---- sources");
    }

    /**
     * 
     * @return
     */
    public SourceFilter.Mode getMode() {
        return this.mode;
    }

    /**
     * 
     * @param mode
     */
    public void setMode(final SourceFilter.Mode mode) {
        this.mode = mode;
    }

    /**
     * 
     * @return
     */
    public InetAddress getGroupAddress() {
        return this.groupAddress;
    }

    /**
     * 
     * @return
     */
    public HashSet<InetAddress> getSourceSet() {
        return this.sources;
    }

    /**
     * 
     * @param newSourceSet
     */
    public void setSourceSet(final HashSet<InetAddress> newSourceSet) {
        this.sources = newSourceSet;
    }

    /**
     * 
     * @return
     */
    public boolean isEmpty() {
        return this.sources.isEmpty();
    }

    /**
     * 
     * @param sourceAddress
     * @return
     */
    public boolean isExcluded(final InetAddress sourceAddress) {
        return (this.mode == Mode.EXCLUDE && this.sources.contains(sourceAddress)) ||
               (this.mode == Mode.INCLUDE && this.sources.isEmpty());
    }

    /**
     * 
     * @param sourceAddress
     * @return
     */
    public boolean isIncluded(final InetAddress sourceAddress) {
        return (this.mode == Mode.INCLUDE && this.sources.contains(sourceAddress)) ||
               (this.mode == Mode.EXCLUDE && this.sources.isEmpty());
    }

    /**
     * 
     * @param sourceAddress
     * @return
     */
    public boolean isFiltered(final InetAddress sourceAddress) {
        return isExcluded(sourceAddress) || !isIncluded(sourceAddress);
    }

    /**
     * Sets filter to EXCLUDE mode and adds the address to the source set.
     * If the filter was in INCLUDE mode, the source list is cleared first.
     * @param sourceAddress
     */
    public void exclude(final InetAddress sourceAddress) {
        if (this.mode == Mode.INCLUDE) {
            this.mode = Mode.EXCLUDE;
            this.sources.clear();
        }
        this.sources.add(sourceAddress);
    }

    /**
     * Sets filter to INCLUDE mode and adds the address to the source set.
     * If the filter was in EXCLUDE mode, the source list is cleared first.
     * @param sourceAddress
     */
    public void include(final InetAddress sourceAddress) {
        if (this.mode == Mode.EXCLUDE) {
            this.mode = Mode.INCLUDE;
            this.sources.clear();
        }
        this.sources.add(sourceAddress);
    }
    
    /**
     * Clears source list and sets mode to EXCLUDE so no sources are filtered.
     * Typically called when joining an any-source multicast (ASM) group.
     * @throws IOException If an attempt is made to join a group already joined.
     */
    public void join() throws IOException {
        if (this.mode == Mode.INCLUDE) {
            this.mode = Mode.EXCLUDE;
        }
        else {
            // The filter was already in EXCLUDE mode - illegal attempt to join the same group again
            throw new IOException("illegal attempt made to join an ASM group to which the channel already subscribes");
        }
    }
    
    /**
     * Clears source list and sets mode to INCLUDE so all sources are filtered.
     * Typically called when leaving an ASM or SSM group.
     * @throws IOException If an attempt is made to leave a group that has not been joined.
     */
    public void leave() throws IOException {
        if (this.mode == Mode.EXCLUDE) {
            this.mode = Mode.INCLUDE;
            this.sources.clear();
        }
        else if (this.isSSM) {
            this.sources.clear();
        }
        else {
            // The filter was already in INCLUDE mode - illegal attempt to leave the same group again
            throw new IOException("illegal attempt made to leave a group to which the channel is not subscribed");
        }
    }

    /**
     * 
     * @param sourceAddress
     * @throws IOException
     */
    public void join(final InetAddress sourceAddress) throws IOException {
        if (!this.sources.contains(sourceAddress)) {
            this.sources.add(sourceAddress);
        }
        else {
            throw new IOException("illegal attempt made to join a source in an SSM group to which the channel already subscribes");
        }
    }
    
    /**
     * 
     * @param sourceAddress
     * @throws IOException
     */
    public void leave(final InetAddress sourceAddress) throws IOException {
        if (this.sources.contains(sourceAddress)) {
            this.sources.remove(sourceAddress);
        }
        else {
            throw new IOException("illegal attempt made to leave a source in an SSM group to which the channel is not subscribed");
        }
    }
    
}