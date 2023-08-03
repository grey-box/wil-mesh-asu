package com.example.greybox.netservice;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MRouterService extends NetService {
    private static final String TAG = "MRouterService";

//    private MRouterWfdModule mWfdModule; // NOTE: see the comment on wfdNetManagerService
    private MRouterNetSockModule mNetSock;

    /// PE_MSG_SPECIFIC_CLIENT
    /// testing
//    HashMap<String, MeshDevice> groupClients = new HashMap<>();
    ArrayList<MeshDevice> groupClients = new ArrayList<>();
    ////
    ///

    // --------------------------------------------------------------------------------------------
    //  Constructors
    // --------------------------------------------------------------------------------------------
    public MRouterService(WfdNetManagerService wfd, Handler handler) {
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
        super.wfdModule.createSoftAP();
    }

    // TODO: maybe this name is not appropriate
    @Override
    public void destroy() {
        mNetSock.closeServerSocket();
    }

    @Override
    public void sendMessage(MeshMessage msg) {
        mNetSock.write(msg);
    }


    /// PE_MSG_SPECIFIC_CLIENT
    private HashMap<String, MeshDevice> buildMeshDeviceClientList(Collection<WifiP2pDevice> list) {
        HashMap<String, MeshDevice> clients = new HashMap<>(list.size());
        for (WifiP2pDevice d : list) {
            clients.put(d.deviceName, new MeshDevice(d.deviceAddress, d.deviceName));
        }
        return clients;
    }

    /// testing
    private void updateClientsList(Collection<WifiP2pDevice> list) {
        groupClients.clear();
        for (WifiP2pDevice d : list) {
            groupClients.add(new MeshDevice(d.deviceAddress, d.deviceName));
        }
    }

    //    public void updateClientsList(Collection<WifiP2pDevice> newList) {
//        groupClients.clear();
//        groupClients.putAll(buildMeshDeviceClientList(newList));
//        Log.d(TAG, "New client list: " + groupClients);
//    }
    ///

    private void notifyClients() {
        Log.d(TAG, " Clients that would be sent:\n" + groupClients);
        MeshMessage msg = new MeshMessage(MeshMessageType.CLIENT_LIST, groupClients, null);
        mNetSock.write(msg);
    }

    private void updateClientListUi() {
        Log.d(TAG, "Updating UI.");
        getClientListUiCallback().updateClientsUi(groupClients);
    }

    @Override
    public void handleThreadMessage(Message msg) {
        Log.d(TAG, "Message processed by the RouterService");
        switch (msg.what) {
            case ThreadMessageTypes.MESSAGE_READ:
                // This message requests display in the UI the data received from another
                // device
                // The object received is a MeshMessage object
                MeshMessage meshMsg = (MeshMessage) msg.obj;

                switch (meshMsg.getMsgType()) {
                    // TODO: for this case we could use the template method design pattern since it's
                    //  almost identical for the Client and the GO
                    case DATA_SINGLE_CLIENT:
                        // TODO: for now we assume only strings are sent as the payload
                        Log.d(TAG, "DATA_SINGLE_CLIENT");
                        Log.d(TAG, "dstDevices: \n" + meshMsg.getDstDevices());

                        // We currently support only one recipient
                        String recipient = meshMsg.getDstDevices().get(0);
                        Log.d(TAG, "recipient: \n" + recipient);

                        if (recipient.isEmpty() || deviceMacAddress.isEmpty()) {
                            return;
                        }

                        Log.d(TAG, "recipient[3:]:    " + recipient.substring(3));
                        Log.d(TAG, "myMacAddress[3:]: " + deviceMacAddress.substring(3));
                        // From the article: the first two characters of a MAC address may change
                        // for the same device and same network interface, and should  be ignored.
                        // TODO: for now, the GO won't display the messages since its macAddress is empty
                        if (recipient.substring(3)
                                .equals(deviceMacAddress.substring(3))) {
                            // The message is for this device
                            getMessageTextUiCallback().updateMessageTextUiCallback((String) meshMsg.getData());
                        } else {
                            // The GO/Router must broadcast the message to let the clients decide if
                            // the message is for them
                            sendMessage(meshMsg);
                        }
                        break;
                    case CLIENT_LIST:
                        // The GO updates the list of devices in ThreadMessageTypes.CLIENT_SOCKET_CONNECTION.
                        // TODO: maybe this will change if we receive the list of another group
                        break;
                    default:
                        break;
                }
                break;

            case ThreadMessageTypes.CLIENT_SOCKET_CONNECTION:
                // A client socket connection received. This is the moment to call the update of
                // the client list
                Log.d(TAG, "Received CLIENT_DEVICE");
                /// PE_MSG_SPECIFIC_CLIENT
                notifyClients();
                updateClientListUi();
                ///
                break;
            default:
                break;
        }
    }
    ///


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

            /// PE_AUTO_CONNECT

            // TODO: According to https://developer.android.com/training/connect-devices-wirelessly/nsd#discover
            //  it's recommended to use a non-fixed port number. Request it to the system and store
            //  it in a variable and pass it around. Also, the port number should be sent in the Bonjour
            //  information when broadcasting the service.
//            serverSocket = new ServerSocket(0);
//            // Store the chosen port.
//            localPort = serverSocket.getLocalPort();
            final int PORT = 8888;
            ///

            // Once the connection info is ready, create the sockets depending on the role of the device
            // Check if we are the GO or a client

            // TODO: the ServerSocket needs to be created at the moment we create the softAP
            //  since it's recommended to have a dynamic port instead of a fixed one.
            // TODO: convert this into a singleton?
            if (mNetSock == null) {
                // Create a ServerSocket
                mNetSock = new MRouterNetSockModule(externalHandler, PORT);

                // This is the time to set the device MAC address
                try {
                    Log.d(TAG, " Setting own MAC address based on local IP address");
                    setDeviceMacAddress(WfdNetManagerService.
                            getMacFromLocalIpAddress(groupOwnerAddress));
                    Log.d(TAG, "getMacAddress(): " + getDeviceMacAddress());
                } catch (SocketException | UnknownHostException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Error while trying to obtain the device MAC address");
                }
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

            /// PE_AUTO_CONNECT
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
//                WfdNetManagerService.getMacFromLocalIpAddress();
            }
            ///

            /// PE_MSG_SPECIFIC_CLIENT
            // TODO: the problem with calling here the update is that the socket connection might
            //  have failed. We must do something to avoid adding an element that doesn't have a socket
            updateClientsList(wifiP2pGroup.getClientList());
            ///
        }
    };
}
