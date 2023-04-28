package com.meshasu;

import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.content.Context;
import android.os.Bundle;
import android.app.Activity;

public class MainActivity extends Activity implements WifiP2pManager.ChannelListener {
    WifiP2pManager manager;
    WifiP2pManager.Channel netChannel;
    IntentFilter intentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        netChannel = manager.initialize(this,getMainLooper(),null);
        //TODO: Initialize receiver here

        intentManager = new IntentFilter();
        intentManager.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentManager.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentManager.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentManager.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
    }

    @Override
    public void onChannelDisconnected() {
        // Not implemented
    }
}