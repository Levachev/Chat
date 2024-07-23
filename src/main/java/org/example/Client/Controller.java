package org.example.Client;

import org.example.Parser;
import sun.misc.Signal;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Controller {
    Controller(){}
    public void run() throws IOException {


        String addr = JOptionPane.showInputDialog("enter ip address", "127.0.0.1");
        String port = JOptionPane.showInputDialog("enter port", "2000");
        String nickname = JOptionPane.showInputDialog("enter ur nickname", "boss");

        if(addr==null || port==null || nickname==null){
            System.out.println("bad input");
            return;
        }

        Client client;

        try {
            client = new Client(addr, Integer.parseInt(port), nickname);
        } catch (NumberFormatException | ClientException e){
            System.out.println("something wrong with server");
            return;
        }

        Client finalClient = client;
        GUI gui = new GUI();

        Thread listenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                finalClient.setListenerThread(Thread.currentThread());
                while (true) {
                    try {
                        String message = recvXML();
                        message=finalClient.parseMessage(message);
                        gui.addMessage(message);
                        gui.inputField.setText("");
                        if(finalClient.isLogin) {
                            gui.setNickname(finalClient.nickname);
                        }
                    } catch (IOException | ClientException e) {
                        System.out.println("something wrong with server");
                        finalClient.terminate();
                    }
                }
            }
            private String recvXML() throws IOException {
                ByteBuffer length = ByteBuffer.allocate(4);
                while (length.hasRemaining())
                {
                    finalClient.client.read(length);
                }
                length.rewind();
                int lengthOfMessage = length.getInt();

                ByteBuffer buffer = ByteBuffer.allocate(lengthOfMessage);
                while (buffer.hasRemaining())
                {
                    finalClient.client.read(buffer);
                }

                return new String(buffer.array()).trim();
            }
        });
        listenerThread.start();

        Signal.handle(new Signal("INT"), signal -> {
            finalClient.terminate();
        });


        gui.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                finalClient.terminate();
            }
        });

        gui.inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyTyped(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    try {
                        String input = gui.getMessage();
                        finalClient.currentMessage = input;

                        finalClient.sendToServer(input);
                    } catch (IOException | ClientException ex) {
                        System.out.println("something wrong with server");
                        finalClient.terminate();
                    }
                }
            }
        });
    }
}

