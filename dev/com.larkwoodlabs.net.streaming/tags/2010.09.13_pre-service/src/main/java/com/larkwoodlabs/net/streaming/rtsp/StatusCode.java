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

/**
 * An enumeration of RTSP and HTTP status codes sent or received by RTSP participants.
 * A StatusCode enumerator is carries an integer status code number and English reason phrase.
 * See [<a href="http://tools.ietf.org/html/rfc2326#page-22">RFC-2326, Section 7.1.1</a>] and
 * [<a href="http://tools.ietf.org/html/rfc2326#page-41">RFC-2326, Section 11</a>].
 * 
 * @author Gregory Bumgardner
 */
public enum StatusCode {

    Unrecognized(0, "Unrecognized"),

    Informational(1, "Informational"),
    Success(2, "Success"),
    Redirection(3, "Redirection"),
    ClientError(4, "Client Error"),
    ServerError(5, "Server Error"),
    
    Continue(100,"Continue"),
    SwitchingProtocols(101,"Switching Protocols"),

    OK(200,"OK"),
    Created(201,"Created"),
    Accepted(201,"Accepted"),
    NonAuthoritativeInformation(203, "Non-Authoritative Information"),
    NoContent(204, "No Content"),
    ResetContent(205, "Reset Content"),
    PartialContent(206, "Partial Content"),
    LowOnStorageSpace(250,"Low On Storage Space"),
    
    MultipleChoices(300,"Multiple Choices"),
    MovedPermanently(301,"Moved Permanently"),
    MovedTemporarily(302,"Moved Temporarily"),
    SeeOther(303,"See Other"),
    NotModified(304,"Not Modified"),
    UseProxy(305,"Use Proxy"),
    TemporaryRedirect(307,"Temporary Redirect"),

    BadRequest(400,"BadRequest"),
    Unauthorized(401,"Unauthorized"),
    PaymentRequired(402,"Payment Required"),
    Forbidden(403,"Forbidden"),
    NotFound(404,"Not Found"),
    MethodNotAllowed(405,"Method Not Allowed"),
    NotAcceptable(406,"Not Acceptable"),
    ProxyAuthenticationRequired(407,"Proxy Authentication Required"),
    RequestTimeout(408,"Request Timeout"),
    Conflict(409,"Conflict"),
    Gone(410,"Gone"),
    LengthRequired(411,"Length Required"),
    PreconditionFailed(412,"Precondition Failed"),
    RequestEntityTooLarge(413,"Request Entity Too Large"),
    RequestUriTooLong(414,"Request Uri Too Long"),
    UnsupportedMediaType(415,"Unsupported Media Type"),
    RequestedRangeNotSatisfiable(416,"Requested range not satisfiable"),
    ExpectationFailed(417,"Expectation Failed"),
    InvalidParameter(451,"Invalid Parameter"),
    IllegalConferenceIdentifier(452,"Illegal Conference Identifier"),
    NotEnoughBandwidth(453,"Not Enough Bandwidth"),
    SessionNotFound(454,"Session Not Found"),
    MethodNotValidInThisState(455,"Method Not Valid In This State"),
    HeaderFieldNotValid(456,"Header Field Not Valid"),
    InvalidRange(457,"Invalid Range"),
    ParameterIsReadOnly(458,"Parameter Is ReadOnly"),
    AggregateOperationNotAllowed(459,"Aggregate Operation Not Allowed"),
    OnlyAggregateOperationAllowed(460,"Only Aggregate Operation Allowed"),
    UnsupportedTransport(461,"Unsupported Transport"),
    DestinationUnreachable(462,"Destination Unreachable"),
    
    InternalServerError(500,"Internal Server Error"),
    NotImplemented(501,"Not Implemented"),
    BadGateway(502,"Bad Gateway"),
    ServiceUnavailable(503,"Service Unavailable"),
    GatewayTimeout(504,"Gateway Timeout"),
    RtspVersionNotSupported(505,"RTSP Version Not Supported"),
    OptionNotSupported(551,"Option Not Supported"),
    
    // Internal signaling
    BadResponse(-1,"Bad Response");

    private int code;
    private String reasonPhrase;
    
    /**
     * Constructs an enumerator from an integer code and reason phrase.
     * @param code - The integer value of the status code.
     * @param reasonPhrase - The associated reason phrase.
     */
    StatusCode(int code, String reasonPhrase) {
        this.code = code;
        this.reasonPhrase = reasonPhrase;
    }
    
    /**
     * Returns the integer value for this status code.
     */
    public int getCode() {
        return this.code;
    }
    
    /**
     * Returns a StatusCode representing the status class for this status code.
     * The status classes are identified by the status codes {@link #Redirection}, {@link #OK}, {@link #MultipleChoices},
     * {@link #BadRequest} and {@link #ServerError}.
     */
    public StatusCode getStatusClass() {
        return getStatusClass(getCode());
    }

    /**
     * Returns the reason phrase for this status code.
     */
    public String getReasonPhrase() {
        return this.reasonPhrase;
    }
    
    /**
     * Returns the StatusCode that carries the specified integer code value or the
     * StatusCode representing the status class of the specified code if no matching
     * StatusCode exists.
     * @param code - The integer status code.
     * @throws IllegalArgumentException If the code value falls outside of the range 100-599.
     */
    public static StatusCode getByCode(final int code) {
        for (StatusCode value : StatusCode.values()) {
            if (value.getCode() == code) return value;
        }
        if (code < 100) return Continue;
        if (code >= 100 && code < 200) return OK;
        if (code >= 300 && code < 400) return MultipleChoices;
        if (code >= 400 && code < 500) return BadRequest;
        if (code >= 500 && code < 600) return InternalServerError;
        throw new IllegalArgumentException("unrecognized status code");
    }
    
    /**
     * Returns a StatusCode representing the status class for the specified integer status code.
     * @param code The integer status code.
     * @throws IllegalArgumentException If the code value falls outside of the range 100-599.
     */
    public static StatusCode getStatusClass(final int code) {
        if (code < 100) return Informational;
        if (code >= 100 && code < 200) return Success;
        if (code >= 300 && code < 400) return Redirection;
        if (code >= 400 && code < 500) return ClientError;
        if (code >= 500 && code < 600) return ServerError;
        throw new IllegalArgumentException("invalid status code class");
    }

}
