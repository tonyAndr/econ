package org.osmdroid;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.osmdroid.views.util.BroadcastNotifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by Tony on 29-Jan-15.
 */
public class UnZipService extends IntentService {

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
//    private BroadcastNotifier mBroadcaster;
    static final String path_osm = Environment.getExternalStorageDirectory().getPath() + "/osmdroid/";

    public UnZipService() {

        super("UnZipService");
//        mBroadcaster = new BroadcastNotifier(getApplicationContext());
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        String dataString = workIntent.getDataString();
        Log.v("IntentURI", dataString);
//        Handler mHandler = new Handler(getMainLooper());
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(getApplicationContext(), "Installing map...", Toast.LENGTH_SHORT).show();
//            }
//        });
//
        Intent intent = new Intent();
        intent.setAction("org.osmdroid.UnzipService");
        intent.putExtra("status", "Installing...");
        sendBroadcast(intent);
        doUnzip(dataString, path_osm);
        intent.putExtra("status", "Done!");
        sendBroadcast(intent);
//
//
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(getApplicationContext(), "Map succesfully installed.", Toast.LENGTH_LONG).show();
//            }
//        });
    }

    public boolean doUnzip(String inputZipFile, String destinationDirectory)
    {
        try {
            int BUFFER = 2048;
            List<String> zipFiles = new ArrayList<String>();
            File sourceZipFile = new File(inputZipFile);
            File unzipDestinationDirectory = new File(destinationDirectory);
            unzipDestinationDirectory.mkdir();
            ZipFile zipFile;
            zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
            Enumeration zipFileEntries = zipFile.entries();
            while (zipFileEntries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
                String currentEntry = entry.getName();
                File destFile = new File(unzipDestinationDirectory, currentEntry);
                if (currentEntry.endsWith(".zip")) {
                    zipFiles.add(destFile.getAbsolutePath());
                }

                File destinationParent = destFile.getParentFile();

                destinationParent.mkdirs();

                try {
                    if (!entry.isDirectory()) {
                        BufferedInputStream is =
                                new BufferedInputStream(zipFile.getInputStream(entry));
                        int currentByte;
                        byte data[] = new byte[BUFFER];

                        FileOutputStream fos = new FileOutputStream(destFile);
                        BufferedOutputStream dest =
                                new BufferedOutputStream(fos, BUFFER);
                        while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
                            dest.write(data, 0, currentByte);
                        }
                        dest.flush();
                        dest.close();
                        is.close();
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            zipFile.close();

            for (Iterator<String> iter = zipFiles.iterator(); iter.hasNext();) {
                String zipName = (String)iter.next();
                doUnzip(
                        zipName,
                        destinationDirectory +
                                File.separatorChar +
                                zipName.substring(0,zipName.lastIndexOf(".zip"))
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false ;
        }
        File sourceZipFile = new File(inputZipFile);
        if (sourceZipFile.exists()) {
            sourceZipFile.delete();
        }

        return true;
    }
}