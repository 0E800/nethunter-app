package com.offsec.nethunter;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.view.View;
import android.widget.Switch;

public class NmapViewModel extends BaseObservable {

    private boolean showAdvanced = false;
    private boolean showOutput = false;

    public void advChecked(View view) {
        showAdvanced = ((Switch) view).isChecked();
        notifyPropertyChanged(BR.showAdvanced);
    }

    public void scanClicked() {
        showAdvanced = false;
        showOutput = true;
        notifyPropertyChanged(BR.showAdvanced);
        notifyPropertyChanged(BR.showOutput);
    }

    @Bindable
    public boolean isShowAdvanced() {
        return showAdvanced;
    }

    @Bindable
    public boolean isShowOutput() {
        return showOutput;
    }

}
