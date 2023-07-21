package com.example.greybox.meshmessage;

/*
 * Data class
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class MeshMessage implements Serializable {

    /// TODO: these fields are mentioned in the article. But I'm not sure they will be used
    private UUID uuid;
    private MeshMessageType msgType;
    private ArrayList<String> visitedMacList;   // NOTE: this seems to be used for clients acting as relays
    private byte[] data;    // the payload
    ///
    private ArrayList<String> endpoints;


    // --------------------------------------------------------------
    //  Getters and setters
    // --------------------------------------------------------------
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public MeshMessageType getMsgType() {
        return msgType;
    }

    public void setMsgType(MeshMessageType msgType) {
        this.msgType = msgType;
    }

    public ArrayList<String> getVisitedMacList() {
        return visitedMacList;
    }

    public void setVisitedMacList(ArrayList<String> visitedMacList) {
        this.visitedMacList = visitedMacList;
    }

    public ArrayList<String> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(ArrayList<String> endpoints) {
        this.endpoints = endpoints;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    // --------------------------------------------------------------
}
