package com.example.greybox.netservice;

import static com.example.greybox.meshmessage.MeshMessageType.NEW_CLIENT_SOCKET_CONNECTION;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import androidx.annotation.Nullable;

import com.example.greybox.MeshDevice;
import com.example.greybox.ThreadMessageTypes;
import com.example.greybox.WfdNetManagerService;
import com.example.greybox.WfdStatusInterpreter;
import com.example.greybox.meshmessage.MeshMessage;
import com.example.greybox.meshmessage.MeshMessageType;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MClientService extends NetService {
    private static final String TAG = "MClientService";

//    private MClientWfdModule mWfdModule; // NOTE: see the comment on wfdNetManagerService
    private MClientNetSockModule mNetSock;


    // --------------------------------------------------------------------------------------------
    //  Constructors
    // --------------------------------------------------------------------------------------------
    public MClientService(Context context, WfdNetManagerService wfd, Handler handler) {
        super(context, wfd, handler);
    }

    // --------------------------------------------------------------------------------------------
    //  Methods
    // --------------------------------------------------------------------------------------------
    @Override
    public void start() {
        super.setConnectionInfoListener(this.connectionInfoListener);
        super.setGroupInfoListener(this.groupInfoListener);
        // NOTE: try to remove any existing group. This prevents a problem related to busy framework
        super.wfdModule.tearDown();
        super.wfdModule.discoverServices();
    }

    @Override
    public void stop() {
        if (mNetSock == null) return;
        mNetSock.closeSocket();
    }

    @Override
    public void sendMessage(MeshMessage msg) {
        mNetSock.write(msg);
    }

    @Override
    public void handleThreadMessage(Message msg) {
        switch (msg.what) {
            case ThreadMessageTypes.MESSAGE_READ:
                // This message requests display in the UI the data received from another device
                // The object received is a MeshMessage object
                MeshMessage meshMsg = (MeshMessage) msg.obj;

                switch (meshMsg.getMsgType()) {
                    // TODO: for this case we could use the template method design pattern since it's
                    //  almost identical for the Client and the GO
                    case DATA_SINGLE_CLIENT:
                        // TODO: for now we assume only strings are sent as the payload
                        Log.d(TAG, "DATA_SINGLE_CLIENT");
                        Log.d(TAG, " dstDevices: \n" + meshMsg.getDstDevices());

                        // We currently support only one recipient
                        UUID recipientId = meshMsg.getDstDevices().get(0);
                        Log.d(TAG, " recipientId: " + recipientId);
                        if (recipientId == null) {
                            return;
                        }

                        Log.d(TAG, " deviceId:    " + getDevice().getDeviceId());
                        if (recipientId.equals(getDevice().getDeviceId())) {
                            // The message is for this device
                            getMessageTextUiCallback().updateMessageTextUiCallback((String) meshMsg.getData());
                        }
                        break;
                    case CLIENT_LIST:
                        Log.d(TAG, "CLIENT_LIST");
                        // NOTE: this case is used mostly by the Client devices. Routers update
                        //  their list differently
                        Log.d(TAG, " Updating the client list UI");
//                        HashMap<String, MeshDevice> groupClients = (HashMap<String, MeshDevice>) (meshMsg.getData());
                        ArrayList<MeshDevice> groupClients = (ArrayList<MeshDevice>) (meshMsg.getData());
                        Log.d(TAG, " Received clients: " + groupClients);
                        getClientListUiCallback().updateClientsUi(groupClients);
                        break;
                }
                break;
            case ThreadMessageTypes.CLIENT_SOCKET_CONNECTION:
                Log.d(TAG, "ThreadMessageTypes.CLIENT_SOCKET_CONNECTION");
                // This device has established a client socket connection. This is the moment to
                // to send its info to the GO
                sendMessage(new MeshMessage(NEW_CLIENT_SOCKET_CONNECTION, getDevice(), null));
                break;
            default:
                break;
        }
    }

    // --------------------------------------------------------------------------------------------
    //  Callbacks / Listeners
    // --------------------------------------------------------------------------------------------
    // Callback interface pour les informations de connexion
    public interface ConnectionInfoReceivedListener {
        void onConnectionInfoReceived(String deviceAddress, int port);
    }

    private ConnectionInfoReceivedListener connectionInfoReceivedListener;

    public void setConnectionInfoReceivedListener(ConnectionInfoReceivedListener listener) {
        this.connectionInfoReceivedListener = listener;
    }

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

            final int PORT = 8888; // TODO: remplacer par la logique de récupération du port dynamiquement

            if (mNetSock == null) {
                mNetSock = new MClientNetSockModule(groupOwnerAddress, externalHandler, PORT, MClientService.this);
                Log.d(TAG, "Starting client thread");
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(mNetSock);

                // Informez le listener que les informations de connexion sont disponibles
                onConnectionInfoReceived(groupOwnerAddress.getHostAddress(), PORT);
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

            // TODO: temp. This is just for debugging.
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
            }
        }
    };

    public void onConnectionInfoReceived(String deviceAddress, int port) {
        if(connectionInfoReceivedListener != null) {
            connectionInfoReceivedListener.onConnectionInfoReceived(deviceAddress, port);
        }
    }

    // TODO: this listener could be used to obtain the device name. Still evaluating if using the
    //  current method (bluetooth name obtained in the NetService constructor) is better since we
    //  avoid async calls that might end up getting an invalid name due to the order of execution
    WifiP2pManager.DeviceInfoListener deviceInfoListener = new WifiP2pManager.DeviceInfoListener() {
        @Override
        public void onDeviceInfoAvailable(@Nullable WifiP2pDevice wifiP2pDevice) {
            // NOTE: this callback is basically useless. WifiP2pDevice only gives us the name of the
            //  device, and whether is a group owner. But it won't give us the MAC address.
            Log.d(TAG, "deviceInfoListener.onDeviceInfoAvailable");
            Log.d(TAG, " wifiP2pDevice:\n" + wifiP2pDevice);
        }
    };
}
