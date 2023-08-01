package com.example.greybox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.example.greybox.netservice.NetService;

/*
    JSGARVEY 03/03/23 - US#206 Citations:
    https://developer.android.com/training/connect-devices-wirelessly/wifi-direct#create-group
    Sarthi Technology - https://www.youtube.com/playlist?list=PLFh8wpMiEi88SIJ-PnJjDxktry4lgBtN3
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "WifiDirectBroadcastRcvr";

    //Wifi P2p Manager provides specif API for managing WIFI p2p connectivity
    private WifiP2pManager mManager;
    // A P2p channel that connects the app to the WIFI p2p framework
    private WifiP2pManager.Channel mChannel;
    private NetService mNetService;


    // --------------------------------------------------------------------------------------------
    //  Constructors
    // --------------------------------------------------------------------------------------------
    public WifiDirectBroadcastReceiver(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, NetService netService){
        this.mManager = mManager; this.mChannel = mChannel; this.mNetService = netService;
    }


    // --------------------------------------------------------------------------------------------
    //  Methods
    // --------------------------------------------------------------------------------------------
    // NOTE: We don't require the actions:
    //  - WIFI_P2P_PEERS_CHANGED_ACTION. We now use NSD to connect to group owners. No need to request
    //    the list of peers.
    //  - WIFI_P2P_THIS_DEVICE_CHANGED_ACTION. The callback associated with this action doesn't provide
    //    valuable information. MAC addresses are anonymized by Android at that point.
    // https://developer.android.com/guide/topics/connectivity/wifip2p
    // onReceive receives the app context and the intent and takes necessary action depending on intent
    @Override
    public void onReceive(Context context, Intent intent) {

        // Get the type of intent of action received by broadcast receiver
        String action = intent.getAction();

        // NOTE: Broadcast intent action indicating that the state of Wi-Fi p2p connectivity has changed.
        // respond to new connections or disconnections (connection changed intent)
        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            /// PE_DBG_TMP
            Log.d(TAG, "*** WIFI_P2P_CONNECTION_CHANGED_ACTION start");
            ///

            // If no manager for the connection exists then return
            if (mManager == null) { return; }

            WifiP2pGroup group = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);

            if (group != null) {
                // Use the group object to retrieve group information
                Log.d(TAG, "Group found.");
                Log.d(TAG, "isGO:      " + group.isGroupOwner());
                Log.d(TAG, "ownerAddr: " + group.getOwner().deviceAddress); // fake info
                Log.d(TAG, "ssid:      " + group.getNetworkName());
                Log.d(TAG, "pass:      " + group.getPassphrase());

                // NOTE: `connectionInfoListener` performs the socket connection
                mManager.requestConnectionInfo(mChannel, mNetService.getConnectionInfoListener());

                // NOTE: `getGroupInfoListener` performs the update of clients
                mManager.requestGroupInfo(mChannel, mNetService.getGroupInfoListener());
            }

            /// PE_DBG_TMP
            Log.d(TAG, "*** WIFI_P2P_CONNECTION_CHANGED_ACTION end");
            ///
        }
    }


}
