package com.offsec.nethunter;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public abstract class KaliBaseFragment extends Fragment {
    public static final String ARG_SECTION_NUMBER = "section_number";

    public KaliBaseFragment() {
        // Required empty public constructor
    }

   public void putSectionNumber(int sectionNumber) {
       Bundle args = new Bundle();
       args.putInt(ARG_SECTION_NUMBER, sectionNumber);
       setArguments(args);
   }

}
