package org.js4ms.rest.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * StatusCode.java [org.js4ms.jsdk:rest]
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


public interface StatusCode {

    public final Status Unrecognized = new Status(0, "Unrecognized");

    public final Status Continue = new Status(100,"Continue");
    public final Status SwitchingProtocols = new Status(101,"Switching Protocols");

    public final Status OK = new Status(200,"OK");
    public final Status Created = new Status(201,"Created");
    public final Status Accepted = new Status(202,"Accepted");
    public final Status NonAuthoritativeInformation = new Status(203, "Non-Authoritative Information");
    public final Status NoContent = new Status(204, "No Content");
    public final Status ResetContent = new Status(205, "Reset Content");
    public final Status PartialContent = new Status(206, "Partial Content");
    public final Status LowOnStorageSpace = new Status(250,"Low On Storage Space");
    
    public final Status MultipleChoices = new Status(300,"Multiple Choices");
    public final Status MovedPermanently = new Status(301,"Moved Permanently");
    public final Status MovedTemporarily = new Status(302,"Moved Temporarily");
    public final Status Found = new Status(302,"Found");
    public final Status SeeOther = new Status(303,"See Other");
    public final Status NotModified = new Status(304,"Not Modified");
    public final Status UseProxy = new Status(305,"Use Proxy");
    public final Status TemporaryRedirect = new Status(307,"Temporary Redirect");

    public final Status BadRequest = new Status(400,"BadRequest");
    public final Status Unauthorized = new Status(401,"Unauthorized");
    public final Status PaymentRequired = new Status(402,"Payment Required");
    public final Status Forbidden = new Status(403,"Forbidden");
    public final Status NotFound = new Status(404,"Not Found");
    public final Status MethodNotAllowed = new Status(405,"Method Not Allowed");
    public final Status NotAcceptable = new Status(406,"Not Acceptable");
    public final Status ProxyAuthenticationRequired = new Status(407,"Proxy Authentication Required");
    public final Status RequestTimeout = new Status(408,"Request Timeout");
    public final Status Conflict = new Status(409,"Conflict");
    public final Status Gone = new Status(410,"Gone");
    public final Status LengthRequired = new Status(411,"Length Required");
    public final Status PreconditionFailed = new Status(412,"Precondition Failed");
    public final Status RequestEntityTooLarge = new Status(413,"Request Entity Too Large");
    public final Status RequestUriTooLong = new Status(414,"Request Uri Too Long");
    public final Status UnsupportedMediaType = new Status(415,"Unsupported Media Type");
    public final Status RequestedRangeNotSatisfiable = new Status(416,"Requested range not satisfiable");
    public final Status ExpectationFailed = new Status(417,"Expectation Failed");
    
    public final Status InternalServerError = new Status(500,"Internal Server Error");
    public final Status NotImplemented = new Status(501,"Not Implemented");
    public final Status BadGateway = new Status(502,"Bad Gateway");
    public final Status ServiceUnavailable = new Status(503,"Service Unavailable");
    public final Status GatewayTimeout = new Status(504,"Gateway Timeout");
    public final Status VersionNotSupported = new Status(505,"Version Not Supported");
    public final Status OptionNotSupported = new Status(551,"Option Not Supported");

    public final Status BadResponse = new Status(0,"Bad response");
}
