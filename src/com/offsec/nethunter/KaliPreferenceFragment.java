package com.offsec.nethunter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.offsec.nethunter.preferences.ConnectionListLoader;

public class KaliPreferenceFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener,
                ConnectionListLoader.ListHolder {


    private static final String ARG_SECTION_NUMBER = "section_number";
    private CharSequence[] connectionEntries = null;
    private CharSequence[] connectionEntryValues = null;
    private boolean connectionEntriesSet = false;
    private boolean valuesSet = false;
    private ListPreference connectionListPreference;

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
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        addPreferencesFromResource(R.xml.preferences);
//    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        getActivity().getSupportLoaderManager().initLoader(
                0, null, new ConnectionListLoader(getActivity(), this));
        connectionListPreference = (ListPreference)
                getPreferenceScreen().findPreference("prefkey_juice_profile");
        if (!connectionEntriesSet && connectionEntries != null) {
            connectionEntriesSet = true;
            connectionListPreference.setEntries(connectionEntries);
            connectionListPreference.setEntryValues(connectionEntryValues);
        }

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

    @Override
    public void onListReturned(CharSequence[] entries, CharSequence[] values) {
        if (!connectionEntriesSet && entries != null) {
            connectionListPreference.setEntries(entries);
            connectionListPreference.setEntryValues(values);
            connectionEntriesSet = true;
            KaliPreferenceFragment.this.connectionEntries = entries;
            KaliPreferenceFragment.this.connectionEntryValues = values;
        }
    }
}
