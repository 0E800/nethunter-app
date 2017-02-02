package com.offsec.nethunter.ssh;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.transition.Slide;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.offsec.nethunter.KaliBaseFragment;
import com.offsec.nethunter.R;
import com.sshtools.net.SocketTransport;
import com.sshtools.ssh.ChannelAdapter;
import com.sshtools.ssh.ChannelOpenException;
import com.sshtools.ssh.PasswordAuthentication;
import com.sshtools.ssh.PseudoTerminalModes;
import com.sshtools.ssh.SshChannel;
import com.sshtools.ssh.SshClient;
import com.sshtools.ssh.SshConnector;
import com.sshtools.ssh.SshException;
import com.sshtools.ssh.SshSession;

import java.io.IOException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SSHFragment extends KaliBaseFragment {

    private LinearLayout llLeft;
    private LinearLayout llRight;
    private ManaSSIDAdapter ssidAdapter;
    private TextView statusView;
    private SshConnector con;
    private LinkedList<String> updateList = new LinkedList<>();
    private Button sshStopButton;
    private TextView numClientsView;
    private int numClients = 0;
    private Pattern p = Pattern.compile("'([^']+)'");


    public static Fragment newInstance(int itemId) {
        SSHFragment fragment = new SSHFragment();
        fragment.putSectionNumber(itemId);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.ssh_fragment, container, false);
        final Button sshButton = (Button) v.findViewById(R.id.ssh_run);
        sshStopButton = (Button) v.findViewById(R.id.ssh_stop);
        sshStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    final SshSession session2 = sshClient
                            .openSessionChannel(eventListener);
                    sshButton.setEnabled(true);
                    sshStopButton.setEnabled(false);
                    PseudoTerminalModes pty = new PseudoTerminalModes(sshClient);
                    pty.setTerminalMode(PseudoTerminalModes.ECHO, false);
                    session2.requestPseudoTerminal("vt100", 80, 24, 0, 0, pty);
                    session2.setAutoConsumeInput(true);
//                    session2.startShell();
                    session2.executeCommand("pkill dnsmasq; pkill python; pkill hostapd; pkill sslsplit");
                    ssidAdapter.resetAll();
                    numClients = 0;
                } catch (SshException | ChannelOpenException e) {
                    e.printStackTrace();
                }

            }
        });
        sshButton.setOnClickListener(sshButtonListener);
        statusView = (TextView) v.findViewById(R.id.ssh_status);
        numClientsView = (TextView) v.findViewById(R.id.num_connected);

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        llLeft = (LinearLayout) view.findViewById(R.id.mana_layout_left);
        ssidAdapter = new ManaSSIDAdapter(llLeft);
        LayoutTransition lt = new LayoutTransition();
        lt.disableTransitionType(LayoutTransition.DISAPPEARING);
        llLeft.setLayoutTransition(lt);

        LayoutTransition lt2 = new LayoutTransition();

    }

    private View.OnClickListener sshButtonListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            try {
                v.setEnabled(false);
                sshStopButton.setEnabled(true);
                executeSSHCommandUsingEventListener("start-mana-full-lollipop");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.start_mana, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    public SshClient sshClient;
//    final SSHConnectionListener threadWithChannelListener = new SSHConnectionListener() {
//
//        @Override
//        public void onSSHConnected(SshClient sshClient) {
//
//        }
//
//        ;CHANGE_DISAPPEARING

    private final ChannelAdapter eventListener = new ChannelAdapter() {

        public void dataReceived(SshChannel channel, byte[] buf,
                                 int offset, int len) {
            final String received = new String(buf).substring(offset, offset + len);
            if (received.contains("Directed")) {
                Matcher m = p.matcher(received);
                while (m.find()) {
                    String ssid = received.substring(m.start() + 1, m.end() - 1);
//                    must be run on ui thread
                    new SSIDAdapterUIThread(getActivity(), ssid).run();
                }
            }
            if (received.contains("Attempting")) {
                String attemptingLine = received.split("response : ")[1];

                String ssid;
                if (attemptingLine.contains("(")) {
                    ssid = attemptingLine.split(" (.*) ")[0].trim();
                } else {
                    ssid = attemptingLine.split(" for")[0].trim();
                }
                //                    must be run on ui thread
                new SSIDAdapterUIThread(getActivity(), ssid).run();
            } else if (received.contains("AP-STA-CONNECTED")) {
                numClients++;
                new SetTextUIThread(getActivity(), String.valueOf(numClients), numClientsView).run();
            } else if (received.contains("AP-STA-DISCONNECTED")) {
                numClients--;
                new SetTextUIThread(getActivity(), String.valueOf(numClients), numClientsView).run();
            } else if (received.contains("STA to kernel")) {
//                couldn't add station to kernel driver
                statusView.setText("Max connections for kernel reached");
            }

//            updateList.addFirst(received);
//            if (updateList.size() > 3) {
//                updateList.remove(updateList.size());
//            }
//
//            StringBuilder sb = new StringBuilder();
//
//            for (String text :
//                    updateList) {
//                sb.append(text + "\n");
//            }
//            statusTextView.setText(sb.toString());
//            statusTextView.append(received + "\n");
            Log.d("Received", received);
        }

        public synchronized void channelClosed(SshChannel channel) {
            notifyAll();
        }

    };

    private class SSIDAdapterUIThread implements Runnable {
        private final Activity activity;
        private final String ssid;

        public SSIDAdapterUIThread(Activity activity, String ssid) {
            this.activity = activity;

            this.ssid = ssid;
        }

        @Override
        public void run() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    ssidAdapter.ssidUpdate(ssid);
                }
            });
        }
    }

    private static class SetTextUIThread implements Runnable {
        private final Activity activity;
        private final String text;
        private TextView textView;

        public SetTextUIThread(Activity activity, String text, TextView textView) {
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

    private void executeSSHCommandUsingEventListener(final String command) throws IOException {
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
                            final SshSession session2 = sshClient
                                    .openSessionChannel();
                            session2.addChannelEventListener(eventListener);
                            PseudoTerminalModes pty = new PseudoTerminalModes(sshClient);
                            pty.setTerminalMode(PseudoTerminalModes.ECHO, false);
                            session2.requestPseudoTerminal("vt100", 80, 24, 0, 0, pty);
                            session2.setAutoConsumeInput(true);
//                    session2.startShell();
                            session2.executeCommand(command);
//
//                    final SshSession session3 = sshClient
//                            .openSessionChannel(eventListener);
//                    pty.setTerminalMode(PseudoTerminalModes.ECHO, false);
//                    session3.requestPseudoTerminal("vt100", 80, 24, 0, 0, pty);
//                    session3.setAutoConsumeInput(true);
//
//                    session3.getOutputStream().write("\n".getBytes());

                            synchronized (eventListener) {
                                eventListener.wait();
                            }
                        } catch (SshException | ChannelOpenException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
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


}
