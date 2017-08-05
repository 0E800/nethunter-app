package com.offsec.nethunter.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;


public class FileWriter {
    private final File file;
    private FileOutputStream fOut;
    private OutputStreamWriter outputWriter;
    private boolean fileClosed = false;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public FileWriter() {
        File path = Environment.getExternalStoragePublicDirectory("/output/");
        // Make sure the path directory exists.
        // Make it, if it doesn't exit
        if (!path.exists()) {
            path.mkdirs();
        }
         file = new File(path, "mana.txt");
        fileClosed = false;
    }

    public void writeToFile(String data) {

        // Save your stream, don't forget to flush() it before closing it.
        if (fileClosed) {
            return;
        }

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            if (fOut == null) {
                fOut = new FileOutputStream(file);
            }
            if (outputWriter == null) {
                outputWriter = new OutputStreamWriter(fOut);
            }
            outputWriter.append(data + "\r\n");

        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public void closeFile() {
        try {
            fileClosed = true;
            outputWriter.flush();
            outputWriter.close();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


