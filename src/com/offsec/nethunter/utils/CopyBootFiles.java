package com.offsec.nethunter.utils;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CopyBootFiles extends AsyncTask<String, String, Void> {

    private static final String TAG = "CopyBootFiles";
    private final NhPaths nh;
    private final FirstBootStatusListener listener;
    private final AssetManager assetManager;

    public CopyBootFiles(FirstBootStatusListener listener, AssetManager assetManager) {
        this.listener = listener;
        this.assetManager = assetManager;
        this.nh = new NhPaths();
    }


    @Override
    protected Void doInBackground(String... data) {

        List<String> bootkali_list = new ArrayList<>();
        bootkali_list.add("bootkali");
        bootkali_list.add("bootkali_init");
        bootkali_list.add("bootkali_login");
        bootkali_list.add("bootkali_bash");
        bootkali_list.add("killkali");

        Log.d(TAG, "COPYING FILES....");
        // 1:1 copy (recursive) of the assets/{scripts, etc, wallpapers} folders to /data/data/...
        publishProgress("Doing app files update. (init.d and filesDir).");
        assetsToFiles(nh.APP_PATH, "", "data");
        // 1:1 copy (recursive) of the configs to  /sdcard...

        publishProgress("Doing sdcard files update. (nh_files).");
        assetsToFiles(nh.SD_PATH, "", "sdcard");

        publishProgress("Fixing permissions for new files");
        ShellExecuter exe = new ShellExecuter();
        exe.RunAsRoot(new String[]{"chmod 700 " + nh.APP_SCRIPTS_PATH + "/*", "chmod 700 " + nh.APP_INITD_PATH + "/*"});

        publishProgress("Checking for SYMLINKS to bootkali....");
        try {
            MakeSYSWriteable();

            // Loop over bootkali list (e.g. bootkali | bootkali_bash | bootkali_env)
            for (String temp : bootkali_list) {

                String sys_temp = "/system/bin/" + temp;

                // Define each as a new file
                File filePath = new File(sys_temp);

                // If the symlink is not found, then go create one!
                if (!isSymlink(filePath)) {
                    publishProgress("Creating symlink for " + temp);
                    NotFound(temp);
                }
            }
            MakeSYSReadOnly();

        } catch (IOException e) {
            e.printStackTrace();
        }

        publishProgress("Checking for chroot....");
        String command = "if [ -d " + nh.CHROOT_PATH + " ];then echo 1; fi"; //check the dir existence
        final String _res = exe.RunAsRootOutput(command);
        if (_res.equals("1")) {
            publishProgress("Chroot Found!");
            listener.onChrootCheckComplete(true);
            // Mount suid /data && fix sudo
            publishProgress(exe.RunAsRootOutput("busybox mount -o remount,suid /data && chmod +s " + nh.CHROOT_PATH + "/usr/bin/sudo && echo \"Initial setup done!\""));
        } else {
            publishProgress("Chroot not Found, install it in Chroot Manager");
        }
        publishProgress("All files copied.");
        try {
            Thread.sleep(1500); /*sleep before showing dialog*/
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(String... progress) {
        listener.onStatusUpdate(progress[0]);
    }

    @Override
    protected void onPostExecute(final Void avoid) {
        listener.onFirstBootComplete();
    }

    private Boolean pathIsAllowed(String path, String copyType) {
        // never copy images, sounds or webkit
        if (!path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit")) {
            if (copyType.equals("sdcard")) {
                if (path.equals("")) {
                    return true;
                } else if (path.startsWith(nh.NH_SD_FOLDER_NAME)) {
                    return true;
                }
                return false;
            }
            if (copyType.equals("data")) {
                if (path.equals("")) {
                    return true;
                } else if (path.startsWith("scripts")) {
                    return true;
                } else if (path.startsWith("wallpapers")) {
                    return true;
                } else if (path.startsWith("etc")) {
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    // now this only copies the folders: scripts, etc , wallpapers to /data/data...
    private void assetsToFiles(String TARGET_BASE_PATH, String path, String copyType) {
        String assets[];
        try {
            // Log.i("tag", "assetsTo" + copyType +"() "+path);
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyFile(TARGET_BASE_PATH, path);
            } else {
                String fullPath = TARGET_BASE_PATH + "/" + path;
                // Log.i("tag", "path="+fullPath);
                File dir = new File(fullPath);
                if (!dir.exists() && pathIsAllowed(path, copyType)) { // copy thouse dirs
                    if (!dir.mkdirs()) {
                        Log.i("tag", "could not create dir " + fullPath);
                    }
                }
                for (String asset : assets) {
                    String p;
                    if (path.equals("")) {
                        p = "";
                    } else {
                        p = path + "/";
                    }
                    if (pathIsAllowed(path, copyType)) {
                        assetsToFiles(TARGET_BASE_PATH, p + asset, copyType);
                    }
                }
            }
        } catch (IOException ex) {
            Log.e("tag", "I/O Exception", ex);
        }
    }

    private void copyFile(String TARGET_BASE_PATH, String filename) {
        InputStream in;
        OutputStream out;
        String newFileName = null;
        try {
            // Log.i("tag", "copyFile() "+filename);
            in = assetManager.open(filename);
            newFileName = TARGET_BASE_PATH + "/" + filename;
            out = new FileOutputStream(newFileName);
            byte[] buffer = new byte[8092];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (Exception e) {
            Log.e("tag", "Exception in copyFile() of " + newFileName);
            Log.e("tag", "Exception in copyFile() " + e.toString());
        }

    }

    // Check for symlink for bootkali
    // http://stackoverflow.com/questions/813710/java-1-6-determine-symbolic-links/813730#813730
    private static boolean isSymlink(File file) throws IOException {
        if (file == null)
            throw new NullPointerException("File must not be null");
        File canon;
        if (file.getParent() == null) {
            canon = file;
        } else {
            File canonDir = file.getParentFile().getCanonicalFile();
            canon = new File(canonDir, file.getName());
        }
        return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
    }

    private void MakeSYSWriteable() {
        ShellExecuter exe = new ShellExecuter();
        Log.d(TAG, "Making /system writeable for symlink");
        exe.RunAsRoot(new String[]{"mount -o rw,remount,rw /system"});
    }

    private void MakeSYSReadOnly() {
        ShellExecuter exe = new ShellExecuter();
        Log.d(TAG, "Making /system readonly for symlink");
        exe.RunAsRoot(new String[]{"mount -o ro,remount,ro /system"});
    }

    private void NotFound(String filename) {
        ShellExecuter exe = new ShellExecuter();
        Log.d(TAG, "Symlinking " + filename);
        Log.d(TAG, "command output: ln -s " + nh.APP_SCRIPTS_PATH + "/" + filename + " /system/bin/" + filename);

        exe.RunAsRoot(new String[]{"ln -s " + nh.APP_SCRIPTS_PATH + "/" + filename + " /system/bin/" + filename});
    }


    public interface FirstBootStatusListener {
        void onChrootCheckComplete(boolean chrootInstalled);

        void onStatusUpdate(String status);

        void onFirstBootComplete();
    }
}