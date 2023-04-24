package com.example.greybox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.MacAddress;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/*
    JSGARVEY 03/03/23 - US#206 Citations:
    https://developer.android.com/training/connect-devices-wirelessly/wifi-direct#create-group
    Sarthi Technology - https://www.youtube.com/playlist?list=PLFh8wpMiEi88SIJ-PnJjDxktry4lgBtN3
 */
public class MainActivity extends FragmentActivity{

    Button btnDiscover, btnSend, btnGroupInfo;
    ListView listView;
    TextView read_msg_box, connectionStatus;
    EditText writeMsg;
    ListView fileList;

    //Wifi Manager primary API for managing all aspects of WIFI connectivity
    WifiManager wifiManager;
    //Wifi P2p Manager provides specif API for managing WIFI p2p connectivity
    WifiP2pManager mManager;
    // A P2p channel that connects the app to the WIFI p2p framework
    WifiP2pManager.Channel mChannel;
    // After connection group stored with all devices and group owner info
    WifiP2pGroup mGroup;
    WifiP2pInfo mWifiP2pInfo;

    String localAddress = "";

    //Broadcast Receiver base class for code that receives and handles broadcast
    // intents sent by the context
    BroadcastReceiver mReceiver;
    // An Intent is a description of an operation to be performed.
    // A filter matches intents and describes the Intent values it matches.
    // Filters by characteristics of intents Actions, Data, and Categories
    IntentFilter mIntentFilter;

    // wifi p2p peers list
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    // array holding names of devices
    String[] deviceNameArray;
    // the p2p peer array will be used to connect to a device
    WifiP2pDevice[] deviceArray;

    // Hardcoded value that indicates to the handler that message has been read
    static final int MESSAGE_READ = 1;
    ServerClass serverClass;
    ClientClass clientClass;
    ServerClass serverClass2;
    ClientClass clientClass2;
    boolean groupOwner = false;
    boolean connected = false;
    int groupNum = 0;

    //imported override method onCreate. Initialize the the activity.
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
        initialWork();
        // adding listeners to the objects
        exListener();
    }

    // initial work for creating objects from onCreate()
    private void initialWork() {
        // create layout objects
        btnGroupInfo = findViewById(R.id.groupinfo);
        btnDiscover= findViewById(R.id.discover);
        btnSend= findViewById(R.id.sendButton);
        listView= findViewById(R.id.peerListView);
        read_msg_box= findViewById(R.id.readMsg);
        connectionStatus= findViewById(R.id.connectionStatus);
        writeMsg = findViewById(R.id.writeMsg);
        fileList = findViewById(R.id.fileList);

        // create wifi manager from the android app context system wifi services
        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // create wifi p2p manager providing the API for managing Wifi peer-to-peer connectivity
        mManager = (WifiP2pManager) getApplicationContext().getSystemService(Context.WIFI_P2P_SERVICE);
        // a channel that connects the app to the wifi p2p framework.
        mChannel = mManager.initialize(this, getMainLooper(),null);
        // create wifi broadcast receiver to receive events from the wifi manager
        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        // indicates whether WiFi P2P is enabled
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // indicates that the available peer list has changed
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // indicates the state of Wifi P2P connectivity has changed
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // indicates this device's configuration details have changed
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    }

    // implemented method for app object action listeners
    private void exListener(){

////////////////////////////////////////////////////////////////////////////////////////////////////
        btnGroupInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str = "";
                if (mWifiP2pInfo.isGroupOwner){
                    str = str + "GROUP OWNER:  ME\n";
                    Collection <WifiP2pDevice> clients = mGroup.getClientList();
                    Iterator<WifiP2pDevice> device = clients.iterator();
                    while (device.hasNext()) {
                        WifiP2pDevice client = device.next();
                        String macString = client.deviceAddress;
                        str = str + "CLIENT :  "+ client.deviceName + " " + macString+ "\n";

                        ////////////REQUIRES API 30////////////////////
//                        MacAddress macAddress = MacAddress.fromString(client.deviceAddress);
//                        InetAddress ipAddress = macAddress.getLinkLocalIpv6FromEui48Mac();
//                        str = str + "CLIENT IP: "+ client.deviceName + " " + ipAddress.toString() + "\n";
                    }
                    str = str + "GROUP NUM: " + groupNum+"\n";
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
                read_msg_box.setText(str);

//                final InetAddress goAddress = mWifiP2pInfo.groupOwnerAddress;
//                int portNumber = Math.abs(goAddress.toString().hashCode() % 65536) + 1024;
//                serverClass = new ServerClass(handler, 8888);
//                serverClass.start();
//                final InetAddress groupOwnerAddress = mWifiP2pInfo.groupOwnerAddress;
//                clientClass = new ClientClass(groupOwnerAddress, handler, 8888);
//                clientClass.start();

            }
        });
