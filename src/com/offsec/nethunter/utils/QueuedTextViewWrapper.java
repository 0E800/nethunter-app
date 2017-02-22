package com.offsec.nethunter.utils;

import android.support.annotation.IntegerRes;
import android.support.annotation.Nullable;
import android.widget.TextView;

import java.util.LinkedList;

public class QueuedTextViewWrapper {
    private TextView textView;
    private static int DISPLAY_TIME = 2000;
    private LinkedList<String> queuedText = new LinkedList<>();
    private LinkedList<Integer> queuedColor = new LinkedList<>();

    private boolean holdForDelay = false;

    public QueuedTextViewWrapper(TextView textView) {
        this.textView = textView;
    }

    public void setText(String text, @Nullable Integer color) {
        if (!holdForDelay) {
            textView.setText(text);
            if (color != null) {
                textView.setTextColor(color);
            }
        } else {
            queuedText.push(text);
            if (color == null) {
                color = -1;
            }
            queuedColor.push(color);
        }
        textView.postDelayed(textAfterDelay, DISPLAY_TIME);
    }

    public static void setDisplayTime(int displayTime) {
        DISPLAY_TIME = displayTime;
    }
    private final Runnable textAfterDelay = new Runnable() {
        @Override
        public void run() {
            if (queuedText.peek() != null) {
                textView.setText(queuedText.pop());
                if (queuedColor.peek() != -1) {
                       textView.setTextColor(queuedColor.pop());
                } else {
                    queuedColor.pop();
                }
                textView.postDelayed(this, DISPLAY_TIME);
            } else {
                textView.setText("");
            }
        }
    };
}
