package com.example.greybox;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/// PE_AUTO_CONNECT
public class WfdNetManagerService {
    private static final String TAG = "WfdNetManagerService";

    private int creationRetriesLeft = 3;
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    public String encryptedSSID;    // TODO: encrypted SSID. Maybe change to private with getter/setter
    public String encryptedPass;    // TODO: encrypted PASS. Maybe change to private with getter/setter
    final String SSID = "DIRECT-Gb";  // TODO: still need to investigate if the name should be DIRECT-<xy-random_name>
    final String PASS = "12345678";   // TODO: modify. If this project is open source, how do we hide this information?
    private static final String ENCRYPTION_KEY = "TODO";  // TODO: is it secure to have the encryption key as an attribute?

    public WfdDnsSdManager wfdDnsSdManager;
    /// TODO: THIS DOESN'T WORK. In one device with Android 13 we cannot get the MAC address. On another
    //   device with Android 12, the MAC obtained is different to that obtained from the WiFi P2P framework.
    //   I'm not sure if I'm using the right interface, but it's not a standard name.
    //   In my devices it's called either: p2p-wlan0-2, p2p0, p2p-wlan0-3
//    final static String WIRELESS_INTERFACE_NAME = "wlan0";  // This interface gives us a different MAC from the used in WiFi Direct
    final static String WIFI_P2P_INTERFACE_NAME = "p2p";    // Will be used with "String.contains()" since varies depending on the device
    ///
    public String deviceMacAddress;

    /// TODO: we still have a dependency on this value. It's used to restart the service discovery
    //   manually (pressing the DISCOVER button) when the connection was not established automatically
    public boolean _isGO = false;

    // TODO: shouldn't the port be requested to the OS by calling SocketServer(0)? But that means
    //  we need to start first the ServerSocket before registering the service. This would
    //  contradict the instructions to open the ServerSocket at the end of initialization
    final int SERVER_PORT = 8888;

    // https://www.notion.so/grey-box/Findings-journal-d3c584f59f3544e0bb02ebb91d3b3e59?pvs=4#d8559b89c0ac47849f6126258394c528
    // The name is subject to change based on conflicts with other services advertised on the same network.
    // NOTE: does this mean that we could have a problem if we expect a fixed name? Check if we ever
    //  use the service name other than for registration.
    final String SERVICE_NAME = "greybox_mesh";
    final String SERVICE_TYPE = "_nsdgreybox._tcp"; // The service type uniquely identifies the service your device is advertising
    WifiP2pDnsSdServiceInfo dnsSdServiceInfo;


    // TODO: think if this class should manage also the WifiP2pManager instance, opening of the channel,
    //  and so on
    // --------------------------------------------------------------------------------------------
    //  Constructors
    // --------------------------------------------------------------------------------------------
    WfdNetManagerService(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        this.manager = manager;
        this.channel = channel;

        wfdDnsSdManager = new WfdDnsSdManager(manager, channel);
    }


    // ---------------------------------------------------------------------------------------------
    //  Methods
    // ---------------------------------------------------------------------------------------------
    // TODO: createSoftAP and makeNSDBroadcast methods are only used by the Router device. So, maybe
    //  it's better to refactor this class and have two versions. One for the Client devices and one
    //  for the Router devices.
    public void createSoftAP() {

        creationRetriesLeft--;

        // Try to get the MAC address since the beginning
        if (deviceMacAddress == null) {
            deviceMacAddress = getDeviceMacAddress();
        }

        if (deviceMacAddress.isEmpty()) {
            Log.e(TAG, "Failed to obtain the MAC address. Auto-connect won't work. Aborting.");
            return;
        }

        // Builder used to build WifiP2pConfig objects for creating or joining a group.
        WifiP2pConfig.Builder configBuilder = new WifiP2pConfig.Builder();

        // WifiP2pConfig.GROUP_OWNER_BAND_AUTO is optional, since it's default
        // Also, setGroupOperatingBand() and setGroupOperatingFrequency() are mutually exclusive
        // Network name MUST start with the prefix DIRECT-<xy>, where <xy> are alphanumeric characters.
        WifiP2pConfig config = configBuilder
                .setNetworkName(SSID)
                .setPassphrase(PASS)
                .build();

        manager.createGroup(channel, config, createGroupListener);
        //
    }

    private String encryptInformation(String data) {
        // TODO: implement the real thing
//        javax.crypto.Cipher.getInstance();
        return data;
    }


    // Network Discovery Service
    // TODO: pass the encrypted MAC address as a parameter or we encrypt it here? Who has
    //  the key to encrypt? I guess that's what answers the question.
    // TODO: if we fail to get the MAC address I think we need to return some error and just abort
    //  the whole operation, not just "return" as it's done right now
    public void makeNSDBroadcast(String enSSID, String enPass) {
        Log.d(TAG, "start makeNSDBroadcast");

        // Create a string map containing information about your service.
        // Map is an ADT whereas HashMap is an implementation of Map
        Map<String, String> record = new HashMap<>();

        // TODO: encrypt MAC address
        // Encryption of the MAC address
        String enMac = deviceMacAddress;
        //

        Log.d(TAG, " Creting the record.");

        // TODO: The article suggests to concatenate the whole info, I guess that would be more secure
        record.put("mac", enMac);
        record.put("ssid", enSSID);
        record.put("pass", enPass);
        record.put("port", String.valueOf(SERVER_PORT));
        record.put("name", "greybox" + (int) (Math.random() * 1000));   // TODO: I guess we need to remove the random number

        // Service information.  Pass it an instance name, service type `_protocol._transportlayer`,
        // and the map containing information other devices will want once they connect to this one
        dnsSdServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_NAME, SERVICE_TYPE, record);