////////////////////////////////////////////////////////////////////////////////////////////////////

        // Discover button to discover peers on the same network
        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // listener discovering peers from broadcast channel
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

                    // if listener created successfully display Discovery Started
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Discovery Started");
                    }
                    // if listener NOT created successfully display Discovery Failed
                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText("Discovery Failed"+i);
                    }
                });
            }
        });

        //Name of discovered peer turned into a button in the listView
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // this array is where the devices are stored for connections
                WifiP2pDevice device = deviceArray[i];
                // Config for setting up p2p connection
                WifiP2pConfig config = new WifiP2pConfig();
                // Set config device address from chosen device
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;

                if(device.isGroupOwner()){
//                    config.groupOwnerIntent = 0;
                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                        // Called when device successfully connected
                        @Override
                        public void onSuccess() {
                            // Pop-up notifying device connected
                            connectionStatus.setText("Connecting to GO "+ device.deviceName);
                        }
                        // Called when device NOT successfully connected
                        @Override
                        public void onFailure(int i) {
                            // Pop-up notifying device NOT connected
                            Toast.makeText(getApplicationContext(),"NOT CONNECTED", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else{
                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                        // Called when device successfully connected
                        @Override
                        public void onSuccess() {
                            // Pop-up notifying device connected
                            Toast.makeText(getApplicationContext(),"CONNECTING TO "+device.deviceName, Toast.LENGTH_SHORT).show();
                        }
                        // Called when device NOT successfully connected
                        @Override
                        public void onFailure(int i) {
                            // Pop-up notifying device NOT connected
                            Toast.makeText(getApplicationContext(),"NOT CONNECTED", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        // Send button listener to send text message between peers
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get text message from EditText Field
                String msg = writeMsg.getText().toString();
                if(mWifiP2pInfo.isGroupOwner){
                    // Class to handle send text message task
                    SendTask task  = new SendTask(msg, serverClass.getSendReceive());
                    task.execute();
                }else{
                    SendTask task  = new SendTask(msg, clientClass.getSendReceive());
                    task.execute();
                }

            }
        });
    }

    // Wifi P2P Manager peer list listener for collecting list of wifi peers
    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {

        // override method to find peers available
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            // if the peer previous peer list does not equal current peer list gotten by listener
            // the peers list has changed and we want to store the new list instead
            if(!peerList.getDeviceList().equals(peers)){
                Toast.makeText(getApplicationContext(), "Peers Changed", Toast.LENGTH_SHORT).show();
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                //store peers list device names to be display and add to device array to be selected
                deviceNameArray = new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index = 0;
                for(WifiP2pDevice device : peerList.getDeviceList()){
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;

                    if(device.isGroupOwner()){
                        connectionStatus.setText("GO FOUND "+device.deviceName);
                    }
                    index++;
                }
                // add all the device names to an adapter then add the adapter to the layout listview
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                listView.setAdapter(adapter);
            }

            // if no peers found pop-up "No Device Found"
            if(peers.size() == 0){
                Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    };

    // interface for callback invocation when connection info is available
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        // If the connection info is available
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            connected = true;
            mWifiP2pInfo = wifiP2pInfo;
            // Get Host Ip Address
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            // If the connection group exists and the device is connection host
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner && !groupOwner) {
                connectionStatus.setText("HOST");
                serverClass = new ServerClass(handler, 8888);
                serverClass.start();
                groupOwner = true;
            // If only the connection group exists
            }
            else if (wifiP2pInfo.groupFormed && !groupOwner) {
                connectionStatus.setText("CLIENT");
                clientClass = new ClientClass(groupOwnerAddress, handler, 8888);
                clientClass.start();
            }
            else if(groupOwner){
                connectionStatus.setText("HOST");
//                serverClass2 = new ServerClass(handler, 8889);
//                serverClass2.start();
            }
            else if (wifiP2pInfo.groupFormed && connected) {
                connectionStatus.setText("CLIENT");
//                clientClass2 = new ClientClass(groupOwnerAddress, handler, 8889);
//                clientClass2.start();
            }

        }
    };

    WifiP2pManager.GroupInfoListener groupInfoListener = new WifiP2pManager.GroupInfoListener(){
        @Override
        public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
////////////////////////////////////////////////////////////////////////////////////////////////////
            mGroup = wifiP2pGroup;
            Collection <WifiP2pDevice> collection = wifiP2pGroup.getClientList();
            groupNum = collection.size();
            String str = "";
            if(wifiP2pGroup.isGroupOwner()){
                str = str + "GROUP OWNER:  ME\n";
                str = str + "GROUP NUM: " + groupNum+"\n";
            } else{
                str = "GROUP OWNER:  "+ wifiP2pGroup.getOwner().deviceName+"  " +wifiP2pGroup.getOwner().deviceAddress+"\n";
            }
            read_msg_box.setText(str);
////////////////////////////////////////////////////////////////////////////////////////////////////
        }
    };

    WifiP2pManager.DeviceInfoListener deviceInfoListener = new WifiP2pManager.DeviceInfoListener() {
        @Override
        public void onDeviceInfoAvailable(@Nullable WifiP2pDevice wifiP2pDevice) {
            // Toast.makeText(getApplicationContext(), "ADDRESS = "+wifiP2pDevice.deviceAddress, Toast.LENGTH_SHORT).show();
            localAddress = wifiP2pDevice.deviceAddress;
        }
    };

    // A Handler allows you to send and process Message and Runnable objects associated with a thread's
    // MessageQueue. Each Handler instance is associated with a single thread and that thread's message
    // queue. Handler.Callback interface you can use when instantiating a Handler to avoid having to
    // implement your own subclass of Handler.
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            // msg.what identifies what message is about
            switch(msg.what){
                // If the message is a text message to be read case=1
                case MESSAGE_READ:
                    // cast the message object as an byte array to store the message
                    byte[] readBuff = (byte[]) msg.obj;
                    // store the byte array as a String to be printed
                    String tempMsg = new String(readBuff,0,msg.arg1);
                    // Add message to devices textview
                    read_msg_box.setText(tempMsg);
                    break;
            }
            return true;
        }
    });

    // When activity enters the resume state after onCreate and onStart
    @Override
    protected  void onResume(){
        super.onResume();
        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this);
        registerReceiver(mReceiver,mIntentFilter);
    }

    // Systems call this method when the user leaves the activity meaning when the activity is no
    // longer in the foreground.
    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(mReceiver);
    }


}
