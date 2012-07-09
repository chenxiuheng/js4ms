package com.larkwoodlabs.net.amt.gateway;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.Log;
import com.larkwoodlabs.util.logging.Logging;


public class AmtUDPInterfaceManager {

    /*-- Static Variables ---------------------------------------------------*/

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(AmtUDPInterfaceManager.class.getName());

    /**
     * The singleton AmtTunnelTransport instance.
     */
    private static final AmtUDPInterfaceManager instance = new AmtUDPInterfaceManager();

    /*-- Static Functions ---------------------------------------------------*/

    /**
     * @return
     */
    public static AmtUDPInterfaceManager getInstance() {
        return AmtUDPInterfaceManager.instance;
    }

    /**
     * @return
     */
    public static InetAddress getDefaultRelayDiscoveryAddress() {
        return AmtIPInterfaceManager.getDefaultRelayDiscoveryAddress();
    }

    /*-- Member Variables ---------------------------------------------------*/

    private final Log log = new Log(this);

    /**
     * Map containing AMT UDP interfaces mapped to relay discovery addresses.
     */
    private HashMap<InetAddress, AmtUDPInterface> interfaces;

    /*-- Member Functions ---------------------------------------------------*/

    /**
     * Private constructor used to construct singleton instance.
     * Use {@link #getInstance()} to retrieve the singleton instance.
     */
    private AmtUDPInterfaceManager() {
        this.interfaces = new HashMap<InetAddress, AmtUDPInterface>();
    }

    /**
     * @param relayDiscoveryAddress
     * @return
     * @throws IOException
     */
    public synchronized AmtUDPInterface getInterface(final InetAddress relayDiscoveryAddress) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterfaceManager.getInterface", Logging.address(relayDiscoveryAddress)));
        }

        AmtUDPInterface udpInterface = this.interfaces.get(relayDiscoveryAddress);

        if (udpInterface == null) {

            if (logger.isLoggable(Level.FINE)) {
                logger.fine(this.log.msg("constructing new AmtUDPInterface"));
            }

            udpInterface = new AmtUDPInterface(this, relayDiscoveryAddress);
            this.interfaces.put(relayDiscoveryAddress, udpInterface);
        }

        udpInterface.acquire();

        return udpInterface;
    }

    /**
     * @param udpInterface
     * @throws InterruptedException
     * @throws IOException
     */
    synchronized void closeInterface(final AmtUDPInterface udpInterface) throws InterruptedException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(this.log.entry("AmtUDPInterfaceManager.closeInterface", udpInterface));
        }

        udpInterface.close();

        // Remove the endpoint from the endpoints map
        this.interfaces.remove(udpInterface.getRelayDiscoveryAddress());
    }


}
