package com.larkwoodlabs.service.protocol.text.server.session;

import java.util.Timer;

import com.larkwoodlabs.util.task.ReschedulableTask;

/**
 * A task used to terminate timed sessions following a period of inactivity.
 * 
 *
 * @author gbumgard
 */
public class SessionTimer extends ReschedulableTask {

    final Session session;

    public SessionTimer(final Timer timer, final Session session) {
        super(timer);
        this.session = session;
    }

    @Override
    public void run() {
        this.session.terminate();
    }

}
