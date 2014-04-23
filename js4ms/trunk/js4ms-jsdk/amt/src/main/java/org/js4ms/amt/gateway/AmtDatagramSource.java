package org.js4ms.amt.gateway;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * AmtDatagramSource.java [org.js4ms.jsdk:amt]
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

import org.js4ms.amt.proxy.SourceFilter;
import org.js4ms.common.util.logging.Log;
import org.js4ms.io.channel.MessageSource;
import org.js4ms.io.channel.OutputChannel;
import org.js4ms.io.net.UdpDatagram;




/**
 * A {@link MessageSource} that constructs an AMT multicast endpoint to forward datagrams
 * sent to a specific port and one or more any-source or source-specific addresses.
 * The multicast group and source addresses are specified when an instance is constructed
 * using a {@link SourceFilter} object.
 * <p>
 * Forwarding can be enabled or disabled using the {@link #start()} and {@link #stop()}
 * methods.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public class AmtDatagramSource
                extends MessageSource<UdpDatagram> {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtDatagramSource.class.getName());

    /*-- Member Variables ----------------------------------------------------*/

    private final Log log = new Log(this);

    private final SourceFilter sourceFilter;

    private final AmtMulticastEndpoint amtEndpoint;

    /**
     * Constructs a message source for UDP datagrams sent to a multicast address.
     * 
     * @param destinationPort
     *            The destination port of the UDP stream.
     * @param sourceFilter
     *            A source filter that identifies the any-source multicast (ASM)
     *            or source-specific multicast (SSM) destination address and
     *            source host address(es) of the UDP datagrams. The source
     *            filter mode in each group record is ignored (assumed to be INCLUDE).
     * @param outputChannel
     *            The channel that will receive datagrams as they arrive.
     * @throws IOException
     *             If an I/O error occurred while constructing the AMT endpoint.
     */
    public AmtDatagramSource(final int destinationPort,
                             final SourceFilter sourceFilter,
                             final OutputChannel<UdpDatagram> outputChannel) throws IOException {
        super(outputChannel);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtDatagramSource", destinationPort, sourceFilter, outputChannel));
        }

        this.sourceFilter = sourceFilter;
        this.amtEndpoint = new AmtMulticastEndpoint(destinationPort, outputChannel);
    }

    /**
     * Constructs a message source for UDP datagrams sent to a multicast address.
     * 
     * @param destinationPort
     *            The destination port of the UDP stream.
     * @param sourceFilter
     *            A source filter that identifies the any-source multicast (ASM)
     *            or source-specific multicast (SSM) destination address and
     *            source host address(es) of the UDP datagrams. The source
     *            filter mode in each group record is ignored (assumed to be INCLUDE).
     * @param relayDiscoveryAddress
     *            The anycast or unicast address used to locate an AMT relay.
     * @param outputChannel
     *            The channel that will receive datagrams as they arrive.
     * @throws IOException
     *             If an I/O error occurred while constructing the AMT endpoint.
     */
    public AmtDatagramSource(final int destinationPort,
                             final SourceFilter sourceFilter,
                             final InetAddress relayDiscoveryAddress,
                             final OutputChannel<UdpDatagram> outputChannel) throws IOException {
        super(outputChannel);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("<ctor>", destinationPort, sourceFilter, relayDiscoveryAddress, outputChannel));
        }

        this.sourceFilter = sourceFilter;
        this.amtEndpoint = new AmtMulticastEndpoint(destinationPort, relayDiscoveryAddress, outputChannel);
    }

    /**
     * Performs actions required to start the message source.
     * 
     * @throws IOException
     *             If an I/O error occurs while starting the message source.
     * @throws InterruptedException 
     */
    @Override
    protected void doStart() throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("doStart"));
        }

        InetAddress groupAddress = this.sourceFilter.getGroupAddress();
        HashSet<InetAddress> sourceAddresses = this.sourceFilter.getSourceSet();
        if (sourceAddresses.size() > 0) {
            for (InetAddress sourceAddress : sourceAddresses) {
                this.amtEndpoint.join(groupAddress, sourceAddress);
            }
        }
        else {
            this.amtEndpoint.join(groupAddress);
        }
    }

    /**
     * Performs actions required to stop the message source.
     * 
     * @throws IOException
     *             If an I/O error occurs while stopping the message source.
     * @throws InterruptedException
     *             If the calling thread is interrupted while stopping the message source.
     */
    @Override
    protected void doStop() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("doStop"));
        }

        this.amtEndpoint.leave();
    }

    /**
     * Performs actions required to close the message source.
     * 
     * @throws IOException
     *             If an I/O error occurs while closing the message source.
     * @throws InterruptedException
     *             If the calling thread is interrupted while closing the message source.
     */
    @Override
    protected void doClose() throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("doClose"));
        }

        this.amtEndpoint.close();

        super.doClose();
    }

}
