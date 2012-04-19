package com.larkwoodlabs.net.amt.client;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.MessageSource;
import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.net.amt.SourceFilter;
import com.larkwoodlabs.net.udp.UdpDatagram;
import com.larkwoodlabs.util.logging.Log;

/**
 * A datagram source that constructs an AMT tunnel to receive packets sent to an ASM or SSM destination address
 * and port and forwards those packets to an {@link OutputChannel<UdpDatagram>} instance.
 * Forwarding can be enabled or disabled using the {@link #start()} and {@link #stop()} methods.
 * 
 * @author gbumgard@cisco.com
 */
public class AmtDatagramSource extends MessageSource<UdpDatagram> {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtDatagramSource.class.getName());


    /*-- Member Variables ----------------------------------------------------*/

    private final Log log = new Log(this);

    private final SourceFilter sourceFilter;

    private final AmtMulticastEndpoint amtEndpoint;


    /**
     * Constructs a relay for UDP datagrams sent to a multicast address.
     * @param sourcePort - The destination port of the UDP stream.
     * @param sourceFilter - A source filter that identifies the any-source multicast (ASM) 
     *                       or source-specific multicast (SSM) destination address and
     *                       source host address(es) of the UDP datagrams.
     * @param outputChannel - The channel that will receive datagrams as they arrive.
     * @throws IOException - If an I/O error occurred while constructing the AMT endpoint.
     */
    public AmtDatagramSource(final int sourcePort,
                             final SourceFilter sourceFilter,
                             final OutputChannel<UdpDatagram> outputChannel) throws IOException {
        super(outputChannel);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("AmtDatagramSource", sourcePort, sourceFilter, outputChannel));
        }

        this.sourceFilter = sourceFilter;
        this.amtEndpoint = new AmtMulticastEndpoint(sourcePort, outputChannel);
    }

    /**
     * Constructs a relay for UDP datagrams sent to a multicast address.
     * @param sourcePort - The destination port of the UDP stream.
     * @param sourceFilter - A source filter that identifies the any-source multicast (ASM) 
     *                       or source-specific multicast (SSM) destination address and
     *                       source host address(es) of the UDP datagrams.
     * @param relayDiscoveryAddress - The anycast or unicast address used to locate an AMT relay.
     * @param outputChannel - The channel that will receive datagrams as they arrive.
     * @throws IOException - If an I/O error occurred while constructing the AMT endpoint.
     */
    public AmtDatagramSource(final int sourcePort,
                             final SourceFilter sourceFilter,
                             final InetAddress relayDiscoveryAddress,
                             final OutputChannel<UdpDatagram> outputChannel) throws IOException {
        super(outputChannel);

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("<ctor>", sourcePort, sourceFilter, relayDiscoveryAddress, outputChannel));
        }

        this.sourceFilter = sourceFilter;
        this.amtEndpoint = new AmtMulticastEndpoint(sourcePort, relayDiscoveryAddress, outputChannel);
    }

    /**
     * Performs actions required to start the relay.
     * @throws IOException If an I/O error occurs while starting the relay.
     */
    @Override
    protected void doStart() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("doStart"));
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
     * Performs actions required to stop the relay.
     * @throws IOException If an I/O error occurs while stopping the relay.
     * @throws InterruptedException If the calling thread is interrupted while stopping the relay.
     */
    @Override
    protected void doStop() throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("doStop"));
        }
        
        this.amtEndpoint.leave();
    }

    /**
     * Performs actions required to close the relay.
     * @throws IOException If an I/O error occurs while closing the relay.
     * @throws InterruptedException If the calling thread is interrupted while closing the relay.
     */
    @Override
    protected void doClose() throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("doClose"));
        }

        this.amtEndpoint.close();

        super.doClose();
    }

}
