package org.js4ms.reflector;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MulticastReflector.java [org.js4ms.jsdk:reflector]
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


import java.net.URI;
import java.util.Vector;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;

import org.js4ms.rtsp.presentation.MediaStream;
import org.js4ms.rtsp.presentation.Presentation;




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
