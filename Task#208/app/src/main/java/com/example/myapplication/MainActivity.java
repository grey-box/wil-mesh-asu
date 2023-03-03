package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    //JSGARVEY  Add Button Objects
    Button btnOnOff, btnDiscover, btnSend;
    ListView listView;
    TextView read_msg_box, connectionStatus;
    EditText writeMSg;

    //JSGARVEY Create Wifi Manager
    WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        exqListener();

    }

    private void exqListener(){

        if(wifiManager.isWifiEnabled()){
            btnOnOff.setText("WIFI ON");
        }else{
            btnOnOff.setText("WIFI OFF");
        }

        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(false);
                    btnOnOff.setText("WIFI OFF");
                }else{
                    wifiManager.setWifiEnabled(true);
                    btnOnOff.setText("WIFI ON");
                }
            }
        });
    }

    private void initialWork() {
        //JSGARVEY create button objects
        btnOnOff=(Button) findViewById(R.id.onOff);
        btnDiscover=(Button) findViewById(R.id.discover);
        btnSend=(Button) findViewById(R.id.sendButton);
        listView=(ListView) findViewById(R.id.peerListView);
        read_msg_box=(TextView) findViewById(R.id.readMsg);
        connectionStatus=(TextView) findViewById(R.id.connectionStatus);
        writeMSg=(EditText) findViewById(R.id.writeMsg);

        //JSGARVEY
        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


    }
}