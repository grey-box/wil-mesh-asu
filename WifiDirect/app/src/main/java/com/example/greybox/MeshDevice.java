package com.example.greybox;

import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;


public class MeshDevice implements Serializable {
    private static final String TAG = "MeshDevice";

    private UUID deviceId;
    private String deviceName = "";
    private ObjectSocketCommunication socketComm = null;
    private boolean isGo = false;

    // --------------------------------------------------------------------------------------------
    //  Constructors
    // --------------------------------------------------------------------------------------------
    public MeshDevice(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public MeshDevice(UUID deviceId, String deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }

    public MeshDevice(UUID deviceId, String deviceName, ObjectSocketCommunication sc) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.socketComm = sc;
    }

    // --------------------------------------------------------------------------------------------
    //  Methods
    // --------------------------------------------------------------------------------------------
    @NonNull
    @Override
    public String toString() {
        return "MeshDevice" +
                "\n deviceId:   " + deviceId +
                "\n deviceName: " + deviceName +
                "\n socketComm: " + socketComm +
                "\n isGo:       " + isGo;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public ObjectSocketCommunication getSocketComm() {
        return socketComm;
    }

    public boolean isGo() {
        return isGo;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}