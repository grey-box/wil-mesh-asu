package com.example.greybox;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.example.greybox.meshmessage.MeshMessage;
import com.example.greybox.meshmessage.MeshMessageType;
import com.example.greybox.netservice.MClientService;
import com.example.greybox.netservice.MRouterService;
import com.example.greybox.netservice.NetService;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/*
    JSGARVEY 03/03/23 - US#206 Citations:
    https://developer.android.com/training/connect-devices-wirelessly/wifi-direct#create-group
    Sarthi Technology - https://www.youtube.com/playlist?list=PLFh8wpMiEi88SIJ-PnJjDxktry4lgBtN3
 */
public class MainActivity extends FragmentActivity {
    private static final String TAG = "MainActivity";

    Button btnDiscover, btnSend, btnGroupInfo;
    ListView listView;
    TextView readMsgBox;
    TextView connectionStatus;
    EditText writeMsg;
    SwitchMaterial connectButton;
    MaterialButtonToggleGroup deviceModeToggleGroup;

    //Wifi P2p Manager provides specif API for managing WIFI p2p connectivity
    WifiP2pManager mManager;
    // A P2p channel that connects the app to the WIFI p2p framework
    WifiP2pManager.Channel mChannel;
    // After connection group stored with all devices and group owner info
    WifiP2pGroup mGroup;
    WifiP2pInfo mWifiP2pInfo;

    // receives and handles broadcast intents sent by the context
    WifiDirectBroadcastReceiver mReceiver;
    // An Intent is a description of an operation to be performed.
    // A filter matches intents and describes the Intent values it matches.
    // Filters by characteristics of intents Actions, Data, and Categories
    IntentFilter mIntentFilter;

    private NetService mNetService;

    WfdNetManagerService wfdNetManagerService;
    Handler uiHandler;

    private MeshDevice msgDstDevice;        // Destination device of the message
    private ArrayList<MeshDevice> groupClientsList = new ArrayList<>();  // List of the connected devices in the group. Used to get the MAC address
    private String[] groupClientsNames;     // List of devices names to be displayed on the UI
    private boolean isAP = false;


    //imported override method onCreate. Initialize the the activity.
    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // call a the layout resource defining the UI
        setContentView(R.layout.activity_main);

