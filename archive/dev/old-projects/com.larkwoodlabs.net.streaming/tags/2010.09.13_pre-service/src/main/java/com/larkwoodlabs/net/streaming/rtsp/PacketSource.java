/*
 * Copyright © 2009-2010 Larkwood Labs Software.
 *
 * Licensed under the Larkwood Labs Software Source Code License, Version 1.0.
 * You may not use this file except in compliance with this License.
 *
 * You may view the Source Code License at
 * http://www.larkwoodlabs.com/source-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the license.
 */

package com.larkwoodlabs.net.streaming.rtsp;

import java.io.IOException;

/**
 * A streaming media packet receiver that delivers incoming packets to a {@link PacketSink}.
 * This class provides simple state management functionality shared by all packet source classes.
 *
 * @author Gregory Bumgardner
 */
public abstract class PacketSource {
    
    /*-- Inner Classes -------------------------------------------------------*/

    /**
     * An enumeration of {@link PacketSource} object states.
     */
    enum State {
        /** Source constructed. */
        Created,
        /** Source starting. */
        Starting,
        /** Source started and forwarding incoming packets. */
        Started,
        /** Source stopping. */
        Stopping,
        /** Source stopped and no longer forwarding incoming packets. */
        Stopped,
        /** Source closing. */
        Closing,
        /** Source closed. */
        Closed,
        /** Source failed due to error. */
        Failed
    }

    /*-- Member Variables ----------------------------------------------------*/

    private State state;
    

    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs the packet source leaving it in the {@link PacketSource.State#Created Created} state.
     */
    protected PacketSource() {
        this.state = State.Created;
    }
    
    /**
     * Gets the current {@link PacketSource.State State} of this source.
     */
    public State getState() {
        return this.state;
    }

    /**
     * Attempts to start this packet source leaving the source in the {@link PacketSource.State#Started Started} state if successful.
     * Calls the {@link #doStart()} method which derived classes must implement to perform any actions required to start the source.
     * @throws IOException If an I/O error occurs while starting the source.
     * @throws InterruptedException
     */
    public final void start() throws IOException, InterruptedException {

        if (this.state == State.Started) {
            return;
        }

        if (this.state == State.Created || this.state == State.Stopped) {
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
     * Performs actions required to start the packet source.
     * @throws IOException If an I/O error occurs while starting the source.
     */
    public abstract void doStart() throws IOException;

    /**
     * Attempts to stop this packet source leaving the source in the {@link PacketSource.State#Stopped Stopped} state if successful.
     * Calls the {@link #doStop()} method which derived classes must implement to perform any actions required to stop the source.
     * @throws IOException If an I/O error occurs while stopping the source.
     * @throws InterruptedException If the calling thread is interrupted while stopping the source.
     */
    public final void stop() throws IOException, InterruptedException {

        if (this.state == State.Stopped) {
            return;
        }

        if (this.state == State.Created || this.state == State.Started) {
            try {
                this.state = State.Stopping;
                doStop();
                this.state = State.Stopped;
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
     * Performs actions required to stop the packet source.
     * @throws IOException If an I/O error occurs while stopping the source.
     * @throws InterruptedException If the calling thread is interrupted while stopping the source.
     */
    public abstract void doStop() throws IOException, InterruptedException;

    /**
     * Attempts to close this packet source leaving the source in the {@link PacketSource.State#Closed Closed} state if successful.
     * Calls the {@link #doClose()} method which derived classes must implement to perform any actions required to stop the source.
     * @throws InterruptedException If the calling thread is interrupted while closing the source.
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
     * Performs actions required to close the packet source.
     * @throws IOException If an I/O error occurs while closing the source.
     * @throws InterruptedException If the calling thread is interrupted while closing the source.
     */
    public abstract void doClose() throws IOException, InterruptedException;

    /**
     * Called to indicate that another operation has failed.
     * Attempts to close this packet source and places the source in the {@link PacketSource.State#Failed Failed} state.
     * @throws InterruptedException If the calling thread is interrupted while closing the source.
     */
    protected void abort() throws InterruptedException {
        switch (this.state) {
        case Created:
        case Starting:
        case Started:
        case Stopping:
        case Stopped:
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
