package com.tonyandr.caminoguide.settings;


import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

import com.tonyandr.caminoguide.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapDownloadFragment extends Fragment {
    MapDownloadFragment fragmentDownload;
    MapDeleteFragment fragmentDelete;
    FragmentManager fragmentManager;
    MaplistAdapter dataAdapter = null;
    private String[] stageNames;
    private List<String> mapsUrls;
    private String mapsUrl = ""; // TODO
    static final String path_osmdroid = Environment.getExternalStorageDirectory().getPath() + "/osmdroid/";
    static DownloadManager downloadManager;
    BroadcastReceiver receiverDownloadComplete;
    BroadcastReceiver receiverNotificationClicked;
    BroadcastReceiver unzipServiceReceiver;
    static long myDownloadReference;
    private ArrayList<MaplistInfo> mapList;
    private ListView listView;

    static final int LIST_ITEM_STATUS_NONE = 0;
    static final int LIST_ITEM_STATUS_INPROGRESS = 1;
    static final int LIST_ITEM_STATUS_EXIST = 2;


    public MapDownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map_download, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        displayListView();
        stageNames = getResources().getStringArray(R.array.stage_names);
        downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        fragmentManager = getFragmentManager();
    }

    public void mapDownload(String title, int position) {
        File f = new File(path_osmdroid);
        if (!f.exists()) {
            f.mkdirs();
        }
//        mapsUrls.add("http://alberguenajera.es/projects/ecn/8-11_allmap.zip");
//        for (int i = 0; i < stageNames.length; i++) {
        int i = 0;
        mapsUrl = null;
        if (mapList.get(position).getStatus() == LIST_ITEM_STATUS_NONE) {
            while (mapsUrl == null && i < 33) {
                Log.d("Download", "Compare " + stageNames[i] + " and " + title);
                if (stageNames[i].equals(title)) {
                    int stage_id = i + 1;
                    if (!(new File(path_osmdroid + "stage" + stage_id + ".stg")).exists()) {
                        Log.d("Download", "Load stage #" + stage_id);
                        mapsUrl = "http://alberguenajera.es/projects/ecn/stage" + stage_id + ".zip";
                    }
                } else if (title.equals("Spain Base Map")) {
                    if (!(new File(path_osmdroid + "8-11_allmap.stg")).exists()) {
                        Log.d("Download", "Load allmap");
                        mapsUrl = "http://alberguenajera.es/projects/ecn/8-11_allmap.zip";
                    }
                } else {
//                mapsUrl = null;
                    Log.d("Download", "mapUrl still is null");
                }
                i++;
            }
        }
//        }
        if (mapsUrl != null) {
            mapList.get(position).setStatus(LIST_ITEM_STATUS_INPROGRESS);
            (((ViewGroup) listView.getChildAt(position)).getChildAt(0)).setVisibility(View.GONE);
            (((ViewGroup) listView.getChildAt(position)).getChildAt(1)).setVisibility(View.VISIBLE);
            dataAdapter.notifyDataSetChanged();
//            Log.d("Download", "Url to load: " + mapsUrl);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mapsUrl));
            request.setTitle(title);
            request.setDescription("Download in progress...");
//        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI, DownloadManager.Request.NETWORK_MOBILE);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//        request.setVisibleInDownloadsUi(true);
            String fileName = URLUtil.guessFileName(mapsUrl, null, MimeTypeMap.getFileExtensionFromUrl(mapsUrl));
            request.setDestinationInExternalPublicDir("/osmdroid", fileName);
            myDownloadReference = downloadManager.enqueue(request);
            Log.d("Download", "Added to queue " + title);
        } else {
            Toast.makeText(getActivity(), "Choose any items", Toast.LENGTH_LONG).show();
        }

    }

    private void checkButtonClick() {
        for (int i = 0; i < mapList.size(); i++) {
            MaplistInfo maplistInfo = mapList.get(i);
            if (maplistInfo.isSelected()) {
//                        responseText.append("\n" + maplistInfo.getTitle());
                Log.d("Download", "Mapinfo Title is " + maplistInfo.getTitle());
                mapDownload(maplistInfo.getTitle(), i);
            }
        }
    }

    private void displayListView() {

        //Array list of countries
        mapList = new ArrayList<>();
        stageNames = getResources().getStringArray(R.array.stage_names);
        File f = new File(path_osmdroid + "8-11_allmap.zip");
        if (!f.exists()) {
            mapList.add(new MaplistInfo(f.length()/1000000+"mb", "Spain Base Map", true, false));
        }
        int count = 1;
        for (String item : stageNames) {
            f = new File(path_osmdroid + "stage" + count + ".zip");
            if (!f.exists()) {
                mapList.add(new MaplistInfo(f.length()/1000000+"mb", item, false, true));
            }
            count++;
        }


        //create an ArrayAdaptar from the String Array
        dataAdapter = new MaplistAdapter(getActivity(), mapList);
        listView = (ListView) getActivity().findViewById(R.id.listView1);
        // Assign adapter to ListView
        listView.setAdapter(dataAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // When clicked, show a toast with the TextView text
                MaplistInfo maplistInfo = (MaplistInfo) parent.getItemAtPosition(position);
                CheckBox cb = (CheckBox) ((ViewGroup) view).getChildAt(0);
                if (!cb.isEnabled()) {
                    Toast.makeText(view.getContext(),
                            maplistInfo.title + " can't be excluded!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    cb.setChecked(!cb.isChecked());
                    maplistInfo.setSelected(cb.isChecked());
                }
//                Toast.makeText(getActivity(),
//                        "Clicked on Row #" + position + " with: " + maplistInfo.getTitle(),
//                        Toast.LENGTH_SHORT).show();

            }
        });

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
//                if (myDownloadReference == reference) {
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
                String fileDesc = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
