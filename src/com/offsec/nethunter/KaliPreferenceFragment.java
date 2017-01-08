package com.offsec.nethunter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import java.util.Objects;


public class KaliPreferenceFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {


    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final int SECTION_AP_IFACE = 2001;
    private String PREF_KEY_MANA_AP_IFC;
    private FragmentSwitcher fragmentSwitcher;

    public KaliPreferenceFragment() {
        // Required empty public constructor
    }

    public static KaliPreferenceFragment newInstance(int sectionNumber) {
        KaliPreferenceFragment fragment = new KaliPreferenceFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        PREF_KEY_MANA_AP_IFC = context.getResources().getString(R.string.prefkey_mana_ap_ifc);
        if (context instanceof FragmentSwitcher) {
            fragmentSwitcher = (FragmentSwitcher) context;
        }
    }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (Objects.equals(preference.getKey(), PREF_KEY_MANA_AP_IFC)) {
            fragmentSwitcher.onAddRemoveFragmentRequested(R.string.prefkey_mana_ap_ifc);
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }
}
