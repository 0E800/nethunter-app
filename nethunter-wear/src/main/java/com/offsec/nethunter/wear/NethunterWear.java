package com.offsec.nethunter.wear;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.Set;

public class NethunterWear extends WearableActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        CapabilityApi.CapabilityListener {

        private static final String TAG = "NethungerWear";

        // Name of capability listed in Phone app's wear.xml.
        // IMPORTANT NOTE: This should be named differently than your Wear app's capability.
        private static final String CAPABILITY_PHONE_APP = "verify_remote_nethunter_phone_app";
        private static final String CAPABILITY_CONTROL_MANA = "control_mana";


        private Node mAndroidPhoneNodeWithApp;

        private GoogleApiClient mGoogleApiClient;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            Log.d(TAG, "onCreate()");
            super.onCreate(savedInstanceState);

            setContentView(R.layout.activity_nethunter_wear);
            setAmbientEnabled();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }


        @Override
        protected void onPause() {
            Log.d(TAG, "onPause()");
            super.onPause();

            if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {
                Wearable.CapabilityApi.removeCapabilityListener(
                        mGoogleApiClient,
                        this,
                        CAPABILITY_PHONE_APP);

                mGoogleApiClient.disconnect();
            }
        }

        @Override
        protected void onResume() {
            Log.d(TAG, "onResume()");
            super.onResume();
            if (mGoogleApiClient != null) {
                mGoogleApiClient.connect();
            }
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d(TAG, "onConnected()");

            // Set up listeners for capability changes (install/uninstall of remote app).
            Wearable.CapabilityApi.addCapabilityListener(
                    mGoogleApiClient,
                    this,
                    CAPABILITY_PHONE_APP);
            Wearable.CapabilityApi.addCapabilityListener(
                    mGoogleApiClient,
                    this,
                    CAPABILITY_CONTROL_MANA);

            checkIfPhoneHasApp();
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "onConnectionSuspended(): connection to location client suspended: " + i);
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.e(TAG, "onConnectionFailed(): " + connectionResult);
        }

        /*
         * Updates UI when capabilities change (install/uninstall phone app).
         */
        public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
            Log.d(TAG, "onCapabilityChanged(): " + capabilityInfo);

            mAndroidPhoneNodeWithApp = pickBestNodeId(capabilityInfo.getNodes());
            verifyNode();
        }

        private void checkIfPhoneHasApp() {
            Log.d(TAG, "checkIfPhoneHasApp()");

            PendingResult<CapabilityApi.GetCapabilityResult> pendingResult =
                    Wearable.CapabilityApi.getCapability(
                            mGoogleApiClient,
                            CAPABILITY_PHONE_APP,
                            CapabilityApi.FILTER_ALL);

            pendingResult.setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {

                @Override
                public void onResult(@NonNull CapabilityApi.GetCapabilityResult getCapabilityResult) {
                    Log.d(TAG, "onResult(): " + getCapabilityResult);

                    if (getCapabilityResult.getStatus().isSuccess()) {
                        CapabilityInfo capabilityInfo = getCapabilityResult.getCapability();
                        mAndroidPhoneNodeWithApp = pickBestNodeId(capabilityInfo.getNodes());
                        verifyNode();

                    } else {
                        Log.d(TAG, "Failed CapabilityApi: " + getCapabilityResult.getStatus());
                    }
                }
            });
        }

        private void verifyNode() {

            if (mAndroidPhoneNodeWithApp != null) {

                // TODO: Add your code to communicate with the phone app via
                // Wear APIs (MessageApi, DataApi, etc.)

                Log.d(TAG, "App installed on " + mAndroidPhoneNodeWithApp.getDisplayName());

            } else {
                Log.d(TAG, "App on phone missing");
            }
        }

        /*
         * There should only ever be one phone in a node set (much less w/ the correct capability), so
         * I am just grabbing the first one (which should be the only one).
         */
        private Node pickBestNodeId(Set<Node> nodes) {
            Log.d(TAG, "pickBestNodeId(): " + nodes);

            Node bestNodeId = null;
            // Find a nearby node/phone or pick one arbitrarily. Realistically, there is only one phone.
            for (Node node : nodes) {
                bestNodeId = node;
            }
            return bestNodeId;
        }
    }