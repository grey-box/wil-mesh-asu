package com.example.greybox;

import android.os.Handler;
import android.renderscript.ScriptGroup;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketCommunication implements Runnable {
    private static final String TAG = "SocketCommunication";

    private Socket socket;
    private Handler handler;
    private InputStream in;
    private OutputStream out;
    private byte[] buffer = new byte[1024]; // Stores the transmitted message


    SocketCommunication(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }


    @Override
    public void run() {

        // Send a this object to be handled in another thread. Just in case we want to communicate
        handler.obtainMessage(MainActivity.HANDLE, this).sendToTarget();

        int bytes;

        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
            Log.i(TAG, "Got in/out streams.");
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
                Log.i(TAG, "Read " + bytes + " bytes.");

                // Communication finished.
                if (bytes == -1) { break; }

                // If there is a message
                if (bytes > 0) {

                    // NOTE: I think handler.post(Runnable) and handler.obtainMessage().sendToTarget()
                    //  have the same effect, so, it's redundant. Or at least in this case that we just
                    //  send a message instead of doing some action (runnable)
                    // Put a message in the `MessageQueue` of the thread handled by `handler`
                    handler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    Log.i("GREYBOX", "ClientSocket. Posted message.");

                    // Put an task in the `MessageQueue` of the thread handled by `handler`
//                    handler.post(() -> {
//                        // NOTE: the methods Handler.obtainX() are like constructors, they build a
//                        //   message but using a recycled object. This is more performant (doesn't
//                        //   allocate memory). With the params we can set its 3 members if we need to.
//                        //   Then we pass that message to the thread referenced by handler
//                        handler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
//                        Log.i("GREYBOX", "ClientSocket. Posted message.");
//                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Error while reading. Socket disconnected?");
                e.printStackTrace();
                // TODO: investigate why we keep logging even after closing the app. Does the socket
                //  survive after the app was killed?
//                    02:12:29.137  W  java.net.SocketException: Connection reset
//                    02:12:29.137  W  	at java.net.SocketInputStream.read(SocketInputStream.java:156)
//                    02:12:29.137  W  	at java.net.SocketInputStream.read(SocketInputStream.java:143)
//                    02:12:29.137  W  	at java.net.SocketInputStream.read(SocketInputStream.java:129)
//                    02:12:29.137  W  	at com.example.greybox.ClientClass.lambda$run$1$com-example-greybox-ClientClass(ClientClass.java:88)
//                    02:12:29.137  W  	at com.example.greybox.ClientClass$$ExternalSyntheticLambda1.run(Unknown Source:2)
//                    02:12:29.137  W  	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1137)
//                    02:12:29.137  W  	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:637)
//                    02:12:29.137  W  	at java.lang.Thread.run(Thread.java:1012)
                // Build and pass a message to indicate to other thread that we disconnected.
                // Hopefully this will get rid of the warnings
                handler.obtainMessage(MainActivity.SOCKET_DISCONNECTION, this).sendToTarget();
//                throw new RuntimeException("Error while reading.");
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

    private void close() {
        try {
            // TODO: do we need to close the streams or is it done automatically since they are `Closeable`?
//            in.close();
//            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error closing socket", e);
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
