package org.js4ms.rest.common;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * RequestException.java [org.js4ms.jsdk:rest]
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


import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.rest.entity.StringEntity;
import org.js4ms.rest.message.ProtocolVersion;
import org.js4ms.rest.message.Response;
import org.js4ms.rest.message.Status;
import org.js4ms.rest.message.StatusLine;
import org.js4ms.server.Connection;




/**
 * An exception used to report recoverable errors that occur while handling an RTSP request.
 * An RTSP exception carries a {@link StatusCode} and error message that can be sent in an RTSP response.
 * 
 * @author Gregory Bumgardner
 */
public final class RequestException extends MessageException {

    /*-- Static Constants ----------------------------------------------------*/

    /**
     * 
     */
    private static final long serialVersionUID = 9146681170276083552L;

    /*-- Static Variables ----------------------------------------------------*/


    /*-- Member Variables ----------------------------------------------------*/

    final Status status;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a request exception from the specified protocol version and status.
     * @param protocolVersion - The {@link ProtocolVersion} of the bad request.
     * @param status - The {@link Status} code to be reported by the exception.
     */
    public RequestException(final ProtocolVersion protocolVersion, final Status status) {
        super(protocolVersion);
        this.status = status;
    }

    /**
     * Constructs a request exception from the specified protocol version,status and message.
     * @param protocolVersion - The {@link ProtocolVersion} of the bad request.
     * @param status - The {@link Status} code to be reported by the exception.
     * @param message - A descriptive error message.
     */
    public RequestException(final ProtocolVersion protocolVersion, final Status status, final String message) {
        super(protocolVersion, message);
        this.status = status;
    }

    /**
     * Constructs a request exception from the specified protocol version, status and Throwable cause.
     * @param protocolVersion - The {@link ProtocolVersion} of the bad request.
     * @param status - The {@link Status} code to be reported by the exception.
     * @param cause - A Throwable representing the root cause for the exception.
     */
    public RequestException(final ProtocolVersion protocolVersion, final Status status, final Throwable cause) {
        super(protocolVersion, cause);
        this.status = status;
    }

    /**
     * Constructs an request exception from the specified protocol version, status, message and Throwable cause.
     * @param protocolVersion - The {@link ProtocolVersion} of the bad request.
     * @param status - The {@link Status} code to be reported by the exception.
     * @param message - A descriptive error message.
     * @param cause - A Throwable representing the root cause for the exception.
     */
    public RequestException(final ProtocolVersion protocolVersion, final Status status, final String message, final Throwable cause) {
        super(protocolVersion, message, cause);
        this.status = status;
    }

    /**
     * Returns the {@link Status} carried by this exception.
     */
    public Status getStatus() {
        return this.status;
    }

    /**
     * Static convenience function that logs and constructs a {@link RequestException}
     * from the specified status.
     * @param status - The status that will be carried by the exception.
     */
    public static RequestException create(final ProtocolVersion protocolVersion,
                                          final Status status) {
        return new RequestException(protocolVersion, status);
    }

    /**
     * Static convenience function that logs and constructs a {@link RequestException}
     * from the specified status.
     * @param status - The status that will be carried by the exception.
     * @param logger - The Logger used to log a message describing the exception.
     */
    public static RequestException create(final ProtocolVersion protocolVersion,
                                          final Status status,
                                          final String logPrefix,
                                          final Logger logger) {
        if (logger.isLoggable(Level.FINE)) logger.fine(logPrefix+"request failed; status=" + status.getCode() + ":'" + status.getReasonPhrase() + "'");
        return new RequestException(protocolVersion, status);
    }

    /**
     * Static convenience function that logs and constructs an {@link RequestException}
     * from the specified status and descriptive error message.
     * @param status - The status that will be carried by the exception.
     * @param message - A descriptive error message.
     */
    public static RequestException create(final ProtocolVersion protocolVersion,
                                          final Status status,
                                          final String message) {
        return new RequestException(protocolVersion, status, message);
    }

