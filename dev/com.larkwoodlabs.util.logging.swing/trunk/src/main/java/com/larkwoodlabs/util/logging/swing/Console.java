package com.larkwoodlabs.util.logging.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;


public class Console extends WindowAdapter implements WindowListener, ActionListener
{
    private JFrame frame;
    private JTextArea textArea;
    private JScrollPane scrollPane;
    private StreamPump outPump;
    private StreamPump errPump;
    private boolean isConsoleOpen = false;
    private Object consoleOpen = new Object();

    static class StreamPump implements Runnable {
        
        Console console;
        Pipe pipe;
        PrintStream printStream;
        BufferedReader reader;
        Thread thread;
        boolean isStarted = false;

        StreamPump(Console console) throws IOException {
            this.console = console;
            this.pipe = Pipe.open();
            this.printStream = new PrintStream(Channels.newOutputStream(pipe.sink()),true);
            this.reader = new BufferedReader(new InputStreamReader(Channels.newInputStream(pipe.source())));
        }
        
        PrintStream getPrintStream() {
            return this.printStream;
        }
        
        synchronized void start() {
            if (!this.isStarted) {
                this.isStarted = true;
                this.thread = new Thread(this);
                this.thread.setDaemon(true);
                this.thread.start();
            }
        }

        synchronized void stop() throws InterruptedException {
            if (this.isStarted) {
                this.isStarted = false;
                this.console.append("# stopping console stream thread...");
                this.thread.interrupt();
                this.printStream.close();
                this.thread.join();
            }
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        this.console.append("# Console reader reached end-of-stream - exiting thread.\n");
                        break;
                    }
                    this.console.append(line+"\n");
                }
                catch (IOException e) {
                    this.console.append("# Console exception - "+e.getClass().getSimpleName()+": "+e.getMessage());
                }
            }
        }
    }
    

     public Console(String title) {
         
        System.out.close();
        System.err.close();
        
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
        
        append("# Redirecting output streams to console...\n");
        
        try
        {
            this.outPump = new StreamPump(this);
            System.setOut(this.outPump.getPrintStream()); 
        } 
        catch (java.io.IOException io)
        {
            append("# Couldn't redirect STDOUT to this console\n"+io.getMessage());
        }
        catch (SecurityException se)
        {
            append("# Couldn't redirect STDOUT to this console\n"+se.getMessage());
        } 
        
        try 
        {
            this.errPump = new StreamPump(this);
            System.setErr(this.errPump.getPrintStream()); 
        } 
        catch (java.io.IOException io)
        {
            append("# Couldn't redirect STDERR to this console\n"+io.getMessage());
        }
        catch (SecurityException se)
        {
            append("# Couldn't redirect STDERR to this console\n"+se.getMessage());
        }       
            
        
        append("# Starting console...\n");
        
        this.outPump.start(); 
        this.errPump.start();
                
        append("# Console started.\n");

        this.isConsoleOpen = true;
    }
    
    public void exit() {
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        close();
    }

    public void close() {
        WindowEvent wev = new WindowEvent(this.frame, WindowEvent.WINDOW_CLOSING);
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
    }
 
    public void waitForClose() throws InterruptedException {

        synchronized (this.consoleOpen) {
            if (this.isConsoleOpen) {
                this.consoleOpen.wait();
            }
        }
    }

    public synchronized void windowClosed(WindowEvent evt)
    {

        synchronized (this.consoleOpen) {
            this.isConsoleOpen = false;
            this.consoleOpen.notifyAll();
        }
    }
        
    public synchronized void windowClosing(WindowEvent evt)
    {
        System.out.close();
        System.err.close();

        try {
            this.outPump.stop();
            this.errPump.stop();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        this.frame.setVisible(false);
        this.frame.dispose();
    }
    
    public synchronized void actionPerformed(ActionEvent evt)
    {
        this.textArea.setText("");
    }

    public void append(final String text) {

        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
              textArea.append(text);
          }
       });

    }

}