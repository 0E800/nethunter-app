package com.offsec.nethunter.ssh;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.offsec.nethunter.KaliBaseFragment;
import com.offsec.nethunter.R;
import com.offsec.nethunter.utils.FileWriter;
import com.offsec.nethunter.utils.QueuedTextViewWrapper;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SSHFragment extends KaliBaseFragment {

    private ManaUpdateAdapter ssidAdapter;
    private ManaUpdateAdapter certificateAdapter;
    private SshConnector con;
    private Button sshStopButton;
    private TextView numClientsView;
    private int numClients = 0;
    private Pattern p = Pattern.compile("'([^']+)'");
    private FileWriter fileWriter;
    private SshSession session = null;
    private boolean shellCancelled = false;
    private boolean shellStarted = false;
    private int textColorSecondary;
    private QueuedTextViewWrapper queuedTextViewWrapper;


    public static Fragment newInstance(int itemId) {
        SSHFragment fragment = new SSHFragment();
        fragment.putSectionNumber(itemId);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        fileWriter = new FileWriter(context);
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        textColorSecondary = typedValue.data;
        super.onAttach(context);
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_start_mana) {
            executeCommand("start-mana-full-lollipop");
            return true;
        } else if (item.getItemId() == R.id.menu_start_mana) {
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.play_mana_fragment, container, false);

        ((Button) v.findViewById(R.id.ssh_run)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeCommand("start-mana-full-lollipop");
            }
        });
        sshStopButton = (Button) v.findViewById(R.id.ssh_stop);
        sshStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    session.getOutputStream().write("\r\n".getBytes());
                    shellCancelled = true;
                    shellStarted = false;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ssidAdapter.resetAll();
                            showStatus("", false);
                        }
                    });
                    numClients = 0;
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        queuedTextViewWrapper = new QueuedTextViewWrapper(
                (TextView) v.findViewById(R.id.ssh_status));
        numClientsView = (TextView) v.findViewById(R.id.num_connected);

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final LinearLayout ssidLayout = (LinearLayout) view.findViewById(R.id.mana_layout_left);
        final LinearLayout llCert = (LinearLayout) view.findViewById(R.id.ll_cert);
        ssidAdapter = new ManaUpdateAdapter(ssidLayout, 3000, 6000);
        certificateAdapter = new ManaUpdateAdapter(llCert, 4000, 7000);
        LayoutTransition lt = new LayoutTransition();
        lt.disableTransitionType(LayoutTransition.DISAPPEARING);
        ssidLayout.setLayoutTransition(lt);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.start_mana, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    public SshClient sshClient;

    private void processShellOutput(String received) {
        fileWriter.writeToFile(received);
        if (received.contains("Directed")) {
            Matcher m = p.matcher(received);
            while (m.find()) {
                String ssid = received.substring(m.start() + 1, m.end() - 1);
//                    must be run on ui thread
                new SSIDAdapterUIThread(getActivity(), ssid, ssidAdapter).run();
            }
        } else if (received.contains("Attempting")) {
            String attemptingLine = received.split("response : ")[1];

            String ssid;
            if (attemptingLine.contains("(")) {
                ssid = attemptingLine.split(" (.*) ")[0].trim();
            } else {
                ssid = attemptingLine.split(" for")[0].trim();
            }
            //                    must be run on ui thread
            new SSIDAdapterUIThread(getActivity(), ssid, ssidAdapter).run();
        } else if (received.contains("AP-STA-CONNECTED")) {
            numClients++;
            new SetTextUIThread(getActivity(), String.valueOf(numClients), numClientsView).run();
        } else if (received.contains("AP-STA-DISCONNECTED")) {
            numClients--;
            new SetTextUIThread(getActivity(), String.valueOf(numClients), numClientsView).run();
        } else if (received.contains("STA to kernel")) {
//                couldn't add station to kernel driver
            showStatus("Max connections for kernel reached", true);
        } else if (received.contains("Original server certificate:")) {
            certificateAdapter.onTextUpdated(received.substring(received.indexOf("/CN=") + 1));
        } else if (received.contains("could not read interface")) {
            showStatus("Could not read interface. Stopping processes", true);
            sshStopButton.callOnClick();
        }
        if (received.contains("UNINITIALIZED->Enabled")) {
            showStatus("Access Point Enabled", false);
        }
        if (received.contains("Hit enter to kill me")) {
            showStatus("Mana fully started", false);
        }

        Log.d("Received", received);
    }

    @Override
    public void onStop() {
        super.onStop();
        fileWriter.closeFile();
    }

    private static class SSIDAdapterUIThread implements Runnable {
        private final Activity activity;
        private final String ssid;
        private ManaUpdateAdapter manaAdapter;

        public SSIDAdapterUIThread(Activity activity, String ssid, ManaUpdateAdapter manaAdapter) {
            this.activity = activity;

            this.ssid = ssid;
            this.manaAdapter = manaAdapter;
        }

        @Override
        public void run() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    manaAdapter.onTextUpdated(ssid);
                }
            });
        }
    }

    private static class SetTextUIThread implements Runnable {
        private final Activity activity;
        private final String text;
        private TextView textView;

        SetTextUIThread(Activity activity, String text, TextView textView) {
            this.activity = activity;

            this.text = text;
            this.textView = textView;
        }

        @Override
        public void run() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    textView.setText(text);
                }
            });
        }
    }

    private void executeCommand(final String command) {
        try {
            con = SshConnector.createInstance();
        } catch (SshException e) {
            e.printStackTrace();
        }
        Thread th = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    final SshClient sshClient = con.connect(new SocketTransport("localhost",
                            22), "root", true);
                    SSHFragment.this.sshClient = sshClient;

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
                            if (!shellStarted) {
                                PseudoTerminalModes pty = new PseudoTerminalModes(sshClient);
                                pty.setTerminalMode(PseudoTerminalModes.ECHO, false);
                                session.requestPseudoTerminal("vt100", 80, 24, 0, 0, pty);
                                session.startShell();
                                shellStarted = true;
//                                session.setAutoConsumeInput(true);
                                InputStreamReader is = new InputStreamReader(session.getInputStream());
                                BufferedReader br = new BufferedReader(is);
                                String line;
                                session.getOutputStream().write((command + "\n").getBytes());
//                                showStatus("Mana started", false);
//                            session.getOutputStream().flush();
                                while ((line = br.readLine()) != null || !shellCancelled) {
                                    processShellOutput(line);
                                }

                                br.close();
                            }

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

    private void showStatus(final String status, final boolean isError) {
//        todo: save animationStatus for restore on view change
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int color;
                if (isError) {
                    color = Color.RED;
                } else {
                    color = textColorSecondary;
                }
                queuedTextViewWrapper.setText(status, color);
            }
        });


    }
}
