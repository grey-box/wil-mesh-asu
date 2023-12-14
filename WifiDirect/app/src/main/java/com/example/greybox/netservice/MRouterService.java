package com.example.greybox.netservice;

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
import com.example.greybox.meshmessage.FilePayload;
import com.example.greybox.meshmessage.MeshMessage;
import com.example.greybox.meshmessage.MeshMessageType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MRouterService extends NetService {
    private static final String TAG = "MRouterService";

//    private MRouterWfdModule mWfdModule; // NOTE: see the comment on wfdNetManagerService
    private MRouterNetSockModule mNetSock;

    private ArrayList<MeshDevice> groupClients = new ArrayList<>();
    private Context context;


    // --------------------------------------------------------------------------------------------
    //  Constructors
    // --------------------------------------------------------------------------------------------
    public MRouterService(Context context, WfdNetManagerService wfd, Handler handler) {
        super(context, wfd, handler);
        this.context = context;
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
        super.wfdModule.createSoftAP();

    }

    @Override
    public void stop() {
        if (mNetSock == null) return;
        mNetSock.closeServerSocket();

    }


    @Override
    public void sendMessage(MeshMessage msg) {
        if (msg.getMsgType() == MeshMessageType.FILE_TRANSFER) {
            sendFile(msg);
        } else {
            mNetSock.write(msg);
        }
    }
    @Override
    public void sendFile(MeshMessage fileMessage) {   mNetSock.writeFile(fileMessage);  }


    private void addClient(MeshDevice newClient) {
        Log.d(TAG, "addClient");
        Log.d(TAG, " newClient: " + newClient);
        groupClients.add(newClient);
    }

    private void removeClient(MeshDevice client) {
        Log.d(TAG, "removeClient");
//        groupClients.remove(client);
        for (int i = 0; i < groupClients.size(); ++i) {
            if (groupClients.get(i)
                    .getDeviceId().equals(client.getDeviceId())) {
                groupClients.remove(i);
                return;
            }
        }
    }

    private void notifyClients() {
        Log.d(TAG, "notifyClients");
        Log.d(TAG, " Clients that would be sent:\n" + groupClients);
        MeshMessage msg = new MeshMessage(MeshMessageType.CLIENT_LIST, groupClients, null);
        mNetSock.write(msg);
    }

    private void updateClientListUi() {
        Log.d(TAG, "Updating UI.");
        getClientListUiCallback().updateClientsUi(groupClients);
    }

    @Override
    public void handleThreadMessage(Message threadMsg) {
        Log.d(TAG, "Message processed by the RouterService");

        switch (threadMsg.what) {
            case ThreadMessageTypes.MESSAGE_READ:
                Log.d(TAG, " ThreadMessageTypes.MESSAGE_READ");
                if (threadMsg.obj == null) {
                    Log.d(TAG, " threadMsg.obj is null");
                    return;
                }
                // This message requests display in the UI the data received from another device
                // The object received is a MeshMessage object
                MeshMessage meshMsg = (MeshMessage) threadMsg.obj;

                switch (meshMsg.getMsgType()) {
                    case NEW_CLIENT_SOCKET_CONNECTION:
                        // A client socket connection received. Update the list of clients and then
                        // send it to the other clients
                        Log.d(TAG, "CLIENT_SOCKET_CONNECTION");

                        addClient((MeshDevice) meshMsg.getData());
                        notifyClients();
                        updateClientListUi();
                        break;
                    // TODO: for this case we could use the template method design pattern since it's
                    //  almost identical for the Client and the GO
                    case DATA_SINGLE_CLIENT:
                        // TODO: for now we assume only strings are sent as the payload
                        Log.d(TAG, "DATA_SINGLE_CLIENT");
                        Log.d(TAG, "dstDevices: \n" + meshMsg.getDstDevices());

                        // We currently support only one recipient
                        UUID recipientId = meshMsg.getDstDevices().get(0);
                        Log.d(TAG, "  recipientId: " + recipientId);

                        if (recipientId == null) {
                            return;
                        }

                        Log.d(TAG, "  deviceId:    " + getDevice().getDeviceId());
                        if (recipientId.equals(getDevice().getDeviceId())) {
                            // The message is for this device
                            getMessageTextUiCallback().updateMessageTextUiCallback((String) meshMsg.getData());
                        } else {
                            // The GO/Router must broadcast the message to let the clients decide if
                            // the message is for them
                            sendMessage(meshMsg);
                        }
                        break;
                    case FILE_TRANSFER:
                        Log.d(TAG, "FILE_TRANSFER");
                        Log.d(TAG, "dstDevices: \n" + meshMsg.getDstDevices());

                        // We currently support only one recipient
                        UUID frecipientId = meshMsg.getDstDevices().get(0);
                        Log.d(TAG, "frecipientId: " + frecipientId);

                        if (frecipientId == null) {
                            return;
                        }

                        Log.d(TAG, "deviceId: " + getDevice().getDeviceId());
                        if (frecipientId.equals(getDevice().getDeviceId())) {
                            FilePayload filePayload = (FilePayload) meshMsg.getData();
                            if (filePayload != null) {
                                byte[] fileData = filePayload.getFileData();
                                String fileName = filePayload.getFileName();
                                if (getFileReceivedUiCallback() != null) {
                                    getFileReceivedUiCallback().updateFileReceivedUi(fileData, fileName);
                                }
                            } else {
                                Log.e(TAG, "Le payload du message n'est pas de type FilePayload.");
                            }
                        }
                        break;


                    case CLIENT_LIST:
                        // The GO updates the list of devices in NEW_CLIENT_SOCKET_CONNECTION.
                        // TODO: maybe this will change if we receive the list of another group
                        break;
                    default:
                        break;
                }
                break;

            case ThreadMessageTypes.CLIENT_SOCKET_CONNECTION:
                // GO doesn't do anything for this type of message. The data will be received as a
                //  MeshMessage and processed in NEW_CLIENT_SOCKET_CONNECTION
            default:
                break;
        }
    }




    // --------------------------------------------------------------------------------------------
    //  Callbacks / Listeners
    // --------------------------------------------------------------------------------------------
    // NOTE: Listener used by the BroadcastReceiver when WIFI_P2P_CONNECTION_CHANGED_ACTION
    //  interface for callback invocation when connection info is available
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        // If the connection info is available
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            Log.d(TAG, "connectionInfoListener.onConnectionInfoAvailable");

            // Get Host Ip Address
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;

            Log.i(TAG, "wifiP2pInfo: " + wifiP2pInfo);

            if (!wifiP2pInfo.groupFormed) {
                Log.i(TAG, "connectionInfoListener: group not formed.");
                return;
            }

            // TODO: According to https://developer.android.com/training/connect-devices-wirelessly/nsd#register
            //  it's recommended to use a non-fixed port number. Request it to the system and store
            //  it in a variable and pass it around. Also, the port number should be sent in the Bonjour
            //  information when broadcasting the service. Therefore, the most reasonable part to do
            //  this should be in WfdNetManagerService.createSoftAP().
            //  Use `serverSocket = new ServerSocket(0);` and then `localPort = serverSocket.getLocalPort();`
            final int PORT = 8888;

            // Once the connection info is ready, create the sockets depending on the role of the device
            if (mNetSock == null) {
                mNetSock = new MRouterNetSockModule(externalHandler, PORT);
            }

            // One thread to wait for each possible client
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Log.i(TAG, "Starting server thread");
            executorService.execute(mNetSock);
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
            Log.d(TAG, "isGO:             " + wifiP2pGroup.isGroupOwner());
            Log.d(TAG, "owner:            " + wifiP2pGroup.getOwner());
            Log.d(TAG, "owner.isGO:       " + wifiP2pGroup.getOwner().isGroupOwner());
            Log.d(TAG, "go.deviceName:    " + wifiP2pGroup.getOwner().deviceName);
            Log.d(TAG, "go.deviceAddress: " + wifiP2pGroup.getOwner().deviceAddress);
            WfdStatusInterpreter.logWifiP2pDeviceStatus(TAG, wifiP2pGroup.getOwner().status);
            Log.d(TAG, "networkName:      " + wifiP2pGroup.getNetworkName());
            Log.d(TAG, "passphrase:       " + wifiP2pGroup.getPassphrase());
            Log.d(TAG, "interface:        " + wifiP2pGroup.getInterface());

            Log.d(TAG, "Client list:\n--------");
            for (WifiP2pDevice d : wifiP2pGroup.getClientList()) {
                Log.d(TAG, d.toString());
            }
        }
    };

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
