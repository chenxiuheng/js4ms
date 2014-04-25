package org.js4ms.io.channel;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * ChannelPump.java [org.js4ms.jsdk:io]
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


import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.common.util.logging.Log;



/**
 * A message pump uses an internal thread to continuously receive messages
 * from an {@link InputChannel} and send those messages to an {@link OutputChannel}.
 * The internal thread is started and stopped using the {@link #start()} and
 * {@link #stop(int)} methods.
 * 
 * @param <MessageType>
 *            The message object type.
 * @author Greg Bumgardner (gbumgard)
 */
public final class ChannelPump<MessageType>
                implements Runnable {

    /*-- Static Variables ----------------------------------------------------*/

    /**
     * The logger used to generate logging messages produced by instances of this class.
     */
    public static final Logger logger = Logger.getLogger(ChannelPump.class.getName());

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * Helper object used to construct log messages.
     */
    protected final Log log = new Log(this);

    /**
     * Monitor object used for thread synchronization.
     */
    protected final Object lock = new Object();

    private final InputChannel<MessageType> inputChannel;

    private final OutputChannel<MessageType> outputChannel;

    private Thread thread = null;

    private boolean isRunning = false;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a channel pump that connects the specified input and output channels.
     * The {@link #start()} method must be called to start the pump.
     * 
     * @param inputChannel
     * @param outputChannel
     */
    public ChannelPump(InputChannel<MessageType> inputChannel,
                       OutputChannel<MessageType> outputChannel) {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("ChannelPump",
                                   inputChannel,
                                   outputChannel));
        }

        this.inputChannel = inputChannel;
        this.outputChannel = outputChannel;
    }

    /**
     * Starts the message pump.
     */
    public final void start() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("start"));
        }

        synchronized (this.lock) {
            if (!this.isRunning) {
                this.thread = new Thread(this, ChannelPump.class.getName());
                this.thread.setDaemon(true);
                this.isRunning = true;
                this.thread.start();
            }
        }
    }

    /**
     * Attempts to stop the message pump within the specified amount of time.
     * 
     * @param milliseconds
     *            The amount of time to wait for the pump to stop.
     *            Pass a value of zero to wait indefinitely.
     * @throws InterruptedException
     */
    public final void stop(int milliseconds) throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("onStop", milliseconds));
        }

        synchronized (this.lock) {
            if (this.isRunning) {
                this.isRunning = false;
                this.thread.interrupt();
                this.thread.join(milliseconds);
                this.thread = null;
            }
        }
    }

    /**
     * Convenience method that stops the channel pump and optionally
     * closes the attached input and output channels.
     * 
     * @throws InterruptedException
     *             The calling thread was interrupted while waiting for the pump to close.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void close() throws InterruptedException, IllegalStateException, IOException {
        stop(0);
        this.inputChannel.close();
        this.outputChannel.close();
    }

    @Override
    public final void run() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("run"));
        }

        logger.fine(log.msg("channel pump started"));

        while (this.isRunning) {
            try {
                transfer();
            }
            catch (IOException e) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("transfer failed with exception - " + e.getClass().getSimpleName() + ":" + e.getMessage()));
                }
                break;
            }
            catch (InterruptedException e) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(log.msg("transfer interrupted - " + e.getClass().getSimpleName() + ":" + e.getMessage()));
                }
                break;
            }
        }

        logger.fine(log.msg("channel pump stopped"));
    }

    /**
     * @throws IOException
     * @throws InterruptedException
     */
    public final void transfer() throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(log.entry("transfer"));
        }

        this.outputChannel.send(this.inputChannel.receive(Integer.MAX_VALUE), Integer.MAX_VALUE);
    }

}
