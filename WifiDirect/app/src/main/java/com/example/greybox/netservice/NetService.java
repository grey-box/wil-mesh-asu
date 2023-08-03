package com.example.greybox.netservice;

import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Message;

import com.example.greybox.MeshDevice;
import com.example.greybox.WfdNetManagerService;
import com.example.greybox.meshmessage.MeshMessage;

import java.util.ArrayList;

public abstract class NetService {

//    private MCWfdModule mWfdModule;
    // TODO: still not sure if we have to split the behavior of WfdNetManagerService in Router/Client
    protected final WfdNetManagerService wfdModule;
    protected final Handler externalHandler;
    protected WifiP2pManager.ConnectionInfoListener connectionInfoListener;
    protected WifiP2pManager.GroupInfoListener groupInfoListener;
    protected ClientListUiCallback clientListUiCallback;
    protected GroupInfoUiCallback groupInfoUiCallback;
    protected MessageTextUiCallback messageTextUiCallback;
    // TODO: determine if this attribute should be here
    // This attribute will be set when the socket connection is established
    protected String deviceMacAddress = "";


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
    protected NetService(WfdNetManagerService wfdModule, Handler externalHandler) {
        this.externalHandler = externalHandler;
        this.wfdModule = wfdModule;
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

    // TODO: do we really want to have this listeners here? We need them for the BroadcastReceiver
    //  but they could be part of the WfdModule
    public WifiP2pManager.ConnectionInfoListener getConnectionInfoListener() { return connectionInfoListener; };

    public WifiP2pManager.GroupInfoListener getGroupInfoListener() { return groupInfoListener; };

    public ClientListUiCallback getClientListUiCallback() { return clientListUiCallback; }

    public GroupInfoUiCallback getGroupInfoUiCallback() { return groupInfoUiCallback; }

    public MessageTextUiCallback getMessageTextUiCallback() { return messageTextUiCallback; }

    public String getDeviceMacAddress() { return this.deviceMacAddress; }


    public void setConnectionInfoListener(WifiP2pManager.ConnectionInfoListener listener) { this.connectionInfoListener = listener; };

    public void setGroupInfoListener(WifiP2pManager.GroupInfoListener listener) { this.groupInfoListener = listener; };

    public void setClientListUiUpdateCallback(ClientListUiCallback cb) { this.clientListUiCallback = cb; }

    public void setGroupInfoUiCallback(GroupInfoUiCallback cb) { this.groupInfoUiCallback = cb; }

    public void setMessageTextUiCallback(MessageTextUiCallback cb) { this.messageTextUiCallback = cb; }

    public void setDeviceMacAddress(String deviceMacAddress) { this.deviceMacAddress = deviceMacAddress; }

    // --------------------------------------------------------------------------------------------
    //  Abstract methods
    // --------------------------------------------------------------------------------------------
    public abstract void start();

    // TODO: maybe this name is not appropriate.
    public abstract void destroy();

    public abstract void sendMessage(MeshMessage msg);

    public abstract void handleThreadMessage(Message msg);
}
