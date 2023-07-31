package com.example.greybox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
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
    TextView read_msg_box, connectionStatus;
    EditText writeMsg;
//    ListView fileList;    // PE_NOTE: disable temporarily since it's not used and affects the UI

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

    ServerClass serverClass;
    ClientClass clientClass;

    boolean connected = false;
    int groupNum = 0;

    // TODO: maybe we need to put these somewhere else
    public static final int MESSAGE_READ = 1,
                            MESSAGE_WRITTEN = 2,
                            SOCKET_DISCONNECTION = 3,
                            HANDLE = 4;
    //
    Handler uiHandler;


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
        initialization();
        // adding listeners to the objects
        setListeners();

        // NOTE: the callback passed as argument contains an implicit reference to MainActivity, but
        //  I guess this case it's ok since we are in the same thread, we just receive messages
        //  from other threads.
        uiHandler = new Handler(getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                switch (message.what) {
                    case MESSAGE_READ:
                        // This message requests display in the UI the data received from another
                        // device
                        // The object received is a bytes[] object
                        String text = new String((byte[])message.obj, StandardCharsets.UTF_8);
                        Log.i(TAG, "Displaying the message on UI.");
                        read_msg_box.setText(text);
                        return true;

                    case MESSAGE_WRITTEN:
                        return true;

                    case SOCKET_DISCONNECTION:
                        return true;

                    default:
                        return false;
                }

            }
        });
    }

    // initial work for creating objects from onCreate()
    private void initialization() {
        // create layout objects
        btnGroupInfo = findViewById(R.id.groupinfo);
        btnDiscover= findViewById(R.id.discover);
        btnSend= findViewById(R.id.sendButton);
        listView= findViewById(R.id.peerListView);
        read_msg_box= findViewById(R.id.readMsg);
        connectionStatus= findViewById(R.id.connectionStatus);
        writeMsg = findViewById(R.id.writeMsg);
//        fileList = findViewById(R.id.fileList);   // PE_NOTE: disable temporarily since it's not used and affects the UI

        // create wifi p2p manager providing the API for managing Wifi peer-to-peer connectivity
        mManager = (WifiP2pManager) getApplicationContext().getSystemService(Context.WIFI_P2P_SERVICE);
        // a channel that connects the app to the wifi p2p framework.
        mChannel = mManager.initialize(this, getMainLooper(),null);
        // create wifi broadcast receiver to receive events from the wifi manager
        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();     // TODO: PE_CMT: This could be a private final field
        // indicates whether WiFi P2P is enabled
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // indicates that the available peer list has changed
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // indicates the state of Wifi P2P connectivity has changed
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // indicates this device's configuration details have changed
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // jsgarvey - disp local mac address
        String str = "";
        String MAC_ADDRESS = getMacAddr();
        str = str + "MAC : " + MAC_ADDRESS + "\n" ;
        read_msg_box.setText(str);
    }

    /**
     JSGARVEY - method for retrieving local device MAC Address
     Sourse - <a href="https://stackoverflow.com/questions/11705906/programmatically-getting-the-mac-of-an-android-device">...</a>
     */
    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:",b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            // Empty Catch
        }
        return "02:00:00:00:00:00";
    }

    // implemented method for app object action listeners
    private void setListeners(){

        /**
         *  jsgarvey - Needs Updating for MAC Address
         */
        btnGroupInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str = "";
                // TODO: PE_FIX: this is a simple fix. I still need to understand why the app doesn't work.
                //  mWifiP2pInfo is null in my case, therefore we cannot access its methods. It seems like
                //  mWifiP2pInfo is assigned an object in the "interfaces" section below. Look for WifiP2pManager.ConnectionInfoListener()
                if (mWifiP2pInfo == null) {
                    // Pop-up notifying device NOT connected
                    Toast.makeText(getApplicationContext(),"No group", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mWifiP2pInfo.isGroupOwner){
                    str = str + "GROUP OWNER:  ME\n";
                    Collection <WifiP2pDevice> clients = mGroup.getClientList();
                    Iterator<WifiP2pDevice> device = clients.iterator();
                    while (device.hasNext()) {
                        WifiP2pDevice client = device.next();
                        String macString = client.deviceAddress;
                        str = str + "CLIENT :  "+ client.deviceName + " " + macString+ "\n";

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

            }
        });

        // Discover button to discover peers on the same network
        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // listener discovering peers from broadcast channel
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

                    // if listener created successfully display Discovery Started
                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "btnDiscover.onClick WifiP2pManager.onSuccess()");
                        connectionStatus.setText("Discovery Started");
                    }
                    // if listener NOT created successfully display Discovery Failed
                    @Override
                    public void onFailure(int i) {
                        Log.i(TAG, "btnDiscover.onClick WifiP2pManager.onFailure()");
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

                if (device.isGroupOwner()) {
                    Log.i(TAG, "Connecting to a GO.");
//                    config.groupOwnerIntent = 0;
                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                        // Called when device successfully connected
                        @Override
                        public void onSuccess() {
                            Log.i(TAG, "Connection to " + device.deviceName + " succeeded.");
                            // Pop-up notifying device connected
                            connectionStatus.setText("Connecting to GO "+ device.deviceName);
                        }
                        // Called when device NOT successfully connected
                        @Override
                        public void onFailure(int i) {
                            Log.i(TAG, "Connection to " + device.deviceName + " failed.");
                            // Pop-up notifying device NOT connected
                            Toast.makeText(getApplicationContext(),"NOT CONNECTED", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.i(TAG, "Negotiating GO role.");
                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                        // Called when device successfully connected
                        @Override
                        public void onSuccess() {
                            Log.i(TAG, "Connection to " + device.deviceName + " succeeded.");
                            // Pop-up notifying device connected
                            Toast.makeText(getApplicationContext(),"CONNECTING TO "+device.deviceName, Toast.LENGTH_SHORT).show();
                        }
                        // Called when device NOT successfully connected
                        @Override
                        public void onFailure(int i) {
                            Log.i(TAG, "Connection to " + device.deviceName + " failed.");
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
                String msg = writeMsg.getText().toString();

                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    if (msg.isEmpty()) {
                        Log.i(TAG, "Empty message.");
                        return;
                    }

                    if (mWifiP2pInfo.isGroupOwner) {
                        Log.i(TAG, "Server sends message: " + msg);
                        // TODO: For now, this method sends the message to all clients connected. We
                        //  need to implement some logic/routing algorithm
                        serverClass.write(msg.getBytes(), 0);
                    } else {
                        Log.i(TAG, "Client sends message: " + msg);
                        clientClass.write(msg.getBytes());
                    }
                });
            }
        });
    }

    // Wifi P2P Manager peer list listener for collecting list of wifi peers
    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {

        // override method to find peers available
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            // TODO: check if every time this listener is called, it means that the peerList is different
            //  from the previous one. I don't think we need to check if lists are different.
//            Log.i(TAG, "onPeersAvailable: received peerList");
            if (!peerList.getDeviceList().equals(peers)) {
//                Log.i(TAG, "onPeersAvailable: peerList != peers");
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                //store peers list device names to be display and add to device array to be selected
                deviceNameArray = new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index = 0;
                for(WifiP2pDevice device : peerList.getDeviceList()){
                    // NOTE: maybe is not so good to modify the name here, consider a data class or
                    //  having another list which will be the "displayName" in which we will add who
                    //  is a GO
                    if (device.isGroupOwner()) {
                        deviceNameArray[index] = device.deviceName + " : (GO)";
                    } else {
                        deviceNameArray[index] = device.deviceName;
                    }

                    deviceArray[index] = device;
                    index++;
                }
                // TODO: RecyclerView is now preferred instead of ListView. Anyway, this is just a
                //  prototype, so it doesn't hurt. But consider changing it after all functionality is working.
                // add all the device names to an adapter then add the adapter to the layout listview
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                listView.setAdapter(adapter);
            }

            // if no peers found pop-up "No Device Found"
            if (peers.size() == 0) {
                Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_SHORT).show();
            }
        }
    };

    // NOTE: Listener used by the BroadcastReceiver when WIFI_P2P_CONNECTION_CHANGED_ACTION
    //  interface for callback invocation when connection info is available
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        // If the connection info is available
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            connected = true;
            mWifiP2pInfo = wifiP2pInfo;
            // Get Host Ip Address
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;

            Log.i(TAG, "wifiP2pInfo: " + wifiP2pInfo);
            Log.i(TAG, "wifiP2pInfo.isGroupOwner: " + wifiP2pInfo.isGroupOwner);
            Log.i(TAG, "wifiP2pInfo.groupOwnerAddress: " + wifiP2pInfo.groupOwnerAddress);
            Log.i(TAG, "wifiP2pInfo.groupFormed: " + wifiP2pInfo.groupFormed);

            if (!wifiP2pInfo.groupFormed) {
                Log.i(TAG, "connectionInfoListener: group not formed.");
                return;
            }

            final int PORT = 8888;

            // Once the connection info is ready, create the sockets depending on the role of the device
            // Check if we are the GO or a client
            if (wifiP2pInfo.isGroupOwner) {
                if (serverClass == null) {
                    // Create a ServerSocket
                    serverClass = new ServerClass(uiHandler, PORT);
                }

                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Log.i(TAG, "Starting server thread");
                executorService.execute(serverClass);
            }
            else {
                // Create a (client) Socket
                clientClass = new ClientClass(groupOwnerAddress, uiHandler, PORT);
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Log.i(TAG, "Starting client thread");
                executorService.execute(clientClass);
            }
        }
    };

    WifiP2pManager.GroupInfoListener groupInfoListener = new WifiP2pManager.GroupInfoListener(){
        @Override
        public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
            mGroup = wifiP2pGroup;
            Collection <WifiP2pDevice> collection = wifiP2pGroup.getClientList();
            groupNum = collection.size();
            String str = "";
            // jsgarvey - disp local mac address
            String MAC_ADDRESS = getMacAddr();
            str = str + "MAC : " + MAC_ADDRESS + "\n" ;

            if(wifiP2pGroup.isGroupOwner()){
                str = str + "GROUP OWNER:  ME\n";
                str = str + "GROUP NUM: " + groupNum+"\n";
            } else{
                // jsgarvey - disp group owners name and mac address
                str = str + "GROUP OWNER:  " + wifiP2pGroup.getOwner().deviceName+"  " +wifiP2pGroup.getOwner().deviceAddress+"\n";
            }
            read_msg_box.setText(str);
        }
    };

    WifiP2pManager.DeviceInfoListener deviceInfoListener = new WifiP2pManager.DeviceInfoListener() {
        @Override
        public void onDeviceInfoAvailable(@Nullable WifiP2pDevice wifiP2pDevice) {
            // Toast.makeText(getApplicationContext(), "ADDRESS = "+wifiP2pDevice.deviceAddress, Toast.LENGTH_SHORT).show();
            localAddress = wifiP2pDevice.deviceAddress;
        }
    };


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
