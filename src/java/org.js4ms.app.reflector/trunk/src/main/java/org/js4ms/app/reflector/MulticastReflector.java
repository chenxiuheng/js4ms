package org.js4ms.app.reflector;


import java.net.URI;
import java.util.Vector;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;

import org.js4ms.service.rtsp.presentation.MediaStream;
import org.js4ms.service.rtsp.presentation.Presentation;




/**
 * 
 * 
 *
 * @author gbumgard
 */
public class MulticastReflector extends Presentation {

    /*-- Static Variables ----------------------------------------------------*/


    /*-- Member Variables ----------------------------------------------------*/

    /**
     * The SDP description of the multicast presentation that will be reflected.
     * This description is used to setup the endpoint(s) required to receive
     * the multicast RTP/RTCP streams.
     * The original SDP description must be transformed into a new description
     * that clients can use to connect to the reflector.
     * The new description is sent in response to a DESCRIBE request.
     */
    protected final SessionDescription inputSessionDescription;


    protected MulticastReflector(final URI presentationUri,
                                 final SessionDescription inputSessionDescription,
                                 final SessionDescription outputSessionDescription) throws SdpException {
        super(presentationUri,
              Source.DESCRIBE,
              outputSessionDescription);
        this.inputSessionDescription = inputSessionDescription;
        constructMediaStreams();
    }

    @Override
    protected boolean doIsPauseSupported() {
        return true;
    }

    @Override
    protected boolean doIsRecordSupported() {
        return false;
    }

    @Override
    protected MediaStream doConstructMediaStream(int index) throws SdpException {
        Vector<?> inputMediaDescriptions = this.inputSessionDescription.getMediaDescriptions(false);
        if (inputMediaDescriptions != null) {
            Vector<?> outputMediaDescriptions = this.sessionDescription.getMediaDescriptions(false);
            return new MulticastReflectorStream(this,
                                                index,
                                                inputSessionDescription, 
                                                (MediaDescription)inputMediaDescriptions.get(index),
                                                this.sessionDescription,
                                                (MediaDescription)outputMediaDescriptions.get(index));
        }
        throw new java.lang.ArrayIndexOutOfBoundsException();
    }

}
