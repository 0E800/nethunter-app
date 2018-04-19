package com.offsec.nethunter;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.InverseBindingAdapter;
import android.databinding.InverseBindingMethod;
import android.databinding.InverseBindingMethods;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MITMFViewModel extends BaseObservable {


    private boolean intervalChecked = false;
    private boolean injectJS;
    private boolean injectHTML;
    private boolean injectHTMLPayload;
    private boolean injectHtmlURLEmpty = true;
    private boolean injectJSEnabled = true;
    private boolean injectionEnabled = false;
    private boolean shellShockEnabled = false;
    private boolean spoofEnabled = false;
    private boolean responderChecked = false;
    private boolean injectJSEmpty = true;



    public void setIntervalChecked(boolean intervalChecked) {
        this.intervalChecked = intervalChecked;
    }

    @JsonIgnore
    public void clickInject(View view) {
        injectionEnabled = ((CheckBox) view).isChecked();
        notifyPropertyChanged(BR.injectionEnabled);
        notifyEnabledChanged();
    }

    @JsonIgnore
    public void clickSpoof(View view) {
        spoofEnabled = ((CheckBox) view).isChecked();
        notifyPropertyChanged(BR.spoofEnabled);
    }

    @Bindable
    public boolean isInjectionEnabled() {
        return injectionEnabled;
    }

    @Bindable
    public boolean isIntervalChecked() {
        return intervalChecked;
    }


    public void setInjectJS(boolean injectJS) {
        this.injectJS = injectJS;
        notifyPropertyChanged(BR.injectJS);
    }


    @Bindable
    public boolean isInjectJS() {
        return injectJS;
    }

    @JsonIgnore
    public void clickInjectJS(View view) {
        injectJS = ((CheckBox) view).isChecked();
        notifyPropertyChanged(BR.injectJS);
    }

    public void setInjectHTML(boolean injectHTML) {
        this.injectHTML = injectHTML;
        notifyPropertyChanged(BR.injectHTML);
    }

    @Bindable
    public boolean isInjectHTML() {
        return injectHTML;
    }


    @JsonIgnore
    public void clickInjectHTML(View view) {
        injectHTML = ((CheckBox) view).isChecked();
        notifyPropertyChanged(BR.injectHTML);
    }

    public void setInjectHTMLPayload(boolean injectHTMLPayload) {
        this.injectHTMLPayload = injectHTMLPayload;
        notifyPropertyChanged(BR.injectHTMLPayload);
    }

    @Bindable
    public boolean isInjectHTMLPayload() {
        return injectHTMLPayload;
    }
    @JsonIgnore
    public void clickInjectHTMLPayload(View view) {
        injectHTMLPayload = ((CheckBox) view).isChecked();
        notifyPropertyChanged(BR.injectHTMLPayload);
    }
    public TextWatcher injectJSWatcher = new TextWatcherAdapter() {
        @Override
        public void afterTextChanged(Editable s) {
            injectJSEmpty = s.toString().equals("");
            notifyEnabledChanged();
        }
    };


    private void notifyEnabledChanged() {
        notifyPropertyChanged(BR.injectJSEnabled);
        notifyPropertyChanged(BR.injectHtmlUrlEnabled);
        notifyPropertyChanged(BR.injectHtmlEnabled);
    }

    public TextWatcher injectHtmlUrlWatcher = new TextWatcherAdapter() {
        @Override
        public void afterTextChanged(Editable s) {
            injectHtmlURLEmpty = s.toString().equals("");
            notifyEnabledChanged();
        }
    };

    private boolean injectHtmlEmpty = true;

    public TextWatcher injectHtmlWatcher = new TextWatcherAdapter() {
        @Override
        public void afterTextChanged(Editable s) {
            injectHtmlEmpty = s.toString().equals("");
            notifyEnabledChanged();
        }
    };

    @Bindable
    public boolean isInjectJSEnabled() {
        return injectHtmlURLEmpty && injectHtmlEmpty && injectionEnabled;
    }

    @Bindable
    public boolean isInjectHtmlUrlEnabled() {
        return injectJSEmpty && injectHtmlEmpty && injectionEnabled;
    }

    @Bindable
    public boolean isInjectHtmlEnabled() {
        return injectJSEmpty && injectHtmlURLEmpty && injectionEnabled;
    }


    @Bindable
    public boolean isShellShockEnabled() {
        return shellShockEnabled && spoofEnabled;
    }

    public void setShellShockEnabled(boolean enabled) {
        shellShockEnabled = enabled;
        notifyPropertyChanged(BR.shellShockEnabled);
    }

    @Bindable
    public boolean isSpoofEnabled() {
        return spoofEnabled;
    }

    public void setInjectHtmlURLEmpty(boolean injectHtmlURLEmpty) {
        this.injectHtmlURLEmpty = injectHtmlURLEmpty;
    }

    public void setInjectJSEnabled(boolean injectJSEnabled) {
        this.injectJSEnabled = injectJSEnabled;
    }

    public void setInjectionEnabled(boolean injectionEnabled) {
        this.injectionEnabled = injectionEnabled;
    }

    public void setSpoofEnabled(boolean spoofEnabled) {
        this.spoofEnabled = spoofEnabled;
    }

    public void setInjectJSEmpty(boolean injectJSEmpty) {
        this.injectJSEmpty = injectJSEmpty;
    }

    public void setInjectJSWatcher(TextWatcher injectJSWatcher) {
        this.injectJSWatcher = injectJSWatcher;
    }

    public void setInjectHtmlUrlWatcher(TextWatcher injectHtmlUrlWatcher) {
        this.injectHtmlUrlWatcher = injectHtmlUrlWatcher;
    }

    public void setInjectHtmlEmpty(boolean injectHtmlEmpty) {
        this.injectHtmlEmpty = injectHtmlEmpty;
    }

    public void setInjectHtmlWatcher(TextWatcher injectHtmlWatcher) {
        this.injectHtmlWatcher = injectHtmlWatcher;
    }

    @Bindable
    public boolean isResponderChecked() {
        return responderChecked;
    }

    public void setResponderChecked(boolean responderChecked) {
        this.responderChecked = responderChecked;
        notifyPropertyChanged(BR.responderChecked);
    }

    private static class TextWatcherAdapter implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }


    @JsonIgnore
    public void responderClicked(View view) {
        responderChecked = ((CheckBox) view).isChecked();
        notifyPropertyChanged(BR.responderChecked);

    }


    public void onClick(View view) {

    }

    @InverseBindingAdapter(attribute = "android:checked", event = "android:checked")
    @JsonIgnore
    public static boolean getViewChecked(CheckBox view) {
        return view.isChecked();
    }

}
