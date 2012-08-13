/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * File: MulticastMediaPlayer.java (org.js4ms.net)
 * 
 * Copyright © 2011-2012 Cisco Systems, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.js4ms.android;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.js4ms.android.R;
import org.js4ms.util.logging.android.LogCatFormatter;
import org.js4ms.util.logging.android.LogCatHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.VideoView;

import com.larkwoodlabs.service.launcher.java.ServiceLauncher;

/**
 * @author Greg Bumgardner (gbumgard)
 */
public class MulticastMediaPlayer extends Activity {

    /**
     * 
     */
    public static final Logger logger = Logger.getLogger(MulticastMediaPlayer.class.getName());

    final private static int ON_START_FAILURE_DIALOG = 0;
    final private static int ON_DISCONNECT_FAILURE_DIALOG = 1;
    final private static int ON_STREAM_COMPLETE_DIALOG = 2;
    final private static int ON_MEDIA_ERROR_DIALOG = 4;

    final private static String RTSP_REFLECTOR_URI = "rtsp://127.0.0.1:8554/reflect?";

    private Uri presentationUri;

    private Uri reflectorRequestUri;

    private VideoView mVideoView;

    private ServiceLauncher launcher = null;

    private ProgressDialog progressDialog;

    private final com.larkwoodlabs.util.logging.Log log = new com.larkwoodlabs.util.logging.Log(this);

    static {
        LogCatHandler handler = new LogCatHandler();
        LogCatFormatter formatter = new LogCatFormatter();
        handler.setFormatter(formatter);
        handler.setLevel(Level.FINEST);
        Logger.getLogger("").addHandler(handler);
        logger.setLevel(Level.FINER);
        com.larkwoodlabs.service.launcher.java.ServiceLauncher.logger.setLevel(Level.FINER);
    }

    /**
     * 
     */
    public MulticastMediaPlayer() {

        // TODO: remove this for standalone execution
        // Debug.waitForDebugger();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logger.finer(log.entry("onCreate", savedInstanceState));

        this.progressDialog = ProgressDialog.show(this, "", getResources().getText(R.string.progress_msg), true);

        if (this.launcher == null) {

            Properties serviceProperties = new Properties();

            ApplicationInfo info = getApplicationInfo();

            ServiceLauncher.DisconnectListener listener = new ServiceLauncher.DisconnectListener() {
                @Override
                public void onDisconnect() {
                    MulticastMediaPlayer.this.mVideoView.stopPlayback();
                    MulticastMediaPlayer.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logger.finer(log.entry("onDisconnect"));
                            showDialog(ON_DISCONNECT_FAILURE_DIALOG);
                        }
                    });
                }
            };

            this.launcher = new ServiceLauncher("dalvikvm",
                                                info.sourceDir,
                                                "org.js4ms.app.RtspMulticastReflector",
                                                8554,
                                                true,
                                                10,
                                                1000,
                                                listener,
                                                serviceProperties);

        }

        setContentView(R.layout.main);

        this.presentationUri = getIntent().getData();

        this.reflectorRequestUri = Uri.parse(RTSP_REFLECTOR_URI + this.presentationUri.toString());

        this.mVideoView = (VideoView) findViewById(R.id.videoView);

        this.mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                logger.fine(log.msg("onPrepared called for " + mp.getClass().getName()));
                MulticastMediaPlayer.this.progressDialog.dismiss();
                Toast.makeText(MulticastMediaPlayer.this,
                               getResources().getText(R.string.playing_msg).toString() + " "
                                       + MulticastMediaPlayer.this.presentationUri,
                               Toast.LENGTH_LONG).show();
            }
        });

        this.mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                logger.fine(log.msg("onCompletion called for " + mp.getClass().getName()));
                MulticastMediaPlayer.this.progressDialog.dismiss();
                showDialog(ON_STREAM_COMPLETE_DIALOG);
            }
        });

        this.mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                logger.fine(log.msg("onError called for " + mp.getClass().getName()));
                MulticastMediaPlayer.this.progressDialog.dismiss();
                showDialog(ON_MEDIA_ERROR_DIALOG);
                return true;
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();

        logger.finer(log.entry("onStart"));

        startPlayout();

    }

    @Override
    public void onStop() {

        logger.finer(log.entry("onStop"));

        mVideoView.stopPlayback();

        super.onStop();
    }

    @Override
    public void onDestroy() {

        logger.finer(log.entry("onDestroy"));

        this.launcher.stop();

        super.onDestroy();
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        logger.finer(log.entry("onCreateDialog", id));

        switch (id) {
        case ON_START_FAILURE_DIALOG:
            return buildServiceFailureDialog(R.string.service_start_failed_msg,
                                             R.string.retry_label,
                                             R.string.exit_label,
                                             R.string.retry_failed_msg);

        case ON_DISCONNECT_FAILURE_DIALOG:
            return buildServiceFailureDialog(R.string.service_disconnected_msg,
                                             R.string.restart_label,
                                             R.string.exit_label,
                                             R.string.restart_failed_msg);

        case ON_STREAM_COMPLETE_DIALOG:
            return buildStreamCompleteDialog();

        case ON_MEDIA_ERROR_DIALOG:
            return buildMediaErrorDialog();
        }

        return null;
    }

    /**
     * 
     */
    private void startPlayout() {
        MulticastMediaPlayer.this.progressDialog.show();
        try {
            if (MulticastMediaPlayer.this.launcher.start()) {
                MulticastMediaPlayer.this.mVideoView.setVideoURI(MulticastMediaPlayer.this.reflectorRequestUri);
                MulticastMediaPlayer.this.mVideoView.start();
            }
            else {
                MulticastMediaPlayer.this.progressDialog.dismiss();
                Toast.makeText(MulticastMediaPlayer.this,
                               getResources().getText(R.string.service_start_failed_msg),
                               Toast.LENGTH_LONG).show();
                showDialog(ON_START_FAILURE_DIALOG);
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @param failureMsgId
     * @param positiveLabelId
     * @param negativeLabelId
     * @param failedMsgId
     * @return
     */
    private Dialog buildServiceFailureDialog(int failureMsgId, int positiveLabelId, int negativeLabelId, final int failedMsgId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getText(failureMsgId)).setCancelable(false)
                .setPositiveButton(getResources().getText(positiveLabelId), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        startPlayout();
                    }
                }).setNegativeButton(getResources().getText(negativeLabelId), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MulticastMediaPlayer.this.finish();
                    }
                });
        return builder.create();
    }

    /**
     * @return
     */
    private Dialog buildStreamCompleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getText(R.string.stream_complete_msg)).setCancelable(false)
                .setNeutralButton(getResources().getText(R.string.ok_label), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        return builder.create();
    }

    /**
     * @return
     */
    private Dialog buildMediaErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getText(R.string.media_error_msg)).setCancelable(false)
                .setPositiveButton(getResources().getText(R.string.retry_label), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        startPlayout();
                    }
                }).setNegativeButton(getResources().getText(R.string.exit_label), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MulticastMediaPlayer.this.finish();
                    }
                });
        return builder.create();
    }
}