//                        get the reason - more detail on the status
                int columnReason = cursor.getColumnIndex(DownloadManager
                        .COLUMN_REASON);
                int reason = cursor.getInt(columnReason);

                switch (status) {
                    case DownloadManager.STATUS_SUCCESSFUL:
                        Toast.makeText(context,
                                fileDesc + " download complete",
                                Toast.LENGTH_SHORT).show();
                        Log.d("Download", "Downloaded " + savedFilePath);
                        for (int i = 0; i < mapList.size(); i++) {
                            if (((mapList.get(i)).getTitle()).equals(fileDesc)) {
                                mapList.get(i).setStatus(LIST_ITEM_STATUS_EXIST);
                                mapList.remove(i);
                                dataAdapter.notifyDataSetChanged();
                            }
                        }
//                        new UnzipTask().execute(savedFilePath);
                        break;
                    case DownloadManager.STATUS_FAILED:
                        Toast.makeText(context,
                                "Unable to download: " + fileDesc,
                                Toast.LENGTH_LONG).show();
                        for (int i = 0; i < mapList.size(); i++) {
                            if (((mapList.get(i)).getTitle()).equals(fileDesc)) {
                                mapList.get(i).setStatus(LIST_ITEM_STATUS_NONE);
                                (((ViewGroup) listView.getChildAt(i)).getChildAt(0)).setVisibility(View.VISIBLE);
                                (((ViewGroup) listView.getChildAt(i)).getChildAt(1)).setVisibility(View.GONE);
                                dataAdapter.notifyDataSetChanged();
                            }
                        }
                        break;
                    case DownloadManager.STATUS_PAUSED:
                        Toast.makeText(context,
                                "PAUSED: " + reason,
                                Toast.LENGTH_LONG).show();
                        break;
                    case DownloadManager.STATUS_PENDING:
                        Toast.makeText(context,
                                "PENDING!",
                                Toast.LENGTH_LONG).show();
                        break;
                    case DownloadManager.STATUS_RUNNING:
                        Toast.makeText(context,
                                "RUNNING!",
                                Toast.LENGTH_LONG).show();
                        break;
                }
                cursor.close();
