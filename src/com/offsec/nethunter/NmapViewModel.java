package com.offsec.nethunter;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.view.View;
import android.widget.Switch;

public class NmapViewModel extends BaseObservable {

    private boolean showAdvanced = false;

    public void advChecked(View view) {
        showAdvanced = ((Switch) view).isChecked();
        notifyPropertyChanged(BR.showAdvanced);
    }

    @Bindable
    public boolean isShowAdvanced() {
        return showAdvanced;
    }
}
