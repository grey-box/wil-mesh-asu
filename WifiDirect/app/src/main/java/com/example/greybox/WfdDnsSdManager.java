package com.example.greybox;

import android.net.MacAddress;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pConfig.Builder;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/// PE_AUTO_CONNECT

// TODO: either delete "Manager" from the class' name or change the name of the `WifiP2pManager manager`
//  attribute since it gives the impression that `manager` could be related to the name of the class
public class WfdDnsSdManager {
    private static final String TAG = "WfdDnsSdManager";

    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private boolean isLocalServiceRegistered = false;
    private boolean isAddServiceRequestMade = false;
    final private HashMap<String, ConnectionData> gosData = new HashMap<>();
//    ArrayList<ConnectionData> groupOwners;
//    ArrayList<HashMap<String, ConnectionData>> groupOwners;
    WifiP2pDnsSdServiceRequest serviceRequest;



    // ---------------------------------------------------------------------------------------------
    //  Constructors
    // ---------------------------------------------------------------------------------------------
    public WfdDnsSdManager(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        this.manager = manager;
        this.channel = channel;
//        this.groupOwners = new ArrayList<>();
    }


    // ---------------------------------------------------------------------------------------------
    //  Methods
    // ---------------------------------------------------------------------------------------------

    // TODO: maybe this method is useless. What if we need to do something else on the callback?
    public void addLocalService(WifiP2pDnsSdServiceInfo serviceInfo) {
        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        manager.addLocalService(channel, serviceInfo, new ActionListener() {
            @Override
            public void onSuccess() {
                // Command successful! Code isn't necessarily needed here,
                // Unless you want to update the UI or add logging statements.
                Log.d(TAG, " addLocalService: Request to add local service succeeded.");
                // NOTE: `serviceInfo` is not human readable, it doesn't contain useful info to print
                Log.d(TAG, " serviceInfo: " + serviceInfo);
                isLocalServiceRegistered = true;
            }

            @Override
            public void onFailure(int errorCode) {
                Log.d(TAG, " addLocalService: Request to add local service failed.");
                Log.d(TAG, " serviceInfo: " + serviceInfo);
                printError(errorCode);
            }
        });
    }

    public void removeLocalService(WifiP2pDnsSdServiceInfo serviceInfo) {

        if (serviceInfo == null) { return; }

        manager.removeLocalService(channel, serviceInfo, new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, " removeLocalService: Request to remove services succeeded.");
                Log.d(TAG, " serviceInfo: " + serviceInfo);
                isLocalServiceRegistered = false;    // Maybe this is unnecessary
            }

