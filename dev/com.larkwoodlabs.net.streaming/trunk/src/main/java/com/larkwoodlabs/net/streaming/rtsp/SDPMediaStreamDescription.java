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

package com.larkwoodlabs.net.streaming.rtsp;

import java.net.InetAddress;
import java.util.Vector;
import java.util.logging.Level;

import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;

import com.larkwoodlabs.net.amt.SourceFilter;
import com.larkwoodlabs.net.streaming.rtsp.TransportDescription.Profile;
import com.larkwoodlabs.net.streaming.rtsp.TransportDescription.Protocol;
import com.larkwoodlabs.net.streaming.rtsp.TransportDescription.Transport;
import com.larkwoodlabs.util.logging.Logging;

/**
 * An RTSP media stream description created from a Session Description Protocol (SDP) media description.
 *
 * @author Gregory Bumgardner
 */
final class SDPMediaStreamDescription extends MediaStreamDescription {
    
    /*-- Member Variables ----------------------------------------------------*/
    
    private SessionDescription sessionDescription;
    private MediaDescription mediaDescription;

    private InetAddress relayDiscoveryAddress;


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a media stream decription from the specified SDP session and media descriptions.
     * @param sessionDescription - An SDP session description.
     * @param mediaDescription - An SDP media description.
     * @param filters - A vector of {@link SourceFilter} objects generated from SDP <code>source-filter</code> attribute records.
     * @param ttl - The time-to-live value for the media stream.
     * @param relayDiscoveryAddress - The AMT relay discovery address as specified in an SDP <code>x-amt-relay-anycast</code> attribute record.
     * @throws RtspException If the SDP elements describe a unsupported transport.
     */
    public SDPMediaStreamDescription(SessionDescription sessionDescription,
                                     MediaDescription mediaDescription,
                                     Vector<SourceFilter> filters,
                                     int ttl,
                                     InetAddress relayDiscoveryAddress) throws RtspException {
        super();

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "SDPMediaStreamDescription.SDPMediaStreamDescription", sessionDescription));
        }
            
        this.sessionDescription = sessionDescription;
        this.mediaDescription = mediaDescription;
        this.filters = filters;
        //this.ttl = ttl;
        this.relayDiscoveryAddress = relayDiscoveryAddress;
        
        init();
    }
        
    private void init() throws RtspException {
        
        try {

            Media media = this.mediaDescription.getMedia();

            // Get ports from media record
            this.firstSourcePort = media.getMediaPort();
            this.sourcePortCount = media.getPortCount();
            if (this.sourcePortCount == 0) this.sourcePortCount = 1;

            // Get transport specification from media record
            // Split transport specification into separate fields: RTP/{AVP|AVP-TCP}[/{UDP|TCP}]
            String transportSpec = media.getProtocol();
            String[] fields = transportSpec.split("/");
            if (fields.length > 0) {
               try {
                   this.protocol = Protocol.valueOf(fields[0]); 
               }
               catch (IllegalArgumentException e) {
                   throw RtspException.create(StatusCode.UnsupportedTransport,
                                              "SDP media record specifies an unrecognized protocol: " + fields[0],
                                              ObjectId, logger);
               }
               if (fields.length > 1) {
                   String[] profileFields = fields[1].split("-");
                   if (profileFields.length > 0) {
                       try {
                           this.profile = Profile.valueOf(profileFields[0]);
                       }
                       catch (IllegalArgumentException e) {
                           throw RtspException.create(StatusCode.UnsupportedTransport,
                                                      "SDP media record specifies an unrecognized profile: " + fields[1],
                                                      ObjectId, logger);
                       }
                       if (profileFields.length > 1) {
                           try {
                               this.transport = Transport.valueOf(profileFields[1]);
                           }
                           catch (IllegalArgumentException e) {
                               throw RtspException.create(StatusCode.UnsupportedTransport,
                                                          "SDP media record specifies an unrecognized transport: " + fields[2],
                                                          ObjectId, logger);
                           }
                       }
                   }
                   if (fields.length > 2) {
                       try {
                           this.transport = Transport.valueOf(fields[2]);
                       }
                       catch (IllegalArgumentException e) {
                           throw RtspException.create(StatusCode.UnsupportedTransport,
                                                      "SDP media record specifies an unrecognized transport: " + fields[2],
                                                      ObjectId, logger);
                       }
                   }
               }
            }


        }
        catch (SdpException e) {
            throw RtspException.create(StatusCode.UnsupportedTransport,
                                       "error in SDP description - " + e.getMessage(),
                                       ObjectId, logger);
        }
    }
    
    /**
     * Returns the SDP session description.
     */
    public SessionDescription getSessionDescription() {
        return this.sessionDescription;
    }

    /**
     * Returns the SDP media description.
     */
    public MediaDescription getMediaDescription() {
        return this.mediaDescription;
    }

    /**
     * Returns the AMT relay discovery address.
     */
    public InetAddress getRelayDiscoveryAddress() {
        return this.relayDiscoveryAddress;
    }
}
