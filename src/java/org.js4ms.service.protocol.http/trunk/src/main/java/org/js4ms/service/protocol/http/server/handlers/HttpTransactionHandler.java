package org.js4ms.service.protocol.http.server.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import org.js4ms.service.protocol.http.HttpMethods;
import org.js4ms.service.protocol.http.HttpStatusCodes;
import org.js4ms.service.protocol.rest.entity.CodecManager;
import org.js4ms.service.protocol.rest.entity.Entity;
import org.js4ms.service.protocol.rest.entity.MediaType;
import org.js4ms.service.protocol.rest.entity.StringEntity;
import org.js4ms.service.protocol.rest.handler.TransactionHandler;
import org.js4ms.service.protocol.rest.message.Method;
import org.js4ms.service.protocol.rest.message.Request;
import org.js4ms.service.protocol.rest.message.Response;



public class HttpTransactionHandler
                implements TransactionHandler {

    @Override
    public boolean handleTransaction(final Request request, final Response response) throws IOException {
        Method method = request.getRequestLine().getMethod();
        if (method == HttpMethods.GET) {
            return doGet(request, response);
        }
        else if (method == HttpMethods.POST) {
            return doPost(request, response);
        }
        else if (method == HttpMethods.PUT) {
            return doPut(request, response);
        }
        else if (method == HttpMethods.PATCH) {
            return doPatch(request, response);
        }
        else if (method == HttpMethods.DELETE) {
            return doDelete(request, response);
        }
        else if (method == HttpMethods.TRACE) {
            return doTrace(request, response);
        }
        else if (method == HttpMethods.CONNECT) {
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
        response.setStatus(HttpStatusCodes.MethodNotAllowed);
        response.setEntity(new StringEntity(HttpStatusCodes.MethodNotAllowed.toString()));
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
                    response.setStatus(HttpStatusCodes.BadRequest);
                    response.setEntity(new StringEntity(HttpStatusCodes.BadRequest.toString()
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
                response.setStatus(HttpStatusCodes.UnsupportedMediaType);
                response.setEntity(new StringEntity(HttpStatusCodes.UnsupportedMediaType.toString()));
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
