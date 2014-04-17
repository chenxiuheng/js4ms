package org.js4ms.service.protocol.rtsp.message;

import org.js4ms.service.protocol.rest.message.Status;
import org.js4ms.service.protocol.rest.message.StatusCode;

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
