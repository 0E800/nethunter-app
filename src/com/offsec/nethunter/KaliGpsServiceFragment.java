package com.offsec.nethunter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.offsec.nethunter.gps.KaliGPSUpdates;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import static android.app.Activity.RESULT_OK;


public class KaliGpsServiceFragment extends Fragment implements KaliGPSUpdates.Receiver {

    private static final String TAG = "KaliGpsServiceFragment";

    private static NhPaths nh;

    private static final String ARG_SECTION_NUMBER = "section_number";
    private KaliGPSUpdates.Provider gpsProvider = null;
    private TextView gpsTextView;
    private final int REQUEST_KISMET_DB = 101;
    public KaliGpsServiceFragment() {
    }

    public static KaliGpsServiceFragment newInstance(int sectionNumber) {
        KaliGpsServiceFragment fragment = new KaliGpsServiceFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.wardriving_gps, container, false);


        nh = new NhPaths();

        addClickListener(R.id.start_kismet, new View.OnClickListener() {
            public void onClick(View v) {

                if (gpsProvider != null) {
                    gpsProvider.onLocationUpdatesRequested(KaliGpsServiceFragment.this);
                    gpsTextView.append("Starting gps updates \n");
                }
            }
        }, rootView);

        addClickListener(R.id.gps_stop, new View.OnClickListener() {
            public void onClick(View v) {
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
        }, rootView);

        addClickListener(R.id.gps_import, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectFile();
            }
        }, rootView);

        return rootView;
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
//            Todo: allow multi-select of zip files
        }
    }

    private void importIntoDB(List<WirelessNetwork> networks) {

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
                if (parser.getName().equals("essid")) {
                    network.ssid = parser.nextText();
                } else if (parser.getName().equals("min-lat")) {
                    network.minLat = Double.parseDouble(parser.nextText());
                } else if (parser.getName().equals("min-lon")) {
                    network.minLon = Double.parseDouble(parser.nextText());
                } else if (parser.getName().equals("max-lat")) {
                    network.maxLat = Double.parseDouble(parser.nextText());
                } else if (parser.getName().equals("max-lon")) {
                    network.maxLon = Double.parseDouble(parser.nextText());
                } else if (parser.getName().equals("avg-lat")) {
                    network.lat = Double.parseDouble(parser.nextText());
                } else if (parser.getName().equals("avg-lon")) {
                    network.lon = Double.parseDouble(parser.nextText());
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
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gpsTextView = (TextView) view.findViewById(R.id.gps_textview);
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
            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.toast_install_terminal), Toast.LENGTH_SHORT).show();
        }
    }

    private static class WirelessNetwork {
        public String ssid;
        public double lat;
        public double lon;
        public double minLat;
        public double maxLat;
        public double minLon;
        public double maxLon;
    }
}

