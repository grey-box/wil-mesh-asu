package com.example.greybox;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.util.Log;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;


/// PE_AUTO_CONNECT
// TODO: I guess this class will disappear, I didn't know there was a different way to work with
//  local networks and Wi-Fi Direct, so, I guess this will serve as a template for the
//  WfdDnsSdManager class.
//  Remove it if necessary.

public class DnsSdManager {
    private static final String TAG = "DNSSDManager";

    NsdManager nsdManager;
    NsdManager.RegistrationListener registrationListener;
    String serviceName;
    NsdServiceInfo serviceInfo; // this is the service we are interested in
    int port;
    InetAddress hostAddress;

    Activity activity;

    // The video recommends to use the Bluetooth name and it's recommended to be unique, but it doesn't
    // really matter since Android will handle any conflicts by appending a number counter to the name.
    // NOTE: does this mean that we could have a problem if we expect a fixed name? Check if we ever
    //  use the service name other than for registration.
    // The name is subject to change based on conflicts with other services advertised on the same network.
    final String SERVICE_NAME = "Grey-box_Mesh";
    // NOTE: in the video is "_myapp.tcp.". Check if this is a problem since there is a StackOverflow
    //  question about something similar
    //  https://www.youtube.com/watch?v=oi_ARV_I8Dc, time 4:41
    //  https://stackoverflow.com/questions/53510192/android-nsd-why-service-type-dont-match
    final String SERVICE_TYPE = "_nsdgreybox._tcp."; // The service type uniquely identifies the service your device is advertising

    // TODO: This list is usually used to present the devices to the user. Maybe we don't need it
    //  since we want to do the connection automatically
    List<NsdServiceInfo> discoveredServices = new ArrayList<>();

    // The listener will get a callback for each device found. Here we handle any errors found.
    // TODO: review best practices about initializing listeners.
    //  1. initialization is done at class level?
    //  2. initialization is done inside a method?
    NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {

        @Override
        public void onDiscoveryStarted(String s) {
            Log.d(TAG, "Service discovery started");
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, serviceType + " service discovery failed. Error code: " + errorCode);
            // TODO: should we retry as in the onStopDiscoveryFailed() method? maybe we need a static
            //  attribute to keep a count on retries
            nsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
            // NOTE: maybe the most important method. It's here where we find out about all the
            //  services and the devices that we are potentially interested in and store them in the
            //  list

            // TODO: implement the real thing
            // A service was found! Do something with it.
            Log.d(TAG, "Service discovery success: " + nsdServiceInfo);

            // Verify if it's a type of service the application can connect to.
            if (!nsdServiceInfo.getServiceType().equals(SERVICE_TYPE)) {
                Log.d(TAG, "Unknown Service Type: " + nsdServiceInfo.getServiceType());
            }
            // Determine if the device just picked up its own broadcast (which is valid).
            else if (nsdServiceInfo.getServiceName().equals(serviceName)) {
                Log.d(TAG, "Same machine: " + serviceName);
            }
            // We found the service we are interested in.
            // NOTE: here we use "contains()" instead of "equals()" since the name should include the
            //  name with a number added by the OS due to name conflicts
            else if (nsdServiceInfo.getServiceName().contains(SERVICE_NAME)) {
                nsdManager.resolveService(nsdServiceInfo, resolveListener);
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            // TODO: Implement the real thing
            Log.e(TAG, "service lost: " + nsdServiceInfo);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discovery stopped: " + serviceType);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, serviceType + " service resolve failed. Error code: " + errorCode);
            // TODO: is this code retrying indefinitely if it fails?  maybe we need a static
            //  attribute to keep a count on retries
            nsdManager.stopServiceDiscovery(this);
        }

    };

    // In this listener is where we find out the IP address and port number of the device we want to
    // connect to.
    // After that, we use a standard socket to make the connection
    NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
        @Override
        public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
            // Use the error code to debug
            Log.e(TAG, nsdServiceInfo.getServiceName() +
                    " service resolve failed. Error code: " + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {

            Log.d(TAG, nsdServiceInfo.getServiceName() + " service resolved.");

            if (serviceInfo.getServiceName().equals(serviceName)) {
                // Should not enter here since we checked this in the discovery listener
                Log.d(TAG, "Same IP.");
                return;
            }

            serviceInfo = nsdServiceInfo;
            port = serviceInfo.getPort();
            hostAddress = serviceInfo.getHost();

            // Once we have the information, we go ahead and connect to the device using a standard
            // socket
        }
    };


    // ---------------------------------------------------------------------------------------------
    //  Constructors
    // ---------------------------------------------------------------------------------------------
    public DnsSdManager(Activity activity) {
        this.activity = activity;
    }


    // ---------------------------------------------------------------------------------------------
    //  Methods
    // ---------------------------------------------------------------------------------------------
    public void registerService(int port) {
        // Create the NsdServiceInfo object, and populate it. This object provides all the info that
        // other devices on the network will use then they're deciding whether or not to connect to
        // to your device
//        NsdServiceInfo serviceInfo = new NsdServiceInfo();
//        serviceInfo.setServiceName(SERVICE_NAME);
//        serviceInfo.setServiceType(SERVICE_TYPE);
//        serviceInfo.setPort(port);

        nsdManager = (NsdManager) activity.getSystemService(Context.NSD_SERVICE);

        // NOTE: I guess I need to call the initialization of the registration listener
        // TODO: if we just initialize this, wouldn't it be better to have it already when creating
        //  the object? I mean, as an attribute. See my question in the other listeners above
        initializeRegistrationListener();

        // Register so Android starts broadcasting the information
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);

    }

    public void initializeRegistrationListener() {

        // TODO: review if initializing here (inside a method) is a good idea
        registrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                // We need to request the name in case the system changed it to resolve a conflict
                // with another device in the network
                serviceName = nsdServiceInfo.getServiceName();
                // NOTE: here is the moment to update the UI if needed (in case we display the name
                //  of the service). But maybe our app won't need this.
                Log.d(TAG, serviceName + " service registered.");
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                Log.e(TAG, nsdServiceInfo.getServiceName() +
                        " service registration failed. Error code: " + errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
                Log.d(TAG, serviceName + " service unregistered.");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
                Log.e(TAG, nsdServiceInfo.getServiceName() +
                        " service deletion failed. Error code: " + errorCode);
            }
        };
    }

    // TODO: I think this has to be on the app object. Or maybe not if we want to simplify it's use
    //  here we hide the arguments required to the user of our class
    public void discoverService () {
        // NOTE: there is a version of this method that accepts an Executor
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void resolveService(NsdServiceInfo serviceInfo) {
        // Asynchronous call. A little bit expensive
        // NOTE: there is a version of this method that accepts an Executor
        nsdManager.resolveService(serviceInfo, resolveListener);
    }

    public void tearDown() {
        // It's very important to call this method when our application exits (either onPause or
        // onDestroy). This makes Android stop advertising the service to other devices.
        // Also stop the service discovery since it's expensive. It must not run unchecked.
        nsdManager.unregisterService(registrationListener);
        nsdManager.stopServiceDiscovery(discoveryListener);
    }
}

///