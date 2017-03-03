package com.offsec.nethunter;

import android.databinding.BaseObservable;
import android.databinding.Bindable;


public class PlayManaViewModel extends BaseObservable {
    private int numConnected = 0;

    @Bindable
    public String getNumConnected() {
        return String.valueOf(numConnected);
    }

    public void incrementConnected() {
        numConnected++;
        notifyPropertyChanged(BR.numConnected);
    }

    public void decrementConnected() {
        numConnected--;
        notifyPropertyChanged(BR.numConnected);
    }

    public void resetClients() {
        numConnected = 0;
        notifyPropertyChanged(BR.numConnected);
    }
}
