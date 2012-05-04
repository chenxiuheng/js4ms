/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: MembershipQuery.java (com.larkwoodlabs.net.amt)
 * 
 * Copyright � 2010-2012 Cisco Systems, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.larkwoodlabs.net.amt;

import java.net.InetAddress;
import java.util.HashSet;

public final class MembershipQuery {

    /*-- Member Variables ---------------------------------------------------*/

    private final InetAddress groupAddress;
    
    private HashSet<InetAddress> sourceAddresses;
    
    private final int maximumResponseDelay;
    private final int robustnessVariable;
    private final int queryInterval;
    

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * 
     * @param groupAddress - The multicast address of the group.
     * @param sourceSet - The source set, if any, listed in the query. May be null.
     *                    This class stores a reference to the original HashSet. 
     * @param maximumResponseDelay - Sets the maximum bounds for the random response delay.
     * @param robustnessVariable - Used to specify the number of times an unsolicited 
     *                             state change report should be sent.
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
     * 
     * @return
     */
    public boolean isGeneralQuery() {
        return this.groupAddress.isAnyLocalAddress();
    }
    
    /**
     * 
     * @return
     */
    public boolean isGroupQuery() {
        return !isGeneralQuery() && !isSourceQuery();
    }
    
    /**
     * 
     * @return
     */
    public boolean isSourceQuery() {
        return !isGeneralQuery() && this.sourceAddresses != null && !this.sourceAddresses.isEmpty();
    }

    /**
     * 
     * @return
     */
    public int getMaximumResponseDelay() {
        return this.maximumResponseDelay;
    }

    /**
     * 
     * @return
     */
    public int getRobustnessVariable() {
        return this.robustnessVariable;
    }

    /**
     * 
     * @return
     */
    public int getQueryInterval() {
        return this.queryInterval;
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
    public HashSet<InetAddress> getSourceAddresses() {
        return this.sourceAddresses;
    }
    
    /**
     * 
     * @param sourceAddresses
     */
    public void setSourceAddresses(final HashSet<InetAddress> sourceAddresses) {
        this.sourceAddresses = sourceAddresses;
    }

    /**
     * 
     * @param sourceAddress
     */
    public void addSourceAddress(final InetAddress sourceAddress) {
        this.sourceAddresses.add(sourceAddress);
    }

}
