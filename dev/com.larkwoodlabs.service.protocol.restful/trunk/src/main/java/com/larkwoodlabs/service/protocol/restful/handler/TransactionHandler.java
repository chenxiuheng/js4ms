package com.larkwoodlabs.service.protocol.restful.handler;

import java.io.IOException;

import com.larkwoodlabs.service.protocol.restful.RequestException;
import com.larkwoodlabs.service.protocol.restful.message.Request;
import com.larkwoodlabs.service.protocol.restful.message.Response;

public interface TransactionHandler {

    /**
     * 
     * @param request - The {@link Request} message sent from the client to the server to initiate the transaction.
     * @param response - The {@link Response} message that will be sent from the server back to the client.
     * @return 
     * <li><code>true</code>
     * to indicate that the handler has changed the response status code and transaction handling should be terminated.
     * <li><code>false</code>
     * to indicate that the handler has not changed the response status code and transaction handling may continue. 
     * @throws RequestException
     * @throws IOException
     */
    boolean handleTransaction(Request request, Response response) throws IOException;
}
