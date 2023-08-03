package com.example.greybox.meshmessage;

/*
 * Data class
 */

import androidx.annotation.NonNull;

import com.example.greybox.MeshDevice;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class MeshMessage implements Serializable {

    /// NOTE: these fields are mentioned in the article
    private final UUID uuid;
    private MeshMessageType msgType;
    private ArrayList<String> visitedMacList = new ArrayList<>();   // NOTE: this seems to be used for clients acting as relays
    private Object payload;

    ///
    // This is not included in the article. In the article, it is inferred who is the recipient of the
    // message by ... . And in the article we can only send to one device or the whole mesh network,
    // here we try to send it to one or multiple devices (less than the whole network) but we are going
    // to start with only one
    private ArrayList<String> dstDevices;


    // --------------------------------------------------------------
    //  Constructors
    // --------------------------------------------------------------
    public MeshMessage(MeshMessageType msgType, Object payload, ArrayList<String> dstDevices) {
        this.uuid = UUID.randomUUID();
        this.msgType = msgType;
        this.payload = payload;
        this.dstDevices = dstDevices;
    }

    // --------------------------------------------------------------
    //  Getters and setters
    // --------------------------------------------------------------
    public UUID getUuid() {
        return uuid;
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

    public ArrayList<String> getDstDevices() {
        return dstDevices;
    }

    public void setDstDevices(ArrayList<String> endpoints) {
        this.dstDevices = endpoints;
    }

    public Object getData() {
        return payload;
    }

    public void setData(Object payload) {
        this.payload = payload;
    }

    // --------------------------------------------------------------


    @NonNull
    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append("uuid: ").append(uuid).append("\n");
        res.append("msgType: ").append(msgType.toString()).append("\n");
        res.append("payload: ");
        if (payload != null) {
            // Need to make some castings depending on the type
            switch (msgType) {
                case DATA_SINGLE_CLIENT:
                    // So far, the payload for this type is a string
                    res.append((String) payload).append("\n");
                    break;
                case CLIENT_LIST:
                    /// testing
//                    res.append((HashMap<String, MeshDevice>) payload).append("\n");
                    res.append((ArrayList<MeshDevice>) payload).append("\n");
                    ///
                    break;
                default:
                    res.append("Unknown payload type").append("\n");
                    res.append(payload);
                    break;
            }
        }
        return res.toString();
    }
}
