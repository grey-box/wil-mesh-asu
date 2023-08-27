package com.example.greybox;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

public class WfdStatusInterpreter {
    private static final String TAG = "WfdErrorInterpreter";

    /*
     * Log with level E the interpretation of the error.
     */
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

    /*
     * Return the name of the error as a string.
     */
    static String errorString(int errorCode) {
        String error = "";

        switch (errorCode) {
            case WifiP2pManager.P2P_UNSUPPORTED:
                error = "P2P_UNSUPPORTED";
                break;
            case WifiP2pManager.ERROR:
                error = "ERROR";
                break;
            case WifiP2pManager.BUSY:
                error = "BUSY";
                break;
        }
        return error;
    }

    /*
     * WifiP2pDevice status interpreter
     */
    public static void logWifiP2pDeviceStatus(String tag, int status) {
        switch (status) {
            case WifiP2pDevice.CONNECTED:
                Log.d(tag, "status: CONNECTED");
                break;
            case WifiP2pDevice.INVITED:
                Log.d(tag, "status: INVITED");
                break;
            case WifiP2pDevice.FAILED:
                Log.d(tag, "status: FAILED");
                break;
            case WifiP2pDevice.AVAILABLE:
                Log.d(tag, "status: AVAILABLE");
                break;
            case WifiP2pDevice.UNAVAILABLE:
                Log.d(tag, "status: UNAVAILABLE");
                break;
        }
    }
}
