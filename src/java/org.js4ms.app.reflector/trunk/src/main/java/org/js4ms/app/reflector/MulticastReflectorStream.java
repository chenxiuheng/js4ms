package org.js4ms.app.reflector;

import gov.nist.javax.sdp.fields.AttributeField;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;

import org.js4ms.common.util.logging.Logging;
import org.js4ms.io.channels.MessageSource;
import org.js4ms.io.channels.OutputChannel;
import org.js4ms.net.multicast.service.amt.gateway.AmtPseudoInterfaceManager;
import org.js4ms.net.multicast.service.proxy.SourceFilter;
import org.js4ms.service.protocol.rtsp.TransportDescription;
import org.js4ms.service.protocol.rtsp.presentation.MediaStream;
import org.js4ms.service.protocol.rtsp.presentation.Presentation;




public class MulticastReflectorStream extends MediaStream {

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * 
     */
    protected final SessionDescription inputSessionDescription;

    protected final MediaDescription inputMediaDescription;
 
    protected final InetAddress relayDiscoveryAddress;

    protected final TransportDescription inputTransportDescription;

    /**
     * 
     * @param inputMediaDescription - SDP description of the reflector input stream (multicast).
     * @param outputMediaDescription - SDP description of the reflector output stream (unicast).
     * @throws SdpException
     */
    public MulticastReflectorStream(final Presentation presentation,
                                    final int streamIndex,
                                    final SessionDescription inputSessionDescription,
                                    final MediaDescription inputMediaDescription,
                                    final SessionDescription outputSessionDescription,
                                    final MediaDescription outputMediaDescription) throws SdpException {
        super(presentation, streamIndex, outputSessionDescription, outputMediaDescription);
        this.inputSessionDescription = inputSessionDescription;
        this.inputMediaDescription = inputMediaDescription;
        this.inputTransportDescription = new TransportDescription(inputSessionDescription, inputMediaDescription);
        this.relayDiscoveryAddress = getRelayDiscoveryAddress(inputSessionDescription, inputMediaDescription);
    }

    @Override
    protected boolean doIsPauseSupported() {
        return true;
    }

    @Override
    protected boolean doIsPlaySupported() {
        return true;
    }

    @Override
    protected boolean doIsRecordSupported() {
        return false;
    }

    @Override
    protected boolean isServerSourceChannelRequired() {
        return true;
    }

    @Override
    protected boolean isClientSourceChannelRequired() {
        return true;
    }

