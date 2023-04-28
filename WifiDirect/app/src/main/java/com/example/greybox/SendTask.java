package com.example.greybox;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class SendTask extends AsyncTask<Void,Void,Void> {
    // Send message string
    String message;
    SendReceive sendReceive;
    // Pass constructor the string message
    SendTask(String msg, SendReceive _sendReceive){
        message=msg;
        this.sendReceive = _sendReceive;
    }
    // Override this method to perform a computation on a background thread.
    @Override
    protected Void doInBackground(Void... args0) {
        // Write message bytes to socket.outputstream
        sendReceive.write(message.getBytes());
        return null;
    }
    // Runs on the UI thread after doInBackground(Params...)
    @Override
    protected void onPostExecute(Void unused) {
        super.onPostExecute(unused);
    }
}

// Server class to be made into own class soon
class ServerClass extends Thread{
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

// Client class to be made into own class soon
class ClientClass extends Thread{
    Socket socket;
    String hostAdd;
    SendReceive sendReceive;
    Handler handler;
    int portNum;
    // Constructor passed the server host IpAddress
    public ClientClass(InetAddress hostAddress, Handler _handler, int _portNum){
        hostAdd = hostAddress.getHostAddress();
        socket = new Socket();
        this.handler = _handler;
        this.portNum = _portNum;
    }

    @Override
    public void run() {
        try {
            // try to connect socket to host socket port
            socket.connect(new InetSocketAddress(hostAdd,portNum), 500);
            // Create sendRecieve task to handle socket stream operations
            sendReceive = new SendReceive(socket, handler);
            // Run the sendReceive object
            sendReceive.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SendReceive getSendReceive(){
        return sendReceive;
    }
}

// Class for handling socket streams
class SendReceive extends Thread{
    // A socket is an endpoint for communication between two machines.
    private Socket socket;
    // This abstract class is the superclass of all classes representing an input stream of bytes.
    private InputStream inputStream;
    // This abstract class is the superclass of all classes representing an output stream of bytes.
    private OutputStream outputStream;
    private Handler handler;
    // Constructor passed socket for end-to-end communication
    public SendReceive(Socket skt, Handler _handler){
        this.handler = _handler;
        // socket for end to end communication
        socket = skt;
        try {
            // Save input and out as actual variables
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    // Method to override that runs when thread starts
    @Override
    public void run() {
        // Stores the transmitted message
        byte[] buffer = new byte[1024];
        int bytes;
        // While socket is still open
        while(socket!=null){
            try {
                // Read message inputstream from socket and store in variable
                bytes = inputStream.read(buffer);
                // If there is a message
                if(bytes > 0){
                    // Pass the message to the handler and have the handler save and convert message
                    // to a string then display the string in the layout
                    handler.obtainMessage(1, bytes, -1, buffer).sendToTarget();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Method used by sendtask that writes the message to the socket outputstream
    public void write(byte[] bytes){
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



