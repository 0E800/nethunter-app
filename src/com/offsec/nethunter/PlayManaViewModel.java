package com.offsec.nethunter;

import android.databinding.BaseObservable;
import android.databinding.Bindable;


public class PlayManaViewModel extends BaseObservable {
    private int numConnected = 0;
    private boolean justReset = false;
    private String statusText = "Status";
    private boolean initialized;
    private boolean started;

    @Bindable
    public String getNumConnected() {
        return String.valueOf(numConnected);
    }

    public void incrementConnected() {
        numConnected++;
        notifyPropertyChanged(BR.numConnected);
    }

    public void decrementConnected() {
        if (!justReset) {
            numConnected--;
            notifyPropertyChanged(BR.numConnected);
        }
    }

    public void resetClients() {
        numConnected = 0;
        justReset = true;
        notifyPropertyChanged(BR.numConnected);
    }

    public void setJustReset(boolean justReset) {
        this.justReset = justReset;
    }

    @Bindable
    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
        notifyPropertyChanged(BR.statusText);
    }

    public void setStarted(boolean started) {
        if (started) {
            this.statusText = "Starting...";
            this.started = true;
            notifyPropertyChanged(BR.statusText);
            notifyPropertyChanged(BR.initialized);
        }
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
        if (initialized) {
            this.statusText = "Running";
            notifyPropertyChanged(BR.initialized);
            notifyPropertyChanged(BR.statusText);
        }

    }


    @Bindable
    public boolean isInitialized() {
        return initialized;
    }


    public void setError(String errorMessage) {
        this.statusText = errorMessage;
        notifyPropertyChanged(BR.statusText);
    }

    @Bindable
    public boolean isStarted() {
        return started;
    }
}
