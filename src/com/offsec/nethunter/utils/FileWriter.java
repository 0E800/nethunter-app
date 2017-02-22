package com.offsec.nethunter.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;

/**
 * Created by ilak on 2/2/17.
 */

public class FileWriter {
    private final WeakReference<Context> contextRef;
    private FileOutputStream fOut;
    private OutputStreamWriter outputWriter;

    public FileWriter(Context context) {
        contextRef = new WeakReference<Context>(context);
    }
    public void writeToFile(String data)
    {
        // Get the directory for the user's public pictures directory.
        final File path =
                Environment.getExternalStoragePublicDirectory
                        (
                                //Environment.DIRECTORY_PICTURES
                                "/output/"
                        );

        // Make sure the path directory exists.
        if(!path.exists())
        {
            // Make it, if it doesn't exit
            path.mkdirs();
        }

        final File file = new File(path, "mana.txt");

        // Save your stream, don't forget to flush() it before closing it.

        try
        {
            if (!file.exists()) {
                file.createNewFile();
            }
            if (fOut == null) {
                fOut = new FileOutputStream(file);
            }
            if (outputWriter == null) {
                outputWriter = new OutputStreamWriter(fOut);
            }
            outputWriter.append(data);
            fOut.flush();

        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
    public void closeFile() {
        try {
            outputWriter.close();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


