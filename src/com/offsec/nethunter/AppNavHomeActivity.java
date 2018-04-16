package com.offsec.nethunter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.offsec.nethunter.gps.KaliGPSUpdates;
import com.offsec.nethunter.gps.LocationUpdateService;
import com.offsec.nethunter.ssh.PlayManaFragment;
import com.offsec.nethunter.utils.CopyBootFiles;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class AppNavHomeActivity extends AppCompatActivity implements
        KaliGPSUpdates.Provider, FragmentSwitcher, CopyBootFiles.FirstBootStatusListener {

    public final static String TAG = "AppNavHomeActivity";
    private static final String CHROOT_INSTALLED_TAG = "CHROOT_INSTALLED_TAG";
    public static final String BOOT_CHANNEL_ID = "BOOT_CHANNEL";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView navigationView;
    private CharSequence mTitle = "NetHunter";
    private SharedPreferences prefs;
    private MenuItem lastSelected;
    private Integer permsCurrent = 1;
    private boolean locationUpdatesRequested = false;
    private KaliGPSUpdates.Receiver locationUpdateReceiver;
    private NhPaths nh;
    private ProgressDialog newAppDialog;
    private boolean backPressWarned = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        prefs = getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);

        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            askMarshmallowPerms(permsCurrent);
        } else {
            checkForRoot();
        }

//        Hack to get files copied over
//        CopyBootFiles mytask = new CopyBootFiles(AppNavHomeActivity.this, AppNavHomeActivity.this.getAssets());
//        mytask.execute();

        setContentView(R.layout.base_layout);

        //set kali wallpaper as background
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeButtonEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }
        mDrawerLayout = findViewById(R.id.drawer_layout);

        navigationView = findViewById(R.id.navigation_view);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout navigationHeadView = (LinearLayout) inflater.inflate(R.layout.sidenav_header, null);
        navigationView.addHeaderView(navigationHeadView);

        FloatingActionButton readmeButton = navigationHeadView.findViewById(R.id.info_fab);
        readmeButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                showLicense();
                return false;
            }
        });

        /// moved build info to the menu
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd KK:mm:ss a zzz",
                Locale.US);

        final String buildTime = sdf.format(BuildConfig.BUILD_TIME);
        TextView buildInfo1 = navigationHeadView.findViewById(R.id.buildinfo1);
        TextView buildInfo2 = navigationHeadView.findViewById(R.id.buildinfo2);
        buildInfo1.setText(String.format("Version: %s (%s)", BuildConfig.VERSION_NAME, Build.TAGS));
        buildInfo2.setText(String.format("Built by %s at %s", BuildConfig.BUILD_NAME, buildTime));

//        if (SharedPreferencesvigationView != null) {
            setupDrawerContent(navigationView);
