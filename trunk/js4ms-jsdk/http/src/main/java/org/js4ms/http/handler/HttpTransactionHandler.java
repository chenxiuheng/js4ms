package org.js4ms.http.handler;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * HttpTransactionHandler.java [org.js4ms.jsdk:http]
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import org.js4ms.http.message.HttpMethod;
import org.js4ms.http.message.HttpStatusCode;
import org.js4ms.rest.entity.CodecManager;
import org.js4ms.rest.entity.Entity;
import org.js4ms.rest.entity.MediaType;
import org.js4ms.rest.entity.StringEntity;
import org.js4ms.rest.handler.TransactionHandler;
import org.js4ms.rest.message.Method;
import org.js4ms.rest.message.Request;
import org.js4ms.rest.message.Response;



public class HttpTransactionHandler
                implements TransactionHandler {

    @Override
    public boolean handleTransaction(final Request request, final Response response) throws IOException {
        Method method = request.getRequestLine().getMethod();
        if (method == HttpMethod.GET) {
            return doGet(request, response);
        }
        else if (method == HttpMethod.POST) {
            return doPost(request, response);
        }
        else if (method == HttpMethod.PUT) {
            return doPut(request, response);
        }
        else if (method == HttpMethod.PATCH) {
            return doPatch(request, response);
        }
        else if (method == HttpMethod.DELETE) {
            return doDelete(request, response);
        }
        else if (method == HttpMethod.TRACE) {
            return doTrace(request, response);
        }
        else if (method == HttpMethod.CONNECT) {
            return doConnect(request, response);
        }
        else {
            return doDefaultResponse(request, response);
        }
    }

    protected boolean doGet(final Request request, final Response response) throws IOException {
        return doDefaultResponse(request, response);
    }

    protected boolean doPost(final Request request, final Response response) throws IOException {
        return doDefaultResponse(request, response);
    }

    protected boolean doPut(final Request request, final Response response) throws IOException {
        return doDefaultResponse(request, response);
    }

    protected boolean doPatch(final Request request, final Response response) throws IOException {
        return doDefaultResponse(request, response);
    }

    protected boolean doDelete(final Request request, final Response response) throws IOException {
        return doDefaultResponse(request, response);
    }

    protected boolean doTrace(final Request request, final Response response) throws IOException {
        return doDefaultResponse(request, response);
    }

    protected boolean doConnect(final Request request, final Response response) throws IOException {
        return doDefaultResponse(request, response);
    }

    private boolean doDefaultResponse(final Request request, final Response response) throws IOException {
        response.setStatus(HttpStatusCode.MethodNotAllowed);
        response.setEntity(new StringEntity(HttpStatusCode.MethodNotAllowed.toString()));
        return false;
    }

    static void parseQueryParameters(final Request request, Map<String, String> map) {
        // Parse query string
        parseParameterString(request.getRequestLine().getUri().getQuery(), map);
    }

    static boolean parseFormParameters(final Request request, final Response response, Map<String, String> map) throws IOException {
        // Parse query string
        if (request.containsHeader(Entity.CONTENT_TYPE)) {
            MediaType mediaType = MediaType.parse(request.getHeader(Entity.CONTENT_TYPE).getValue());
            if (mediaType.equals(MediaType.MULTIPART_FORM_DATA_TYPE)) {
                if (!mediaType.containsParameter("boundary")) {
                    response.setStatus(HttpStatusCode.BadRequest);
                    response.setEntity(new StringEntity(HttpStatusCode.BadRequest.toString()
                                                        + " - multipart content type missing boundary parameter"));
                    return false;
                }

                return parseMultipartFormParameters((new BufferedReader(
                                                                        new InputStreamReader(
                                                                                              request.getEntity()
                                                                                                              .getContent(CodecManager.getManager()
                                                                                                                                          .getCodec("*"))))
                                                                     .readLine()),
                                                    mediaType.getParameterValue("boundary").trim().replaceAll("^\"|\"$", ""),
                                                    map);
            }
            else if (mediaType.equals(MediaType.APPLICATION_X_WWW_FORM_URLENCODED_TYPE)) {
                parseParameterString(new BufferedReader(
                                                        new InputStreamReader(
                                                                              request.getEntity()
                                                                                              .getContent(CodecManager.getManager()
                                                                                                                          .getCodec("*"))))
                                                     .readLine(), map);
            }
            else {
                response.setStatus(HttpStatusCode.UnsupportedMediaType);
                response.setEntity(new StringEntity(HttpStatusCode.UnsupportedMediaType.toString()));
                return false;
            }
        }
        parseParameterString(request.getEntity().toString(), map);
        return false;
    }

    static boolean parseMultipartFormParameters(final String entity, final String boundary, final Map<String, String> map) {
        // TODO
        /*
        String parts[] = entity.split("^--"+boundary+"(--)$");
        for (String part : parts) {
        }
        **/
        return false;
    }

    static void parseParameterString(final String parameterString, final Map<String, String> map) {
        if (parameterString != null && parameterString.length() > 0) {
            String parameters[] = parameterString.split("[&;]");
            for (String parameter : parameters) {
                if (parameter.length() > 0) {
                    String pair[] = parameter.split("=");
                    String parameterName;
                    try {
                        parameterName = URLDecoder.decode(pair[0], "UTF-8");
                    }
                    catch (UnsupportedEncodingException e) {
                        throw new Error(e);
                    }
                    if (pair.length == 2) {
                        map.put(parameterName, pair[1]);
                    }
                    else {
                        map.put(parameterName, null);
                    }
                }
            }
        }
    }
}
