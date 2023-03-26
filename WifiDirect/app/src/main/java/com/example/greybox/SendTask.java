package com.example.greybox;

import android.os.AsyncTask;

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