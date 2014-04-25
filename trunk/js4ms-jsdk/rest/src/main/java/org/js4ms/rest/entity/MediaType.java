package org.js4ms.rest.entity;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MediaType.java [org.js4ms.jsdk:rest]
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MediaType {

    private static final String APPLICATION = "application";

    private static final String MULTIPART = "multipart";

    private static final String TEXT = "text";

    public static final MediaType APPLICATION_ATOM_XML_TYPE = new MediaType(APPLICATION, "atom+xml");

    public static final MediaType APPLICATION_ECMASCRIPT_TYPE = new MediaType(APPLICATION, "ecmascript");

    public static final MediaType APPLICATION_JSON_TYPE = new MediaType(APPLICATION, "json");

    public static final MediaType APPLICATION_JAVASCRIPT_TYPE = new MediaType(APPLICATION, "javascript");

    public static final MediaType APPLICATION_OCTET_STREAM_TYPE = new MediaType(APPLICATION, "octet-stream");

    public static final MediaType APPLICATION_RDF_XML_TYPE = new MediaType(APPLICATION, "rdf+xml");

    public static final MediaType APPLICATION_RSS_XML_TYPE = new MediaType(APPLICATION, "rss+xml");

    public static final MediaType APPLICATION_SDP_TYPE = new MediaType(APPLICATION, "sdp");

    public static final MediaType APPLICATION_SOAP_XML_TYPE = new MediaType(APPLICATION, "soap+xml");

    public static final MediaType APPLICATION_XHTML_XML_TYPE = new MediaType(APPLICATION, "xhtml+xml");

    public static final MediaType APPLICATION_XML_DTD_TYPE = new MediaType(APPLICATION, "xml-dtd");

    public static final MediaType APPLICATION_ZIP_TYPE = new MediaType(APPLICATION, "zip");

    public static final MediaType APPLICATION_X_GZIP_TYPE = new MediaType(APPLICATION, "x-gzip");

    public static final MediaType APPLICATION_X_WWW_FORM_URLENCODED_TYPE = new MediaType(APPLICATION,
                                                                                         "x-www-form-urlencoded");

    public static final MediaType APPLICATION_X_JAVASCRIPT_TYPE = new MediaType(APPLICATION, "x-javascript");

    public static final MediaType MULTIPART_MIXED_TYPE = new MediaType(MULTIPART, "mixed");

    public static final MediaType MULTIPART_ALTERNATIVE_TYPE = new MediaType(MULTIPART, "alternative");

    public static final MediaType MULTIPART_RELATED_TYPE = new MediaType(MULTIPART, "related");

    public static final MediaType MULTIPART_FORM_DATA_TYPE = new MediaType(MULTIPART, "form-data");

    public static final MediaType MULTIPART_SIGNED_TYPE = new MediaType(MULTIPART, "signed");

    public static final MediaType MULTIPART_ENCRYPTED_TYPE = new MediaType(MULTIPART, "encrypted");

    public static final MediaType TEXT_CSS_TYPE = new MediaType(TEXT, "text/css");

    public static final MediaType TEXT_HTML_TYPE = new MediaType(TEXT, "text/html");

    public static final MediaType TEXT_PLAIN_TYPE = new MediaType(TEXT, "text/plain");

    public static final MediaType TEXT_XML_TYPE = new MediaType(TEXT, "text/xml");

    public static final MediaType TEXT_WILDCARD_TYPE = new MediaType(TEXT,"*");

    public static final MediaType WILDCARD_TYPE = new MediaType("*","*");

    private final String type;

    private final String subtype;

    private final Map<String, String> parameters;

    /**
     * Parses Media Type description to create a MediaType object.
     * The description must take the following form:
     * 
     * <pre>
     *        media-type     = type "/" subtype *( ";" parameter )
     *        type           = token
     *        subtype        = token
     * </pre>
     * 
     * @param description
     *            The media type description (i.e. the value of a Content-Type header).
     * @return
     */
    static public MediaType parse(final String description) {
        if (description != null && description.length() > 0) {
            String components[] = description.split("[;]");
            String mediaType = components[0];
            String types[] = mediaType.split("[/]");
            if (types.length < 2) {
                throw new IllegalArgumentException("media type description is missing subtype identifier");
            }
            String type = types[0];
            String subtype = types[1].trim();
            if (components.length > 1) {
                HashMap<String, String> map = new HashMap<String, String>();
                for (int i = 1; i < components.length; i++) {
                    String parameter = components[i];
                    if (parameter.length() > 0) {
                        String pair[] = parameter.split("=");
                        if (pair.length > 1) {
                            map.put(pair[0].trim(), pair[1].trim());
                        }
                        else {
                            map.put(pair[0].trim(), null);
                        }
                    }
                }
                return new MediaType(type, subtype, map);
            }
            else {
                return new MediaType(type, subtype);
            }
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    public MediaType(final String type, final String subtype) {
        this.type = type;
        this.subtype = type;
        this.parameters = null;
    }

    public MediaType(final String type, final String subtype, final Map<String, String> parameters) {
        this.type = type;
        this.subtype = type;
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    public String getType() {
        return this.type;
    }

    public String getSubtype() {
        return this.subtype;
    }

    public Map<String, String> getParameters() {
        return this.parameters;
    }

    public String getParameterValue(final String parameterName) {
        if (this.parameters != null) {
            return this.parameters.get(parameterName);
        }
        else {
            return null;
        }
    }

    public boolean containsParameter(final String parameterName) {
        if (this.parameters != null) {
            return this.parameters.containsKey(parameterName);
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        String result = this.type + "/" + this.subtype;
        if (this.parameters != null) {
            for (String parameterName : this.parameters.keySet()) {
                String parameterValue = this.parameters.get(parameterName);
                if (parameterValue != null) {
                    result += ";" + parameterName + "=" + parameterValue;
                }
                else {
                    result += ";" + parameterName;
                }
            }
        }
        return result;
    }

    @Override
    public boolean equals(final Object object) {
        if (object instanceof MediaType) {
            MediaType other = (MediaType)object;
            if (other.type.equals(this.type) && other.subtype.equals(this.subtype)) {
                if (other.parameters == null && this.parameters == null) {
                    return true;
                }
                else if (other.parameters != null && this.parameters != null && other.parameters.size() == this.parameters.size()) {
                    for (String parameter :  other.parameters.keySet() ) {
                        if (!this.parameters.containsKey(parameter) || !this.parameters.get(parameter).equals(other.parameters.get(parameter))) {
                            return false;
                        }
                    }
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }
    
}
