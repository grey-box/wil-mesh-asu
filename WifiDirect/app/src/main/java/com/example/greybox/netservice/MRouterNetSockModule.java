package com.example.greybox.netservice;

import android.os.Handler;
import android.util.Log;

import com.example.greybox.ObjectSocketCommunication;
import com.example.greybox.meshmessage.MeshMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MRouterNetSockModule implements Runnable {
    private static final String TAG = "MRouterNetSockModule";

    private final ServerSocket serverSocket;
    private final Handler parentHandler;

    ///
    // TODO: tmp class to have a structure of clients and assign them an id
    public volatile int clientId;
//    volatile HashMap<String, MeshDevice> clientList;
    volatile ArrayList<ObjectSocketCommunication> clientSockets;
    ///
    private final Object mutex = new Object();


    // --------------------------------------------------------------------------------------------
    //  Constructors
    // --------------------------------------------------------------------------------------------
    public MRouterNetSockModule(Handler parentHandler, int portNum) throws RuntimeException {
        try {
            // Set server socket port number
            this.serverSocket = new ServerSocket(portNum);
            Log.d(TAG, "ServerSocket created.");
        } catch (IOException e) {
            Log.e(TAG, "ServerSocket creation failed.", e);
            throw new RuntimeException("ServerSocket couldn't be created.");
        }
        this.parentHandler = parentHandler;
//        this.clientList = new HashMap<>();
        this.clientSockets = new ArrayList<>();
    }


    // --------------------------------------------------------------------------------------------
    //  Methods
    // --------------------------------------------------------------------------------------------
    @Override
    public void run() {
        Socket socket = null;
        ObjectSocketCommunication socketComm;
        ///
        // We need to know when the socket client connects to us. After that happens, the comm channel
        // is ready to send the client list to the clients.
        // NOTE: By default the Handler is associated with the thread it is instantiated on
        // TODO: UPDATE: delete all references to this code. It's better to do it in the SocketCommunicationObjects class
//        if (socketHandler == null) {
//            socketHandler = new Handler(Looper.myLooper(), socketCommMessageCallback);
//        }
        ///

        try {
            // accept and store socket from server socket
            Log.d(TAG, "ServerSocket. Waiting for client.");
            socket = serverSocket.accept();
        } catch (IOException e) {
            Log.e(TAG, "Connection with client failed.", e);
            if (socket == null) return;
            if (!socket.isClosed()) {
                closeClientSocket(socket);
            }
            return;
        }

        try {
            // TODO: this doesn't seem to execute if the connection failed from the side of the client
            //  due to a VPN, so, I'm assuming this it's still waiting for a connection in serverSocket.accpet()
            //  the problem is that the WifiP2pManager changed of state (an intent was received).
            // TODO: Also, if the client disconnects, we are not handling correctly the event. The log
            //  is messed up because we handle the exception but we don't do anything, just printing
            //  the error in an infinite loop.
            // wait to see if the socket is connected. If not, abort
//            Thread.sleep(500);
            Log.d(TAG, " socket.isConnected(): " + socket.isConnected());
            Log.d(TAG, " socket.isClosed():    " + socket.isClosed());
            if (socket.isClosed() || !socket.isConnected()) {
                Log.e(TAG, " ServerClass. Throwing error.");
                Log.d(TAG, " socket.isConnected(): " + socket.isConnected());
                Log.d(TAG, " socket.isClosed():    " + socket.isClosed());
                closeClientSocket(socket);
                throw new IOException();
            }

            // Notify the parent thread that a client socket connected successfully. This is used
            // to update the list of clients that is sent to the clients
            socketComm = new ObjectSocketCommunication(socket, parentHandler);

            Log.d(TAG, "Client connected.");
            Log.d(TAG, "  getInetAddress:        " + socket.getInetAddress());  // Returns the address to which the socket is connected.
            Log.d(TAG, "  getLocalAddress:       " + socket.getLocalAddress());   // Gets the local address to which the socket is bound.
            Log.d(TAG, "  getLocalSocketAddress: " + socket.getLocalSocketAddress()); // Returns the address of the endpoint this socket is bound to.
        } catch (IOException e) {
            Log.e(TAG, "Connection with client failed.", e);
            e.printStackTrace();
            closeClientSocket(socket);
            return;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error.", e);
            e.printStackTrace();
            closeClientSocket(socket);
            return;
        }

        // Critical section
        synchronized (mutex) {
            clientId++;
//                clientList.put(new MeshDevice(clientId, socketComm));
            // TODO: need to remove the proper instance when the socket disconnects. How can we know
            //  which one disconnected?  We would need to identify the socketComm
            clientSockets.add(socketComm);
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Log.d(TAG, "Creating thread communicate (read).");
        executorService.execute(socketComm);
    }

    public void write(MeshMessage msg) {
//        for (Map.Entry<String, MeshDevice> entry : clientList.entrySet()) {
//            entry.getValue().socketComm.write(msg);
//        }
        for (ObjectSocketCommunication s : clientSockets) {
            Log.d(TAG, "Writing to: " + s.getObjectOutputStream());
            s.write(msg);
        }
    }

    public ServerSocket getServerSocket() { return serverSocket; }

    public void closeServerSocket() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while closing the server socket", e);
        }
    }

    private void closeClientSocket(Socket s) {
        try {
            s.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while closing the client socket", e);
        }
    }
}
