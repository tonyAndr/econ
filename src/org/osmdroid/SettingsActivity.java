package org.osmdroid;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.SyncStateContract;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Toast;

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

import static org.osmdroid.views.util.UnzipUtility.unzip;

public class SettingsActivity extends ActionBarActivity {

    static final String mapsUrl = "http://alberguenajera.es/projects/ecn/Mapsforge2-11_2015-01-28_210011.zip";
    static final String path_osmdroid = Environment.getExternalStorageDirectory().getPath() + "/osmdroid/";
    static DownloadManager downloadManager;
    BroadcastReceiver receiverDownloadComplete;
    BroadcastReceiver receiverNotificationClicked;
    static long myDownloadReference;

    // Defines a custom Intent action
    public static final String BROADCAST_ACTION = "com.example.android.threadsample.BROADCAST";

    // Defines the key for the status "extra" in an Intent
    public static final String EXTENDED_DATA_STATUS = "com.example.android.threadsample.STATUS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getFragmentManager().beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }

    public void mapDownload() {
        File f = new File(path_osmdroid);
        if (!f.exists()) {
            f.mkdirs();
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mapsUrl));
        request.setTitle("Map download.");
        request.setDescription("Map is being downloaded...");

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION);
        request.setVisibleInDownloadsUi(true);

        String fileName = URLUtil.guessFileName(mapsUrl, null, MimeTypeMap.getFileExtensionFromUrl(mapsUrl));

        request.setDestinationInExternalPublicDir("/osmdroid", fileName);

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        myDownloadReference = downloadManager.enqueue(request);
    }
    @Override
    public void onResume() {
        super.onResume();

//        filter for notifications - only acts on notification
//              while download busy
//        IntentFilter filter = new IntentFilter(DownloadManager
//                .ACTION_NOTIFICATION_CLICKED);
//
//        receiverNotificationClicked = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                String extraId = DownloadManager
//                        .EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS;
//                long[] references = intent.getLongArrayExtra(extraId);
//                for (long reference : references) {
//                    if (reference == myDownloadReference) {
////                        do something with the download file
//                    }
//                }
//            }
//        };
//        registerReceiver(receiverNotificationClicked, filter);

//        filter for download - on completion
        IntentFilter intentFilter = new IntentFilter(DownloadManager
                .ACTION_DOWNLOAD_COMPLETE);

        receiverDownloadComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long reference = intent.getLongExtra(DownloadManager
                        .EXTRA_DOWNLOAD_ID, -1);
                if (myDownloadReference == reference) {
//                    do something with the download file
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(reference);
                    Cursor cursor = downloadManager.query(query);

                    cursor.moveToFirst();
//                        get the status of the download
                    int columnIndex = cursor.getColumnIndex(DownloadManager
                            .COLUMN_STATUS);
                    int status = cursor.getInt(columnIndex);

                    int fileNameIndex = cursor.getColumnIndex(DownloadManager
                            .COLUMN_LOCAL_FILENAME);
                    String savedFilePath = cursor.getString(fileNameIndex);

//                        get the reason - more detail on the status
                    int columnReason = cursor.getColumnIndex(DownloadManager
                            .COLUMN_REASON);
                    int reason = cursor.getInt(columnReason);

                    switch (status) {
                        case DownloadManager.STATUS_SUCCESSFUL:

//                                start activity to display the downloaded image
//                                Intent intentDisplay = new Intent(SettingsActivity.this,
//                                        DisplayActivity.class);
//                                intentDisplay.putExtra("uri", savedFilePath);
//                                startActivity(intentDisplay);

                            Intent mServiceIntent = new Intent(context, UnZipService.class);
                            mServiceIntent.setData(Uri.parse(savedFilePath));
                            startService(mServiceIntent);

                            break;
                        case DownloadManager.STATUS_FAILED:
                            Toast.makeText(SettingsActivity.this,
                                    "FAILED: " + reason,
                                    Toast.LENGTH_LONG).show();
                            break;
                        case DownloadManager.STATUS_PAUSED:
                            Toast.makeText(SettingsActivity.this,
                                    "PAUSED: " + reason,
                                    Toast.LENGTH_LONG).show();
                            break;
                        case DownloadManager.STATUS_PENDING:
                            Toast.makeText(SettingsActivity.this,
                                    "PENDING!",
                                    Toast.LENGTH_LONG).show();
                            break;
                        case DownloadManager.STATUS_RUNNING:
                            Toast.makeText(SettingsActivity.this,
                                    "RUNNING!",
                                    Toast.LENGTH_LONG).show();
                            break;
                    }
                    cursor.close();
                }
            }
        };
        registerReceiver(receiverDownloadComplete, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiverDownloadComplete);
//            unregisterReceiver(receiverNotificationClicked);
    }



    /**
     * A placeholder fragment containing a simple view.
     */
    public static class SettingsFragment extends PreferenceFragment {

        public SettingsFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.settings);

            Preference dl_pref = (Preference) findPreference("dl_key");
            dl_pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    //open browser or intent here
                    ((SettingsActivity)getActivity()).mapDownload();
                    return false;
                }
            });
        }
    }



}
