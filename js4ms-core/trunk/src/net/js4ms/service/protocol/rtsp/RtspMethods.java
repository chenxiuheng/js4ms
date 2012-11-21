package net.js4ms.service.protocol.rtsp;

import net.js4ms.service.protocol.rest.message.Method;

public interface RtspMethods {

    public static final Method GET = new Method("GET");
    public static final Method POST = new Method("POST");
    public static final Method ANNOUNCE = new Method("ANNOUNCE");
    public static final Method OPTIONS = new Method("OPTIONS");
    public static final Method DESCRIBE = new Method("DESCRIBE");
    public static final Method SETUP = new Method("SETUP");
    public static final Method PLAY = new Method("PLAY");
    public static final Method PAUSE = new Method("PAUSE");
    public static final Method RECORD = new Method("RECORD");
    public static final Method TEARDOWN = new Method("TEARDOWN");
    public static final Method GET_PARAMETER = new Method("GET_PARAMETER");
    public static final Method SET_PARAMETER = new Method("SET_PARAMETER");
    public static final Method REDIRECT = new Method("REDIRECT");

}
