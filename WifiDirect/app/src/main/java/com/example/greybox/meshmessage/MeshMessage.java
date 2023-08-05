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
    private final UUID msgId;
    private MeshMessageType msgType;
    private ArrayList<UUID> visitedDevices = new ArrayList<>();   // NOTE: this seems to be used for clients acting as relays
    private Object payload;

    ///
    // This is not included in the article. Authors don't specify how to send a message to a specific device
    private ArrayList<UUID> dstDevices;


    // --------------------------------------------------------------
    //  Constructors
    // --------------------------------------------------------------
    public MeshMessage(MeshMessageType msgType, Object payload, ArrayList<UUID> dstDevices) {
        this.msgId = UUID.randomUUID();
        this.msgType = msgType;
        this.payload = payload;
        this.dstDevices = dstDevices;
    }

    // --------------------------------------------------------------
    //  Getters and setters
    // --------------------------------------------------------------
    public UUID getMsgId() {
        return msgId;
    }

    public MeshMessageType getMsgType() {
        return msgType;
    }

    public ArrayList<UUID> getVisitedDevices() {
        return visitedDevices;
    }

    public void setVisitedDevices(ArrayList<UUID> visitedDevices) {
        this.visitedDevices = visitedDevices;
    }

    public ArrayList<UUID> getDstDevices() {
        return dstDevices;
    }

    public Object getData() {
        return payload;
    }

    // --------------------------------------------------------------


    @NonNull
    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append("uuid: ").append(msgId).append("\n");
        res.append("msgType: ").append(msgType.toString()).append("\n");
        res.append("payload: ");
        if (getData() != null) {
            // Need to make some castings depending on the type
            switch (msgType) {
                case DATA_SINGLE_CLIENT:
                    // So far, the payload for this type is a string
                    res.append((String) getData()).append("\n");
                    break;
                case CLIENT_LIST:
                    /// testing
//                    res.append((HashMap<String, MeshDevice>) payload).append("\n");
                    res.append((ArrayList<MeshDevice>) getData()).append("\n");
                    ///
                    break;
                default:
                    // TODO: maybe the casting is not required. At least if the object implements toString()
                    res.append("Unknown payload type").append("\n");
                    res.append(getData());
                    break;
            }
        }
        return res.toString();
    }
}