            @Override
            public void onFailure(int errorCode) {
                Log.d(TAG, " removeLocalService: Request to remove services failed.");
                Log.d(TAG, " serviceInfo: " + serviceInfo);
                printError(errorCode);
            }
        });
    }


    private void registerListeners() {

        Log.d(TAG, "registerListeners start");

        // Implements a Map object to pair a device address with the "name".
        WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomain,
                                                  Map<String, String> record,
                                                  WifiP2pDevice device) {
                /* Callback includes:
                 * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
                 * record: TXT record dta as a map of key/value pairs.
                 * device: The device running the advertised service.
                 */
                Log.d(TAG, "DnsSdTxtRecord available: " + record.toString());
                Log.d(TAG, "domain: " + fullDomain);
                Log.d(TAG, "device: \n" + device);
                Log.d(TAG, "record: \n" + record);
                Log.d(TAG, "device.address: " + device.deviceAddress);

                /// Client procedure (4.4). Step 1
                // TODO: decrypt the data and possibly parse it
                // Decrypting and parsing data here...
                //

                // TODO: "greybox" should be a constant defined somewhere. Maybe in WfdNetManagerService
                // TODO: if we have many services, I don't know if this is async and we can enter here
                //  while reading from it in `onDnsSdServiceAvailable`. So, we might end up reading
                //  wrong data. For now, we assume there is only one service.
                if (Objects.requireNonNull(record.get("name")).contains("greybox")) {
                    // TODO: need to check that every key exists before creating the object?
                    // The article recommends to store the info in a list
                    gosData.put(device.deviceAddress,
                            new ConnectionData(record.get("mac"),
                            record.get("ssid"),
                            record.get("pass"),
                            Integer.parseInt(Objects.requireNonNull(record.get("port")))));

                    // TODO: the article suggests to start a timer once we received a
                    //
                    //
                }
                //


            }
        };

        // TODO: I don't know if this callback is useful in our case. The connection information is
        //  in the TXT record. Here we don't have direct access to it in the arguments. Maybe I need
        //  to do the same as the example
        // This receives the actual description and connection information. The service response
        // listener uses the relationship device-name created in txtRecordListener to link the DNS
        // record with the corresponding service information.
        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName,
                                                String registrationType,
                                                WifiP2pDevice deviceInfo) {
                Log.d(TAG, "onDnsSdServiceAvailable");
                Log.d(TAG, " instanceName:     " + instanceName);       // service name: SERVICE_NAME (greybox_mesh)
                Log.d(TAG, " registrationType: " + registrationType);   // service type: SERVICE_TYPE + .local. (_nsdgreybox._tcp.local.)
                Log.d(TAG, " deviceInfo:       " + deviceInfo);         // device info (WifiP2pDevice)

                // TODO: why? this is from https://developer.android.com/training/connect-devices-wirelessly/nsd-wifi-direct
                // Update the device name with the human-friendly version from
                // the DnsTxtRecord, assuming one arrived.
//                deviceInfo.deviceName = names.containsKey(deviceInfo.deviceAddress) ?
//                        names.get(deviceInfo.deviceAddress) : deviceInfo.deviceName;

                // TODO: I won't have a UI element to display this, but it could be useful as a
                //  reference
                // Add to the custom adapter defined specifically for showing wifi devices.
//                WiFiDirectServicesList fragment = (WiFiDirectServicesList) getFragmentManager()
//                        .findFragmentById(R.id.frag_peerlist);
//                WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) fragment.getListAdapter());
//                adapter.add(deviceInfo);
//                adapter.notifyDataSetChanged();

                ///
                // Connect to the GO
                // TODO: I think this code should be in another class. Maybe in WfdNetManagerService
                ConnectionData connectionData = gosData.get(deviceInfo.deviceAddress);
                if (connectionData != null) {
                    // Builder used to build WifiP2pConfig objects for creating or joining a group.
                    WifiP2pConfig.Builder configBuilder = new WifiP2pConfig.Builder();

                    Log.d(TAG, "ConnectionData.getSsid(): " + connectionData.getSsid());
                    Log.d(TAG, "ConnectionData.getPass(): " + connectionData.getPass());

                    WifiP2pConfig config = configBuilder
                            .setNetworkName(connectionData.getSsid())
                            .setPassphrase(connectionData.getPass())
                            .build();

                    manager.connect(channel, config, new ActionListener() {

                        @Override
                        public void onSuccess() {
                            // TODO: I guess I can't use a `connectionData` since it will create an
                            //  implicit reference. Need to review
                            // TODO: I guess this would be still a security issue, so, this should be
                            //  just for debugging/testing
//                            Log.d(TAG, " Connected to: " + connectionData.getSsid()
//                                    + "\n" + connectionData.getDeviceAddress());
                            Log.d(TAG, " CONNECTED!");
                        }

                        @Override
                        public void onFailure(int errorCode) {
                            Log.d(TAG, "Failed to create group. Error code: " + errorCode);
                            // TODO: is there any way to retry creation of the group if the first attempt failed?

                            // TODO: this should be a function
                            switch (errorCode) {
                                case WifiP2pManager.P2P_UNSUPPORTED:
                                    Log.e(TAG, " Failed because Wi-Fi Direct is not supported on the device.");
                                    break;

                                case WifiP2pManager.ERROR:
                                    Log.e(TAG, " Failed due to an internal error.");
                                    break;

                                case WifiP2pManager.BUSY:
                                    Log.e(TAG, " Failed due to the framework is busy and is unable to attend the request.");
                                    break;
                            }
                        }
                    });
                }

            }
        };

        // Once both listeners are implemented, add them to the `WifiP2pManager` using the
        // `setDnsSdResponseListeners()` method.
        manager.setDnsSdResponseListeners(channel, servListener, txtRecordListener);

        Log.d(TAG, "registerListeners end");
    }

    /*
     * Add a request to the framework for the services we want to discover (those added with
     * `addLocalService()`). Calling `onSuccess()` or `onFailure()` just means whether the framework
     *  received the request.
     */
    private void addServiceRequest() {
        Log.d(TAG, "addServiceRequest start");

        if (serviceRequest == null) {
//            serviceRequest = WifiP2pDnsSdServiceRequest.newInstance(SERVICE_NAME, SERVICE_TYPE);
            serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        }

        manager.addServiceRequest(channel, serviceRequest, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, " addServiceRequest: Request to add services succeeded.");
                Log.d(TAG, " serviceRequest: " + serviceRequest);
                isAddServiceRequestMade = true;
            }

            @Override
            public void onFailure(int errorCode) {
                Log.d(TAG, " addServiceRequest: Request to add services failed.");
                printError(errorCode);
            }
        });

        Log.d(TAG, "addServiceRequest end");
    }


    /*
     * This is just to send a request to start the discovery of the service added with
     * `addLocalService()`. If `onSuccess()` is called, it just means that the request was processed,
     * it doesn't mean that the service was found.
     * If the service is found, it's processed on `onDnsSdTxtRecordAvailable()` and
     * `onDnsSdServiceAvailable()` callbacks.
     * A discovery process involves scanning for requested services for the purpose of establishing a
     * connection to a peer that supports an available service. The services to be discovered are
     * specified with calls to addServiceRequest(Channel, WifiP2pServiceRequest, ActionListener).
     */
    private void requestDiscoverServices() {
        manager.discoverServices(channel, new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, " discoverServices: Request to discover services succeeded.");
            }

            @Override
            public void onFailure(int errorCode) {
                Log.e(TAG, " discoverServices: Request to discover services failed.");
                printError(errorCode);
            }
        });
    }


    public void discoverServices() {
        Log.d(TAG, "discoverServices start");
        // Register the service only once
        // TODO: I think this should be one method call. Internally we determine if we should
        //  add the service
        Log.d(TAG, " isLocalServiceRegistered: " + isLocalServiceRegistered);
        Log.d(TAG, " isAddServiceRequestMade:  " + isAddServiceRequestMade);

//        if (!isAddServiceRequestMade) {
            registerListeners();
            addServiceRequest();
//        }
        requestDiscoverServices();
        Log.d(TAG, "discoverServices end");
    }

    // Helper method to interpret the WifiP2pManager errors
    private void printError(int errorCode) {
        switch (errorCode) {
            case WifiP2pManager.P2P_UNSUPPORTED:
                Log.e(TAG, " Failed because Wi-Fi Direct is not supported on the device.");
                break;

            case WifiP2pManager.ERROR:
                Log.e(TAG, " Failed due to an internal error.");
                break;

            case WifiP2pManager.BUSY:
                Log.e(TAG, " Failed due to the framework is busy and is unable to attend the request.");
                break;
        }
    }
}
///