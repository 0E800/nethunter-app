package com.offsec.nethunter;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.offsec.nethunter.adapters.WifiteRecyclerAdapter;
import com.offsec.nethunter.databinding.WifiteBinding;
import com.offsec.nethunter.ssh.PlayManaFragment;
import com.sshtools.net.SocketTransport;
import com.sshtools.ssh.ChannelOpenException;
import com.sshtools.ssh.PasswordAuthentication;
import com.sshtools.ssh.PseudoTerminalModes;
import com.sshtools.ssh.SshClient;
import com.sshtools.ssh.SshConnector;
import com.sshtools.ssh.SshException;
import com.sshtools.ssh.SshIOException;
import com.sshtools.ssh.SshSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class WifiteFragment extends KaliBaseFragment {

    private WifiteBinding binding;
    private SshSession session = null;
    private SshConnector con;
    private SshClient sshClient;
    private boolean shellStarted = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        SshClient sshClient = null;
        try {
            sshClient = con.connect(new SocketTransport("localhost",
                    22), "root", true);
        } catch (SshException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        WifiteFragment.this.sshClient = sshClient;

        PasswordAuthentication pwd = new PasswordAuthentication();
        pwd.setPassword("k9k3a47d");

        if (sshClient != null) {
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
//                        session.getOutputStream().write((command + "\n").getBytes());
//                            session.getOutputStream().flush();
//                        while ((line = br.readLine()) != null || !shellCancelled) {
//                            processShellOutput(line);
//                        }

                        br.close();
                    }

                } catch (SshException | ChannelOpenException e) {
                    e.printStackTrace();
                } catch (SshIOException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }




        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = WifiteBinding.inflate(inflater, container, false);
        WifiteRecyclerAdapter adapter = new WifiteRecyclerAdapter();
        binding.wifiteRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        binding.wifiteRecycler.setAdapter(adapter);

        return binding.getRoot();
    }
}
