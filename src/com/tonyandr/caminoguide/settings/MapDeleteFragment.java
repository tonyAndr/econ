package com.tonyandr.caminoguide.settings;


import android.app.Fragment;
import android.app.FragmentManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

import com.tonyandr.caminoguide.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapDeleteFragment extends Fragment {
    FragmentManager fragmentManager;

    MaplistAdapter dataAdapter = null;
    private String[] stageNames;
    static final String path_osmdroid = Environment.getExternalStorageDirectory().getPath() + "/osmdroid/";
    private ArrayList<MaplistInfo> mapList;
    private ListView listView;
    static final int LIST_ITEM_STATUS_NONE = 0;
    static final int LIST_ITEM_STATUS_INPROGRESS = 1;
    static final int LIST_ITEM_STATUS_EXIST = 2;

    public MapDeleteFragment() {
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
        return inflater.inflate(R.layout.fragment_map_delete, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        displayListView();
        checkButtonClick();
        stageNames = getResources().getStringArray(R.array.stage_names);
        fragmentManager = getFragmentManager();
    }

    private void checkButtonClick() {
        for (int i = 0; i < mapList.size(); i++) {
            MaplistInfo maplistInfo = mapList.get(i);
            if (maplistInfo.isSelected()) {
                Log.d("Download", "Mapinfo Title is " + maplistInfo.getTitle());
                mapList.get(i).setStatus(LIST_ITEM_STATUS_INPROGRESS);
                (((ViewGroup)listView.getChildAt(i)).getChildAt(0)).setVisibility(View.GONE);
                (((ViewGroup)listView.getChildAt(i)).getChildAt(1)).setVisibility(View.VISIBLE);
                dataAdapter.notifyDataSetChanged();
                new DeleteTask().execute(maplistInfo.getTitle());
            }
        }
    }
    private void deleteFromList(String title) {
        for (int i = 0; i < mapList.size(); i++) {
            MaplistInfo maplistInfo = mapList.get(i);
            if (title.equals(maplistInfo.getTitle())) {
                mapList.get(i).setStatus(LIST_ITEM_STATUS_NONE);
                mapList.remove(i);
                dataAdapter.notifyDataSetChanged();
            }
        }
    }

    private void DeleteFiles(File f) { // Delete *.tiles and *.stg

        FileInputStream is;
        BufferedReader reader;
        File toDel;
        Log.d("Delete", "path: " +f.getAbsolutePath());
        if (f.exists()) {
            try {
                is = new FileInputStream(f);
                reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                while (line != null) {
                    toDel = new File(path_osmdroid+"/tiles"+line.replace("\\", "/"));
                    toDel.delete();
                    line = reader.readLine();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        else {
            Log.d("Delete", "not exists");
        }
        DeleteFoldersRecursive(new File(path_osmdroid + "tiles/MapQuestOSM/"));
        f.delete();
    }

    private void DeleteFoldersRecursive(File f) { // Recursive delete empty dirs
        if (f.isDirectory()){
            Log.d("Delete", "" + f.getAbsolutePath() + " is dir");
            if (f.list().length > 0) {
                for (File child : f.listFiles())
                    DeleteFoldersRecursive(child);
            }

            Log.d("Delete", f.getAbsolutePath() +" deleted: " + f.delete());
        }
    }
    private void mapDelete(String title) { // Delete selected stage's maps
        int count = 1;
        int stageId = 0;
        for (String item : stageNames) {
            if (item.equals(title)) {
                stageId = count;
            }
            count++;
        }

        if (stageId != 0) {
            File f = new File(path_osmdroid + "stage" + stageId + ".stg");
            DeleteFiles(f);
        }
        if (title.equals("Spain Base Map")) {
            File f = new File(path_osmdroid + "8-11_allmap.stg");
            DeleteFiles(f);

        }

    }

    private void displayListView() {

        //Array list of countries
        mapList = new ArrayList<>();
        stageNames = getResources().getStringArray(R.array.stage_names);
        boolean stages_exist = false;

        File f = new File(path_osmdroid + "8-11_allmap.stg");
        if (f.exists()) {
            mapList.add(new MaplistInfo("40mb", "Spain Base Map", false, true));
        }

        int count = 1;
        for (String item : stageNames) {
            if ((new File(path_osmdroid + "stage" + count + ".stg")).exists()) {
                mapList.add(new MaplistInfo("10mb", item, false, true));
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
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private class DeleteTask extends AsyncTask<String, Void, Void> {
        public DeleteTask() {
        }
        private String title;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Void doInBackground(String... params) {
            title = params[0];
            Log.d("DeleteTask", "Task for: " + title +" started");
            mapDelete(title);
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            deleteFromList(title);
            Log.d("DeleteTask", "Task for: " + title +" finished");
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_mapfragment_delete, menu);
        super.onCreateOptionsMenu(menu, inflater);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete_map) {
            checkButtonClick();
            return true;
        }
        if (id == R.id.action_download_fragment) {

            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
