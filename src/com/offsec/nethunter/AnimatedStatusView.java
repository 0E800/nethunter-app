package com.offsec.nethunter;


import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.TranslateAnimation;
import android.widget.ProgressBar;
import android.widget.TextView;

public class AnimatedStatusView {

    private final ViewGroup parentView;
    private final ProgressBar progressBar;
    private final TextView statusView;
    private final ObjectAnimator slideIn;

    public AnimatedStatusView(ViewGroup parentView, ProgressBar progressBar, TextView statusView) {
        this.parentView = parentView;
        this.progressBar = progressBar;
        this.statusView = statusView;

        slideIn = ObjectAnimator.ofFloat(parentView, View.TRANSLATION_Y,
                -parentView.getHeight(), 0);
        slideIn.setDuration(1000);
    }

    public void startAnimation() {
            slideIn.start();
            statusView.setText("Starting up...");
        progressBar.setVisibility(View.VISIBLE);
    }

    public void manaInitialized(String text, boolean error) {
        progressBar.setVisibility(View.INVISIBLE);
        statusView.setText(text);
        if (error) {
            statusView.setTextColor(Color.RED);
        }
        parentView.postDelayed(new Runnable() {
            @Override
            public void run() {
                slideIn.reverse();
                statusView.animate()
                        .alpha(0)
                        .setDuration(1000)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                statusView.setTextColor(Color.BLACK);
                            }
                        })
                        .start();
            }
        }, 3000);
    }


}
