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

package com.larkwoodlabs.channels;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.larkwoodlabs.util.logging.LoggableBase;
import com.larkwoodlabs.util.logging.Logging;

/**
 * A message pump uses an internal thread to continuously receive messages
 * from an {@link InputChannel} and send those messages to an {@link OutputChannel}.
 * The internal thread is started and stopped using the
 * {@link #start()} and {@link #stop(int)} methods.
 *
 * @param <MessageType> - The message object type.
 * 
 * @author Gregory Bumgardner
 */
public final class ChannelPump<MessageType>
                   extends LoggableBase
                   implements Runnable {

    
    /*-- Static Variables ----------------------------------------------------*/
    
    public static final Logger logger = Logger.getLogger(ChannelPump.class.getName());

    
    /*-- Member Variables ----------------------------------------------------*/

    protected final Object lock = new Object();

    private final InputChannel<MessageType> inputChannel;
    private final OutputChannel<MessageType> outputChannel;

    private Thread thread = null;
    private boolean isRunning = false;


    /*-- Member Functions ----------------------------------------------------*/

    /**
     * Constructs a channel pump that connects the specified input and output channels.
     * The {@link #start()} method must be called to start the pump.
     */
    public ChannelPump(InputChannel<MessageType> inputChannel,
                       OutputChannel<MessageType> outputChannel) {

        if (logger.isLoggable(Level.FINER)) {
             logger.finer(Logging.entering(ObjectId,
                                           "ChannelPump.ChannelPump",
                                           inputChannel,
                                           outputChannel));
        }

        this.inputChannel = inputChannel;
        this.outputChannel = outputChannel;
    }
    
    @Override
    public Logger getLogger() {
        return logger;
    }

    /**
     * Starts the message pump.
     */
    public final void start() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ChannelPump.start"));
        }

        synchronized (this.lock) {
            if (!this.isRunning) {
                this.thread = new Thread(this,ChannelPump.class.getName());
                this.thread.setDaemon(true);
                this.isRunning = true;
                this.thread.start();
            }
        }
    }

    /**
     * Attempts to stop the message pump within the specified amount of time.
     * @param milliseconds - The amount of time to wait for the pump to stop. Pass a value of zero to wait indefinitely.
     * @throws InterruptedException
     */
    public final void stop(int milliseconds) throws InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ChannelPump.onStop", milliseconds));
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
     * @param closeChannels - Indicates whether the attached channels should be closed.
     * @throws InterruptedException - The calling thread was interrupted while waiting for the pump to close.
     * @throws IOException 
     * @throws IllegalStateException 
     */
    public void close(boolean closeChannels) throws InterruptedException, IllegalStateException, IOException {
        stop(0);
        if (closeChannels) {
            this.inputChannel.close(true);
            this.outputChannel.close(true);
        }
    }


    @Override
    public final void run() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ChannelPump.run"));
        }

        logger.fine(ObjectId + " channel pump started");

        while (this.isRunning) {
            try {
                transfer();
            }
            catch (IOException e) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(ObjectId + " transfer failed with exception - "+e.getClass().getSimpleName()+":"+e.getMessage());
                }
                break;
            }
            catch (InterruptedException e) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(ObjectId + " transfer interrupted - "+e.getClass().getSimpleName()+":"+e.getMessage());
                }
                break;
            }
        }

        logger.fine(ObjectId + " channel pump stopped");
    }

    public final void transfer() throws IOException, InterruptedException {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer(Logging.entering(ObjectId, "ChannelPump.transfer"));
        }

        this.outputChannel.send(this.inputChannel.receive(Integer.MAX_VALUE),Integer.MAX_VALUE);
    }

}
