package org.js4ms.amt.proxy;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MembershipQuery.java [org.js4ms.jsdk:amt]
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


import java.net.InetAddress;
import java.util.HashSet;

/**
 * A protocol-independent representation of an IGMPv3 or MLDv2 query message.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public final class MembershipQuery {

    /*-- Member Variables ---------------------------------------------------*/

    private final InetAddress groupAddress;

    private HashSet<InetAddress> sourceAddresses;

    private final int maximumResponseDelay;

    private final int robustnessVariable;

    private final int queryInterval;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Constructs an instance from the specified parameters.
     * 
     * @param groupAddress
     *            - The multicast address of the group.
     * @param sourceSet
     *            - The source set, if any, listed in the query. May be null.
     *            This class stores a reference to the original HashSet.
     * @param maximumResponseDelay
     *            - Sets the maximum bounds for the random response delay.
     * @param robustnessVariable
     *            - Used to specify the number of times an unsolicited
     *            state change report should be sent.
     */
    public MembershipQuery(final InetAddress groupAddress,
                           final HashSet<InetAddress> sourceSet,
                           final int maximumResponseDelay,
                           final int robustnessVariable,
                           final int queryInterval) {
        this.groupAddress = groupAddress;
        this.sourceAddresses = sourceSet;
        this.maximumResponseDelay = maximumResponseDelay;
        this.robustnessVariable = robustnessVariable;
        this.queryInterval = queryInterval;
    }

    /**
     * Indicates whether the membership query is a general query (the group address
     * is unspecified - all zeroes).
     * 
     * @return A boolean value where <code>true</code> indicates that the
     *         query is a general query.
     */
    public boolean isGeneralQuery() {
        return this.groupAddress.isAnyLocalAddress();
    }

    /**
     * Indicates whether the membership query is a group-specific query (the group
     * address is not zero).
     * 
     * @return A boolean value where <code>true</code> indicates that the query is a
     *         group-specific query.
     */
    public boolean isGroupQuery() {
        return !isGeneralQuery() && !isSourceQuery();
    }

    /**
     * Indicates whether the membership query is a group and source-specific query (the
     * group address
     * is not zero and one or more sources are included).
     * 
     * @return A boolean value where <code>true</code> indicates that the query is
     *         a group and source-specific query.
     */
    public boolean isSourceQuery() {
        return !isGeneralQuery() && this.sourceAddresses != null && !this.sourceAddresses.isEmpty();
    }

    /**
     * Gets the maximum response delay value specified when the query object was
     * constructed.
     * This value is linked the Max Resp Code value contained an IGMPv3 or MLDv2 query
     * packet.
     * 
     * @return An integer number of seconds.
     */
    public int getMaximumResponseDelay() {
        return this.maximumResponseDelay;
    }

    /**
     * Gets the robustness variable value specified when the query object was constructed.
     * This value is linked to the Robustness Variable value contained in an IGMPv3 or
     * MLDv2 query packet.
     * 
     * @return An integer value indicating the number of retransmissions that should
     *         be made to report a group membership state change.
     */
    public int getRobustnessVariable() {
        return this.robustnessVariable;
    }

    /**
     * Gets the query interval value specified when the query object was constructed.
     * This value is linked to the QQIC value contained in an IGMPv2 or MLDv2 query
     * packet.
     * 
     * @return An integer number of seconds.
     */
    public int getQueryInterval() {
        return this.queryInterval;
    }

    /**
     * Gets the multicast group address specified when the query object was constructed.
     * 
     * @return An InetAddress containing the multicast group address being queried.
     */
    public InetAddress getGroupAddress() {
        return this.groupAddress;
    }

    /**
     * Gets the source address set.
     * 
     * @return A HashSet of source addresses that are being queried.
     */
    public HashSet<InetAddress> getSourceAddresses() {
        return this.sourceAddresses;
    }

    /**
     * Sets the source address set.
     * 
     * @param sourceAddresses
     *            A HashSet containing source addresses being queried.
     */
    public void setSourceAddresses(final HashSet<InetAddress> sourceAddresses) {
        this.sourceAddresses = sourceAddresses;
    }

    /**
     * Adds a source address to the set of source address associated with the query
     * object.
     * 
     * @param sourceAddress
     *            An InetAddress containing a source address.
     *            The source address address family (IPv4 or IPv6) must match that of the
     *            group address.
     */
    public void addSourceAddress(final InetAddress sourceAddress) {
        this.sourceAddresses.add(sourceAddress);
    }

}
