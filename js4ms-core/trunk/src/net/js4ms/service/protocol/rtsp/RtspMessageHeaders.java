package net.js4ms.service.protocol.rtsp;

import net.js4ms.service.protocol.rest.MessageHeaders;

public interface RtspMessageHeaders extends MessageHeaders {
    public static final String BANDWIDTH = "Bandwidth"; // rtsp(R)
    public static final String BLOCKSIZE = "Blocksize"; // rtsp(R)
    public static final String CONFERENCE = "Conference"; // rtsp(R)
    public static final String CONTENT_BASE = "Content-Base"; // rtsp(e)
    public static final String PUBLIC = "Public"; // rtsp(r)
    public static final String RTP_INFO = "RTP-Info"; // rtsp(r)
    public static final String SCALE = "Scale"; // rtsp(R->r)
    public static final String SESSION = "Session"; // rtsp(R->r)
    public static final String SPEED = "Speed"; // rtsp(R->r)
    public static final String TIMESTAMP = "Timestamp"; // rtsp(R->r)
    public static final String TRANSPORT = "Transport"; // rtsp(R->r)
    public static final String X_SESSIONCOOKIE = "x-sessioncookie";
}
