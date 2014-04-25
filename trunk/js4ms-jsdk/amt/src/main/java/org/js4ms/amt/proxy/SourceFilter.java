package org.js4ms.amt.proxy;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * SourceFilter.java [org.js4ms.jsdk:amt]
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


import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Logging;


/**
 * Describes the current reception state for a single multicast group address.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
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

    /**
     * An enumeration of filter modes.
     */
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

    private HashSet<InetAddress> sources = new HashSet<InetAddress>();

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs a filter with a mode of {@link Mode#INCLUDE INCLUDE} and an empty source
     * list.
     * 
     * @param groupAddress
     *            - The address of the multicast group whose source filter
     *            state is described by this object.
     */
    public SourceFilter(final InetAddress groupAddress) {
        this.mode = Mode.INCLUDE;
        this.groupAddress = groupAddress;
    }

    /**
     * Logs the values for private members using the specified logger.
     * 
     * @param logger
     */
    public void log(final Logger logger, final Level level) {
        logger.log(level," : group-address=" + Logging.address(groupAddress));
        logger.log(level," : filter-mode=" + (this.mode == Mode.INCLUDE ? "INCLUDE" : "EXCLUDE"));
        logger.log(level," : ----> sources");
        for (InetAddress address : this.sources) {
            logger.log(level," : " + Logging.address(address));
        }
        logger.log(level," : <---- sources");
    }

    /**
     * Returns the current {@link SourceFilter.Mode Mode}.
     * 
     * @return
     */
    public SourceFilter.Mode getMode() {
        return this.mode;
    }

    /**
     * Sets the filter {@link SourceFilter.Mode Mode}.
     * 
     * @param mode
     *            - The new filter mode.
     */
    public void setMode(final SourceFilter.Mode mode) {
        this.mode = mode;
    }

    /**
     * Returns the address of the multicast group whose filter state is
     * described by this object.
     */
    public InetAddress getGroupAddress() {
        return this.groupAddress;
    }

    /**
     * Returns a reference to the current source list.
     */
    public HashSet<InetAddress> getSourceSet() {
        return this.sources;
    }

    /**
     * Replaces the current source list with a new source list.
     * 
     * @param newSourceSet
     *            - The new source list.
     */
    public void setSourceSet(final HashSet<InetAddress> newSourceSet) {
        this.sources = newSourceSet;
    }

    /**
     * Returns <code>true</code> if the filter's source list is empty.
     */
    public boolean isEmpty() {
        return this.sources.isEmpty();
    }

    /**
     * Indicates whether the specified source address is excluded based on the
     * current state of the filter.
     * 
     * @param sourceAddress
     *            - The source address to check.
     */
    public boolean isExcluded(final InetAddress sourceAddress) {
        return (this.mode == Mode.EXCLUDE && this.sources.contains(sourceAddress)) ||
               (this.mode == Mode.INCLUDE && this.sources.isEmpty());
    }

    /**
     * Indicates whether the specified source address is included based on the
     * current state of the filter.
     * 
     * @param sourceAddress
     *            - The source address to check.
     */
    public boolean isIncluded(final InetAddress sourceAddress) {
        return (this.mode == Mode.INCLUDE && this.sources.contains(sourceAddress)) ||
               (this.mode == Mode.EXCLUDE && this.sources.isEmpty());
    }

    /**
     * @param sourceAddress
     * @return
     */
    public boolean isFiltered(final InetAddress sourceAddress) {
        return isExcluded(sourceAddress) || !isIncluded(sourceAddress);
    }

    /**
     * Updates the source filter from another source filter. <li>If this filter is in the
     * INCLUDE mode and the other filter is in the EXCLUDE mode, this filter is changed to
     * match the other filter. <li>If this filter is in the INCLUDE mode and the other
     * filter is in the INCLUDE mode, the source list of the other filter is merged <li>If
     * this filter is in the EXCLUDE mode and the other filter is in the INCLUDE mode,
     * this filter is changed to match the other filter. <li>If this filter is in the
     * EXCLUDE mode and the other filter is in the EXCLUDE mode, the source list of the
     * other filter is merged with this filter's source list.
     * 
     * @param sourceAddress
     */
    public void apply(final SourceFilter filter) {
        if (this.mode != filter.mode) {
            this.mode = filter.mode;
            this.sources = new HashSet<InetAddress>(filter.sources);
        }
        else {
            this.sources.addAll(filter.sources);
        }
    }

    /**
     * Sets filter to EXCLUDE mode and adds the address to the source set.
     * If the filter was in INCLUDE mode, the source list is cleared first.
     * 
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
     * 
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
     * 
     * @throws IOException
     *             If an attempt is made to join a group already joined.
     */
    public void join() throws IOException {
        if (this.mode == Mode.INCLUDE) {
            this.mode = Mode.EXCLUDE;
        }
        else {
            // The filter was already in EXCLUDE mode - illegal attempt to join the same
            // group again
            throw new IOException("illegal attempt made to join an ASM group to which the channel already subscribes");
        }
    }

    /**
     * Clears source list and sets mode to INCLUDE so all sources are filtered.
     * Typically called when leaving an ASM or SSM group.
     * 
     * @throws IOException
     *             If an attempt is made to leave a group that has not been joined.
     */
    public void leave() throws IOException {
        if (this.mode == Mode.EXCLUDE) {
            this.mode = Mode.INCLUDE;
            this.sources.clear();
        }
        else if (!isEmpty()) {
            this.sources.clear();
        }
        else {
            // The filter was already in INCLUDE mode - illegal attempt to leave the same
            // group again
            throw new IOException("illegal attempt made to leave a group to which the channel is not subscribed");
        }
    }

    /**
     * Adds the specified source address to the source list to indicate
     * an SSM join for the source and source filter group.
     * 
     * @param sourceAddress
     *            - The source address component of an (S,G) pair to be added to this
     *            filter.
     * @throws IOException
     *             If the specified source address is already contained in the filter
     *             source list.
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
     * Removes the specified source address from the source list to indicate
     * an SSM leave for the source and source filter group.
     * 
     * @param sourceAddress
     *            - The source address component of an (S,G) pair to be removed from this
     *            filter.
     * @throws IOException
     *             If the specified source address is not contained in the filter source
     *             list.
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
