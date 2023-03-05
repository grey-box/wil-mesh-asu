package com.meshasu;

import android.net.wifi.p2p.WifiP2pManager;
import android.content.Context;
import android.os.Bundle;
import android.app.Activity;

public class MainActivity extends Activity implements WifiP2pManager.ChannelListener {
    WifiP2pManager manager;
    WifiP2pManager.Channel netChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        netChannel = manager.initialize(this,getMainLooper(),null);
        //TODO: Initialize receiver here
    }

    @Override
    public void onChannelDisconnected() {
        // Not implemented
    }
}