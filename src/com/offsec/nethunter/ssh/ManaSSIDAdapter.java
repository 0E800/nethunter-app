package com.offsec.nethunter.ssh;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.offsec.nethunter.R;

import java.util.ArrayList;

public class ManaSSIDAdapter {
    private final LinearLayout parent;
    private final LayoutInflater layoutInflater;

    public ManaSSIDAdapter(LinearLayout parent) {
        this.parent = parent;
        this.layoutInflater = LayoutInflater.from(parent.getContext());
    }
    private ArrayList<ViewHolder> viewHolders = new ArrayList<>();
    private ArrayList<String> ssids = new ArrayList<>();

    private final EventUpdateListener listener = new EventUpdateListener() {
        @Override
        public synchronized void onViewFadedOut(ViewHolder viewHolder) {
            parent.removeView(viewHolder.tv);
            ssids.remove(viewHolder.ssid);
            viewHolders.remove(viewHolder);
        }
    };


    public void ssidUpdate(String ssid) {
        int ind = ssids.indexOf(ssid);
        if (ind == -1) {
            ViewHolder vh = new ViewHolder(listener, ssid);
            vh.bind(layoutInflater, parent);
            ssids.add(ssid);
            viewHolders.add(vh);
        } else {
            viewHolders.get(ind).resetAnimator();
        }
    }

    public void resetAll() {
        parent.removeAllViews();
        ssids = new ArrayList<>();
        viewHolders = new ArrayList<>();
    }


    public interface EventUpdateListener {
        void onViewFadedOut(ViewHolder viewHolder);

    }



    public static class ViewHolder {
        private static final long START_DELAY = 2000;
        private static final int FADE_DURATION = 5000;
        private EventUpdateListener listener;
        public String ssid;
        TextView tv;
        ObjectAnimator animator;


        public ViewHolder(EventUpdateListener listener, String ssid) {

            this.listener = listener;
            this.ssid = ssid;
        }

        public synchronized TextView bind(LayoutInflater inflater, ViewGroup parent) {
            tv = (TextView) inflater.inflate(R.layout.mana_ssid_textview, parent, false);
            parent.addView(tv);
            tv.setText(ssid);
            animator = ObjectAnimator.ofFloat(tv, "alpha", 1f, 0.1f);
            animator.setStartDelay(START_DELAY);
            animator.setDuration(FADE_DURATION);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                        listener.onViewFadedOut(ViewHolder.this);
                }

                @Override
                public void onAnimationStart(Animator animation) {
                }

            });
            animator.start();
            return tv;
        }

        public void resetAnimator() {
            animator.start();
//            animator.start();
        }


    }


}
