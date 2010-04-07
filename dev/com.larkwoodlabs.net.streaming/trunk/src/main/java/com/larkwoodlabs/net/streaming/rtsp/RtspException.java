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


public final class RtspException extends Exception {

    private static final long serialVersionUID = -1089201947642565979L;

    StatusCode statusCode;
    
    public RtspException(int statusCode) {
        this.statusCode = StatusCode.getByCode(statusCode);
    }
    
    public RtspException(StatusCode statusCode) {
        this.statusCode = statusCode;
    }
    
    public RtspException(StatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public RtspException(StatusCode statusCode, Throwable cause) {
        super(cause);
        this.statusCode = statusCode;
    }

    public RtspException(StatusCode statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public StatusCode getStatusCode() {
        return this.statusCode;
    }

    public static RtspException create(final StatusCode statusCode,
                                       final String objectId,
                                       final Logger logger) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(objectId + " throwing RtspException statusCode=" + statusCode.getCode() + ":'" + statusCode.getReasonPhrase() + "'");
        } 
        return new RtspException(statusCode);
    }

    public static RtspException create(final StatusCode statusCode,
                                       final String message,
                                       final String objectId,
                                       final Logger logger) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(objectId + " throwing RtspException statusCode=" + statusCode.getCode() + ":'" + statusCode.getReasonPhrase() + "' - " + message);
        } 
        return new RtspException(statusCode, message);
    }

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
