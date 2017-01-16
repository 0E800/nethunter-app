package com.offsec.nethunter;

import android.app.Activity;
import android.database.Cursor;

import com.sonelli.juicessh.pluginlibrary.PluginClient;
import com.sonelli.juicessh.pluginlibrary.PluginContract;
import com.sonelli.juicessh.pluginlibrary.exceptions.ServiceNotConnectedException;
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener;
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener;

import java.util.UUID;


public class SSHManager extends PluginClient implements OnSessionStartedListener {
    private static SSHManager thisManager = null;
    public static final int REQUEST_ID = 492;
    private int sessionID = -1;
    private String sessionKey;
    private UUID connectionId;
    private boolean isConnecting = false;
    private String queuedCommand = null;
    private OnSessionExecuteListener onSessionExecutedListener;

    public static SSHManager getInstance() {
        if (thisManager == null) {
            thisManager = new SSHManager();
        }
        return thisManager;
    }

    public void connect(Activity activity) {
        isConnecting = true;
        Cursor cursor = activity.getContentResolver().query(
                PluginContract.Connections.CONTENT_URI,
                new String[]{PluginContract.Connections.COLUMN_ID, PluginContract.Connections.COLUMN_ADDRESS},
                PluginContract.Connections.COLUMN_ADDRESS + " = ?",
                new String[]{"127.0.0.1"}, null);

        while (cursor.moveToNext()) {

            // Then update them
            connectionId = UUID.fromString(cursor.getString(
                    cursor.getColumnIndex(PluginContract.Connections.COLUMN_ID)));
        }
        cursor.close();

//        todo: check for more than 1 connection or better yet, select in settings
        try {
            thisManager.connect(activity, connectionId, this, REQUEST_ID);
        } catch (ServiceNotConnectedException e) {
            e.printStackTrace();
        }
    }

    public void execute(String command, OnSessionExecuteListener listener) throws ServiceNotConnectedException {
        onSessionExecutedListener = listener;
        if (isConnecting && !isConnected()) {
            this.queuedCommand = command;
        }
        if (!isConnecting && !isConnected() || sessionID == -1 || sessionKey == null) {
            throw new ServiceNotConnectedException();
        } else {
            thisManager.executeCommandOnSession(sessionID, sessionKey, command, listener);
        }
    }

    @Override
    public void executeCommandOnSession(int sessionId, String sessionKey, String command, OnSessionExecuteListener listener) throws ServiceNotConnectedException {
        queuedCommand = null;
        super.executeCommandOnSession(sessionId, sessionKey, command, listener);
    }

    @Override
    public void onSessionStarted(int sessionID, String sessionKey) {
        this.sessionID = sessionID;
        this.sessionKey = sessionKey;
        if (queuedCommand != null) {
            try {
                thisManager.executeCommandOnSession(sessionID, sessionKey, queuedCommand, onSessionExecutedListener);
            } catch (ServiceNotConnectedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSessionCancelled() {

    }
}
