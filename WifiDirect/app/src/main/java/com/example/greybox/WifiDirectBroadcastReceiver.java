package com.example.greybox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;

/*
    JSGARVEY 03/03/23 - US#206 Citations:
    https://developer.android.com/training/connect-devices-wirelessly/wifi-direct#create-group
    Sarthi Technology - https://www.youtube.com/playlist?list=PLFh8wpMiEi88SIJ-PnJjDxktry4lgBtN3
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver {
    //Wifi P2p Manager provides specif API for managing WIFI p2p connectivity
    private WifiP2pManager mManager;
    // A P2p channel that connects the app to the WIFI p2p framework
    private WifiP2pManager.Channel mChannel;
    // Main activity of the app
    private MainActivity mActivity;

    //Constructor taking wifi p2p manager, the channel for the receiver to monitor, and the main activity
    public WifiDirectBroadcastReceiver(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, MainActivity mActivity){

        this.mManager = mManager;
        this.mChannel = mChannel;
        this.mActivity = mActivity;
    }

    // onReceive receives the app context and the intent and takes necessary action depending on intent
    @Override
    public void onReceive(Context context, Intent intent) {

        // Get the type of intent of action received by broadcast receiver
        String action = intent.getAction();
        // check to see if Wifi state has changed meaning if the wifi has been turned off or not
        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
            // returns state value as int or -1 if no value found
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            // if wifi p2p state is enabled
            if(state==WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                //Pop-up (toast) informing wifi is on
                Toast.makeText(context, "WIFI IS  ON",Toast.LENGTH_SHORT).show();
            }else{
                //Pop-up (toast) informing wifi is off
                Toast.makeText(context, "WIFI IS  OFF",Toast.LENGTH_SHORT).show();
            }
        // else if the peers list in wifi p2p manager has changed
        }
        else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            //if wifi p2p manager is not null meaning there are peers to list
            if(mManager!=null){
                // gets a list of current peers in p2p manager
                mManager.requestPeers(mChannel, mActivity.peerListListener);
            }
        // respond to new connections or disconnections (connection changed intent)
        }
        else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            // If no manager for the connection exists then return
            if(mManager==null)
            {
                return;
            }
            /* !!!DEPRECATED!!!
                Object for storing addition network information
             */
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            /* !!!DEPRECATED!!!
                https://developer.android.com/reference/android/net/NetworkInfo
                Checks if network connectivity exists and connection can be established
             */
            if(networkInfo.isConnected()){
                mManager.requestConnectionInfo(mChannel,mActivity.connectionInfoListener);
            }else{
                mActivity.connectionStatus.setText("DEVICE DISCONNECTED");
            }
        // respond to this device's wifi state changing
        }
        else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
            // TBD
        }
    }
}
