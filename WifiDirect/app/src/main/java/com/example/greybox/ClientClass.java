package com.example.greybox;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class ClientClass implements Runnable {
    private static final String TAG = "ClientClass";

    private InetAddress hostAddress;
    Socket socket;
    Handler handler;
    int portNum;
    SocketCommunication socketComm;

    // Constructor passed the server host IpAddress
    public ClientClass(InetAddress hostAddress, Handler handler, int portNum){
        this.hostAddress = hostAddress;
        socket = new Socket();
        this.handler = handler;
        this.portNum = portNum;
    }

    @Override
    public void run() {
        try {
            // try to connect socket to host socket port
            Log.i(TAG, "Connecting to server.");
            // TODO: the socket has to be bounded or not? Sometimes I see a message in logs of a IP
            //  address being already in use. But it's like a warning.
            socket.bind(null);
            // TODO: timeout should be a constant in another class
            socket.connect(new InetSocketAddress(hostAddress.getHostAddress(), portNum), 500);
            Log.i(TAG, "Client connected to Server: " + socket.isConnected());
        } catch (IOException e) {
            Log.i(TAG, "Error during connection.");
            closeSocket();
        }

        socketComm = new SocketCommunication(socket, handler);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Log.i(TAG, "Creating thread communicate (read).");
        executorService.execute(socketComm);
    }

    public void write(byte[] msg) {
        socketComm.write(msg);
    }

    public void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error while closing client socket", e);
//            throw new RuntimeException("Error while closing client socket");
        }
    }
    ////
}
