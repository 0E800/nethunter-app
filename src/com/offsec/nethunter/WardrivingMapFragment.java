package com.offsec.nethunter;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.offsec.nethunter.gps.WarDrivingDbContract;
import com.offsec.nethunter.gps.WardrivingDbHelper;

import java.util.ArrayList;
import java.util.List;

public class WardrivingMapFragment extends Fragment implements OnMapReadyCallback {

    private MapView mapView;
    private boolean mapReady = false;
    private boolean dbReady = true;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.wardriving_map, container, false);

        mapView = rootView.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        return rootView;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details
            map.getUiSettings().setMyLocationButtonEnabled(true);
            return;
        }

        // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
        MapsInitializer.initialize(this.getActivity());
        // Updates the location and zoom of the MapView
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(43.1, -87.9), 10);
        map.animateCamera(cameraUpdate);
        mapReady = true;
        drawSSIDsOnMap(map);
    }

    private void drawSSIDsOnMap(GoogleMap map) {
        if (mapReady && dbReady) {
            DatabaseAccess dbAccess = new DatabaseAccess(getActivity(), map);
            dbAccess.execute(null, null);
        }
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }

    private static class DatabaseAccess extends AsyncTask<Void, Void, List<MarkerOptions>> {

        private final WardrivingDbHelper dbHelper;
        private final GoogleMap map;

        public DatabaseAccess(Context context, GoogleMap map) {

            dbHelper = new WardrivingDbHelper(context);
            this.map = map;
        }

        @Override
        protected final List<MarkerOptions> doInBackground(Void...aVoid) {

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            // Create a new map of values, where column names are the keys
            String[] projection = {
                    WarDrivingDbContract.WifiEntry.COLUMN_NAME_BSSID,
                    WarDrivingDbContract.WifiEntry.COLUMN_NAME_SSID,
                    WarDrivingDbContract.WifiEntry.COLUMN_NAME_LAT,
                    WarDrivingDbContract.WifiEntry.COLUMN_NAME_LON
            };


            Cursor cursor = db.query(
                    WarDrivingDbContract.WifiEntry.TABLE_NAME, projection, null, null, null, null, null);

            // Bssids already in database. delete any duplicate rows
            List<String> bssids = new ArrayList<>();
            List<MarkerOptions> markerOptions = new ArrayList<>();
            while(cursor.moveToNext()) {
                String bssid = cursor.getString(
                        cursor.getColumnIndexOrThrow(WarDrivingDbContract.WifiEntry.COLUMN_NAME_SSID));
                String ssid = cursor.getString(
                        cursor.getColumnIndexOrThrow(WarDrivingDbContract.WifiEntry.COLUMN_NAME_SSID));
                Double lat = cursor.getDouble(
                        cursor.getColumnIndexOrThrow(WarDrivingDbContract.WifiEntry.COLUMN_NAME_LAT));
                Double lon = cursor.getDouble(
                        cursor.getColumnIndexOrThrow(WarDrivingDbContract.WifiEntry.COLUMN_NAME_LON));

                if (bssids.contains(bssid)) {
                    db.delete(WarDrivingDbContract.WifiEntry.TABLE_NAME,
                            WarDrivingDbContract.WifiEntry.COLUMN_NAME_BSSID + " == ?", new String[]{bssid});
                } else {
                    bssids.add(bssid);
                    MarkerOptions markerOption = new MarkerOptions()
                            .position(new LatLng(lat, lon))
                            .title(ssid);

                    markerOptions.add(markerOption);
                }


            }
            cursor.close();

            return markerOptions;
        }

        @Override
        protected void onPostExecute(List<MarkerOptions> markerOptions) {
            super.onPostExecute(markerOptions);

            for (MarkerOptions marker: markerOptions) {
                map.addMarker(marker);
            }
        }
    }

}
