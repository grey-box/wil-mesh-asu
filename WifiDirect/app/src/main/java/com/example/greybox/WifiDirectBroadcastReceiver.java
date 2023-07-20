package com.example.greybox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

/*
    JSGARVEY 03/03/23 - US#206 Citations:
    https://developer.android.com/training/connect-devices-wirelessly/wifi-direct#create-group
    Sarthi Technology - https://www.youtube.com/playlist?list=PLFh8wpMiEi88SIJ-PnJjDxktry4lgBtN3
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "WifiDirectBroadcastRece";

    //Wifi P2p Manager provides specif API for managing WIFI p2p connectivity
    private WifiP2pManager mManager;
    // A P2p channel that connects the app to the WIFI p2p framework
    private WifiP2pManager.Channel mChannel;
    // Main activity of the app
    private MainActivity mActivity;

    //Constructor taking wifi p2p manager, the channel for the receiver to monitor, and the main activity
    // TODO: I just realized that this class and MainActivity are tightly coupled because we are passing
    //  specifically a MainActivity object. Consider refactoring this, but for now is not really important
    public WifiDirectBroadcastReceiver(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, MainActivity mActivity){
        this.mManager = mManager; this.mChannel = mChannel; this.mActivity = mActivity;
    }

    // https://developer.android.com/guide/topics/connectivity/wifip2p
    // onReceive receives the app context and the intent and takes necessary action depending on intent
    @Override
    public void onReceive(Context context, Intent intent) {

        // Get the type of intent of action received by broadcast receiver
        String action = intent.getAction();

        // NOTE: Broadcast intent action indicating that the available peer list has changed.
        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            /// PE_DBG_TMP
//            Log.d(TAG, "*** WIFI_P2P_PEERS_CHANGED_ACTION start");
            ///

            //if wifi p2p manager is not null meaning there are peers to list
            if (mManager != null) {
                // gets a list of current peers in p2p manager
                mManager.requestPeers(mChannel, mActivity.peerListListener);
            }
            Log.d(TAG, "Peer list changed.");
            // TODO: PE_CMT: https://developer.android.com/guide/topics/connectivity/wifip2p
            //  Move this comment as part of the if below.
        // respond to new connections or disconnections (connection changed intent)

            /// PE_DBG_TMP
//            Log.d(TAG, "*** WIFI_P2P_PEERS_CHANGED_ACTION end");
            ///
        }
        // NOTE: Broadcast intent action indicating that the state of Wi-Fi p2p connectivity has changed.
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            /// PE_DBG_TMP
            Log.d(TAG, "*** WIFI_P2P_CONNECTION_CHANGED_ACTION start");
            ///

            // If no manager for the connection exists then return
            if (mManager == null) { return; }

            // TODO: I think we don't need this method call in the auto-connect feature since we built
            //  the group, so, we already have the information of the connection
//            mManager.requestConnectionInfo(mChannel, mActivity.connectionInfoListener);

            WifiP2pGroup group = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);

            if (group != null) {
                // Use the group object to retrieve group information
                /// PE_AUTO_CONNECT
                Log.d(TAG, "Group found.");
                Log.d(TAG, "isGO:      " + group.isGroupOwner());
                Log.d(TAG, "ownerAddr: " + group.getOwner().deviceAddress); // fake info
                Log.d(TAG, "ssid:      " + group.getNetworkName());
                Log.d(TAG, "pass:      " + group.getPassphrase());

                // Step 2
                // TODO: these two methods calls (makeNSDBroadcast() and discoverServices()) should
                //  be in a separate thread?
                // TODO: I don't know if this is a horrible dependency with the MainActivity.
                //  Maybe it's OK.
                // If this device is a GO, broadcast its service as GO
                if (group.isGroupOwner()) {
                    // TODO: encrypt everything that will be broadcast for NSD (ssid, passprhase, mac)
                    mActivity.wfdNetManagerService.makeNSDBroadcast(group.getNetworkName(),
                            group.getPassphrase());
                    // tmp
                    mActivity.wfdNetManagerService._isGO = true;
                    //
                }

                // TODO: `connectionInfoListener` performs the socket connection. So either move socket
                //  connection somewhere else or leave it as is. I already added some `if`s to execute
                //  the creation of the threads only once
                mManager.requestConnectionInfo(mChannel, mActivity.connectionInfoListener);
                ///

                // TODO: I think we don't need this method call in the auto-connect feature since we built
                //  the group, so, we already have the information of the connection
                // Do something with the retrieved group information
//                mManager.requestGroupInfo(mChannel, mActivity.groupInfoListener);
            }

            /// PE_DBG_TMP
            Log.d(TAG, "*** WIFI_P2P_CONNECTION_CHANGED_ACTION end");
            ///
        }
        //  Broadcast when a device's details have changed, such as the device's name.
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            /// PE_DBG_TMP
            Log.d(TAG, "*** WIFI_P2P_THIS_DEVICE_CHANGED_ACTION start");
            ///
            // respond to this device's wifi state changing
            mManager.requestDeviceInfo(mChannel, mActivity.deviceInfoListener);
            /// PE_DBG_TMP
            Log.d(TAG, "*** WIFI_P2P_THIS_DEVICE_CHANGED_ACTION end");
            ///
        }
    }


}
