package com.offsec.nethunter.ssh;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.offsec.nethunter.AnimatedStatusView;
import com.offsec.nethunter.KaliBaseFragment;
import com.offsec.nethunter.PlayManaBinding;
import com.offsec.nethunter.PlayManaViewModel;
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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PlayManaFragment extends KaliBaseFragment {

    private ManaUpdateAdapter ssidAdapter;
    private ManaUpdateAdapter certificateAdapter;
    private SshConnector con;
    private Pattern p = Pattern.compile("'([^']+)'");
    private FileWriter fileWriter;
    private SshSession session = null;
    private boolean shellCancelled = false;
    private boolean shellStarted = false;
    private final PlayManaViewModel viewModel = new PlayManaViewModel();
    private PlayManaBinding binding;
    private AnimatedStatusView animatedStatusView;


    public static PlayManaFragment newInstance(int itemId) {
        PlayManaFragment fragment = new PlayManaFragment();
        fragment.putSectionNumber(itemId);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        super.onAttach(context);
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_start_mana) {
            executeInitialCommand("start-mana-full-lollipop");
            return true;
        } else if (item.getItemId() == R.id.menu_stop_mana) {

            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = PlayManaBinding.inflate(inflater, container, false);
        binding.setViewModel(viewModel);


        binding.sshRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileWriter = new FileWriter();
                executeInitialCommand("start-mana-full-lollipop");
                viewModel.setInitialized(true);
//                replayFromFile();
            }
        });
        binding.sshStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    executeAdditionalCommand("\r\n".getBytes());
                    shellCancelled = true;
                    shellStarted = false;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ssidAdapter.resetAll();
                        }
                    });
                    viewModel.resetClients();
                    fileWriter.closeFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        return binding.getRoot();
    }

    private void executeAdditionalCommand(final byte[] bytes) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    session.getOutputStream().write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    private void replayFromFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File path = Environment.getExternalStoragePublicDirectory("/output/");

//Get the text file
                File file = new File(path,"manareally.txt");

//Read text from file
                try {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = br.readLine()) != null) {
                        Thread.sleep(500);
                        processShellOutput(line);
                    }
                    br.close();
                }
                catch (IOException e) {
                    //You'll need to add proper error handling here
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        })
                .start();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final LinearLayout ssidLayout = view.findViewById(R.id.mana_layout_left);
        final LinearLayout llCert = view.findViewById(R.id.ll_cert);
        ssidAdapter = new ManaUpdateAdapter(ssidLayout, 3000, 6000);
        certificateAdapter = new ManaUpdateAdapter(llCert, 4000, 7000);
        LayoutTransition lt = new LayoutTransition();
        lt.disableTransitionType(LayoutTransition.DISAPPEARING);
        ssidLayout.setLayoutTransition(lt);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.start_mana, menu);
    }

    public SshClient sshClient;

    private void processShellOutput(String received) {
        fileWriter.writeToFile(received);
        if (received.contains("Directed")) {
            Matcher m = p.matcher(received);
            while (m.find()) {
                String ssid = received.substring(m.start() + 1, m.end() - 1);
//                    must be run on ui thread
                new ViewAdapter(getActivity(), ssid, ssidAdapter).run();
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
            new ViewAdapter(getActivity(), ssid, ssidAdapter).run();

        } else if (received.contains("AP-STA-CONNECTED")) {
           viewModel.incrementConnected();
        } else if (received.contains("AP-STA-DISCONNECTED")) {
            viewModel.decrementConnected();
        } else if (received.contains("STA to kernel")) {
//                couldn't add station to kernel driver
            viewModel.setError("Max connections for kernel");

        } else if (received.contains("Subject DN:")) {
            String cert = received.split("O=")[1];
            cert = cert.substring(0, cert.indexOf('/'));

            new ViewAdapter(getActivity(), cert, certificateAdapter).run();

        } else if (received.contains("could not read interface")) {
            viewModel.setError("Could not find wlan1");
            binding.sshStop.callOnClick();
        } else if (received.contains("AP-ENABLED")) {
        } else if (received.contains("Hit enter to kill me")) {
            viewModel.setStarted(true);
        } else if (received.contains("Cannot find device")) {
            viewModel.setError("Could not find wlan1");
            binding.getRoot().postDelayed(new Runnable() {
                @Override
                public void run() {
                    binding.sshStop.callOnClick();
                }
            }, 5000);
        }

        Log.d("Received", received);
    }

    private static class ViewAdapter implements Runnable {
        private final Activity activity;
        private final String text;
        private ManaUpdateAdapter manaAdapter;

        public ViewAdapter(Activity activity, String text, ManaUpdateAdapter manaAdapter) {
            this.activity = activity;

            this.text = text;
            this.manaAdapter = manaAdapter;
        }

        @Override
        public void run() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    manaAdapter.onTextUpdated(text);
                }
            });
        }
    }

    private void executeInitialCommand(final String command) {
        viewModel.setJustReset(false);
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
                    PlayManaFragment.this.sshClient = sshClient;

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
}
