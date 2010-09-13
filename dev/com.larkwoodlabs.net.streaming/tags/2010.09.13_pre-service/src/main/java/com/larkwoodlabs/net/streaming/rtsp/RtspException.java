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

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An exception used to report recoverable errors that occur while handling an RTSP request.
 * An RTSP exception carries a {@link StatusCode} and error message that can be sent in an RTSP response.
 * 
 * @author Gregory Bumgardner
 */
public final class RtspException extends Exception {

    /*-- Static Variables ----------------------------------------------------*/

    private static final long serialVersionUID = -1089201947642565979L;


    /*-- Member Variables ----------------------------------------------------*/

    StatusCode statusCode;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs an RTSP exception from the specified status code number.
     * The status code number converted into a {@link StatusCode} value by
     * calling the {@link StatusCode#getByCode()} method.
     * @param statusCode - The status code number that will be returned in an RTSP response.
     */
    public RtspException(int statusCode) {
        this.statusCode = StatusCode.getByCode(statusCode);
    }
    
    /**
     * Constructs an RTSP exception from the specified status code.
     * @param statusCode - The status code that will be carried by the exception.
     */
    public RtspException(StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Constructs an RTSP exception from the specified status code and message.
     * @param statusCode - The status code that will be carried by the exception.
     * @param message - A descriptive error message.
     */
    public RtspException(StatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Constructs an RTSP exception from the specified status code and Throwable cause.
     * @param statusCode - The status code that will be carried by the exception.
     * @param cause - A Throwable representing the root cause for the exception.
     */
    public RtspException(StatusCode statusCode, Throwable cause) {
        super(cause);
        this.statusCode = statusCode;
    }

    /**
     * Constructs an RTSP exception from the specified status code, message and Throwable cause.
     * @param statusCode - The status code that will be carried by the exception.
     * @param message - A descriptive error message.
     * @param cause - A Throwable representing the root cause for the exception.
     */
    public RtspException(StatusCode statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Returns the {@link StatusCode} carried by this exception.
     */
    public StatusCode getStatusCode() {
        return this.statusCode;
    }

    /**
     * Static convenience function that logs and constructs an {@link RTSPException}
     * from the specified status code.
     * @param statusCode - The status code that will be carried by the exception.
     * @param objectId - An object identifier that will appear in the log message.
     * @param logger - The Logger used to log a message describing the exception.
     */
    public static RtspException create(final StatusCode statusCode,
                                       final String objectId,
                                       final Logger logger) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(objectId + " throwing RtspException statusCode=" + statusCode.getCode() + ":'" + statusCode.getReasonPhrase() + "'");
        } 
        return new RtspException(statusCode);
    }

    /**
     * Static convenience function that logs and constructs an {@link RTSPException}
     * from the specified status code and descriptive error message.
     * @param statusCode - The status code that will be carried by the exception.
     * @param message - A descriptive error message.
     * @param objectId - An object identifier that will appear in the log message.
     * @param logger - The Logger used to log a message describing the exception.
     */
    public static RtspException create(final StatusCode statusCode,
                                       final String message,
                                       final String objectId,
                                       final Logger logger) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(objectId + " throwing RtspException statusCode=" + statusCode.getCode() + ":'" + statusCode.getReasonPhrase() + "' - " + message);
        } 
        return new RtspException(statusCode, message);
    }

    /**
     * Static convenience function that logs and constructs an {@link RTSPException}
     * from the specified status code and Throwable cause.
     * @param statusCode - The status code that will be carried by the exception.
     * @param cause - A Throwable representing the root cause for the exception.
     * @param objectId - An object identifier that will appear in the log message.
     * @param logger - The Logger used to log a message describing the exception.
     */
    public static RtspException create(final StatusCode statusCode,
                                       final Throwable cause,
                                       final String objectId,
                                       final Logger logger) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(objectId + " throwing RtspException statusCode=" + statusCode.getCode() + ":'" + statusCode.getReasonPhrase() + "'");
            logCause(cause, objectId, logger);
        } 
        return new RtspException(statusCode, cause);
    }

    /**
     * Static convenience function that logs and constructs an {@link RTSPException}
     * from the specified status code, message and Throwable cause.
     * @param statusCode - The status code that will be carried by the exception.
     * @param message - A descriptive error message.
     * @param cause - A Throwable representing the root cause for the exception.
     * @param objectId - An object identifier that will appear in the log message.
     * @param logger - The Logger used to log a message describing the exception.
     */
    public static RtspException create(final StatusCode statusCode,
                                       final String message,
                                       final Throwable cause,
                                       final String objectId,
                                       final Logger logger) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(objectId + " throwing RtspException statusCode=" + statusCode.getCode() + ":'" + statusCode.getReasonPhrase() + "' " + message);
            logCause(cause, objectId, logger);
        } 
        return new RtspException(statusCode, message, cause);
    }
    
    private static void logCause(final Throwable cause,
                                 final String objectId,
                                 final Logger logger) {
        StackTraceElement[] frames = cause.getStackTrace();
        logger.fine(objectId + " ----> Cause");
        logger.fine(objectId + " " + cause.getClass().getName() + ":" + cause.getMessage());
        for (StackTraceElement frame : frames) {
            logger.fine(objectId + " : " + frame.toString());
        }
        logger.fine(objectId + " <---- Cause");
    }
    
    /**
     * Constructs a {@link Response} object from this exception.
     * An entity containing an error message and stack trace will be added to the response
     * if an error message or Throwable cause is specified when this exception was constructed.
     */
    public Response createResponse() {
        Response response = new Response(this.statusCode);
        String entity = getMessage() + "\n";
        Throwable cause = getCause();
        if (cause != null) {
            for (StackTraceElement frame : cause.getStackTrace()) {
                entity += frame.toString() + "\n";
            }
        }
        response.setEntity(new StringEntity(entity));
        return response;
    }

    /**
     * Uses the status code and any error message or Throwable cause associated with this
     * exception to set the status code and entity of the specified {@link Response}.
     * @param response - The response that is to be modified to report the RTSP error described by this exception.
     */
    public void setResponse(Response response) {
        response.setStatusCode(this.statusCode);
        String entity = getMessage() + "\n";
        if (this.statusCode.getStatusClass() == StatusCode.ServerError) {
            Throwable cause = getCause();
            if (cause != null) {
                for (StackTraceElement frame : cause.getStackTrace()) {
                    entity += frame.toString() + "\n";
                }
            }
            response.setEntity(new StringEntity(entity));
        }
    }
}
