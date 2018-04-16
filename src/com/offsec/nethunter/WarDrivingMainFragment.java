package com.offsec.nethunter;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.offsec.nethunter.gps.KaliGPSUpdates;
import com.offsec.nethunter.gps.WarDrivingDbContract;
import com.offsec.nethunter.gps.WardrivingDbHelper;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import static android.app.Activity.RESULT_OK;


public class WarDrivingMainFragment extends Fragment implements KaliGPSUpdates.Receiver {

    private static final String TAG = "WarDrivingMainFragment";

    private static NhPaths nh;

    private static final String ARG_SECTION_NUMBER = "section_number";
    private KaliGPSUpdates.Provider gpsProvider = null;
    private TextView gpsTextView;
    private final int REQUEST_KISMET_DB = 101;
    public WarDrivingMainFragment() {
    }

    public static WarDrivingMainFragment newInstance(int sectionNumber) {
        WarDrivingMainFragment fragment = new WarDrivingMainFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.wardriving_gps, container, false);


        nh = new NhPaths();

        addClickListener(R.id.start_kismet, new View.OnClickListener() {
            public void onClick(View v) {

                if (gpsProvider != null) {
                    gpsProvider.onLocationUpdatesRequested(WarDrivingMainFragment.this);
                    gpsTextView.append("Starting gps updates \n");
                }
            }
        }, rootView);

        addClickListener(R.id.gps_stop, new View.OnClickListener() {
            public void onClick(View v) {
                stopGPS();
            }
        }, rootView);