    /**
     * 
     * @param layerIndex - The stream layer index.
     * @param channelIndex - The index used to select a port within a layer (e.g. 0=RTP and 1=RTCP).
     * @param packetSink - The output channel that will receive packets generated by the packet source.
     * @return
     * @throws SdpException 
     * @throws IOException 
     */
    @Override
    protected MessageSource<ByteBuffer> constructServerPacketSource(final int layerIndex,
                                                                    final int channelIndex,
                                                                    final OutputChannel<ByteBuffer> packetSink) throws SdpException, IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("constructServerPacketSource", layerIndex, channelIndex, packetSink));
        }

        // Compute layer address by adding index to base address.

        byte[] address = this.inputTransportDescription.getDestination().getAddress();

        int i = address.length - 1;
        address[i] += layerIndex;
        while (i > 0 && address[i--] == 0) {
            address[i]++;
        }

        InetAddress groupAddress = InetAddress.getByAddress(address);

        // Construct source filter for layer address (describes group membership state).
        // A separate filter is constructed for each channel, even though
        // an RTP/RTCP channel pair shares the same filter state.
        SourceFilter filter = constructSourceFilter(groupAddress);

        // Compute port number
        int portOffset = layerIndex * this.inputTransportDescription.getPortsPerLayer() + channelIndex;
        int port = this.inputTransportDescription.getFirstClientPort() + portOffset;

        MulticastPacketSource packetSource = new MulticastPacketSource(port, filter, this.relayDiscoveryAddress, packetSink);

        return packetSource;
    }

    @Override
    public OutputChannel<ByteBuffer> constructServerPacketSink(final int layerIndex, final int channelIndex) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("constructServerPacketSource", layerIndex, channelIndex));
        }

        return new OutputChannel<ByteBuffer>() {

            @Override
            public void send(ByteBuffer message, int milliseconds) throws IOException, InterruptedIOException, InterruptedException {
                if (logger.isLoggable(Level.FINE)) {
                    logger.finer(log.msg("received packet from client on layer="+layerIndex+" channel="+channelIndex));
                }
            }

            @Override
            public void close() throws IOException, InterruptedException {
            }
        };
    }

    /**
     * Constructs a {@link SourceFilter} instance for a group address.
     * @throws SdpException 
     */
    private SourceFilter constructSourceFilter(final InetAddress groupAddress) throws SdpException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("constructSourceFilter", Logging.address(groupAddress)));
        }

        SourceFilter filter = new SourceFilter(groupAddress);

        // Iterate over any media level source-filter attribute records
        Vector<?> attributes = this.inputMediaDescription.getAttributes(false);
        if (attributes != null) {
            // Iterate over all of the media level attributes looking for source-filter records
            for (int j=0; j<attributes.size(); j++) {
                Object o = attributes.elementAt(j);
                if (o instanceof AttributeField) {
                    AttributeField field = (AttributeField)o;
                    String name = field.getName();
                    if (name != null) {
                        if (name.equals(MulticastReflectorFactory.SOURCE_FILTER_SDP_ATTRIBUTE)) {
                            String value;
                            value = field.getValue();
                            if (value != null) {
                                applySourceFilterAttribute(filter, value, false);
                            }
                        }
                    }
                }
            }
        }

        attributes = this.inputSessionDescription.getAttributes(false);
        if (attributes != null) {
            // Iterate over all of the session level attributes looking for source-filter records
            for (int j=0; j<attributes.size(); j++) {
                Object o = attributes.elementAt(j);
                if (o instanceof AttributeField) {
                    AttributeField field = (AttributeField)o;
                    String name = field.getName();
                    if (name != null) {
                        if (name.equals(MulticastReflectorFactory.SOURCE_FILTER_SDP_ATTRIBUTE)) {
                            String value;
                            value = field.getValue();
                            if (value != null) {
                                applySourceFilterAttribute(filter, value, true);
                            }
                        }
                    }
                }
            }
        }
        return filter;
    }

    /**
     * Updates the source filter for a single media stream address.
     * <pre>
     * The source-filter attribute has the following syntax:
     * 
     *        a=source-filter: <filter-mode> <filter-spec>
     * 
     *    The <filter-mode> is either "incl" or "excl" (for inclusion or
     *    exclusion, respectively).  The <filter-spec> has four sub-components:
     * 
     *        <nettype> <address-types> <dest-address> <src-list>
     * 
     *    A <filter-mode> of "incl" means that an incoming packet is accepted
     *    only if its source address is in the set specified by <src-list>.  A
     *    <filter-mode> of "excl" means that an incoming packet is rejected if
     *    its source address is in the set specified by <src-list>.
     * 
     *    The first sub-field, <nettype>, indicates the network type, since SDP
     *    is protocol independent.  This document is most relevant to the value
     *    "IN", which designates the Internet Protocol.
     * 
     *    The second sub-field, <address-types>, identifies the address family,
     *    and for the purpose of this document may be either <addrtype> value
     *    "IP4" or "IP6".  Alternately, when <dest-address> is an FQDN, the
     *    value MAY be "*" to apply to both address types, since either address
     *    type can be returned from a DNS lookup.
     * 
     *    The third sub-field, <dest-address>, is the destination address,
     *    which MUST correspond to one or more of the session's "connection-
     *    address" field values.  It may be either a unicast or multicast
     *    address, an FQDN, or the "*" wildcard to match any/all of the
     *    session's "connection-address" values.
     * 
     *    The fourth sub-field, <src-list>, is the list of source
     *    hosts/interfaces in the source-filter, and consists of one or more
     *    unicast addresses or FQDNs, separated by space characters.
     * 
     * </pre>
     * 
     * @throws SdpException The source-filter attribute value is invalid or improperly used.
     */
   private void applySourceFilterAttribute(final SourceFilter filter,
                                           final String filterAttribute,
                                           final boolean isSessionLevel) throws SdpException {

       if (logger.isLoggable(Level.FINER)) {
           logger.finer(log.entry("applySourceFilterAttribute", filter, filterAttribute, isSessionLevel));
       }

        StringTokenizer tokenizer = new StringTokenizer(filterAttribute);
        if (tokenizer.hasMoreElements()) {
            String filterMode = tokenizer.nextToken();
            if (filterMode.equals("incl") || filterMode.equals("excl")) {
                if (tokenizer.hasMoreElements()) {
                    String netType = tokenizer.nextToken();
                    if (netType.equals("IN")) {
                        if (tokenizer.hasMoreElements()) {
                            String addressType = tokenizer.nextToken();
                            if (addressType.equals("IP4") || addressType.equals("IP6") || addressType.equals("*")) {
                                if (tokenizer.hasMoreElements()) {
                                    String destinationAddress = tokenizer.nextToken();
                                    InetAddress groupAddress;
                                    if (destinationAddress.equals("*")) {
                                        if (!filter.isEmpty()) {
                                            // Ignore wild-card if a filter has already been applied.
                                            return;
                                        }
                                        groupAddress = filter.getGroupAddress();
                                    }
                                    else
                                    {
                                        try {
                                            groupAddress = InetAddress.getByName(destinationAddress);
                                        }
                                        catch (UnknownHostException e) {
                                            throw new SdpException("the destination address in an SDP source-filter attribute record cannot be resolved");
                                        }
                                    }
                                    if (!groupAddress.equals(filter.getGroupAddress())) {
                                        // This source-filter record does not apply to this filter
                                        return;
                                    }
                                    else {
                                        // The source-filter list applies to this filter
                                        if (!filter.isEmpty()) {
                                            if (isSessionLevel) {
                                                // A media-level source-filter has already been applied to the group address. 
                                                // Since the media-level attribute overrides any session-level attribute
                                                // that applies to the same group address, we will ignore this attribute.
                                                return;
                                            }
                                            else {
                                                throw new SdpException("the SDP description contains multiple source-filter attribute records that apply to the same destination address");
                                            }
                                        }
                                        if (tokenizer.hasMoreElements()) {
                                            if (filterMode.equals("incl")) {
                                                while (tokenizer.hasMoreElements()) {
                                                    String sourceAddress = tokenizer.nextToken();
                                                    try {
                                                        filter.include(InetAddress.getByName(sourceAddress));
                                                    }
                                                    catch (UnknownHostException e) {
                                                        throw new SdpException("a source address in a source-filter attribute record cannot be resolved");
                                                    }
                                                }
                                            }
                                            else {
                                                while (tokenizer.hasMoreElements()) {
                                                    String sourceAddress = tokenizer.nextToken();
                                                    try {
                                                        filter.exclude(InetAddress.getByName(sourceAddress));
                                                    }
                                                    catch (UnknownHostException e) {
                                                        throw new SdpException("a source address in a source-filter attribute record cannot be resolved");
                                                    }
                                                }
                                            }
                                        }
                                        else {
                                            throw new SdpException("source-filter attribute record is malformed - it does not specify any source addresses");
                                        }
                                    }
                                }
                                else {
                                    throw new SdpException("source-filter attribute record is malformed");
                                }
                            }
                            else {
                                throw new SdpException("source-filter attribute record specifies an invalid address type");
                            }
                        }
                        else {
                            throw new SdpException("source-filter attribute record is malformed");
                        }
                    }
                    else {
                        throw new SdpException("source-filter attribute record specifies an invalid network protocol");
                    }
                }
                else {
                    throw new SdpException("source-filter attribute record is malformed");
                }   
            }
            else {
                throw new SdpException("source-filter attribute record specifies an invalid filter mode");
            }
        }
        else {
            throw new SdpException("source-filter attribute record is malformed");
        }
    }

   private static InetAddress getRelayDiscoveryAddress(final SessionDescription sessionDescription,
                                                       final MediaDescription mediaDescription) throws SdpException {
       // Look for relay discovery address attribute record
       InetAddress relayDiscoveryAddress = AmtPseudoInterfaceManager.getDefaultRelayDiscoveryAddress();
       String anycastAddress = mediaDescription.getAttribute(MulticastReflectorFactory.AMT_RELAY_DISCOVERY_ADDRESS_SDP_ATTRIBUTE);
       if (anycastAddress == null) {
           anycastAddress = sessionDescription.getAttribute(MulticastReflectorFactory.AMT_RELAY_DISCOVERY_ADDRESS_SDP_ATTRIBUTE);
       }
       if (anycastAddress != null) {
           try {
               relayDiscoveryAddress = InetAddress.getByName(anycastAddress);
           }
           catch (UnknownHostException e) {
               throw new SdpException("cannot resolve address specified in "+MulticastReflectorFactory.AMT_RELAY_DISCOVERY_ADDRESS_SDP_ATTRIBUTE);
           }
       }
       return relayDiscoveryAddress;
   }

}
