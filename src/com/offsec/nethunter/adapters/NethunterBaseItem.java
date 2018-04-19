package com.offsec.nethunter.adapters;

import android.support.annotation.NonNull;

public abstract class NethunterBaseItem implements Comparable<NethunterBaseItem>{
    private final int viewType;

    public static final int NET_HEADING = 0;
    public static final int NET_ITEM = 1;
    public static final int HID_HEADING = 2;
    public static final int HID_SWITCH = 3;
    public static final int HID_ITEM = 4;
    public static final int BUSYBOX_HEADING = 5;
    public static final int BUSYBOX_ITEM = 6;
    public static final int KERNEL_HEADING = 7;
    public static final int KERNEL_ITEM = 8;
    public static final int NH_TERMINAL_HEADING = 9;
    public static final int NH_TERMINAL_ITEM = 10;
    public static final int EXTERNAL_IP_HEADING = 11;
    public static final int EXTERNAL_IP = 12;
    public static final int LOADING = -1;

    protected NethunterBaseItem(int viewType) {
        this.viewType = viewType;
    }

    @Override
    public int compareTo(@NonNull NethunterBaseItem other) {
        if (other.getViewType() == this.viewType) {
            return 0;
        } else if (other.getViewType() < this.viewType) {
            return 1;
        } else {
            return -1;
        }

    }

    public int getViewType() {
        return viewType;
    }
}