//                }
            }
        };

        getActivity().registerReceiver(receiverDownloadComplete, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(receiverDownloadComplete);
//            unregisterReceiver(receiverNotificationClicked);
    }
//
//    private class UnzipTask extends AsyncTask<String, Void, Void> {
//        public UnzipTask() {
//        }
//
//        private String path_osm;
//
//
//        @Override
//        protected void onPreExecute() {
//            path_osm = Environment.getExternalStorageDirectory().getPath() + "/osmdroid/";
//
//        }
//
//        @Override
//        protected Void doInBackground(String... params) {
//            doUnzip(params[0], path_osm);
//            Log.d("UNZIP", "Unzipped: " + params[0]);
//            return null;
//        }
//
//        @Override
//        protected void onProgressUpdate(Void... values) {
//            super.onProgressUpdate(values);
//        }
//
//        @Override
//        protected void onPostExecute(Void aVoid) {
////            Toast.makeText(getActivity(), "unzip done", Toast.LENGTH_SHORT).show();
//        }
//
//        public boolean doUnzip(String inputZipFile, String destinationDirectory) {
//            try {
//                int BUFFER = 2048;
//                List<String> zipFiles = new ArrayList<String>();
//                File sourceZipFile = new File(inputZipFile);
//                File unzipDestinationDirectory = new File(destinationDirectory);
//                unzipDestinationDirectory.mkdir();
//                ZipFile zipFile;
//                zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
//                Enumeration zipFileEntries = zipFile.entries();
//                while (zipFileEntries.hasMoreElements()) {
//                    ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
//                    String currentEntry = entry.getName();
//                    File destFile = new File(unzipDestinationDirectory, currentEntry);
//                    if (currentEntry.endsWith(".zip")) {
//                        zipFiles.add(destFile.getAbsolutePath());
//                    }
//
//                    File destinationParent = destFile.getParentFile();
//
//                    destinationParent.mkdirs();
//
//                    try {
//                        if (!entry.isDirectory()) {
//                            BufferedInputStream is =
//                                    new BufferedInputStream(zipFile.getInputStream(entry));
//                            int currentByte;
//                            byte data[] = new byte[BUFFER];
//
//                            FileOutputStream fos = new FileOutputStream(destFile);
//                            BufferedOutputStream dest =
//                                    new BufferedOutputStream(fos, BUFFER);
//                            while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
//                                dest.write(data, 0, currentByte);
//                            }
//                            dest.flush();
//                            dest.close();
//                            is.close();
//                        }
//                    } catch (IOException ioe) {
//                        ioe.printStackTrace();
//                    }
//                }
//                zipFile.close();
//
//                for (Iterator<String> iter = zipFiles.iterator(); iter.hasNext(); ) {
//                    String zipName = (String) iter.next();
//                    doUnzip(
//                            zipName,
//                            destinationDirectory +
//                                    File.separatorChar +
//                                    zipName.substring(0, zipName.lastIndexOf(".zip"))
//                    );
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                return false;
//            }
//            File sourceZipFile = new File(inputZipFile);
//            if (sourceZipFile.exists()) {
//                sourceZipFile.delete();
//            }
//
//            return true;
//        }
//    }

    private void deleteFromList(String title) {
        ArrayList<MaplistInfo> mapList = dataAdapter.mapList;
        for (int i = 0; i < mapList.size(); i++) {
            MaplistInfo maplistInfo = mapList.get(i);
            if (title.equals(maplistInfo.getTitle())) {
                mapList.remove(i);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_mapfragment_download, menu);
        super.onCreateOptionsMenu(menu, inflater);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_download_map) {
            checkButtonClick();
            return true;
        }
        if (id == R.id.action_delete_fragment) {
            if (fragmentManager.findFragmentByTag("MapDownloadFragment") != null) {
                fragmentDelete = new MapDeleteFragment();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.mapmanager_fragment_holder_id, fragmentDelete, "MapDeleteFragment");
                transaction.addToBackStack(MapDeleteFragment.class.getName());
                transaction.commit();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
