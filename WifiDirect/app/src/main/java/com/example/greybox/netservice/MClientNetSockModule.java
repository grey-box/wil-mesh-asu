package com.example.greybox.netservice;

import android.os.Handler;
import android.util.Log;

import com.example.greybox.ObjectSocketCommunication;
import com.example.greybox.WfdNetManagerService;
import com.example.greybox.meshmessage.MeshMessage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MClientNetSockModule implements Runnable {
    private static final String TAG = "MClientNetSockModule";

    private InetAddress hostAddress;
    private Socket socket;
    private Handler handler;
    private int portNum;
    private ObjectSocketCommunication socketComm;

    // --------------------------------------------------------------------------------------------
    //  Constructors
    // --------------------------------------------------------------------------------------------
    public MClientNetSockModule(InetAddress hostAddress, Handler handler, int portNum){
        this.hostAddress = hostAddress;
        socket = new Socket();
        this.handler = handler;
        this.portNum = portNum;
        Log.d(TAG, "handler: " + handler);
    }


    // --------------------------------------------------------------------------------------------
    //  Methods
    // --------------------------------------------------------------------------------------------
    @Override
    public void run() {
        try {
            // try to connect socket to host socket port
            Log.d(TAG, "Connecting to server.");

            Log.d(TAG, "hostAddress.getHostAddress(): " + hostAddress.getHostAddress());
            Log.d(TAG, "portNum: " + portNum);

            socket.bind(null);
            // TODO: timeout should be a constant in another class
            socket.connect(new InetSocketAddress(hostAddress.getHostAddress(), portNum), 500);

            Log.d(TAG, "Client connected to Server: " + socket.isConnected());
            Log.d(TAG, "  getInetAddress:        " + socket.getInetAddress());  // Returns the address to which the socket is connected.
            Log.d(TAG, "  getLocalAddress:       " + socket.getLocalAddress());   // Gets the local address to which the socket is bound.
            Log.d(TAG, "  getLocalSocketAddress: " + socket.getLocalSocketAddress()); // Returns the address of the endpoint this socket is bound to.
            Log.d(TAG, "  MAC address:           " + WfdNetManagerService.getMacFromLocalIpAddress(socket.getLocalAddress()));

        } catch (IOException e) {
            Log.e(TAG, "Error during connection.");
            closeSocket();
            return;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error. + ", e);
            closeSocket();
            return;
        }

        socketComm = new ObjectSocketCommunication(socket, handler);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Log.d(TAG, "Creating thread communicate (read).");
        executorService.execute(socketComm);
    }

    public void write(MeshMessage msg) {
        socketComm.write(msg);
    }

    public void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error while closing client socket", e);
        }
    }

    public Socket getSocket() { return socket; }
    ////
}
