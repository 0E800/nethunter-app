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

import static android.R.attr.animationDuration;

public class ManaUpdateAdapter {
    private final LinearLayout parent;
    private final LayoutInflater layoutInflater;
    private final int animationStartDelay;
    private final int animationDuration;

    public ManaUpdateAdapter(LinearLayout parent, int animationStartDelay, int animationDuration) {
        this.parent = parent;
        this.layoutInflater = LayoutInflater.from(parent.getContext());
        this.animationStartDelay = animationStartDelay;
        this.animationDuration = animationDuration;
    }
    private ArrayList<ViewHolder> viewHolders = new ArrayList<>();
    private ArrayList<String> textList = new ArrayList<>();

    private final EventUpdateListener listener = new EventUpdateListener() {
        @Override
        public synchronized void onViewFadedOut(ViewHolder viewHolder) {
            parent.removeView(viewHolder.tv);
            textList.remove(viewHolder.text);
            viewHolders.remove(viewHolder);
        }
    };


    public void onTextUpdated(String text) {
        int ind = textList.indexOf(text);
        if (ind == -1) {
            ViewHolder vh = new ViewHolder(listener, text, animationStartDelay, animationDuration);
            vh.bind(layoutInflater, parent);
            textList.add(text);
            viewHolders.add(vh);
        } else {
            viewHolders.get(ind).resetAnimator();
        }
    }

    public void resetAll() {
        parent.removeAllViews();
        textList = new ArrayList<>();
        viewHolders = new ArrayList<>();
    }


    public interface EventUpdateListener {
        void onViewFadedOut(ViewHolder viewHolder);

    }



    public static class ViewHolder {
        private EventUpdateListener listener;
        public String text;
        private final int animationStartDelay;
        private final int animationDuration;
        TextView tv;
        ObjectAnimator animator;


        public ViewHolder(EventUpdateListener listener, String text, int animationStartDelay, int animationDuration) {

            this.listener = listener;
            this.text = text;
            this.animationStartDelay = animationStartDelay;
            this.animationDuration = animationDuration;
        }

        public synchronized TextView bind(LayoutInflater inflater, ViewGroup parent) {
            tv = (TextView) inflater.inflate(R.layout.mana_ssid_textview, parent, false);
            parent.addView(tv);
            tv.setText(text);
            animator = ObjectAnimator.ofFloat(tv, "alpha", 1f, 0f);
            animator.setStartDelay(animationStartDelay);
            animator.setDuration(animationDuration);
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
        }


    }


}