        //pop up notifying if device supports wifi p2p
        if(getPackageManager().hasSystemFeature("android.hardware.wifi.direct")){
            Toast.makeText(getApplicationContext(), "WIFI DIRECT SUPPORTED", Toast.LENGTH_SHORT).show();
        }
        // creating objects
        initialization();
        // adding listeners to the objects
        setListeners();
    }

    // initial work for creating objects from onCreate()
    private void initialization() {
        // create layout objects

        btnGroupInfo = findViewById(R.id.groupInfo);
        btnDiscover= findViewById(R.id.discover);
        btnSend= findViewById(R.id.sendButton);
        listView= findViewById(R.id.peerListView);
        readMsgBox = findViewById(R.id.readMsg);
        connectionStatus= findViewById(R.id.connectionStatus);
        writeMsg = findViewById(R.id.writeMsg);
        connectButton = findViewById(R.id.connectBtn);
        deviceModeToggleGroup = findViewById(R.id.deviceModeToggleGroup);

        // create wifi p2p manager providing the API for managing Wifi peer-to-peer connectivity
        mManager = (WifiP2pManager) getApplicationContext().getSystemService(Context.WIFI_P2P_SERVICE);
        // a channel that connects the app to the wifi p2p framework.
        mChannel = mManager.initialize(this, getMainLooper(),null);
        // create wifi broadcast receiver to receive events from the wifi manager
        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, mNetService);

        mIntentFilter = new IntentFilter();
        // indicates the state of Wifi P2P connectivity has changed
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // TODO: This might be used to obtain the device name, but our current approach is to use bluetooth name
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        Log.d(TAG, "Creating WfdNetManagerService");
        wfdNetManagerService = new WfdNetManagerService(mManager, mChannel);

        // TODO: it would be better if the messages are processed by the NetService (Router and Client)
        uiHandler = new Handler(getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                switch (message.what) {
                    case ThreadMessageTypes.MESSAGE_READ:
                        // This message requests display in the UI the data received from another
                        // device
                        // The object received is a MeshMessage object
                        MeshMessage meshMsg = (MeshMessage) message.obj;

                        switch (meshMsg.getMsgType()) {
                            // Just to indicate the type of messages we want the NetService to handle
                            case CLIENT_LIST:
                            case DATA_SINGLE_CLIENT:
                            default:
                                // Let the NetService to handle the message
                                mNetService.handleThreadMessage(message);
                                break;
                        }
                        break;

                    // TODO: Maybe this type won't be necessary after all
                    case ThreadMessageTypes.MESSAGE_WRITTEN:
                        break;

                    case ThreadMessageTypes.SOCKET_DISCONNECTION:
                        ObjectSocketCommunication sc = (ObjectSocketCommunication) message.obj;
                        Log.d(TAG, "Disconnecting SocketCommunication");
                        sc.close();
                        break;

                    // Just to indicate the type of messages we want the NetService to handle
                    case ThreadMessageTypes.CLIENT_SOCKET_CONNECTION:
                    default:
                        // NOTE: CLIENT_SOCKET_CONNECTION is handled by the NetService.
                        //  Router must build and send a list to the clients. Nothing to do for Clients
                        mNetService.handleThreadMessage(message);
                        break;
                }
                return true;
            }
        });
    }

    // implemented method for app object action listeners
    private void setListeners(){
        connectButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    deviceModeToggleGroup.setEnabled(false);
                    if (isAP) {
                        mNetService = new MRouterService(getApplicationContext(), wfdNetManagerService, uiHandler);
                    } else {
                        mNetService = new MClientService(getApplicationContext(), wfdNetManagerService, uiHandler);
                    }

                    mNetService.setClientListUiUpdateCallback(updateClientsListCallback);
                    mNetService.setGroupInfoUiCallback(groupInfoUiCallback);
                    mNetService.setMessageTextUiCallback(messageTextUiCallback);

                    // Since we create the WifiDirectBroadcastReceiver before setting mNetService, we
                    // end up passing a null object. We need to set it here
                    mReceiver.setNetService(mNetService);

                    mNetService.start();
                } else {
                    deviceModeToggleGroup.setEnabled(true);
                    mNetService.stop();
                    // TODO: maybe this should be part of mNetService.stop();
                    wfdNetManagerService.tearDown();
                    mNetService = null;
                    mReceiver.setNetService(null);
                }
            }
        });

        deviceModeToggleGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (isChecked) {
                    isAP = (checkedId == R.id.apModeBtn);
                }
            }
        });

        btnGroupInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str = "";

                if (mWifiP2pInfo == null) {
                    // Pop-up notifying device NOT connected
                    Toast.makeText(getApplicationContext(),"No group", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (mWifiP2pInfo.isGroupOwner){
                    str = str + "GROUP OWNER:  ME\n";
                    Collection <WifiP2pDevice> clients = mGroup.getClientList();
                    for (WifiP2pDevice client : clients) {
                        String macString = client.deviceAddress;
                        str = str + "CLIENT :  " + client.deviceName + " " + macString + "\n";

                    }
                    str = str + "GROUP NUM:\n";
                }else {
                    str = "GROUP OWNER:  "+mGroup.getOwner().deviceName+"  "+mWifiP2pInfo.groupOwnerAddress.getHostAddress()+"\n";
                    try {
                        String last = "";
                        // Get all network interfaces on the device
                        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                        // Loop over the interfaces to find the IP address of the device
                        while (interfaces.hasMoreElements()) {
                            NetworkInterface iface = interfaces.nextElement();
                            Enumeration<InetAddress> addresses = iface.getInetAddresses();
                            while (addresses.hasMoreElements()) {
                                InetAddress addr = addresses.nextElement();
                                // Check that the address is not a loopback address (e.g. 127.0.0.1)
                                if (!addr.isLoopbackAddress()) {
                                    last = "LOCAL IP:  "+addr.getHostAddress()+"\n";
                                }
                            }
                        }
                        str = str + last;
                    } catch (SocketException e) {
                        System.out.println("Error getting network interfaces: " + e.getMessage());
                    }
                }
                readMsgBox.setText(str);

            }
        });

        // Discover button to discover peers on the same network
        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // NOTE: Here we use the button to start the broadcast and discover manually if the first
                //  automatic discovery failed.

                // NOTE: Need another way to distinguish between group owner and client. Maybe this
                //  is part of a refactor and it should be done within the MRouterService/MClientService
                //  classes
                if (wfdNetManagerService._isGO) {
                    Log.d(TAG, " btnDiscover onClick: No action.");
                } else {
                    // NOTE: all these are async calls
                    Log.d(TAG, "Starting service discovery again.");
                    wfdNetManagerService.wfdDnsSdManager.discoverServices();
                }
            }
        });

        //Name of discovered peer turned into a button in the listView
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // We only update the destination device of the message
                msgDstDevice = groupClientsList.get(i);
                Log.d(TAG, "Selected destination device: " + msgDstDevice);
            }
        });

        // Send button listener to send text message between peers
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (msgDstDevice == null) {
                    Log.i(TAG, "Null recipient.");
                    Toast.makeText(getApplicationContext(), "Select a recipient from the list", Toast.LENGTH_SHORT).show();
                    return;
                }

                String msg = writeMsg.getText().toString();
                if (msg.isEmpty()) {
                    Log.i(TAG, "Empty message.");
                    Toast.makeText(getApplicationContext(), "Write a message", Toast.LENGTH_SHORT).show();
                    return;
                }

                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        // TODO: right now we only support one destination device
                        ArrayList<UUID> dstList = new ArrayList<>(1);
                        dstList.add(msgDstDevice.getDeviceId());

                        MeshMessage meshMessage = new MeshMessage(MeshMessageType.DATA_SINGLE_CLIENT,
                                msg,
                                dstList);
                        mNetService.sendMessage(meshMessage);
                    }
                });
            }
        });
    }

    // TODO: this is not the correct way to do this. It's just to save development time. The best
    //  would be to use the MVVM model to deal with UI updates. Also, if we use lists, we should use
    //  the RecyclerView element in our UI.
    /*
     * This method will give a new use to the `peerList`, `deviceNameArray` and `deviceArray` and
     * their related UI view, given that with the automatic connection feature we no longer use the
     * peerList to display the possible peers
     */
     NetService.ClientListUiCallback updateClientsListCallback = new NetService.ClientListUiCallback() {
        @Override
        public void updateClientsUi(ArrayList<MeshDevice> clients) {
            Log.d(TAG, "Updating client list");
            Log.d(TAG, "List of clients received: " + clients);
            groupClientsList.clear();
            groupClientsList.addAll(new ArrayList<>(clients));

            // store the device names to be display
            groupClientsNames = new String[clients.size()];
            Log.d(TAG, "Number of clients: " + clients.size());

            // Append " : GO" to the name if the device is a GO.
            for (int i = 0; i < clients.size(); ++i) {
                groupClientsNames[i] = groupClientsList.get(i).getDeviceName() + " - " + groupClientsList.get(i).getDeviceId();
                if (groupClientsList.get(i).isGo()) {
                    groupClientsNames[i] += " - GO";
                }
            }

            // TODO: RecyclerView is now preferred instead of ListView.
            // add all the device names to an adapter then add the adapter to the layout listview
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(),
                    android.R.layout.simple_list_item_1,
                    groupClientsNames);
            listView.setAdapter(adapter);
        }
    };


    NetService.GroupInfoUiCallback groupInfoUiCallback = new NetService.GroupInfoUiCallback() {
        @Override
        public void updateGroupInfoUi(WifiP2pGroup wifiP2pGroup) {
            Log.d(TAG, "groupInfoUiCallback");
            StringBuilder stringBuilder = new StringBuilder("");

            if (wifiP2pGroup.isGroupOwner()) {
                stringBuilder.append("GROUP OWNER:  ME\n")
                        .append("GROUP NUM: ")
                        .append(wifiP2pGroup.getClientList().size())
                        .append("\n");
            } else {
                stringBuilder.append("GROUP OWNER:  ")
                        .append(wifiP2pGroup.getOwner().deviceName)
                        .append(wifiP2pGroup.getOwner().deviceAddress)
                        .append("\n");
            }

            Log.d(TAG, " read_msg_box: " + stringBuilder);
            readMsgBox.setText(stringBuilder.toString());
        }
    };

    NetService.MessageTextUiCallback messageTextUiCallback = new NetService.MessageTextUiCallback() {
        @Override
        public void updateMessageTextUiCallback(String msgText) {
            readMsgBox.setText(msgText);
        }
    };
    ///


    // When activity enters the resume state after onCreate and onStart
    @Override
    protected void onResume(){
        super.onResume();
        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, mNetService);
        registerReceiver(mReceiver,mIntentFilter);
        /// NOTE: example code from https://developer.android.com/training/connect-devices-wirelessly/nsd#teardown
