package org.js4ms.io.channel;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * MessageSource.java [org.js4ms.jsdk:io]
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

/**
 * An abstract message source that sends a stream of messages to an {@link OutputChannel}.
 * 
 * @author Greg Bumgardner (gbumgard)
 */
public abstract class MessageSource<Message> {

    /*-- Inner Classes -------------------------------------------------------*/

    /**
     * An enumeration of {@link MessageSource} object states.
     */
    enum State {
        /** Source open but not sending messages. */
        Ready,
        /** Source starting. */
        Starting,
        /** Source started and sending messages. */
        Started,
        /** Source stopping. */
        Stopping,
        /** Source closing. */
        Closing,
        /** Source closed. */
        Closed,
        /** Source failed due to error. */
        Failed
    }

    /*-- Member Variables ----------------------------------------------------*/

    /**
     * Tracks the current state for this message source;
     */
    private State state;

    /**
     * The output channel that will receive messages produced by this message source.
     */
    private final OutputChannel<Message> outputChannel;

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs the message source leaving it in the {@link MessageSource.State#Ready
     * Ready} state.
     * 
     * @param outputChannel
     */
    protected MessageSource(final OutputChannel<Message> outputChannel) {
        this.state = State.Ready;
        this.outputChannel = outputChannel;
    }

    /**
     * Gets the current {@link MessageSource.State State} of this source.
     * 
     * @return An enumerator value describing the current state.
     */
    public State getState() {
        return this.state;
    }

    /**
     * Attempts to start this message source leaving the source in the
     * {@link MessageSource.State#Started Started} state if
     * successful.
     * Calls the {@link #doStart()} method which derived classes must implement to perform
     * any actions required to start the source.
     * 
     * @throws IOException
     *             If an I/O error occurs while starting the source.
     * @throws InterruptedException
     * @throws IllegalStateException
     *             The message source cannot be started in its current state.
     */
    public final void start() throws IOException, InterruptedException {

        if (this.state == State.Started) {
            return;
        }

        if (this.state == State.Ready) {
            try {
                this.state = State.Starting;
                doStart();
                this.state = State.Started;
            }
            catch (IOException e) {
                abort();
                throw e;
            }
        }
        else {
            throw new IllegalStateException();
        }
    }

    /**
     * Performs actions required to start the start source.
     * 
     * @throws IOException
     *             If an I/O error occurs while starting the source.
     * @throws InterruptedException
     */
    protected abstract void doStart() throws IOException, InterruptedException;

    /**
     * Attempts to stop this message source leaving the source in the
     * {@link MessageSource.State#Ready Ready} state if successful.
     * Calls the {@link #doStop()} method which derived classes must implement to perform
     * any actions required to stop the source.
     * 
     * @throws IOException
     *             If an I/O error occurs while stopping the source.
     * @throws InterruptedException
     *             If the calling thread is interrupted while stopping the source.
     * @throws IllegalStateException
     *             The message source cannot be stopped in its current state.
     */
    public final void stop() throws IOException, InterruptedException {

        if (this.state == State.Ready) {
            return;
        }

        if (this.state == State.Started) {
            try {
                this.state = State.Stopping;
                doStop();
                this.state = State.Ready;
            }
            catch (IOException e) {
                abort();
                throw e;
            }
        }
        else {
            throw new IllegalStateException();
        }
    }

    /**
     * Performs actions required to stop the message source.
     * 
     * @throws IOException
     *             If an I/O error occurs while stopping the source.
     * @throws InterruptedException
     *             If the calling thread is interrupted while stopping the source.
     */
    protected abstract void doStop() throws IOException, InterruptedException;

    /**
     * Attempts to close this message source leaving the source in the
     * {@link MessageSource.State#Closed Closed} state if successful.
     * Calls the {@link #doClose()} method which derived classes must implement to perform
     * any actions required to stop the source.
     * 
     * @throws InterruptedException
     *             If the calling thread is interrupted while closing the source.
     * @throws IllegalStateException
     *             The message source cannot be closed in its current state.
     */
    public final void close() throws InterruptedException {

        if (this.state == State.Closed) {
            return;
        }

        if (this.state != State.Failed) {
            try {
                this.state = State.Closing;
                doClose();
                this.state = State.Closed;
            }
            catch (IOException e) {
                abort();
            }
        }
        else {
            throw new IllegalStateException();
        }
    }

    /**
     * Performs actions required to close the message source.
     * Default implementation closes the OutputChannel bound to this message source.
     * 
     * @throws IOException
     *             If an I/O error occurs while closing the source.
     * @throws InterruptedException
     *             If the calling thread is interrupted while closing the source.
     */
    protected void doClose() throws IOException, InterruptedException {
        this.outputChannel.close();
    }

    /**
     * Called to indicate that another operation has failed.
     * Attempts to close this message source and places the source in the
     * {@link MessageSource.State#Failed Failed} state.
     * 
     * @throws InterruptedException
     *             If the calling thread is interrupted while closing the source.
     */
    protected void abort() throws InterruptedException {
        switch (this.state) {
            case Ready:
            case Starting:
            case Started:
            case Stopping:
                try {
                    doClose();
                    this.state = State.Closed;
                }
                catch (IOException e) {
                    this.state = State.Failed;
                }
                break;
            case Closing:
                this.state = State.Failed;
                break;
            case Closed:
            case Failed:
                break;
        }
    }

}
