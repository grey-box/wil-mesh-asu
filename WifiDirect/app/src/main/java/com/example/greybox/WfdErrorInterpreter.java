package com.example.greybox;

import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

public class WfdErrorInterpreter {
    private static final String TAG = "WfdErrorInterpreter";

    static void logError(int errorCode) {
        switch (errorCode) {
            case WifiP2pManager.P2P_UNSUPPORTED:
                Log.e(TAG, " Failed because Wi-Fi Direct is not supported on the device.");
                break;

            case WifiP2pManager.ERROR:
                Log.e(TAG, " Failed due to an internal error.");
                break;

            case WifiP2pManager.BUSY:
                Log.e(TAG, " Failed due to the framework is busy and is unable to attend the request.");
                break;
        }
    }
}
