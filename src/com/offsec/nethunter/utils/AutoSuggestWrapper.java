package com.offsec.nethunter.utils;


import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.widget.ArrayAdapter;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class AutoSuggestWrapper {
    private final String PREF_KEY;
    private final SharedPreferences prefs;
    private Set<String> suggestionsSet;
    private final AppCompatAutoCompleteTextView autoCompleteTextView;

    public AutoSuggestWrapper(Activity context, String preferenceKey,
                              AppCompatAutoCompleteTextView autoCompleteTextview,
                              @LayoutRes int adapterLayoutRes,
                              @Nullable String[] defaultElements) {

        this.PREF_KEY = preferenceKey;
        autoCompleteTextView = autoCompleteTextview;

        prefs = PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());
        suggestionsSet = prefs.getStringSet(preferenceKey, new TreeSet<String>());


        if (defaultElements != null) {
            Collections.addAll(suggestionsSet, defaultElements);
        }
        String[] suggestionArray;
        suggestionArray = new String[suggestionsSet.size()];
        suggestionsSet.toArray(suggestionArray);
        autoCompleteTextView.setAdapter(new ArrayAdapter<>(context, adapterLayoutRes, suggestionArray));
    }

    public void onInputComplete() {
        String text = getText();
        suggestionsSet.add(text);
        prefs.edit()
                .putStringSet(PREF_KEY, suggestionsSet)
                .apply();
    }

    @NonNull
    public String getText() {
        return autoCompleteTextView.getText().toString();
    }

}
