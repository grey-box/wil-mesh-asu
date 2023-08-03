package com.example.greybox;

import android.os.Handler;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;

public class SocketCommunication implements Runnable, Serializable {
    private static final String TAG = "SocketCommunication";

    private Socket socket;
    private Handler handler;
    private InputStream in;
    private OutputStream out;
    private byte[] buffer = new byte[1024]; // Stores the transmitted message


    public SocketCommunication(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }


    @Override
    public void run() {

        // Send a this object to be handled in another thread. Just in case we want to communicate
        handler.obtainMessage(ThreadMessageTypes.HANDLE, this).sendToTarget();

        int bytes;

        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
            Log.d(TAG, "Got in/out streams.");
        } catch (IOException e) {
            Log.e(TAG, "Exception while getting input and output streams.", e);
            close();
            return;
        }

        // While socket is still open
        while (true) {
            try {
                // Read message inputStream from socket and store in variable
                bytes = in.read(buffer);
                Log.d(TAG, "Read " + bytes + " bytes.");

                // Communication finished.
                if (bytes == -1) { break; }

                // If there is a message
                if (bytes > 0) {
                    // Put a message in the `MessageQueue` of the thread handled by `handler`
                    handler.obtainMessage(ThreadMessageTypes.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    Log.d(TAG, "Posted a message.");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error while reading. Socket disconnected?", e);
                // Build and pass a message to indicate to other thread that we disconnected.
                handler.obtainMessage(ThreadMessageTypes.SOCKET_DISCONNECTION, this).sendToTarget();
                break;
            }
        }
    }

    public void write(byte[] msg) {
        try {
            out.write(msg);
        } catch (IOException e) {
            Log.e(TAG, "Exception while writing to output stream.", e);
        }
    }

    public void close() {
        Log.d(TAG, "Closing socket.");
        safeClose(in);
        safeClose(out);
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

    Socket getSocket() { return socket; }
}
