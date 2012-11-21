package net.js4ms.service.protocol.rtsp.server;

import java.io.IOException;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.js4ms.service.Server;
import net.js4ms.service.protocol.rest.message.Request;
import net.js4ms.service.protocol.rest.message.Response;
import net.js4ms.service.protocol.rest.server.session.AbstractTimedSession;
import net.js4ms.service.protocol.rest.server.session.SessionManager;
import net.js4ms.service.protocol.rtsp.RtspMessageHeaders;
import net.js4ms.service.protocol.rtsp.RtspMethods;
import net.js4ms.service.protocol.rtsp.presentation.Presentation;
import net.js4ms.util.logging.Log;


public class RtspSession extends AbstractTimedSession {

    /*-- Static Constants ----------------------------------------------------*/
    
    public static final int DEFAULT_SESSION_TIMEOUT = 60000; // Timeout for inactive sessions

    public static final String SESSION_TIMEOUT_PROPERTY = Server.SERVICE_PROPERTY_PREFIX+"session.timeout";


    /*-- Static Variables ----------------------------------------------------*/

    public static final Logger logger = Logger.getLogger(RtspSession.class.getName());

    static int getSessionTimeout() {
        String property = System.getProperty(SESSION_TIMEOUT_PROPERTY);
        if (property != null) {
            try {
                return Integer.parseInt(property);
            }
            catch (NumberFormatException e) {
                logger.warning(SESSION_TIMEOUT_PROPERTY+"="+property+" is not a valid timeout value");
            }
        }
        return DEFAULT_SESSION_TIMEOUT;
    }

    /*-- Member Variables ----------------------------------------------------*/

    protected final Log log = new Log(this);

    final Presentation presentation;


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * 
     */
    public RtspSession(final String sessionId,
                       final Presentation presentation,
                       final SessionManager sessionManager,
                       final Timer sessionTimer) {
        super(sessionId, sessionManager, sessionTimer, getSessionTimeout());

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("<ctor>", identifier, presentation));
        }

        this.presentation = presentation;

    }

    @Override
    public boolean doHandleTransaction(Request request, Response response) throws IOException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("handleTransaction", request, response));
        }

        if (this.presentation.handleTransaction(request, response)) {

            // If the response to TEARDOWN request carries a 'Connection: close' then terminate the session.
            // The 'close' indicates that the TEARDOWN request URI was an aggregate control URI.
            if (request.getRequestLine().getMethod().equals(RtspMethods.TEARDOWN) &&
                response.getHeader(RtspMessageHeaders.CONNECTION).getValue().equalsIgnoreCase("close")) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("terminating session "+this.identifier+" in response to an aggregate TEARDOWN request"));
                }
                super.terminate();
            }
            return true;
        }
        return false;
    }

    @Override
    public void terminate() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("terminate"));
        }

        this.presentation.close();
        super.terminate();
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

}
