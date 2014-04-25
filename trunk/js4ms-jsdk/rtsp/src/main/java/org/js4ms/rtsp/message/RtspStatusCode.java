package org.js4ms.rtsp.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * RtspStatusCode.java [org.js4ms.jsdk:rtsp]
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

import org.js4ms.rest.message.Status;
import org.js4ms.rest.message.StatusCode;

public interface RtspStatusCode extends StatusCode {

    public final Status InvalidMedia = new Status(415,"Invalid Media"); // Unsupported Media Type
    public final Status InvalidParameter = new Status(451,"Invalid Parameter");
    public final Status ParameterNotUnderstood = new Status(451,"Parameter Not Understood");
    public final Status IllegalConferenceIdentifier = new Status(452,"Illegal Conference Identifier");
    public final Status NotEnoughBandwidth = new Status(453,"Not Enough Bandwidth");
    public final Status SessionNotFound = new Status(454,"Session Not Found");
    public final Status MethodNotValidInThisState = new Status(455,"Method Not Valid In This State");
    public final Status HeaderFieldNotValid = new Status(456,"Header Field Not Valid");
    public final Status InvalidRange = new Status(457,"Invalid Range");
    public final Status ParameterIsReadOnly = new Status(458,"Parameter Is ReadOnly");
    public final Status AggregateOperationNotAllowed = new Status(459,"Aggregate Operation Not Allowed");
    public final Status OnlyAggregateOperationAllowed = new Status(460,"Only Aggregate Operation Allowed");
    public final Status UnsupportedTransport = new Status(461,"Unsupported Transport");
    public final Status DestinationUnreachable = new Status(462,"Destination Unreachable");

}
