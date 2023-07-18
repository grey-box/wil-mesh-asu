package com.example.greybox;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


class ServerClass implements Runnable {
    private static final String TAG = "ServerClass";

    private final ServerSocket serverSocket;
    private final Handler handler;

    ///
    // TODO: maybe it will be better to have something like a HashMap where we can look for a name.
    //  I might need another class that can model better the client socket, like having an id and
    //  their own InputStream and OutputStream.
    //  Why all examples create a local variable for Input and Output, isn't the same as calling the
    //  getters?
    // TODO: tmp class to have a structure of clients and assign them an id
    public volatile int clientId;
    volatile ArrayList<Client> clientList;
    ///
    private final Object mutex = new Object();

    private static class Client {
        public int id;
        SocketCommunication socketComm;

        Client(int id, SocketCommunication s) {
            this.socketComm = s;
            this.id = id;
        }
    }


    public ServerClass(Handler handler, int portNum) throws RuntimeException {
        try {
            // Set server socket port number
            this.serverSocket = new ServerSocket(portNum);
            Log.i(TAG, "ServerSocket created.");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "ServerSocket creation failed.", e);
            throw new RuntimeException("ServerSocket couldn't be created.");
        }
        this.handler = handler;
        this.clientList = new ArrayList<>(10);
    }

    public void write(byte[] bytes, int clientID) {
        // TODO: implement how to send the message to the correct client. Right now we send it
        //  to all of them
        for (Client c : clientList) {
            c.socketComm.write(bytes);
        }
    }

    @Override
    public void run() {
        Socket socket;
        SocketCommunication socketComm;
        InputStream in;
        OutputStream out;

        try {
            // accept and store socket from server socket
            Log.i(TAG, "ServerSocket. Waiting for client.");
            socket = serverSocket.accept();

            socketComm = new SocketCommunication(socket, handler);
            // Critical section
            synchronized (mutex) {
                clientId++;
                clientList.add(new Client(clientId, socketComm));
            }
            Log.i(TAG, "Client connected.");
        } catch (IOException e) {
            Log.e(TAG, "Connection with client failed.", e);
            e.printStackTrace();
            return;
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Log.i(TAG, "Creating thread communicate (read).");
        executorService.execute(socketComm);

        // NOTE: if we got here, the socket communication failed or was closed. Remove the reference
        //  in the list of clients
        synchronized (mutex) {
            // TODO: Unfortunately, with a list we need to check all elements to know which
            //  to remove, since the index might have changed since it's not a static array
            int idx = 0;
            // TODO: check if removing all closed sockets from the list is dangerous
            // TODO: verify also if the message MainActivity.SOCKET_DISCONNECTION we send in
            //  SocketCommunication could affect this section
            for (Client c : clientList) {
                if (c.socketComm.isClosed()) {
                    clientList.remove(idx);
                }
                idx++;
            }
        }
    }
}
