package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity mainActivity;

    public WifiDirectBroadcastReceiver(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, MainActivity mainActivity){

        this.mManager = mManager;
        this.mChannel = mChannel;
        this.mainActivity = mainActivity;
    }

    /**
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            if(state==WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                Toast.makeText(context, "Wifi is  ON",Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(context, "Wifi is  OFF",Toast.LENGTH_SHORT).show();
            }
        }else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            if(mManager!=null){
                mManager.requestPeers(mChannel, mainActivity.peerListListener);
            }
        }else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            // TBD
        }else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
            // TBD
        }


    }
}
