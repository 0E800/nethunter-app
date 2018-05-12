package com.offsec.nethunter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.offsec.nethunter.adapters.InterfaceItem;
import com.offsec.nethunter.adapters.NethunterBaseItem;
import com.offsec.nethunter.adapters.NethunterInterfaceAdapter;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
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
    private NethunterInterfaceAdapter<InterfaceItem> interfaceAdapter;
    private SwipeRefreshLayout refreshLayout;
    private ListView list;
    private View rootView;

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

        rootView = inflater.inflate(R.layout.nethunter, container, false);

        View.OnClickListener externalIpListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getExternalIp();
            }
        };

        View.OnClickListener hidSwitchListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(((Switch) v).isChecked())
                {
                    setHIDOn();
                }
                else {
                    setHIDOff();
                }
            }
        };

        refreshLayout = (SwipeRefreshLayout) rootView;
        refreshLayout.setRefreshing(true);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateListView();
            }
        });

        interfaceAdapter = new NethunterInterfaceAdapter<>(requireContext(), hidSwitchListener,
                externalIpListener);
        list = rootView.findViewById(R.id.list);
        list.setAdapter(interfaceAdapter);

        list.addHeaderView(LayoutInflater.from(list.getContext()).inflate(
                R.layout.nethunter_header, list, false));

        populateListView(interfaceAdapter);

        // HID Switch for newer kernels to turn on HID


        return rootView;
    }

    private void updateListView() {

        populateListView(interfaceAdapter);
        refreshLayout.setRefreshing(false);
    }

    private void getExternalIp() {

//        final TextView ip = requireActivity().findViewById(R.id.editText2);
//        ip.setText(R.string.please_wait);

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
                requireActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        interfaceAdapter.setExternalIPText(done);
                    }
                });
            }
        }).start();
        // CHECK FOR ROOT ACCESS

    }

    private void setHIDOn() {
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


    private void populateListView(final NethunterInterfaceAdapter<InterfaceItem> interfaceAdapter) {
        interfaceAdapter.clear();
        list.setAdapter(interfaceAdapter);

        // Add the headings
        interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.NET_HEADING,
                R.string.nethunter_interface_heading));
        interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.HID_HEADING,
                R.string.nethunter_hid_heading));
        interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.HID_SWITCH,
                R.string.nethunter_hid_switch_turn_on));
        interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.BUSYBOX_ITEM,
                R.string.nethunter_busybox_title));
        interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.KERNEL_HEADING,
                R.string.nethunter_kernel_heading));
        interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.NH_TERMINAL_HEADING,
                R.string.nethunter_terminal_detect));
        interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.EXTERNAL_IP_HEADING,
                R.string.nethunter_external_ip_heading));
        interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.EXTERNAL_IP,
                R.string.get_external_ip));


        nh = new NhPaths();

        final boolean installed = appInstalledOrNot("com.offsec.nhterm");

        // 1 thread, 2 commands
//        new Thread(new Runnable() {
//            public void run() {

                String busyboxVer = nh.whichBusybox();

                ShellExecuter exe = new ShellExecuter();
                String commandNet = "/system/bin/ip -o addr show";
                String commandHID[] = {"sh", "-c", "ls /dev/hidg*"};
                String commandBUSYBOX[] = {"sh", "-c", busyboxVer + " | " + busyboxVer + " head -1 | " + busyboxVer + " awk '{print $2}'"};
                String commandKernelVer[] = {"/system/bin/sh", "-c", "cat /proc/version"};


                final String outputNET = exe.executeCommand(commandNet);
                final List<String> netArray = parseNetworkInterfaces(outputNET);

                final String outputHID = exe.executeCommand(commandHID);
                final String outputBUSYBOX = exe.executeCommand(commandBUSYBOX);
                final String outputKernelVer = exe.executeCommand(commandKernelVer);

                final String[] hidArray = outputHID.split("\n");
                final String[] kernelverArray = outputKernelVer.split("\n");

                if (outputNET.equals("")) {
                    interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.NET_ITEM,
                            R.string.nethunter_no_interfaces_detected));
                } else {
                    for (final String netEntry : netArray) {
                        interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.NET_ITEM,
                                netEntry));
//                        textView.setOnLongClickListener(new View.OnLongClickListener() {
//                            @Override
//                                        public boolean onLongClick(View v) {
//                                            Log.d("CLICKED", netEntry);
//                                            String itemData = netEntry.split("\\s+")[2];
//                                            doCopy(itemData);
//                                            return true;
//                                        }
//                                    });
                            }
                }
                if (outputHID.equals("")) {
                        interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.HID_ITEM,
                                R.string.nethunter_no_hid));
                } else {
                    for (final String hidEntry : hidArray) {
//                                    textView.setOnLongClickListener(new View.OnLongClickListener() {
//                                        @Override
//                                        public boolean onLongClick(View v) {
//                                            Log.d("CLICKED", hidEntry);
//                                            doCopy(hidEntry);
//                                            return true;
//                                        }
//                                    });
                        interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.HID_ITEM,
                                hidEntry));
                    }
                }

                if (outputBUSYBOX.equals("")) {
                    interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.BUSYBOX_ITEM,
                            R.string.nethunter_busybox_not_detected));
                } else {
                    interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.BUSYBOX_ITEM,
                            outputBUSYBOX));
                }

                if (outputKernelVer.equals("")) {
                    interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.KERNEL_ITEM,
                            R.string.nethunter_not_find_kernel));
                } else {
                    interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.KERNEL_ITEM,
                            outputKernelVer));
                }

                if(!installed) {
                    interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.NH_TERMINAL_ITEM,
                            R.string.nethunter_terminal_not_installed));
                } else {
                    interfaceAdapter.add(new InterfaceItem(NethunterBaseItem.NH_TERMINAL_ITEM,
                            R.string.nethunter_terminal_installed));
                }

                refreshLayout.setRefreshing(false);
            }

//        }).start();
//    }

    @NonNull
    private List<String> parseNetworkInterfaces(String outputNET) {
        String[] split = outputNET.split("\n");
        String[] splitLine;
        String[] ifaceLine = new String[3];
        final List<String> netArray = new LinkedList<>();
        for (String line : split) {
            if (line.contains("inet")) {
                splitLine = line.substring(line.indexOf(":") + 1).split(" ");
                int numFound = 0;
                for (String sequence: splitLine) {
                    if (!sequence.equals("")) {
                        ifaceLine[numFound] = sequence;
                        numFound++;
                    }
                    if (numFound == 3) {
                        netArray.add(String.join(" ", ifaceLine));
                        break;
                    }

                }
            }
        }
        return netArray;
    }

    @SuppressWarnings("SameParameterValue")
    private boolean appInstalledOrNot(String uri) {
        PackageManager pm = requireActivity().getPackageManager();
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
            Snackbar.make(rootView, "Copied: ", Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            Snackbar.make(rootView, "Error copying:  ", Snackbar.LENGTH_SHORT).show();
        }
    }
}