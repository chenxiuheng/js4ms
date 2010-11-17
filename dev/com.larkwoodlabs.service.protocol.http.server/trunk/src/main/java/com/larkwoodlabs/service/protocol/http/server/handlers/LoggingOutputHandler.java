package com.larkwoodlabs.service.protocol.http.server.handlers;

import java.io.IOException;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.logging.XMLFormatter;

import com.larkwoodlabs.service.protocol.http.HttpMessageHeaders;
import com.larkwoodlabs.service.protocol.http.HttpStatusCodes;
import com.larkwoodlabs.service.protocol.text.entity.Entity;
import com.larkwoodlabs.service.protocol.text.entity.StringEntity;
import com.larkwoodlabs.service.protocol.text.handler.TransactionHandler;
import com.larkwoodlabs.service.protocol.text.message.Header;
import com.larkwoodlabs.service.protocol.text.message.Request;
import com.larkwoodlabs.service.protocol.text.message.Response;
import com.larkwoodlabs.util.logging.JsonLogFormatter;
import com.larkwoodlabs.util.logging.LogFormatter;

/**
 * An HTTP transaction handler that publishes log records to an open connection.
 * Used to stream log records to a client in response to an HTTP request.
 * 
 * Parameters in the request URI query string are used to control the  output format for the log records.<p>
 * The 'output' query string parameter is used to specify the output format.
 * The choices are 'text', 'xml', 'json', 'jsonp'.
 * The default format is 'text'.
 * If the output format is 'jsonp', then the URI query string must also include
 * a 'callback' parameter that gives the name of a function to use in the
 * the JSONP callback.
 * 
 * @see {@link LoggingConfigurationHandler} - provides logging configuration control via HTTP requests.
 * @author gbumgard@cisco.com
 */
public class LoggingOutputHandler implements TransactionHandler {

    @Override
    public boolean handleTransaction(Request request, Response response) throws IOException {

        String outputType = null;
        String jsonpCallback = null;

        String query = request.getRequestLine().getUri().getQuery();
        if (query != null && query.length() > 0) {
            String parameters[] = query.split("[&;]");
            for (String parameter : parameters) {
                if (parameter.length() > 0) {
                    String pair[] = parameter.split("=");
                     if (pair[0].equalsIgnoreCase("output")) {
                         if (pair.length == 2) {
                             outputType = pair[1];
                         }
                         else {
                             response.setStatus(HttpStatusCodes.BadRequest);
                             response.setEntity(new StringEntity("output parameter value is missing"));
                             return true;
                         }
                     }
                     else if (pair[0].equalsIgnoreCase("callback")) {
                         if (pair.length == 2) {
                             jsonpCallback= pair[1];
                         }
                         else {
                             response.setStatus(HttpStatusCodes.BadRequest);
                             response.setEntity(new StringEntity("callback parameter value is missing"));
                             return true;
                         }
                     }
                }
            }
        }

        if (outputType == null) {
            outputType = "text";
        }

        Formatter formatter;
        
        if (outputType.equals("text")) {
            formatter = new LogFormatter();
            response.setHeader(new Header(Entity.CONTENT_TYPE,"text/plain"));
        }
        else if (outputType.equals("xml")) {
            formatter = new XMLFormatter();
            response.setHeader(new Header(Entity.CONTENT_TYPE,"text/xml"));
        }
        else if (outputType.equals("json")) {
            formatter = new JsonLogFormatter();
            response.setHeader(new Header(Entity.CONTENT_TYPE,"application/json"));
        }
        else if (outputType.equals("jsonp")) {
            if (jsonpCallback == null) {
                response.setStatus(HttpStatusCodes.BadRequest);
                response.setEntity(new StringEntity("callback parameter value is missing"));
                return true;
            }
            formatter = new JsonLogFormatter(jsonpCallback);
            response.setHeader(new Header(Entity.CONTENT_TYPE,"application/javascript"));
        }
        else {
            response.setStatus(HttpStatusCodes.BadRequest);
            response.setEntity(new StringEntity("output parameter value is invalid"));
            return true;
        }

        response.setStatus(HttpStatusCodes.OK);
        response.setHeader(new Header(Entity.CONTENT_LENGTH,"-1"));
        response.setHeader(new Header(HttpMessageHeaders.CONNECTION,"close"));
        response.send();

        final StreamHandler handler = new StreamHandler(response.getConnection().getOutputStream(), formatter) {

            @Override
            public void close() {
                // Do nothing here as we don't want the stream handler closing or flushing the connection output stream.
            }

            @Override
            public void publish(final LogRecord record) {
                super.publish(record);
                // Flush after every record so log messages are sent when they are generated.
                flush();
            }
        };

        handler.setLevel(Level.FINEST);

        handler.setErrorManager(new ErrorManager() {
            @Override
            public void error(String msg, Exception ex, int code) {
                Logger.getLogger("").removeHandler(handler);
            }
        });

        Logger.getLogger("").addHandler(handler);

        // Generate a log message so the client will have something to work with.
        Logger.getLogger("").info("Log started");
        return true;
    }

}
