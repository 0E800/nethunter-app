package com.offsec.nethunter.gps;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class WardrivingDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "wardriving.db";

    public WardrivingDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + WarDrivingDbContract.WifiEntry.TABLE_NAME + " (" +
                    WarDrivingDbContract.WifiEntry._ID + " INTEGER PRIMARY KEY," +
                    WarDrivingDbContract.WifiEntry.COLUMN_NAME_BSSID + " TEXT," +
                    WarDrivingDbContract.WifiEntry.COLUMN_NAME_SSID + " TEXT," +
                    WarDrivingDbContract.WifiEntry.COLUMN_NAME_LAT + " REAL)" +
                    WarDrivingDbContract.WifiEntry.COLUMN_NAME_LON + " REAL)" +
                    WarDrivingDbContract.WifiEntry.COLUMN_NAME_MIN_LAT + " REAL)" +
                    WarDrivingDbContract.WifiEntry.COLUMN_NAME_MIN_LON + " REAL)" +
                    WarDrivingDbContract.WifiEntry.COLUMN_NAME_MAX_LAT + " REAL)" +
                    WarDrivingDbContract.WifiEntry.COLUMN_NAME_MAX_LON + " REAL)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + WarDrivingDbContract.WifiEntry.TABLE_NAME;

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