//        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // detail for android 5 devices
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.darkTitle));
        }


        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, NetHunterFragment.newInstance(R.id.nethunter_item))
                .commit();

        // if the nav bar hasn't been seen, let's show it
        if (!prefs.getBoolean("seenNav", false)) {
            mDrawerLayout.openDrawer(GravityCompat.START);
            SharedPreferences.Editor ed = prefs.edit();
            ed.putBoolean("seenNav", true);
            ed.apply();
        }

        if (lastSelected == null) { // only in the 1st create
            lastSelected = navigationView.getMenu().getItem(0);
            lastSelected.setChecked(true);
        }
        mDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout, R.string.drawer_opened, R.string.drawer_closed);
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        mDrawerToggle.syncState();
        // pre-set the drawer options
        setDrawerOptions();

        nh = new NhPaths();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            CharSequence name = getString(R.string.boot_notification_channel);
            String description = getString(R.string.boot_notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(BOOT_CHANNEL_ID, name, importance);
            mChannel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(mChannel);
            }
        }


    }


    public void setDrawerOptions() {
        Menu menuNav = navigationView.getMenu();
        if (prefs.getBoolean(CHROOT_INSTALLED_TAG, false)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    menuNav.setGroupEnabled(R.id.chrootDependentGroup, true);
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
//                !!!!!!!!!!!!!!!!!! HACK FOR DEV APP UPDATES !!!!!!!!!!!!11
                public void run() {
                    menuNav.setGroupEnabled(R.id.chrootDependentGroup, true);
                }
            });
        }
    }

    private void showLicense() {
        // @binkybear here goes the changelog etc... \n\n%s
        String readmeData = String.format("%s\n\n%s",
                getResources().getString(R.string.licenseInfo),
                getResources().getString(R.string.nhwarning));

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("README INFO")
                .setMessage(readmeData)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }); //nhwarning
        AlertDialog ad = adb.create();
        ad.setCancelable(false);
        ad.getWindow().getAttributes().windowAnimations = R.style.DialogStyle;
        ad.show();
    }

    /* if the chroot isn't set up, don't show the chroot options */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawers();
                } else {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        // only change it if is no the same as the last one
                        if (lastSelected != menuItem) {
                            //remove last
                            lastSelected.setChecked(false);
                            // udpate for the next
                            lastSelected = menuItem;
                        }
                        //set checked
                        menuItem.setChecked(true);
                        mDrawerLayout.closeDrawers();
                        int itemId = menuItem.getItemId();

                        switchFragment(itemId);
                        return true;
                    }
                });
    }

    private void switchFragment(int itemId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        backPressWarned = false;
        switch (itemId) {
            case R.id.nethunter_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, NetHunterFragment.newInstance(itemId))
                        .commit();
                break;

            case R.id.deauth_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, DeAuthFragment.newInstance(itemId))
                        .commit();
                break;

            case R.id.run_mana:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, PlayManaFragment.newInstance(itemId))
                        .commit();
                break;
            case R.id.kaliservices_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, KaliServicesFragment.newInstance(itemId))
                        .commit();
                break;

            case R.id.custom_commands_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, CustomCommandsFragment.newInstance(itemId))
                        .commit();
                break;

            case R.id.hid_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, HidFragment.newInstance(itemId))
                        .commit();
                break;
            case R.id.duckhunter_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, DuckHunterFragment.newInstance(itemId))
                        .commit();
                break;
            case R.id.badusb_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, BadusbFragment.newInstance(itemId))
                        .commit();
                break;
            case R.id.mana_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, ManaFragment.newInstance(itemId))
                        .commit();
                break;
            case R.id.macchanger_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, MacchangerFragment.newInstance(itemId))
                        .commit();
                break;
            case R.id.createchroot_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, ChrootManagerFragment.newInstance(itemId))
                        .commit();
                break;
            case R.id.mpc_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, MPCFragment.newInstance(itemId))
                        .commit();
                break;
            case R.id.mitmf_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, MITMfFragment.newInstance(itemId))
                        .commit();
                break;
            case R.id.vnc_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, VNCFragment.newInstance(itemId))
                        .commit();
                break;
            case R.id.searchsploit_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, SearchSploitFragment.newInstance(itemId))
                        .commit();
                break;
            case R.id.nmap_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, NmapFragment.newInstance(itemId))
                        .commit();
                break;
            case R.id.pineapple_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, PineappleFragment.newInstance(itemId))
                        .commit();
                break;

            case R.id.gps_item:
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, WardrivingFragment.newInstance(itemId))
                        .commit();
                break;