        Log.d(TAG, "dnsSdServiceInfo: " + dnsSdServiceInfo.toString());

        wfdDnsSdManager.addLocalService(dnsSdServiceInfo);

        Log.d(TAG, "end makeNSDBroadcast");
    }

    // Does this makes sense? This is just a wrapper.
    public void discoverServices() {
        wfdDnsSdManager.discoverServices();
    }

    // TODO: still unclear if we are going to need this. For now I hope it will help to have a better
    //  behavior of the prototype, since the T95 doesn't seem to have a way to manually delete the
    //  groups as happens with mobile devices. This will be called on `onDestroy()`
    public void tearDown() {

        wfdDnsSdManager.removeLocalService(dnsSdServiceInfo);

        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Group removed successfully.");
            }

            @Override
            public void onFailure(int errorCode) {
                Log.e(TAG, "Failed to remove group. Error code: " + errorCode);
            }
        });
    }


    // ---------------------------------------------------------------------------------------------
    //  Listeners
    // ---------------------------------------------------------------------------------------------
    WifiP2pManager.ActionListener createGroupListener = new WifiP2pManager.ActionListener() {

        @Override
        public void onSuccess() {
            Log.d(TAG, "Succeed to create group.");
            /// TODO: encrypt everything that will be broadcast for NSD (ssid, passphrase, mac)
            encryptedSSID = SSID;
            encryptedPass = PASS;
            ///

            // TODO: makeNSDBroadcast() and discoverServices() should be performed in a separate thread.
            makeNSDBroadcast(encryptedSSID, encryptedPass);
            _isGO = true;
        }

        @Override
        public void onFailure(int errorCode) {
            Log.d(TAG, "Failed to create group. Error code: " + errorCode);
            WfdStatusInterpreter.logError(errorCode);

            // TODO: is there any way to retry creation of the group if the first attempt failed?
            if ((errorCode == WifiP2pManager.BUSY) && creationRetriesLeft > 0) {
                Log.d(TAG, "Retrying...");
                createSoftAP();
            }
        }
    };


    /// PE_MSG_SPECIFIC_CLIENT
    // Helper methods. From https://www.baeldung.com/java-mac-address
    private static String macBytesToString(byte[] hardwareAddress) {
        String[] hexadecimal = new String[hardwareAddress.length];
        for (int i = 0; i < hardwareAddress.length; i++) {
            hexadecimal[i] = String.format("%02x", hardwareAddress[i]);
        }
        String mac = String.join(":", hexadecimal);
        Log.d(TAG, " MAC address:  " + mac);
        return mac;
    }

    /*
     * This method won't work if SDK target is Android 11+
     */
    public static String getMacFromLocalIpAddress(String localIpAddress) throws UnknownHostException, SocketException {
        InetAddress localIP = null;
        NetworkInterface ni = null;

        // Assume we receive alwasy an IP, but some methods return an IP string with the format "/192.168.49.71".
        // Remove the first slash if it's there.
        if (localIpAddress.charAt(0) == '/') {
            localIpAddress = localIpAddress.substring(1);
        }
        Log.d(TAG, " localIpAddress: " + localIpAddress);

        try {
            // NOTE: here localIP is just the same as the input argument localIpAddress. If we are
            //  going to pass the result of socket.getLocalAddress(), we don't need to to this step
            //  since we already pass a InetAddress object.
            localIP = InetAddress.getByName(localIpAddress);
            Log.d(TAG, " localIP:      " + localIP);
        } catch (UnknownHostException e) {
            throw new UnknownHostException();
        }

        try {
            ni = NetworkInterface.getByInetAddress(localIP);
            Log.d(TAG, " netInterface: " + ni);
        } catch (SocketException e) {
            throw new SocketException();
        }
        // NOTE: this will be null if target=Android 11+: https://developer.android.com/training/articles/user-data-ids#mac-11-plus
        byte[] macAddress = ni.getHardwareAddress();
        return macBytesToString(macAddress);
    }

    /*
     * This method won't work if SDK target is Android 11+
     */
    public static String getMacFromLocalIpAddress(InetAddress localIpAddress) throws UnknownHostException, SocketException {
        Log.d(TAG, " localIpAddress: " + localIpAddress);
        NetworkInterface ni = null;

        try {
            ni = NetworkInterface.getByInetAddress(localIpAddress);
            Log.d(TAG, " netInterface: " + ni);
        } catch (SocketException e) {
            throw new SocketException();
        }
        // NOTE: this will be null if target=Android 11+: https://developer.android.com/training/articles/user-data-ids#mac-11-plus
        byte[] macAddress = ni.getHardwareAddress();
        return macBytesToString(macAddress);
    }
    /// PE_MSG_SPECIFIC_CLIENT

    /*
     * This method won't work if SDK target is Android 11+
     */
    public static String getDeviceMacAddress() {
        try {
            List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface netInterface : networkInterfaces) {
                if (!netInterface.getName().contains(WIFI_P2P_INTERFACE_NAME)) continue;
//                if (!netInterface.getName().equalsIgnoreCase(WIFI_P2P_INTERFACE_NAME)) continue;

                byte[] macAddress = netInterface.getHardwareAddress();
                return macBytesToString(macAddress);
            }
        } catch (SocketException e) {
            Log.e(TAG, "Failed to get the MAC address.");
            e.printStackTrace();
        }

        return "";
    }
}
///
