package com.offsec.nethunter.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import com.offsec.nethunter.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NethunterInterfaceAdapter<T extends InterfaceItem> extends BaseAdapter {

    private final View.OnClickListener switchListener;
    private final View.OnClickListener externalIpListener;
    private final LayoutInflater layoutInflater;
    private final List<T> interfaceItems;
    private TextView externalIpLabel;

    public NethunterInterfaceAdapter(@NonNull Context context,
                                     View.OnClickListener switchListener,
                                     View.OnClickListener externalIpListener) {
        this.switchListener = switchListener;
        this.externalIpListener = externalIpListener;
        this.layoutInflater = LayoutInflater.from(context);
        this.interfaceItems = new ArrayList<>();
    }

    public void add(@Nullable T item) {
        if (item != null) {
            interfaceItems.add(item);
            Collections.sort(interfaceItems);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        InterfaceItem item = interfaceItems.get(position);
        if (item != null) {
            return item.getViewType();
        } else {
            return -1;
        }
    }

    @Override
    public int getCount() {
        return interfaceItems.size();
    }

    @Override
    public T getItem(int position) {
        return interfaceItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        InterfaceItem item = getItem(position);
        if (item != null) {
            return populateViewForType(item, parent);
        } else {
            return null;
        }

    }

    private View populateViewForType(InterfaceItem item, ViewGroup parent) {
        switch (item.getViewType()) {
            case InterfaceItem.NET_HEADING:
            case InterfaceItem.HID_HEADING:
            case InterfaceItem.BUSYBOX_HEADING:
            case InterfaceItem.KERNEL_HEADING:
            case InterfaceItem.NH_TERMINAL_HEADING:
            case InterfaceItem.EXTERNAL_IP_HEADING:
                TextView heading = (TextView) layoutInflater
                        .inflate(R.layout.nethunter_item_heading, parent, false);
                heading.setText(item.getTextResource());
                return heading;
            case InterfaceItem.NET_ITEM:
            case InterfaceItem.HID_ITEM:
            case InterfaceItem.BUSYBOX_ITEM:
            case InterfaceItem.KERNEL_ITEM:
            case InterfaceItem.NH_TERMINAL_ITEM:
                TextView subItem = (TextView) layoutInflater
                        .inflate(R.layout.nethunter_item, parent, false);
                int textResource = item.getTextResource();
                if (textResource == -1) {
                    subItem.setText(item.getTextString());
                } else {
                    subItem.setText(item.getTextResource());
                }
                return subItem;
            case InterfaceItem.HID_SWITCH:
                Switch mSwitch = (Switch) layoutInflater
                        .inflate(R.layout.nethunter_item_switch, parent, false);
                mSwitch.setOnClickListener(switchListener);
                mSwitch.setText(item.getTextResource());
                return mSwitch;
            case InterfaceItem.EXTERNAL_IP:
                View layout = layoutInflater
                        .inflate(R.layout.nethunter_item_external_ip, parent, false);
                externalIpLabel = layout.findViewById(R.id.text);
                externalIpLabel.setText(item.getTextResource());
                Button button = layout.findViewById(R.id.external_ip_button);
                button.setOnClickListener(externalIpListener);
                return layout;
            case InterfaceItem.LOADING:
                return null;
            default:
                return null;

        }
    }

    public void setExternalIPText(String text) {
        if (externalIpLabel != null) {
            externalIpLabel.setText(text);
        }
    }

    @Override
    public int getViewTypeCount() {
        return 14;
    }

    public void clear() {
        this.interfaceItems.clear();
        notifyDataSetChanged();

    }
}
