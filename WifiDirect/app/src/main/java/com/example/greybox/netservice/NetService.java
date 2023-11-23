package com.example.greybox.netservice;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import com.example.greybox.MeshDevice;
import com.example.greybox.WfdNetManagerService;
import com.example.greybox.meshmessage.MeshMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public abstract class NetService {
    private static final String TAG = "NetService";

//    private MCWfdModule mWfdModule;
    // TODO: still not sure if we have to split the behavior of WfdNetManagerService in Router/Client
    protected WfdNetManagerService wfdModule;
    protected Handler externalHandler;
    protected WifiP2pManager.ConnectionInfoListener connectionInfoListener;
    protected WifiP2pManager.GroupInfoListener groupInfoListener;
    protected ClientListUiCallback clientListUiCallback;
    protected GroupInfoUiCallback groupInfoUiCallback;
    protected MessageTextUiCallback messageTextUiCallback;
    private MeshDevice device;  // The representation of the device this NetService is running on
    private HashMap<UUID, MeshDevice> deviceList = new HashMap<>();



    // --------------------------------------------------------------------------------------------
    //  Interfaces
    // --------------------------------------------------------------------------------------------
    public interface ClientListUiCallback {
        /// testing
//        void updateClientsUi(HashMap<String, MeshDevice> clients);
        void updateClientsUi(ArrayList<MeshDevice> clients);
        ///
    }


    public interface GroupInfoUiCallback {
        void updateGroupInfoUi(WifiP2pGroup wifiP2pGroup);
    }

    public interface MessageTextUiCallback {
        void updateMessageTextUiCallback(String msgText);
    }


    // --------------------------------------------------------------------------------------------
    //  Constructors
    // --------------------------------------------------------------------------------------------
    protected NetService(Context context, WfdNetManagerService wfdModule, Handler externalHandler) {
        this.externalHandler = externalHandler;
        this.wfdModule = wfdModule;
        this.device = new MeshDevice(UUID.randomUUID());
        // TODO: this method might not work and maybe we have to use the WifiP2pDevice info in the
        //  WifiDirectBroadcastReceiver class when processing WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
        this.device.setDeviceName(Settings.Secure.getString(context.getContentResolver(), "bluetooth_name"));
        Log.d(TAG, " deviceName: " + this.device.getDeviceName());
        this.deviceList = new HashMap<>();
    }

    // --------------------------------------------------------------------------------------------
    //  Getter/Setter methods
    // --------------------------------------------------------------------------------------------
    public WfdNetManagerService getWfdModule() {
        return wfdModule;
    }

    public Handler getExternalHandler() {
        return externalHandler;
    }

    public WifiP2pManager.ConnectionInfoListener getConnectionInfoListener() { return connectionInfoListener; };

    public WifiP2pManager.GroupInfoListener getGroupInfoListener() { return groupInfoListener; };

    public ClientListUiCallback getClientListUiCallback() { return clientListUiCallback; }

    public GroupInfoUiCallback getGroupInfoUiCallback() { return groupInfoUiCallback; }

    public MessageTextUiCallback getMessageTextUiCallback() { return messageTextUiCallback; }

    public MeshDevice getDevice() { return this.device; }


    public void setConnectionInfoListener(WifiP2pManager.ConnectionInfoListener listener) { this.connectionInfoListener = listener; };

    public void setGroupInfoListener(WifiP2pManager.GroupInfoListener listener) { this.groupInfoListener = listener; };

    public void setClientListUiUpdateCallback(ClientListUiCallback cb) { this.clientListUiCallback = cb; }

    public void setGroupInfoUiCallback(GroupInfoUiCallback cb) { this.groupInfoUiCallback = cb; }

    public void setMessageTextUiCallback(MessageTextUiCallback cb) { this.messageTextUiCallback = cb; }

    // --------------------------------------------------------------------------------------------
    //  Abstract methods
    // --------------------------------------------------------------------------------------------


    public abstract void start();

    public abstract void stop();

    public abstract void sendMessage(MeshMessage msg);

    public abstract void handleThreadMessage(Message msg);
}
