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

final class SDPMediaStreamDescription extends MediaStreamDescription {
    
    /*-- Inner Classes -------------------------------------------------------*/
    /*-- Static Constants ----------------------------------------------------*/
    /*-- Static Variables ----------------------------------------------------*/
    /*-- Static Functions ----------------------------------------------------*/
    /*-- Member Variables ----------------------------------------------------*/
    
    SessionDescription sessionDescription;
    MediaDescription mediaDescription;

    //private int ttl;
    
    private InetAddress relayDiscoveryAddress;


    /*-- Member Functions ----------------------------------------------------*/

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
    
    public SessionDescription getSessionDescription() {
        return this.sessionDescription;
    }
    
    public MediaDescription getMediaDescription() {
        return this.mediaDescription;
    }

    public InetAddress getRelayDiscoveryAddress() {
        return this.relayDiscoveryAddress;
    }
}
