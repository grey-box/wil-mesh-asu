package com.example.greybox;

import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

// Class for handling socket streams
public class SendReceive extends Thread{
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
