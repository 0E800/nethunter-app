package com.offsec.nethunter.gps;

import android.provider.BaseColumns;

public final class WarDrivingDbContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private WarDrivingDbContract() {}

    /* Inner class that defines the table contents */
    public static class WifiEntry implements BaseColumns {
        public static final String TABLE_NAME = "wifi";
        public static final String _ID = "id";
        public static final String COLUMN_NAME_BSSID = "bssid";
        public static final String COLUMN_NAME_SSID = "ssid";
        public static final String COLUMN_NAME_LAT = "lat";
        public static final String COLUMN_NAME_LON = "lon";
        public static final String COLUMN_NAME_MIN_LAT = "min_lat";
        public static final String COLUMN_NAME_MIN_LON = "min_lon";
        public static final String COLUMN_NAME_MAX_LAT = "max_lat";
        public static final String COLUMN_NAME_MAX_LON = "max_lon";
    }

}