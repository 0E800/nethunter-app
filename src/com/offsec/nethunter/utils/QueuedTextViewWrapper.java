package com.offsec.nethunter.utils;

import android.widget.TextView;

public class QueuedTextViewWrapper {
    private TextView textView;
    private static int DISPLAY_TIME = 2000;

    public QueuedTextViewWrapper(TextView textView) {
        this.textView = textView;
    }

    public void setText(String text) {

    }

    public static void setDisplayTime(int displayTime) {
        DISPLAY_TIME = displayTime;
    }
}