    /**
     * Static convenience function that logs and constructs an {@link RequestException}
     * from the specified status and descriptive error message.
     * @param status - The status that will be carried by the exception.
     * @param message - A descriptive error message.
     * @param logger - The Logger used to log a message describing the exception.
     */
    public static RequestException create(final ProtocolVersion protocolVersion,
                                          final Status status,
                                          final String message,
                                          final String logPrefix,
                                          final Logger logger) {
        if (logger.isLoggable(Level.FINE)) logger.fine(logPrefix+"request failed; status=" + status.getCode() + ":'" + status.getReasonPhrase() + "' - " + message);
        return new RequestException(protocolVersion, status, message);
    }

    /**
     * Static convenience function that logs and constructs an {@link RequestException}
     * from the specified status and Throwable cause.
     * @param status - The status that will be carried by the exception.
     * @param cause - A Throwable representing the root cause for the exception.
     */
    public static RequestException create(final ProtocolVersion protocolVersion,
                                          final Status status,
                                          final Throwable cause) {
        return new RequestException(protocolVersion, status, cause);
    }

    /**
     * Static convenience function that logs and constructs an {@link RequestException}
     * from the specified status and Throwable cause.
     * @param status - The status that will be carried by the exception.
     * @param cause - A Throwable representing the root cause for the exception.
     * @param logger - The Logger used to log a message describing the exception.
     */
    public static RequestException create(final ProtocolVersion protocolVersion,
                                          final Status status,
                                          final Throwable cause,
                                          final String logPrefix,
                                          final Logger logger) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(logPrefix+"request failed; status=" + status.getCode() + ":'" + status.getReasonPhrase() + "'");
            logCause(cause,logPrefix,logger);
        } 
        return new RequestException(protocolVersion, status, cause);
    }

    /**
     * Static convenience function that logs and constructs a {@link RequestException}
     * from the specified status, message and Throwable cause.
     * @param status - The status that will be carried by the exception.
     * @param message - A descriptive error message.
     * @param cause - A Throwable representing the root cause for the exception.
     */
    public static RequestException create(final ProtocolVersion protocolVersion,
                                          final Status status,
                                          final String message,
                                          final Throwable cause) {
        return new RequestException(protocolVersion, status, message, cause);
    }

    /**
     * Static convenience function that logs and constructs a {@link RequestException}
     * from the specified status, message and Throwable cause.
     * @param status - The status that will be carried by the exception.
     * @param message - A descriptive error message.
     * @param cause - A Throwable representing the root cause for the exception.
     * @param log - The Log used to log a message describing the exception.
     */
    public static RequestException create(final ProtocolVersion protocolVersion,
                                          final Status status,
                                          final String message,
                                          final Throwable cause,
                                          final String logPrefix,
                                          final Logger logger) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(slog.msg("request failed; status=" + status.getCode() + ":'" + status.getReasonPhrase() + "' " + message));
            logCause(cause, logPrefix, logger);
        } 
        return new RequestException(protocolVersion, status, message, cause);
    }

    private static void logCause(final Throwable cause,
                                 final String logPrefix,
                                 final Logger logger) {
        StackTraceElement[] frames = cause.getStackTrace();
        logger.fine(slog.msg("----> Cause"));
        logger.fine(logPrefix+cause.getClass().getName() + ":" + cause.getMessage());
        for (StackTraceElement frame : frames) {
            logger.fine(slog.msg(": " + frame.toString()));
        }
        logger.fine(slog.msg("<---- Cause"));
    }

    /**
     * Constructs a {@link Response} object from this exception.
     * An entity containing an error message and stack trace will be added to the response
     * if an error message or Throwable cause is specified when this exception was constructed.
     */
    public Response createResponse(Connection connection) {
        Response response = new Response(connection, new StatusLine(this.protocolVersion, this.status));
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
     * Uses the status and any error message or Throwable cause associated with this
     * exception to set the status and entity of the specified {@link Response}.
     * @param response - The response that is to be modified to report the error described by this exception.
     */
    public void setResponse(Response response) {
        response.getStatusLine().setStatus(this.status);
        String entity = getMessage() + "\n";
        Throwable cause = getCause();
        if (cause != null) {
            for (StackTraceElement frame : cause.getStackTrace()) {
                entity += frame.toString() + "\n";
            }
        }
        response.setEntity(new StringEntity(entity));
    }
}
