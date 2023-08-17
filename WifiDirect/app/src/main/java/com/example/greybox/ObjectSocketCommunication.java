package com.example.greybox;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.example.greybox.meshmessage.MeshMessage;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ObjectSocketCommunication implements Runnable {
    private static final String TAG = "SocketCommunication";

    private final Socket socket;
    private final Handler handler;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    private HandlerThread workerThread;
    private Handler workerHandler;


    public ObjectSocketCommunication(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
        // Used for write operations
        this.workerThread = new HandlerThread("SocketCommObj");
        this.workerThread.start();
        this.workerHandler = new Handler(workerThread.getLooper());
        //
    }


    @Override
    public void run() {
        try {
            // NOTE: the order of creation matters. From the documentation of ObjectInputStream(InputStream in)
            //  and this post (https://stackoverflow.com/questions/14110986/new-objectinputstream-blocks):
            //  A serialization stream header is read from the stream and verified. This constructor
            //  will block until the corresponding ObjectOutputStream has written and flushed the
            //  header.
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.flush();
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            Log.d(TAG, "Got in/out streams.");
        } catch (IOException e) {
            Log.e(TAG, "Exception while getting input and output streams.", e);
            close();
            return;
        }


        /// PE_MSG_SPECIFIC_CLIENTS
        // NOTE: Unfortunately, this is action is only meant for clients. CLIENT_SOCKET_CONNECTION
        //  indicates the client to send its info to the GO so it can update the list of clients
        //  and then broadcast it to other clients
        handler.obtainMessage(ThreadMessageTypes.CLIENT_SOCKET_CONNECTION, this).sendToTarget();
        ///

        // While socket is still open
        while (!socket.isClosed()) {
            try {
                Log.d(TAG, "Waiting for messages...");
                MeshMessage msg = (MeshMessage) objectInputStream.readObject();
                Log.d(TAG, "Read message: " + msg);

                // If there is a message
                if (msg != null) {
                    Log.d(TAG, "Read message.");
                    // Put a message in the `MessageQueue` of the thread handled by `handler`
                    handler.obtainMessage(ThreadMessageTypes.MESSAGE_READ, msg).sendToTarget();
                }
            } catch (IOException | ClassNotFoundException e) {
                Log.e(TAG, "Error while reading.", e);
                e.printStackTrace();
                // Build and pass a message to indicate to other thread that we disconnected.
                handler.obtainMessage(ThreadMessageTypes.SOCKET_DISCONNECTION, this).sendToTarget();
                close();
            }
        }
    }

    public void write(MeshMessage msg) {
        workerHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Writing message: " + msg);
                    Log.d(TAG, " objectOutputStream: " + objectOutputStream);
                    // Prevent using back references: https://stackoverflow.com/questions/12341086/java-socket-serialization-object-wont-update
                    // If we don't call reset(), only the most recently connected device will get the
                    // complete list of clients
                    // NOTE: the pair ObjectOutputStream.writeUnshared() and ObjectInputStream.readUnshared()
                    //  didn't prevent this issue.
                    objectOutputStream.reset();
                    objectOutputStream.writeObject(msg);
                } catch (IOException e) {
                    Log.e(TAG, "Exception while writing to output stream.", e);
                }
            }
        });
    }

    public void close() {
        Log.d(TAG, "Closing socket.");
        safeClose(objectOutputStream);
        safeClose(objectInputStream);
        safeClose(socket);
    }

    private void safeClose(Closeable obj) {
        if (obj == null) return;

        try {
            obj.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error closing object " + obj, e);
        }
    }

    public boolean isClosed() {
        if (socket == null) {
            return true;
        }
        return socket.isClosed();
    }

    public Socket getSocket() { return socket; }

    public ObjectOutputStream getObjectOutputStream() { return objectOutputStream; } // TODO: Remove. Debugging
}
