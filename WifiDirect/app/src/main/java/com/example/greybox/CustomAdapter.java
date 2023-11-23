package com.example.greybox;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private ArrayList<MeshDevice> devices;
    private static final int GROUP_CHAT_POSITION = 0;

    public CustomAdapter(Context context, ArrayList<MeshDevice> devices) {
        this.inflater = LayoutInflater.from(context);
        this.devices = devices;
    }

    @Override
    public int getCount() {
        // +1 pour la ligne "Group chat"
        return devices.size() + 1;
    }

    @Override
    public Object getItem(int position) {
        if (position == GROUP_CHAT_POSITION) {
            return "Group chat";
        } else {
            return devices.get(position - 1);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        if (position == GROUP_CHAT_POSITION) {
            textView.setText("Group chat");
        } else {
            MeshDevice device = devices.get(position - 1);
            textView.setText(device.getDeviceName()); // ou toute autre propriété de MeshDevice
        }

        return convertView;
    }
}
