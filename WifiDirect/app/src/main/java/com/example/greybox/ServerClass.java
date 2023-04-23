package com.example.greybox;

import android.os.Handler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// Server class to be made into own class soon
public class ServerClass extends Thread{
    Socket socket;
    ServerSocket serverSocket;
    SendReceive sendReceive;
    Handler handler;
    int portNum;
    public ServerClass(Handler _handler, int _portNum){
        this.handler = _handler;
        this.portNum = _portNum;
    }

    @Override
    public void run() {
        try {
            // Set server socket port number
            serverSocket = new ServerSocket(portNum);
            // accept and store socket from server socket
            socket = serverSocket.accept();
            // Create sendRecieve task to handle socket stream operations
            sendReceive = new SendReceive(socket, handler);
            sendReceive.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public SendReceive getSendReceive(){
        return sendReceive;
    }
}