//
//            case R.id.settings_item:
//                fragmentManager
//                        .beginTransaction()
//                        .replace(R.id.container, KaliPreferenceFragment.newInstance(itemId))
//                        .commit();
//                break;

        }
    }

    private void checkForRoot() {
        Log.d("AppNav", "Checking for Root");
        new Thread(new Runnable() {
            @Override
            public void run() {
                ShellExecuter exe = new ShellExecuter();
                onCheckedForRoot(exe.isRootAvailable());
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (lastSelected != null && lastSelected.getItemId() != R.id.nethunter_item && !backPressWarned) {
            switchFragment(R.id.nethunter_item);
            lastSelected.setChecked(false);
            lastSelected = null;
            switchFragment(R.id.nethunter_item);
        } else if (!backPressWarned) {
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
            backPressWarned = true;
        } else {
            finish();
        }
    }

    private void askMarshmallowPerms(Integer permnum) {
        if (permnum == 1) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
            }
        }
        if (permnum == 2) {
            if (ContextCompat.checkSelfPermission(this,
                    "com.offsec.nhterm.permission.RUN_SCRIPT")
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{"com.offsec.nhterm.permission.RUN_SCRIPT"},
                        2);
            }
        }
        if (permnum == 3) {
            if (ContextCompat.checkSelfPermission(this,
                    "com.offsec.nhterm.permission.RUN_SCRIPT_SU")
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{"com.offsec.nhterm.permission.RUN_SCRIPT_SU"},
                        3);
            }
        }
        if (permnum == 4) {
            if (ContextCompat.checkSelfPermission(this,
                    "com.offsec.nhterm.permission.RUN_SCRIPT_NH")
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{"com.offsec.nhterm.permission.RUN_SCRIPT_NH"},
                        4);
            }
        }
        if (permnum == 5) {
            Log.d("HOLA", "CODE0: " + permnum);
            if (ContextCompat.checkSelfPermission(this,
                    "com.offsec.nhterm.permission.RUN_SCRIPT_NH_LOGIN")
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{"com.offsec.nhterm.permission.RUN_SCRIPT_NH_LOGIN"},
                        5);
            }
        }
        if (permnum == 6) {
            Log.d("HOLA", "CODE0: " + permnum);
            if (ContextCompat.checkSelfPermission(this,
                    "com.offsec.nhvnc.permission.OPEN_VNC_CONN")
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{"com.offsec.nhvnc.permission.OPEN_VNC_CONN"},
                        6);
            }
        }
        if (permnum == 7) {
            Log.d("HOLA", "CODE0: " + permnum);
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        7);
            }
        }
        if (permnum == 8) {
            Log.d("HOLA", "CODE0: " + permnum);
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        8);
            }
            // Add checkForRoot after last permission check.  Sometimes files don't copy over on first run
            // so we need to force it to check.  Doing it earlier runs risk of no permission and SuperSU
            // display conflicting with permission requests
            checkForRoot();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Integer permsNum = 8;
            if (permsCurrent < permsNum) {
                Log.d("AppNav", "Ask permission");
                permsCurrent++;
                askMarshmallowPerms(permsCurrent);
            } else {
                Log.d("AppNav", "Permissions granted");
                checkForRoot();
            }
        } else {
            Log.d("AppNav", "Not granted permission");
            askMarshmallowPerms(permsCurrent);
        }
    }

    @Override
    public void onLocationUpdatesRequested(KaliGPSUpdates.Receiver receiver) {
        this.locationUpdateReceiver = receiver;
        locationUpdatesRequested = true;
        if (updateServiceBound) {
            locationService.requestUpdates(receiver);
        } else {
            Intent intent = new Intent(getApplicationContext(), LocationUpdateService.class);
            bindService(intent, locationServiceConnection, Context.BIND_AUTO_CREATE);
        }



    }

    private LocationUpdateService locationService;
    private boolean updateServiceBound = false;
    private ServiceConnection locationServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to Update Service, cast the IBinder and get LocalService instance
            LocationUpdateService.ServiceBinder binder = (LocationUpdateService.ServiceBinder) service;
            locationService = binder.getService();
            updateServiceBound = true;
            if (locationUpdatesRequested) {
                locationService.requestUpdates(locationUpdateReceiver);
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            updateServiceBound = false;
        }
    };


    @Override
    public void onStopRequested() {
        if (locationService != null) {
            locationService.stopUpdates();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onAddRemoveFragmentRequested(int fragmentId) {
        switch (fragmentId) {
            case R.string.prefkey_mana_ap_ifc:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, AddRemoveListPreference.newInstance(fragmentId))
                        .commit();
        }

    }

    public void onCheckedForRoot(boolean rootEnabled) {
        if (rootEnabled) {
            checkForNewAppBuild();
            setDrawerOptions();
        } else {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle(R.string.rootdialogtitle)
                    .setMessage(R.string.rootdialogmessage)
                    .setPositiveButton(R.string.rootdialogposbutton, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            checkForRoot();
                        }
                    })
                    .setNegativeButton(R.string.rootdialognegbutton, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            AlertDialog ad = adb.create();
            ad.setCancelable(false);
            ad.show();
        }
    }

    private final String COPY_ASSETS_TAG = "COPY_ASSETS_TAG";

    private void checkForNewAppBuild() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd KK:mm:ss a zzz",
                Locale.US);
        String buildTime = sdf.format(BuildConfig.BUILD_TIME);

        File sdCardDir = new File(nh.APP_SD_FILES_PATH);
        File scriptsDir = new File(nh.APP_SCRIPTS_PATH);
        File etcDir = new File(nh.APP_INITD_PATH);

        if (!prefs.getString(COPY_ASSETS_TAG, buildTime)
                .equals(buildTime) || !sdCardDir.isDirectory() || !scriptsDir.isDirectory() || !etcDir.isDirectory()) {
            CopyBootFiles mytask = new CopyBootFiles(AppNavHomeActivity.this, AppNavHomeActivity.this.getAssets());
            newAppDialog = new ProgressDialog(this);
            newAppDialog.setTitle("New app build detected:");
            newAppDialog.setMessage("Coping new files...");
            newAppDialog.setCancelable(false);
            newAppDialog.show();
            prefs.edit()
                    .putString(COPY_ASSETS_TAG, buildTime)
                    .apply();
            mytask.execute();
        } else {
            Log.d(COPY_ASSETS_TAG, "FILES NOT COPIED");
        }


    }

    @Override
    public void onChrootCheckComplete(boolean chrootInstalled) {
        setDrawerOptions();
    }

    @Override
    public void onStatusUpdate(String status) {
        if (newAppDialog!= null) {
            newAppDialog.setMessage(status);
        }
    }

    @Override
    public void onFirstBootComplete() {
        if (newAppDialog != null) {
            newAppDialog.dismiss();
        }
    }
}

