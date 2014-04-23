package org.js4ms.rest.message;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * Status.java [org.js4ms.jsdk:rest]
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


import java.io.IOException;
import java.io.OutputStream;


/**
 * A response message status description.
 * A status carries an integer status code number and English reason phrase.
 * 
 * @author Gregory Bumgardner
 */
public final class Status {
    
    /*-- Static Constants ----------------------------------------------------*/

    public static Status Unrecognized = new Status(0, "Unrecognized");
    public static Status Informational = new Status(100, "Informational");
    public static Status Success = new Status(200, "Informational");
    public static Status Redirection = new Status(300, "Redirection");
    public static Status ClientError = new Status(400, "Client Error");
    public static Status ServerError = new Status(500, "Server Error");
    public static Status GlobalError = new Status(600, "Global Error");
    
    
    /*-- Member Variables ----------------------------------------------------*/

    protected int code;

    protected String reasonPhrase;

    
    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a status object from an integer code and reason phrase.
     * @param code - The integer value of the status code.
     * @param reasonPhrase - The associated reason phrase.
     */
    public Status(final int code, final String reasonPhrase) {
        this.code = code;
        this.reasonPhrase = reasonPhrase;
    }
    
    /**
     * Returns the integer status code for this status object.
     */
    public int getCode() {
        return this.code;
    }
    
    /**
     * Returns the reason phrase for this status object.
     */
    public String getReasonPhrase() {
        return this.reasonPhrase;
    }
    
    @Override
    public boolean equals(final Object object) {
        return (object instanceof Status) && this.code == (((Status)object).code);
    }

    @Override
    public String toString() {
        return String.valueOf(this.code) + " " + this.reasonPhrase;
    }

    /**
     * Returns a StatusCode representing the status class for this status code.
     * The status classes are identified by the status codes {@link #Redirection}, {@link #OK}, {@link #Redirection},
     * {@link #BadRequest}, {@link #ServerError} and {@link #GlobalError}.
     */
    public Status getStatusClass() {
        return getStatusClass(getCode());
    }

    /**
     * Returns a StatusCode representing the status class for the specified integer status code.
     * @param code The integer status code.
     * @throws IllegalArgumentException If the code value falls outside of the range 100-699.
     */
    public static Status getStatusClass(final int code) {
        if (code < 100) return Informational;
        if (code >= 100 && code < 200) return Success;
        if (code >= 300 && code < 400) return Redirection;
        if (code >= 400 && code < 500) return ClientError;
        if (code >= 500 && code < 600) return ServerError;
        if (code >= 600 && code < 700) return GlobalError;
        throw new IllegalArgumentException("invalid status code class");
    }

    public void writeTo(final OutputStream outstream) throws IOException {
        outstream.write(String.valueOf(this.code).getBytes("UTF8"));
        outstream.write(' ');
        outstream.write(this.reasonPhrase.getBytes("UTF8"));
    }

}
