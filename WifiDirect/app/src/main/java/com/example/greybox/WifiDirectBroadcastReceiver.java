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
    public void setNetService(NetService netService) { this.mNetService = netService; }

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
            if (mManager == null) {
                Log.e(TAG, "mManager is null");
                return;
            }

            WifiP2pGroup group = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);

            if (group == null) {
                Log.e(TAG, "Group is null");
                return;
            }{
                // Use the group object to retrieve group information
                Log.d(TAG, "Group found. isGO: " + group.isGroupOwner() +
                        ", ownerAddr: " + group.getOwner().deviceAddress +
                        ", ssid: " + group.getNetworkName() +
                        ", pass: " + group.getPassphrase());

                mManager.requestConnectionInfo(mChannel, mNetService.getConnectionInfoListener());
                mManager.requestGroupInfo(mChannel, mNetService.getGroupInfoListener());
            }

            /// PE_DBG_TMP
            Log.d(TAG, "*** WIFI_P2P_CONNECTION_CHANGED_ACTION end");
            ///
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            /// PE_DBG_TMP
            Log.d(TAG, "*** WIFI_P2P_THIS_DEVICE_CHANGED_ACTION start");
            ///
            // We use this intent to set the name of the own device
            // TODO: we try to use the bluetooth name in NetService. It's better to avoid an async
            //  call that can screw up the information, but not sure it will work
            //            mManager.requestDeviceInfo(mChannel, mNetService.get);
            /// PE_DBG_TMP
            Log.d(TAG, "*** WIFI_P2P_THIS_DEVICE_CHANGED_ACTION end");
            ///
        }

    }


}
