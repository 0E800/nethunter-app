package com.offsec.nethunter;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;


public class AddRemoveListPreference extends KaliBaseFragment {

    private static final String ARG_PREF_KEY = "ARG_PREF_KEY";
    private ImageButton deleteButton;
    private ArrayList<String> entries = new ArrayList<>(4);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_remove_list, container, false);
        deleteButton = (ImageButton) view.findViewById(R.id.autocomplete_delete_button);
        final Button addButton = (Button) view.findViewById(R.id.add_button);
        final EditText editText = (EditText) view.findViewById(R.id.edittext_add_autocomplete);
        final ListView listView = (ListView) view.findViewById(R.id.suggestion_listview);
        final CustomArrayAdapter adapter = new CustomArrayAdapter(getActivity(),
                R.layout.autosuggest_edit_item, entries, checkedItemListener);
        listView.setAdapter(adapter);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SparseArrayCompat<String> toDelete = adapter.getCheckedItems();
                for (int i = 0; i < toDelete.size(); i++) {
                    entries.remove(toDelete.valueAt(i));
                    adapter.clearChecked();
                    adapter.notifyDataSetChanged();
                }

            }
        });


        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String entry = editText.getText().toString();
                if (!entries.contains(entry)) {
                    entries.add(entry);
                    adapter.notifyDataSetChanged();
                }
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Bundle b = getArguments();

        String key = context.getResources().getString(b.getInt(ARG_SECTION_NUMBER));
        Set<String> entriesSet = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getStringSet(key, null);
        if (entriesSet != null) {
            entries.addAll(entriesSet);
        }
    }

    public static AddRemoveListPreference newInstance(int prefKeyId) {
        AddRemoveListPreference fragment = new AddRemoveListPreference();
        fragment.putSectionNumber(prefKeyId);
        return fragment;
    }


    public static class CustomArrayAdapter extends ArrayAdapter<String> {

        private final CheckedItemListener checkedItemListener;

        private SparseArrayCompat<String> checkedList = new SparseArrayCompat<>(4);

        public CustomArrayAdapter(Context context, int resource, ArrayList<String> entries,
                                  CheckedItemListener listener) {
            super(context, resource, entries);
            this.checkedItemListener = listener;
        }

        public void clearChecked() {
            checkedList.clear();
        }

        interface CheckedItemListener {

            void onCheckedItemsChanged(int numChecked);


        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View v = convertView;
            if (v == null) {
                LayoutInflater li = LayoutInflater.from(parent.getContext());
                v = li.inflate(R.layout.autosuggest_edit_item, parent, false);
            }

            final String entry = getItem(position);

            TextView text = (TextView) v.findViewById(R.id.item_text);
            final CheckBox checkBox = (CheckBox) v.findViewById(R.id.item_checkbox);

            text.setText(entry);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkBox.toggle();
                    if (checkBox.isChecked()) {
                        checkedList.append(position, entry);

                    } else {
//                        leaves list unchanged if not present
                        checkedList.remove(position);
                    }
                    checkedItemListener.onCheckedItemsChanged(checkedList.size());

                }
            });

            return v;

        }
        public SparseArrayCompat<String> getCheckedItems() {
            return checkedList;
        }

    }
    private final CustomArrayAdapter.CheckedItemListener checkedItemListener =
            new CustomArrayAdapter.CheckedItemListener() {
                @Override
                public void onCheckedItemsChanged(int numChecked) {
                    if (numChecked > 0 && !deleteButton.isShown()) {
                        deleteButton.setVisibility(View.VISIBLE);
                    } else if (numChecked == 0 && deleteButton.isShown()) {
                        deleteButton.setVisibility(View.GONE);
                    }
                }
            };


}
