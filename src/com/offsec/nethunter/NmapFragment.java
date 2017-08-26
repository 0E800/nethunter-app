package com.offsec.nethunter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.offsec.nethunter.databinding.NmapBinding;
import com.offsec.nethunter.ssh.PlayManaFragment;
import com.offsec.nethunter.utils.NhPaths;
import com.sshtools.net.SocketTransport;
import com.sshtools.ssh.ChannelOpenException;
import com.sshtools.ssh.PasswordAuthentication;
import com.sshtools.ssh.PseudoTerminalModes;
import com.sshtools.ssh.SshClient;
import com.sshtools.ssh.SshConnector;
import com.sshtools.ssh.SshException;
import com.sshtools.ssh.SshSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class NmapFragment extends Fragment {

    private static final String TAG = "NMAPFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";

    NhPaths nh;
    private NmapBinding nmapBinding;
    private NmapViewModel viewModel;
    private int interfacePosition = -1;
    private int timingIndex = -1;
    private int techniqueIndex = -1;
    private SshSession session = null;
    private boolean shellCancelled = false;

    public NmapFragment() {
    }

    public static NmapFragment newInstance(int sectionNumber) {
        NmapFragment fragment = new NmapFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        nmapBinding = NmapBinding.inflate(inflater, container, false);
        viewModel = new NmapViewModel();
        nmapBinding.setViewModel(viewModel);

        // NMAP Interface Spinner
        ArrayAdapter<CharSequence> interfaceAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.nmap_interface_array, android.R.layout.simple_spinner_item);
        interfaceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        nmapBinding.nmapIntSpinner.setAdapter(interfaceAdapter);
        nmapBinding.nmapIntSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                interfacePosition = pos;
          
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Another interface callback
            }
        });

        // NMAP Technique Spinner
        ArrayAdapter<CharSequence> techAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.nmap_scantechnique_array, android.R.layout.simple_spinner_item);
        interfaceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        nmapBinding.nmapScanTechSpinner.setAdapter(techAdapter);
        nmapBinding.nmapScanTechSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                techniqueIndex = pos;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Another interface callback
            }
        });


        // NMAP Timing Spinner
        ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.nmap_timing_array, android.R.layout.simple_spinner_item);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        nmapBinding.nmapTimingSpinner.setAdapter(timeAdapter);
        nmapBinding.nmapTimingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                timingIndex = pos;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Another interface callback
            }
        });

        return nmapBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nmapBinding.nmapScanButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        viewModel.scanClicked();
                        String command = "nmap " + getCmd();
                        startNmap(command);
                    }
                });
    }

    private void startNmap(String command) {
        SshConnector con = null;
        try {
            con = SshConnector.createInstance();
        } catch (SshException e) {
            con = null;
            e.printStackTrace();
        }
        SshConnector finalCon = con;
        Thread th = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    final SshClient sshClient = finalCon.connect(new SocketTransport("localhost",
                            22), "root", true);

                    PasswordAuthentication pwd = new PasswordAuthentication();
                    pwd.setPassword("toor");
                    try {
                        sshClient.authenticate(pwd);
                    } catch (SshException e) {
                        e.printStackTrace();
                    }


                    if (sshClient.isAuthenticated()) {
                        try {
                            if (session == null) {
                                session = sshClient.openSessionChannel();
                            }
                            PseudoTerminalModes pty = new PseudoTerminalModes(sshClient);
                            pty.setTerminalMode(PseudoTerminalModes.ECHO, false);
                            session.requestPseudoTerminal("vt100", 80, 24, 0, 0, pty);
                            session.startShell();
//                                session.setAutoConsumeInput(true);
                            InputStreamReader is = new InputStreamReader(session.getInputStream());
                            BufferedReader br = new BufferedReader(is);
                            String line;
                            session.getOutputStream().write((command + "\n").getBytes());
//                            session.getOutputStream().flush();
                            while ((line = br.readLine()) != null || !shellCancelled) {
                                appendText(line);
                            }

                            br.close();

                        } catch (SshException | ChannelOpenException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException | SshException e) {
                    e.printStackTrace();
                }
            }
        });
        th.start();


    }

    //    called from background thread
    private void appendText(String line) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                nmapBinding.nmapOutput.append(line);
            }
        }).start();
    }

    private String getCmd() {
        StringBuilder sb = new StringBuilder();
        switch (interfacePosition) {
            case 1:
                sb.append(" -e wlan0");
                break;
            case 2:
                sb.append(" -e wlan1");
                break;
            case 3:
                sb.append(" -e eth0");
                break;
            case 4:
                sb.append(" -e rndis0");
                break;
            default:
                sb.append(" -e wlan0");
                break;
        }

        switch (techniqueIndex) {
            case 1:
                sb.append(" -sS");
                break;
            case 2:
                sb.append(" -sT");
                break;
            case 3:
                sb.append(" -sA");
                break;
            case 4:
                sb.append(" -sW");
                break;
            case 5:
                sb.append(" -sM");
                break;
            case 6:
                sb.append(" -sN");
                break;
            case 7:
                sb.append(" -sF");
                break;
            case 8:
                sb.append(" -sX");
                break;
        }

        sb.append(timingIndex > 0 ? " -T " + (timingIndex - 1) : "")
                .append(nmapBinding.nmapAllCheck.isChecked() ? " -A" : "")
                .append(nmapBinding.nmapFastmodeCheck.isChecked() ? " -F" : "")
                .append(nmapBinding.nmapPingCheck.isChecked() ? " -sn" : "")
                .append(nmapBinding.nmapTopPortsCheck.isChecked() ? " --top-ports 20" : "")
                .append(nmapBinding.nmapUdpCheckbox.isChecked() ? " -sU" : "")
                .append(nmapBinding.nmapOpen.isChecked() ? " --open" : "")
                .append(nmapBinding.nmapIpv6Check.isChecked() ? " -6" : "")
                .append(nmapBinding.nmapSVCheckbox.isChecked() ? " -sV" : "")
                .append(nmapBinding.nmapOsonlyCheck.isChecked() ? " -O" : "")
                .append(!nmapBinding.nmapSearchbar.getText().toString().isEmpty() ?
                        " " + nmapBinding.nmapSearchbar.getText().toString() : "")
                .append(!nmapBinding.nmapPorts.getText().toString().isEmpty() ?
                        " -p " + nmapBinding.nmapPorts.getText().toString() : "");

        String command = sb.toString();
        Log.d("NMAP CMD OUTPUT: ", "nmap " + command);

        return command;
    }

    private void intentClickListener_NH(final String command) {
        try {
            Intent intent =
                    new Intent("com.offsec.nhterm.RUN_SCRIPT_NH");
            intent.addCategory(Intent.CATEGORY_DEFAULT);

            intent.putExtra("com.offsec.nhterm.iInitialCommand", command);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.toast_install_terminal), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
