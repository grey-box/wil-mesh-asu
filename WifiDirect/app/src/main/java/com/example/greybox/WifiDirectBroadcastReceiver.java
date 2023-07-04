package com.example.greybox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pGroup;
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
        this.mManager = mManager; this.mChannel = mChannel; this.mActivity = mActivity;
    }

    // onReceive receives the app context and the intent and takes necessary action depending on intent
    @Override
    public void onReceive(Context context, Intent intent) {

        // Get the type of intent of action received by broadcast receiver
        String action = intent.getAction();

        // NOTE: Broadcast intent action indicating that the available peer list has changed.
        if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            //if wifi p2p manager is not null meaning there are peers to list
            if(mManager!=null){
                // gets a list of current peers in p2p manager
                mManager.requestPeers(mChannel, mActivity.peerListListener);
            }
            // TODO: PE_CMT: https://developer.android.com/guide/topics/connectivity/wifip2p
            //  Move this comment as part of the if below.
        // respond to new connections or disconnections (connection changed intent)
        }
        // NOTE: Broadcast intent action indicating that the state of Wi-Fi p2p connectivity has changed.
        else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            // If no manager for the connection exists then return
            if(mManager==null) { return; }

            mManager.requestConnectionInfo(mChannel, mActivity.connectionInfoListener);
            /* !!!DEPRECATED!!!
                Object for storing addition network information
             */
            // TODO: PE_CMT: comment out this line since it's not used and it's deprecated.
            //  Actually, it would be better to remove commented code.
            //  These lines come directly from the video, do we need them or is it just to get some
            //  info about the connection? If we do, we need to find the new proper way to do it. So
            //  far it seems is useless info about a device being connected.
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            /* !!!DEPRECATED!!!
                https://developer.android.com/reference/android/net/NetworkInfo
                Checks if network connectivity exists and connection can be established
             */
//            if(networkInfo.isConnected()){
//                mManager.requestConnectionInfo(mChannel,mActivity.connectionInfoListener);
//            }else{
//                mActivity.connectionStatus.setText("DEVICE DISCONNECTED");
//            }

            WifiP2pGroup group = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
            if (group != null) {
                // Use the group object to retrieve group information
                String ownerAddress = group.getOwner().deviceAddress;
                String ssid = group.getNetworkName();
                String passphrase = group.getPassphrase();
                // Do something with the retrieved group information
                mManager.requestGroupInfo(mChannel, mActivity.groupInfoListener);
            }
        // TODO: PE_CMT: https://developer.android.com/guide/topics/connectivity/wifip2p
        //  Move this comment as part of the if below.
        // respond to this device's wifi state changing
        }
        // TODO: PE_CMT: https://developer.android.com/guide/topics/connectivity/wifip2p
        //  Broadcast when a device's details have changed, such as the device's name.
        else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
            mManager.requestDeviceInfo(mChannel, mActivity.deviceInfoListener);
        }
    }


}
