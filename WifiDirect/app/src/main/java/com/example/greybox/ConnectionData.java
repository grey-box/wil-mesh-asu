package com.example.greybox;

/*
 * This is just a data class to store information like the SSID, PASS, MAC, and so on
 */
public class ConnectionData {
    private String deviceAddress;
    private String ssid;
    private String pass;

    public ConnectionData(String deviceAddress, String ssid, String pass) {
        this.deviceAddress = deviceAddress;
        this.ssid = ssid;
        this.pass = pass;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }
}
