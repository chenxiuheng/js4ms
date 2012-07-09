package com.larkwoodlabs.net.amt.gateway;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.channels.MessageKeyExtractor;
import com.larkwoodlabs.channels.OutputChannel;
import com.larkwoodlabs.channels.OutputChannelMap;
import com.larkwoodlabs.channels.OutputChannelTransform;
import com.larkwoodlabs.net.Precondition;
import com.larkwoodlabs.net.ip.IPPacket;
import com.larkwoodlabs.net.ip.ipv4.IPv4Packet;
import com.larkwoodlabs.net.ip.ipv6.IPv6Packet;
import com.larkwoodlabs.net.udp.UdpDatagram;
import com.larkwoodlabs.util.logging.Log;
import com.larkwoodlabs.util.logging.Logging;

public class AmtUDPInterface {

    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(AmtUDPInterface.class.getName());

    static final int MAX_REASSEMBLY_CACHE_SIZE = 100;

    /*-- Member Variables ---------------------------------------------------*/

    protected final Log log = new Log(this);

    private final AmtUDPInterfaceManager manager;

    private final AmtIPInterface ipInterface;

    private int referenceCount = 0;

    private ChannelMembershipManager ipv4MembershipManager = null;

    private ChannelMembershipManager ipv6MembershipManager = null;

    OutputChannelMap<IPPacket> outputChannelMap;

    /**
     * @param manager
     * @param relayDiscoveryAddress
     * @throws IOException
     */
    AmtUDPInterface(final AmtUDPInterfaceManager manager,
                    final InetAddress relayDiscoveryAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.AmtUDPInterface", manager, Logging.address(relayDiscoveryAddress)));
        }

        this.manager = manager;

        this.ipInterface = AmtIPInterfaceManager.getInstance().getInterface(relayDiscoveryAddress);

        // Create an extractor to differentiate between IGMP, MLD and other message types.
        MessageKeyExtractor<IPPacket> protocolExtractor = new MessageKeyExtractor<IPPacket>() {

            @Override
            public Byte getKey(IPPacket packet) {
                return packet.getVersion();
            }
        };

        // Create the output channel map that will be used to route IP packets to the
        // appropriate recipients.
        this.outputChannelMap = new OutputChannelMap<IPPacket>(protocolExtractor);

        // Create packet assembler that will reassemble packets from the IP interface and
        // forward them to the output channel map
        PacketAssembler assembler = new PacketAssembler(outputChannelMap,
                                                        MAX_REASSEMBLY_CACHE_SIZE,
                                                        new Timer("AMT UDP Interface"));

