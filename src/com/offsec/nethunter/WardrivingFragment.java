package com.offsec.nethunter;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class WardrivingFragment extends Fragment {
    private ViewPager mViewPager;
    private static final String ARG_SECTION_NUMBER = "section_number";


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.wardriving, container, false);
        TabsPagerAdapter tabsPagerAdapter = new TabsPagerAdapter(getActivity().getSupportFragmentManager());

        mViewPager = rootView.findViewById(R.id.pagerWardriving);
        mViewPager.setAdapter(tabsPagerAdapter);

        setHasOptionsMenu(true);
        return rootView;
    }

    public static WardrivingFragment newInstance(int sectionNumber) {
        WardrivingFragment fragment = new WardrivingFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;}

    public class TabsPagerAdapter extends FragmentPagerAdapter {


        TabsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new WarDrivingMainFragment();
                case 1:
                    return new WardrivingMapFragment();
                default:
                    return new WarDrivingMainFragment();
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Wardriving Setup";
                case 1:
                    return "Map";
                default:
                    return "Wardriving Setup";
            }
        }
    } //end class


}
