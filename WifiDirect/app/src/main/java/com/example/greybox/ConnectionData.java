package com.example.greybox;

/*
 * This is just a data class to store information like the SSID, PASS, MAC, and so on
 */
public class ConnectionData {
    private String deviceAddress;
    private String ssid;
    private String pass;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    private int port;

    public ConnectionData(String deviceAddress, String ssid, String pass, int port) {
        this.deviceAddress = deviceAddress;
        this.ssid = ssid;
        this.pass = pass;
        this.port = port;
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