        this.ipInterface.addOutputChannel(assembler);

    }

    /**
     * Gets the Relay Discovery Address associated with this interface.
     * 
     * @return An InetAddress object containing the Relay Discovery Address
     */
    public final InetAddress getRelayDiscoveryAddress() {
        return this.ipInterface.getRelayDiscoveryAddress();
    }

    /**
     * 
     */
    final synchronized void acquire() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.acquire"));
        }

        this.referenceCount++;
    }

    /**
     * @throws InterruptedException
     * @throws IOException
     */
    final synchronized void release() throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.release"));
        }

        if (--this.referenceCount == 0) {
            this.manager.closeInterface(this);
        }
    }

    /**
     * @throws InterruptedException
     * @throws IOException
     */
    public final void close() throws InterruptedException, IOException {
        this.ipInterface.release();
    }

    private final void constructIPv4MembershipManager() {

        this.ipv4MembershipManager = new ChannelMembershipManager(this.ipInterface);
        // Create channels that transform route UDP packets to the appropriate channel
        // membership manager channels.
        this.outputChannelMap
                        .put(IPv4Packet.INTERNET_PROTOCOL_VERSION,
                             new OutputChannelTransform<IPPacket, UdpDatagram>(
                                                                               this.ipv4MembershipManager.getDispatchChannel(),
                                                                               new MulticastDataTransform()));
    }

    private final void constructIPv6MembershipManager() {

        this.ipv6MembershipManager = new ChannelMembershipManager(this.ipInterface);
        // Create channels that transform route UDP packets to the appropriate channel
        // membership manager channels.
        this.outputChannelMap
                        .put(IPv6Packet.INTERNET_PROTOCOL_VERSION,
                             new OutputChannelTransform<IPPacket, UdpDatagram>(
                                                                               this.ipv6MembershipManager.getDispatchChannel(),
                                                                               new MulticastDataTransform()));
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param port
     * @throws IOException
     * @throws InterruptedException
     */
    public final void join(final OutputChannel<UdpDatagram> pushChannel,
                           final InetAddress groupAddress,
                           int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.join", pushChannel, Logging.address(groupAddress), port));
        }

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4MembershipManager == null) {
                constructIPv4MembershipManager();
            }
            this.ipv4MembershipManager.join(pushChannel, groupAddress, port);
        }
        else {
            if (this.ipv6MembershipManager == null) {
                constructIPv6MembershipManager();
            }
            this.ipv6MembershipManager.join(pushChannel, groupAddress, port);
        }
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @param port
     * @throws IOException
     * @throws InterruptedException
     */
    public final void join(final OutputChannel<UdpDatagram> pushChannel,
                           final InetAddress groupAddress,
                           final InetAddress sourceAddress,
                           final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.join",
                                        pushChannel,
                                        Logging.address(groupAddress),
                                        Logging.address(sourceAddress),
                                        port));
        }

        Precondition.checkAddresses(groupAddress, sourceAddress);

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4MembershipManager == null) {
                constructIPv4MembershipManager();
            }
            this.ipv4MembershipManager.join(pushChannel, groupAddress, sourceAddress, port);
        }
        else {
            if (this.ipv6MembershipManager == null) {
                constructIPv6MembershipManager();
            }
            this.ipv6MembershipManager.join(pushChannel, groupAddress, sourceAddress, port);
        }
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel,
                            final InetAddress groupAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.leave", pushChannel, Logging.address(groupAddress)));
        }

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4MembershipManager == null) {
                constructIPv4MembershipManager();
            }
            this.ipv4MembershipManager.leave(pushChannel, groupAddress);
        }
        else {
            if (this.ipv6MembershipManager == null) {
                constructIPv6MembershipManager();
            }
            this.ipv6MembershipManager.leave(pushChannel, groupAddress);
        }
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param port
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel,
                            final InetAddress groupAddress,
                            final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.leave", pushChannel, Logging.address(groupAddress), port));
        }

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4MembershipManager == null) {
                constructIPv4MembershipManager();
            }
            this.ipv4MembershipManager.leave(pushChannel, groupAddress, port);
        }
        else {
            if (this.ipv6MembershipManager == null) {
                constructIPv6MembershipManager();
            }
            this.ipv6MembershipManager.leave(pushChannel, groupAddress, port);
        }
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel,
                            final InetAddress groupAddress,
                            final InetAddress sourceAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.leave",
                                        pushChannel,
                                        Logging.address(groupAddress),
                                        Logging.address(sourceAddress)));
        }

        Precondition.checkAddresses(groupAddress, sourceAddress);

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4MembershipManager == null) {
                constructIPv4MembershipManager();
            }
            this.ipv4MembershipManager.leave(pushChannel, groupAddress, sourceAddress);
        }
        else {
            if (this.ipv6MembershipManager == null) {
                constructIPv6MembershipManager();
            }
            this.ipv6MembershipManager.leave(pushChannel, groupAddress, sourceAddress);
        }
    }

    /**
     * @param pushChannel
     * @param groupAddress
     * @param sourceAddress
     * @param port
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel,
                            final InetAddress groupAddress,
                            final InetAddress sourceAddress,
                            final int port) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.leave",
                                        pushChannel,
                                        Logging.address(groupAddress),
                                        Logging.address(sourceAddress),
                                        port));
        }

        Precondition.checkAddresses(groupAddress, sourceAddress);

        if (groupAddress instanceof Inet4Address) {
            if (this.ipv4MembershipManager == null) {
                constructIPv4MembershipManager();
            }
            this.ipv4MembershipManager.leave(pushChannel, groupAddress, sourceAddress, port);
        }
        else {
            if (this.ipv6MembershipManager == null) {
                constructIPv6MembershipManager();
            }
            this.ipv6MembershipManager.leave(pushChannel, groupAddress, sourceAddress, port);
        }

    }

    /**
     * @param pushChannel
     * @throws IOException
     */
    public final void leave(final OutputChannel<UdpDatagram> pushChannel) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterface.leave", pushChannel));
        }

        if (this.ipv4MembershipManager != null) {
            this.ipv4MembershipManager.leave(pushChannel);
        }
        else if (this.ipv6MembershipManager != null) {
            this.ipv6MembershipManager.leave(pushChannel);
        }

    }
}