        addClickListener(R.id.gps_import, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectFile();
            }
        }, rootView);

        return rootView;
    }

    private void stopGPS() {
        if (gpsProvider != null) {
            gpsProvider.onStopRequested();
            gpsTextView.append("Stopping gps updates \n");
            new Thread(new Runnable() {
                public void run() {
                    ShellExecuter exe = new ShellExecuter();
                    String command = "su -c '" + nh.APP_SCRIPTS_PATH + "/stop-gpsd'";
                    Log.d(TAG, command);
                    exe.RunAsRootOutput(command);
                }
            }).start();
        }
    }

    private void selectFile() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Choose Kismet Output"), REQUEST_KISMET_DB);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_KISMET_DB && resultCode==RESULT_OK)
        {
            Uri fileUri = data.getData();
            try {
                InputStream is = getActivity().getContentResolver().openInputStream(fileUri);
                List<WirelessNetwork> networks = parseXMLInput(is);
                importIntoDB(networks);
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }
//            todo: allow multi-select
        }
    }

    private void importIntoDB(List<WirelessNetwork> networks) {
        DatabaseAccess dbAccess = new DatabaseAccess(getActivity());
        dbAccess.execute(networks);

    }

    private static class DatabaseAccess extends  AsyncTask<List<WirelessNetwork>, Void, Void> {

        private final WardrivingDbHelper dbHelper;
        private final AlertDialog.Builder dialogBuilder;

        public DatabaseAccess(Context context) {

            dbHelper = new WardrivingDbHelper(context);
            dialogBuilder = new AlertDialog.Builder(context);
        }

        @SafeVarargs
        @Override
        protected final Void doInBackground(List<WirelessNetwork>... networkLists) {
            List<WirelessNetwork> networks = networkLists[0];

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            // Create a new map of values, where column names are the keys
            for (WirelessNetwork network : networks) {
                ContentValues values = new ContentValues();
                values.put(WarDrivingDbContract.WifiEntry.COLUMN_NAME_BSSID, network.bssid);
                values.put(WarDrivingDbContract.WifiEntry.COLUMN_NAME_SSID, network.ssid);
                values.put(WarDrivingDbContract.WifiEntry.COLUMN_NAME_LAT, network.lat);
                values.put(WarDrivingDbContract.WifiEntry.COLUMN_NAME_LON, network.lon);
                values.put(WarDrivingDbContract.WifiEntry.COLUMN_NAME_MIN_LAT, network.minLat);
                values.put(WarDrivingDbContract.WifiEntry.COLUMN_NAME_MIN_LON, network.minLon);
                values.put(WarDrivingDbContract.WifiEntry.COLUMN_NAME_MAX_LAT, network.maxLat);
                values.put(WarDrivingDbContract.WifiEntry.COLUMN_NAME_MAX_LON, network.maxLon);

                db.insert(WarDrivingDbContract.WifiEntry.TABLE_NAME, null, values);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            AlertDialog dialog = dialogBuilder.setTitle(R.string.import_complete)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create();
            dialog.show();
        }
    }

    private List<WirelessNetwork> parseXMLInput(InputStream is) throws XmlPullParserException, IOException {
        List<WirelessNetwork> networks = new LinkedList<>();
        XmlPullParserFactory xmlF = XmlPullParserFactory.newInstance();
        XmlPullParser parser = xmlF.newPullParser();
        parser.setInput(new InputStreamReader(is));
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String name = parser.getName();
                if (name.contains("wireless-network")) {
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        if (parser.getAttributeName(i).equals("type") &&
                                parser.getAttributeValue(i).equals("infrastructure")) {
                            //parse through inner xml tags
                            networks.add(populateNetwork(parser));
                        }
                    }
                }
            }
            event = parser.next();
        }
        return networks;
    }

    private WirelessNetwork populateNetwork(XmlPullParser parser) throws XmlPullParserException, IOException {
        WirelessNetwork network = new WirelessNetwork();
        int event;
        event = parser.next();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case "essid":
                        network.ssid = parser.nextText();
                        break;
                    case "BSSID":
                        network.bssid = parser.nextText();
                        break;
                    case "min-lat":
                        network.minLat = Double.parseDouble(parser.nextText());
                        break;
                    case "min-lon":
                        network.minLon = Double.parseDouble(parser.nextText());
                        break;
                    case "max-lat":
                        network.maxLat = Double.parseDouble(parser.nextText());
                        break;
                    case "max-lon":
                        network.maxLon = Double.parseDouble(parser.nextText());
                        break;
                    case "avg-lat":
                        network.lat = Double.parseDouble(parser.nextText());
                        break;
                    case "avg-lon":
                        network.lon = Double.parseDouble(parser.nextText());
                        break;
                }
            } else if (event == XmlPullParser.END_TAG &&
                    parser.getName().equals("wireless-network")) {
                return network;
            }
            event = parser.next();
        }
        return null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gpsTextView = view.findViewById(R.id.gps_textview);
    }

    private void addClickListener(int buttonId, View.OnClickListener onClickListener, View rootView) {
        rootView.findViewById(buttonId).setOnClickListener(onClickListener);
    }


    @Override
    public void onAttach(Context context) {
        if (context instanceof KaliGPSUpdates.Provider) {
            this.gpsProvider = (KaliGPSUpdates.Provider) context;
        }

        super.onAttach(context);
    }

    @Override
    public void onPositionUpdate(String nmeaSentences) {

    }

    @Override
    public void onFirstPositionUpdate() {
//        Got first position update, start Kismet
        gpsTextView.append("First fix received. Starting Kismet \n");
        startKismet();
    }

    private void startKismet() {
        try {
            Intent intent =
                    new Intent("com.offsec.nhterm.RUN_SCRIPT_NH");
            intent.addCategory(Intent.CATEGORY_DEFAULT);

            intent.putExtra("com.offsec.nhterm.iInitialCommand", "/usr/bin/start-kismet");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireActivity().getApplicationContext(), getString(R.string.toast_install_terminal), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopGPS();
    }

    private static class WirelessNetwork {
        String bssid;
        String ssid;
        double lat;
        double lon;
        double minLat;
        double maxLat;
        double minLon;
        double maxLon;
    }
}

