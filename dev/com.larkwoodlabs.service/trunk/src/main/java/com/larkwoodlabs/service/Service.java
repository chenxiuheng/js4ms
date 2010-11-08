package com.larkwoodlabs.service;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;


public interface Service {

    /**
     * Starts the service.
     * Used to initialize the service, start threads or timers and acquire resources.
     */
    void start();

    /**
     * Stops the service.
     * Used to terminate threads and timers and releases resources.
     */
    void stop();

    /**
     * Reads and processes a single message or stream of messages.
     * @param connection
     * @throws IOException
     */
    void service(Connection connection) throws EOFException,
                                               SocketException,
                                               IOException,
                                               InterruptedException;

}
