package org.js4ms.service.protocol.http;

import com.larkwoodlabs.service.protocol.rest.message.Method;

public interface HttpMethods {
    public static final Method GET = new Method("GET");
    public static final Method HEAD = new Method("HEAD");
    public static final Method POST = new Method("POST");
    public static final Method PUT = new Method("PUT");
    public static final Method DELETE = new Method("DELETE");
    public static final Method TRACE = new Method("TRACE");
    public static final Method CONNECT = new Method("CONNECT");
    public static final Method PATCH = new Method("PATCH");
}
