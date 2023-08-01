package com.example.greybox.netservice;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.greybox.MainActivity;
import com.example.greybox.WfdNetManagerService;
import com.example.greybox.WfdStatusInterpreter;
import com.example.greybox.meshmessage.MeshMessage;

import java.net.InetAddress;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MClientService extends NetService {
    private static final String TAG = "MClientService";

//    private MClientWfdModule mWfdModule; // NOTE: see the comment on wfdNetManagerService
    private MClientNetSockModule mNetSock;


    // --------------------------------------------------------------------------------------------
    //  Constructors
    // --------------------------------------------------------------------------------------------
    // TODO: Just for now, this class will be tightly coupled with MainActivity's View objects like
    //  read_msg_box
    public MClientService(WfdNetManagerService wfd, Handler handler) {
        super(wfd, handler);
        Log.d(TAG, "handler: " + handler);
    }

    // --------------------------------------------------------------------------------------------
    //  Methods
    // --------------------------------------------------------------------------------------------
    @Override
    public void start() {
        super.setConnectionInfoListener(this.connectionInfoListener);
        super.setGroupInfoListener(this.groupInfoListener);
        // NOTE: try to remove any existing group. Currently the first attempt to create a group
        //  fails because the framework is busy. Hopefully this solves the issue
        super.wfdModule.tearDown();
        super.wfdModule.discoverServices();
    }

    @Override
    public void destroy() {
        mNetSock.closeSocket();
    }

    @Override
    public void sendMessage(MeshMessage msg) {
        mNetSock.write(msg);
    }

    @Override
    public void handleThreadMessage(Message msg) {
        switch (msg.what) {
//            case ThreadMessageType
            default:
                break;
        }
    }


    // --------------------------------------------------------------------------------------------
    //  Callbacks / Listeners
    // --------------------------------------------------------------------------------------------
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        // If the connection info is available
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            Log.d(TAG, "connectionInfoListener.onConnectionInfoAvailable");

            // Get Host Ip Address
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;

            Log.d(TAG, "wifiP2pInfo: " + wifiP2pInfo);

            if (!wifiP2pInfo.groupFormed) {
                Log.d(TAG, "connectionInfoListener: group not formed.");
                return;
            }

            // TODO: According to https://developer.android.com/training/connect-devices-wirelessly/nsd#discover
            //  it's recommended to use a non-fixed port number. Request it to the system and store
            //  it in a variable and pass it around.
            final int PORT = 8888;
            ///

            // Once the connection info is ready, create the sockets depending on the role of the device
            // Check if we are the GO or a client
            // TODO: use return instead of indenting all the code: `if (clientClass != null) return`
            //  Or convert this into a singleton?
            if (mNetSock == null) {
                mNetSock = new MClientNetSockModule(groupOwnerAddress, externalHandler, PORT);
                Log.d(TAG, "Starting client thread");
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(mNetSock);
            }
        }
    };

    // NOTE: this implementation is the same for both RouterService and ClientService. It could be
    //  part of the NetService abstract class and replace it if necessary, or find another way to do it
    WifiP2pManager.GroupInfoListener groupInfoListener = new WifiP2pManager.GroupInfoListener(){
        @Override
        public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
            Log.d(TAG, "groupInfoListener.onGroupInfoAvailable");

            if (groupInfoUiCallback != null) {
                groupInfoUiCallback.updateGroupInfoUi(wifiP2pGroup);
            }

            /// PE_AUTO_CONNECT
            // TODO: it seems that at this point the device (an object) returned by `wifiP2pGroup.getOwner()` is
            //  not initialized
            Log.d(TAG, "wifiP2pGroup:\n" + wifiP2pGroup + "\n");
            Log.d(TAG, "isGO:          " + wifiP2pGroup.isGroupOwner());
            Log.d(TAG, "owner:         " + wifiP2pGroup.getOwner());
            Log.d(TAG, "owner.isGO:    " + wifiP2pGroup.getOwner().isGroupOwner());
            Log.d(TAG, "deviceName:    " + wifiP2pGroup.getOwner().deviceName);
            Log.d(TAG, "deviceAddress: " + wifiP2pGroup.getOwner().deviceAddress);
            WfdStatusInterpreter.logWifiP2pDeviceStatus(TAG, wifiP2pGroup.getOwner().status);
            Log.d(TAG, "networkName:   " + wifiP2pGroup.getNetworkName());
            Log.d(TAG, "passphrase:    " + wifiP2pGroup.getPassphrase());
            Log.d(TAG, "interface:     " + wifiP2pGroup.getInterface());

            Log.d(TAG, "Client list:\n--------");
            for (WifiP2pDevice d : wifiP2pGroup.getClientList()) {
                Log.d(TAG, d.toString());
//                WfdNetManagerService.getMacFromLocalIpAddress();
            }
            ///
        }
    };

    // TODO: this listener is useless since we don't get any valuable information because of Android's
    //  MAC anonymization. Remove it.
    WifiP2pManager.DeviceInfoListener deviceInfoListener = new WifiP2pManager.DeviceInfoListener() {
        @Override
        public void onDeviceInfoAvailable(@Nullable WifiP2pDevice wifiP2pDevice) {
            // NOTE: this callback is basically useless. WifiP2pDevice only gives us the name of the
            //  device, and whether is a group owner. But it won't give us the MAC address.
            Log.d(TAG, "deviceInfoListener.onDeviceInfoAvailable");
//            localAddress = wifiP2pDevice.deviceAddress;
            Log.d(TAG, "wifiP2pDevice:\n" + wifiP2pDevice);
            Log.d(TAG, "isGroupOwner(): " + wifiP2pDevice.isGroupOwner());
        }
    };

    // TODO: this listener is useless since we simply don't use it anymore with the auto-connect
    //  feature. Remove it.
    // Wifi P2P Manager peer list listener for collecting list of wifi peers
    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {

        // override method to find peers available
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            // TODO: check if every time this listener is called, it means that the peerList is different
            //  from the previous one. I don't think we need to check if lists are different.
            Log.i(TAG, "peerListListener.onPeersAvailable");
        }
    };
}
