package com.example.greybox.meshmessage;

import java.io.Serializable;

public class FilePayload implements Serializable {
    private byte[] fileData;
    private String fileName;

    public FilePayload(byte[] fileData, String fileName) {
        this.fileData = fileData;
        this.fileName = fileName;
    }

    // Getters
    public byte[] getFileData() {
        return fileData;
    }

    public String getFileName() {
        return fileName;
    }
}

