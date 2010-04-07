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


public abstract class PacketSource {
    
    enum State {
        Created,
        Starting,
        Started,
        Stopping,
        Stopped,
        Closing,
        Closed,
        Failed
    }

    private State state;
    
    protected PacketSource() {
        this.state = State.Created;
    }
    
    public State getState() {
        return this.state;
    }

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

    public abstract void doStart() throws IOException;

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

    public abstract void doStop() throws IOException, InterruptedException;

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

    public abstract void doClose() throws IOException, InterruptedException;

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
