package org.js4ms.common.util.logging.swing;

/*
 * #%L
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *  
 * Console.java [org.js4ms.jsdk:common]
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;


public class Console extends OutputStream 
{
    public class Window extends WindowAdapter implements WindowListener, ActionListener {

        private JFrame frame;
        private JTextArea textArea;
        private JScrollPane scrollPane;
        private boolean isOpen;
        private Object onOpen = new Object();

        public Window(String title) {

            this.frame=new JFrame(title);
            
            Dimension screenSize=Toolkit.getDefaultToolkit().getScreenSize();
            Dimension frameSize=new Dimension((int)(screenSize.width/2),(int)(screenSize.height/2));
            int x=(int)(frameSize.width/2);
            int y=(int)(frameSize.height/2);
            this.frame.setBounds(x,y,frameSize.width,frameSize.height);
            
            this.textArea=new JTextArea();
            this.textArea.setEditable(false);
            this.textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JButton button=new JButton("clear");
            
            this.scrollPane = new JScrollPane(this.textArea);
            this.frame.getContentPane().setLayout(new BorderLayout());
            this.frame.getContentPane().add(this.scrollPane,BorderLayout.CENTER);
            this.frame.getContentPane().add(button,BorderLayout.SOUTH);
            this.frame.setVisible(true);     
            this.frame.getTitle();
            this.frame.addWindowListener(this);
            button.addActionListener(this);
            this.isOpen = true;
        }

        public void appendText(final String text) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                  textArea.append(text);
              }
           });
        }

        public void setText(final String text) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                  textArea.setText(text);
              }
           });
        }

        public void close() {
            WindowEvent wev = new WindowEvent(this.frame, WindowEvent.WINDOW_CLOSING);
            Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
        }

        public void exit() {
            this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            close();
        }

        public void waitForClose() throws InterruptedException {
            synchronized (this.onOpen) {
                if (this.isOpen) {
                    this.onOpen.wait();
                }
            }
        }

        public synchronized void windowClosed(WindowEvent evt) {

            synchronized (this.onOpen) {
                this.isOpen = false;
                this.onOpen.notifyAll();
            }
        }

        public synchronized void windowClosing(WindowEvent evt) {

            this.frame.setVisible(false);
            this.frame.dispose();
        }

        public synchronized void actionPerformed(ActionEvent evt) {
            this.textArea.setText("");
        }
    }

    private Window window;
    private String encoding;
    private boolean waitForClose;

    public Console(String title, boolean waitForClose) {
        this(title, Charset.defaultCharset().displayName(), waitForClose);
    }

    public Console(String title, String encoding, boolean waitForClose) {

        this.encoding = encoding;
        this.waitForClose = waitForClose;
        this.window = new Window(title);

        System.out.close();
        System.err.close();

        this.window.appendText("# Redirecting output streams to console...\n");

        try {
            System.setOut(new PrintStream(this)); 
        }
        catch (SecurityException se) {
            System.out.println("# Couldn't redirect STDOUT to this console"+se.getMessage());
        }

        try {
            System.setErr(new PrintStream(this)); 
        } 
        catch (SecurityException se) {
            System.out.println("# Couldn't redirect STDERR to this console"+se.getMessage());
        }

        System.out.println("# Streams redirected to console");
    }

    @Override
    public void close() {
        if (this.waitForClose ) {
            try {

                this.window.appendText("# Console stopped - console may now be closed");

                this.window.waitForClose();

            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        else {
            this.window.exit();
        }
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        this.window.appendText(new String(bytes, encoding));
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        this.window.appendText(new String(bytes, offset, length, encoding));
    }

    @Override
    public void write(int b) throws IOException {
        this.window.appendText(String.valueOf((char)b));
    }
}

