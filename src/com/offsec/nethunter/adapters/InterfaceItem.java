package com.offsec.nethunter.adapters;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

public class InterfaceItem extends NethunterBaseItem {

    @StringRes
    private final int textResource;
    private final String textString;
    @StringRes
    private int buttonText;

    public InterfaceItem(int viewType, @StringRes  int textResource) {
        super(viewType);
        this.textResource = textResource;
        this.textString = null;
    }

    public InterfaceItem(int viewType, int textResource, int buttonText) {
        super(viewType);
        this.textResource = textResource;
        this.buttonText = buttonText;
        this.textString = null;
    }

    public InterfaceItem(int viewType, String textString) {
        super(viewType);
        this.textResource = -1;
        this.textString = textString;
    }

    public int getTextResource() {
        return textResource;
    }

    public String getTextString() {
        return textString;
    }

    public int getButtonText() {
        return buttonText;
    }

    public void setButtonText(int buttonText) {
        this.buttonText = buttonText;
    }
}
