package org.js4ms.io.net;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MulticastEndpoint.java [org.js4ms.jsdk:io]
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

import org.js4ms.io.channel.MessageInput;



/**
 * Interface exposed by objects that forward multicast datagrams as requested
 * using the join and leave methods.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public interface MulticastEndpoint
                extends MessageInput<UdpDatagram> {

    /**
     * Joins the specified any-source multicast group address.
     * 
     * @param groupAddress
     *            The multicast group to join.
     * @throws IOException
     *             The operation failed.
     * @throws InterruptedException 
     */
    void join(InetAddress groupAddress) throws IOException, InterruptedException;

    /**
     * Joins the specified source-specific multicast source-group address pair.
     * 
     * @param groupAddress
     *            The multicast group to join.
     * @param sourceAddress
     *            The address of a source for the specified group.
     * @throws IOException
     *             The operation failed.
     * @throws InterruptedException 
     */
    void join(InetAddress groupAddress, InetAddress sourceAddress) throws IOException, InterruptedException;

    /**
     * Starts forwarding of datagrams sent to the specified any-source multicast
     * group address and UDP port number. This operation may result in the endpoint
     * joining the
     * specified group. See {@link #join(InetAddress)}.
     * 
     * @param groupAddress
     *            The multicast group to join.
     * @param port
     *            A UDP port number in the range 0-32767.
     * @throws IOException
     * @throws InterruptedException 
     */
    void join(InetAddress groupAddress, int port) throws IOException, InterruptedException;

    /**
     * Starts forwarding of datagrams sent to the specified source-specific
     * multicast address pair and UDP port number. This operation may result in the
     * endpoint joining the specified source-group pair.
     * See {@link #join(InetAddress,InetAddress)}.
     * 
     * @param groupAddress
     *            The multicast group to join.
     * @param sourceAddress
     *            The address of a source for the specified group.
     * @param port
     *            A UDP port number in the range 0-32767.
     * @throws IOException
     *             The operation failed.
     * @throws InterruptedException 
     */
    void join(InetAddress groupAddress, InetAddress sourceAddress, int port) throws IOException, InterruptedException;

    /**
     * Leaves the specified multicast group. This method will cause the
     * endpoint to leave any any-source multicast group and all source-specific
     * multicast pairs with that group address.
     * 
     * @param groupAddress
     *            The multicast group to leave.
     * @throws IOException
     *             The operation failed.
     */

    void leave(InetAddress groupAddress) throws IOException;

    /**
     * Leaves the specified source-specific multicast source group pair.
     * 
     * @param groupAddress
     *            The multicast group to leave.
     * @param sourceAddress
     *            The address of a source for the specified group.
     * @throws IOException
     *             The operation failed.
     */
    void leave(InetAddress groupAddress, InetAddress sourceAddress) throws IOException;

    /**
     * Stops forwarding of datagrams sent to the specified group address and
     * UDP port number. This operation may result in the endpoint leaving the specified
     * group address. See {@link #leave(InetAddress)}.
     * 
     * @param groupAddress
     *            The multicast group to leave.
     * @param port
     *            A UDP port number in the range 0-32767.
     * @throws IOException
     */
    void leave(InetAddress groupAddress, int port) throws IOException;

    /**
     * Stops forwarding of datagrams sent to the specified source-specific multicast
     * source-group address pair and UDP port number. This operation may result in the
     * endpoint leaving the source-group pair entirely.
     * See {@link #leave(InetAddress,InetAddress)}.
     * 
     * @param groupAddress
     *            The multicast group to leave.
     * @param sourceAddress
     *            The address of a source for the specified group.
     * @param port
     *            A UDP port number in the range 0-32767.
     * @throws IOException
     *             The operation failed.
     */
    void leave(InetAddress groupAddress, InetAddress sourceAddress, int port) throws IOException;

    /**
     * Leaves all any-source and source-specific groups that
     * are currently joined.
     * 
     * @throws IOException
     *             The operation failed.
     */
    void leave() throws IOException;
}
