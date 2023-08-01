package com.example.greybox;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.net.SocketException;
import java.net.UnknownHostException;

// TODO: how to deal with many groups? Maybe a client sends it to its GO, and the GO decides if
//  it should send it to all other GOs he reaches to, and those GO will evaluate if they have access
//  to the desired client or if it repeats the process.

// TODO: we should model the roles of GroupOwner and Client. Maybe with interfaces.
//  These interfaces should be implemented by a class that maybe inherits from MeshDevice.
//  The GroupOwner interface should have the methods to notify clients of some event.
public class MeshDevice implements Serializable {
    private static final String TAG = "MeshDevice";

    public String macAddress;
//    public String localIpAddress;
    public String name = "";
    public ObjectSocketCommunication socketComm = null;   // TODO: we now use ObjectSocketCommunication.
    public boolean isGo = false;

    // --------------------------------------------------------------------------------------------
    //  Constructors
    // --------------------------------------------------------------------------------------------
    public MeshDevice(String macAddress, String name, ObjectSocketCommunication sc) {
        this.macAddress = macAddress;
//        this.localIpAddress = localIpAddress;
        this.name = name;
        this.socketComm = sc;
    }

    public MeshDevice(ObjectSocketCommunication sc) {
        this.socketComm = sc;
        Log.d(TAG, " socketComm: " + sc);
        Log.d(TAG, " socketComm.socket: " + sc.getSocket());
        Log.d(TAG, " getLocalAddress: " + sc.getSocket().getLocalAddress());
        try {
            this.macAddress = WfdNetManagerService.getMacFromLocalIpAddress(sc.getSocket().getLocalAddress());
            Log.d(TAG, " macAddress: " + this.macAddress);
        } catch (UnknownHostException | SocketException e) {
            Log.e(TAG, "Couldn't get the MAC address from local IPAddress", e);
            throw new RuntimeException(e);
        }
    }

    public MeshDevice(String macAddress, String name) {
        this.macAddress = macAddress;
//        this.localIpAddress = localIpAddress;
        this.name = name;
    }

    // --------------------------------------------------------------------------------------------
    //  Methods
    // --------------------------------------------------------------------------------------------
    @NonNull
    @Override
    public String toString() {
        return "MeshDevice" +
                "  macAddress: " + macAddress +
                "  name: " + name +
                "  socketComm: " + socketComm +
                "  isGo: " + isGo;
    }
}