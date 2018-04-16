package com.offsec.nethunter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetHunterFragment extends Fragment {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */

    private static NhPaths nh;
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String IP_REGEX = "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b";
    private static final Pattern IP_REGEX_PATTERN = Pattern.compile(IP_REGEX);
    Switch HIDSwitch;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */


    public NetHunterFragment() {

    }

    public static NetHunterFragment newInstance(int sectionNumber) {
        NetHunterFragment fragment = new NetHunterFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.nethunter, container, false);
        TextView ip = rootView.findViewById(R.id.editText2);
        ip.setFocusable(false);
        addClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getExternalIp();
            }
        }, rootView);
        getInterfaces(rootView);

        // HID Switch for newer kernels to turn on HID
        HIDSwitch = rootView.findViewById(R.id.hidSWITCH);
        HIDSwitch.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if(HIDSwitch.isChecked())
                {
                    setHIDON();
                }
                else {
                    setHIDOff();
                }
            }
        });

        return rootView;
    }

    private void addClickListener(View.OnClickListener onClickListener, View rootView) {
        rootView.findViewById(R.id.button1).setOnClickListener(onClickListener);
    }

    private void getExternalIp() {

        final TextView ip = (TextView) getActivity().findViewById(R.id.editText2);
        ip.setText("Please wait...");

        new Thread(new Runnable() {
            final StringBuilder result = new StringBuilder();

            public void run() {

                try {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                    URLConnection urlcon = new URL("https://api.ipify.org").openConnection();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));
                    String line;
                    while ((line = rd.readLine()) != null) {
                        result.append(line);
                    }
                } catch (Exception e) {
                    result.append("Check connection!");
                }
                final String done;
                Matcher p = IP_REGEX_PATTERN.matcher(result.toString());
                if (p.matches() || result.toString().equals("Check connection!")) {
                    done = result.toString();
                } else {
                    done = "Invalid IP!";
                }
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        ip.setText(done);
                    }
                });
            }
        }).start();
        // CHECK FOR ROOT ACCESS

    }

    private void setHIDON() {
        new Thread(new Runnable(){

            public void run(){
                try {
                    Process p = Runtime.getRuntime().exec("su -c getprop sys.usb.config > /data/local/usb.config.tmp && su -c setprop sys.usb.config `cat /data/local/usb.config.tmp`,hid");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }

    private void setHIDOff() {
        new Thread(new Runnable(){

            public void run(){
                try {
                    Process p = Runtime.getRuntime().exec("su -c setprop sys.usb.config `cat /data/local/usb.config.tmp` && su -c rm /data/local/usb.config.tmp");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }


    private void getInterfaces(final View rootView) {

        nh = new NhPaths();

        final boolean installed = appInstalledOrNot("com.offsec.nhterm");

        // 1 thread, 2 commands
        final LinearLayout ifaceParent = (LinearLayout) rootView.findViewById(R.id.iface_parent);
        final TextView busyboxIfaces = (TextView) rootView.findViewById(R.id.editTextBUSYBOX); // BUSYBOX IFACES
        final TextView kernelverIfaces = (TextView) rootView.findViewById(R.id.editTextKERNELVER); // BUSYBOX IFACES
        final TextView terminalIfaces = (TextView) rootView.findViewById(R.id.editTextNHTerminal); // BUSYBOX IFACES

        new Thread(new Runnable() {
            public void run() {

                String busybox_ver = nh.whichBusybox();

                ShellExecuter exe = new ShellExecuter();
                String commandNET[] = {"/system/bin/ip -o addr show | busybox awk '/inet/ {print $2, $3, $4}'"};
                String commandHID[] = {"sh", "-c", "ls /dev/hidg*"};
                String commandBUSYBOX[] = {"sh", "-c", busybox_ver + " | " + busybox_ver + " head -1 | " + busybox_ver + " awk '{print $2}'"};
                String commandKERNELVER[] = {"sh", "-c", "cat /proc/version"};

                final String outputNET = exe.executeCommand(commandNET);
                final String outputHID = exe.executeCommand(commandHID);
                final String outputBUSYBOX = exe.executeCommand(commandBUSYBOX);
                final String outputKERNELVER = exe.executeCommand(commandKERNELVER);

                final String[] netArray = outputNET.split("\n");
                final String[] hidArray = outputHID.split("\n");
                final String[] busyboxArray = outputBUSYBOX.split("\n");
                final String[] kernelverArray = outputKERNELVER.split("\n");

                    ifaceParent.post(new Runnable() {
                        @Override
                        public void run() {
                            ifaceParent.findViewById(R.id.nethunter_loading_layout).setVisibility(View.GONE);
                            ifaceParent.findViewById(R.id.nethunter_current_interfaces).setVisibility(View.VISIBLE);

                            int ind = 3;
                            if (outputNET.equals("")) {
                                TextView textView = new TextView(new ContextThemeWrapper(
                                        ifaceParent.getContext(),
                                        R.style.Nethunter_NetworkInterfaceText));
                                textView.setFocusable(false);
                                textView.setText("No network interfaces detected");
                                ifaceParent.addView(textView,ind);
                                ind++;

                            } else {
                                for (final String netEntry :
                                        netArray) {
                                    TextView textView = new TextView(new ContextThemeWrapper(
                                            ifaceParent.getContext(),
                                            R.style.Nethunter_NetworkInterfaceText));
                                    textView.setText(netEntry);
                                    textView.setOnLongClickListener(new View.OnLongClickListener() {
                                        @Override
                                        public boolean onLongClick(View v) {
                                            Log.d("CLICKED", netEntry);
                                            String itemData = netEntry.split("\\s+")[2];
                                            doCopy(itemData);
                                            return true;
                                        }
                                    });
                                    ifaceParent.addView(textView, ind);
                                    ind++;

                                }
                            }
                            ind++;
                            ifaceParent.findViewById(R.id.nethunter_hid_title).setVisibility(View.VISIBLE);

                            if (outputHID.equals("")) {
                                TextView textView = new TextView(new ContextThemeWrapper(
                                        ifaceParent.getContext(),
                                        R.style.Nethunter_NetworkInterfaceText));
                                textView.setFocusable(false);
                                textView.setText("No HID interfaces detected");
                                ifaceParent.addView(textView,ind);
                                ind++;

                            } else {
                                for (final String netEntry :
                                        hidArray) {
                                    TextView textView = new TextView(new ContextThemeWrapper(
                                            ifaceParent.getContext(),
                                            R.style.Nethunter_NetworkInterfaceText));
                                    textView.setText(netEntry);
                                    textView.setOnLongClickListener(new View.OnLongClickListener() {
                                        @Override
                                        public boolean onLongClick(View v) {
                                            Log.d("CLICKED", netEntry);
                                            String itemData = netEntry;
                                            doCopy(itemData);
                                            return true;
                                        }
                                    });
                                    ifaceParent.addView(textView, ind);
                                    ind++;
                                }
                            }

                            ifaceParent.findViewById(R.id.nethunter_busybox_title).setVisibility(View.VISIBLE);
                            busyboxIfaces.setVisibility(View.VISIBLE);
                            if (outputBUSYBOX.equals("")) {
                                busyboxIfaces.setText("Busnethunter_ext_ip_titleybox not detected!");
                                busyboxIfaces.setFocusable(false);
                            } else {
                                busyboxIfaces.setText(outputBUSYBOX);
                            }

                            ifaceParent.findViewById(R.id.nethunter_kernel_version).setVisibility(View.VISIBLE);
                            kernelverIfaces.setVisibility(View.VISIBLE);
                            if (outputKERNELVER.equals("")) {
                                kernelverIfaces.setText("Could not find kernel version!");
                                kernelverIfaces.setFocusable(false);
                            } else {
                                kernelverIfaces.setVisibility(View.GONE);
                                kernelverIfaces.setText(outputKERNELVER);
                            }

                            ifaceParent.findViewById(R.id.nethunter_kernel_version).setVisibility(View.VISIBLE);
                            terminalIfaces.setVisibility(View.VISIBLE);
                            if(!installed) {
                                // Installed, make note!
                                terminalIfaces.setText("Nethunter Terminal is NOT installed!");
                                terminalIfaces.setFocusable(false);
                            } else {
                                // Not installed, make note!
                                terminalIfaces.setText("Nethunter Terminal is installed");
                            }

                            ifaceParent.findViewById(R.id.nethunter_ext_ip_title).setVisibility(View.VISIBLE);
                            ifaceParent.findViewById(R.id.nethunter_ext_ip_layout).setVisibility(View.VISIBLE);

                        }
                    });
                }
        }).start();
    }

    private boolean appInstalledOrNot(String uri) {
        PackageManager pm = getActivity().getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    // Now we can copy and address from networks!!!!!! Surprise! ;)
    private void doCopy(String text) {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("WordKeeper", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Copied: " + text, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error copying: " + text, Toast.LENGTH_SHORT).show();
        }
    }
}