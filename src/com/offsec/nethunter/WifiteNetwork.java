package com.offsec.nethunter;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

/**
 * Created by ilak on 12/11/17.
 */

public class WifiteNetwork extends BaseObservable {
    private String essid;


    @Bindable
    public String getEssid() {
        return essid;
    }

    public void setEssid(String essid) {
        this.essid = essid;
        notifyPropertyChanged(BR.essid);
    }
}