//        if (nsdHelper != null) {
//        if (wfdDnsSdService != null) {
//            nsdHelper.registerService(connection.getLocalPort());
//            nsdHelper.discoverServices();
//            wfdDnsSdService.discoverServices();
//        }
        ///
    }

    // Systems call this method when the user leaves the activity meaning when the activity is no
    // longer in the foreground.
    @Override
    protected void onPause(){
        /// TODO: do we need to stop and unregister the services when the app is paused/destroyed in
        //   the case of WiFi Direct for SD as it is shown with the guide of NSD for local networks?
        //   The guide for WiFi Direct for SD (https://developer.android.com/training/connect-devices-wirelessly/nsd-wifi-direct)
        //   doesn't say anything but, the guide for NSD on a local network (https://developer.android.com/training/connect-devices-wirelessly/nsd)
        //   does the following:
        // NOTE: example code from https://developer.android.com/training/connect-devices-wirelessly/nsd#teardown
//        if (nsdManager != null) {
//            nsdManager.tearDown();
//        }
        ///

        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        /// TODO: same as the comment in the `onPause()` method above.
//        nsdHelper.tearDown();
//        connection.tearDown();
        ///
        if (mNetService != null) mNetService.stop();
        mChannel.close();
        super.onDestroy();
    }
}
