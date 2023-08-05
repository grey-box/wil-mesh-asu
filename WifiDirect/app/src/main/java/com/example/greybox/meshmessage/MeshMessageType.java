package com.example.greybox.meshmessage;

public enum MeshMessageType {

    DATA_SINGLE_CLIENT, // For now, "data" means text but in the future it might be raw data
    DATA_ALL_NETWORK,
    CMD_SINGLE_CLIENT,
    CMD_ALL_NETWORK,

    //
    NEW_CLIENT_SOCKET_CONNECTION,
    CLIENT_LIST,
    //
}
