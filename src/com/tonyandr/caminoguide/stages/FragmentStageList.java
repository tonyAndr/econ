package com.tonyandr.caminoguide.stages;


import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.tonyandr.caminoguide.R;
import com.tonyandr.caminoguide.constants.AppConstants;
import com.tonyandr.caminoguide.utils.GeoMethods;
import com.tonyandr.caminoguide.utils.JsonFilesHandler;

import org.json.JSONException;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class FragmentStageList extends Fragment implements AppConstants  {
    private SharedPreferences mPrefs;
    FragmentStageView fragmentView;
    FragmentManager fragmentManager;
//    Communicater communicater;
    private JsonFilesHandler jfh;
    private ListView listView;
    private Parcelable mListState;
    private StagesAdapter adapter;
//    private StagesAdapter adapter;
    public FragmentStageList() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stage_list, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.stage_activity_list_fragment_title);
    }

    @Override
    public void onPause() {
        super.onPause();
    }



    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        jfh = new JsonFilesHandler(getActivity());
        mPrefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Construct the data source
        ArrayList<StageListItem> arrayOfStages = new ArrayList<StageListItem>();
        // Create the adapter to convert the array to views
        adapter = new StagesAdapter(getActivity(), arrayOfStages);
        // Attach the adapter to a ListView
        listView = (ListView) getActivity().findViewById(R.id.stageslistview);
        listView.setAdapter(adapter);

        fillListView();
        if (savedInstanceState != null) {
//            Toast.makeText(getActivity(), "restored", Toast.LENGTH_SHORT).show();
            listView.setSelectionFromTop(savedInstanceState.getInt("listIndex"), savedInstanceState.getInt("listTop"));
        }

        CurrentStage currentStage = new CurrentStage();
        currentStage.execute();
        fragmentManager = getFragmentManager();
        fragmentView = new FragmentStageView();
//        this.setCommunicater(communicater);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                TextView sn = (TextView) view.findViewById(R.id.tv_stagenumber);
                TextView ft = (TextView) view.findViewById(R.id.tv_from_to);
                int stageId = position+1;
                String fromto = ft.getText().toString();
//                communicater.respond(stageId, fromto);
                final SharedPreferences.Editor edit = mPrefs.edit();
                edit.putInt(PREFS_STAGELIST_STAGEID,stageId);
                edit.putString(PREFS_STAGELIST_FROMTO, fromto);
                edit.commit();
                if (fragmentManager.findFragmentByTag("FragmentList") != null) {
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    transaction.replace(R.id.stage_fragment_holder_id, fragmentView, "FragmentView");
                    transaction.addToBackStack(FragmentStageView.class.getName());
                    transaction.commit();
                }
            }
        });
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int index = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());

        outState.putInt("listIndex", index);
        outState.putInt("listTop", top);
    }

    private void markCurrentStage(int stage) {
        View v = listView.getChildAt(stage-1);
        v.setBackgroundColor(Color.rgb(244,68,68));
        ((TextView)((ViewGroup)v).getChildAt(1)).setTextColor(Color.WHITE);
        ((ImageView)((ViewGroup)v).getChildAt(2)).setImageResource(R.drawable.list_triangle_white);
    }

    class CurrentStage extends AsyncTask<Void, Void, Void> {
        private CurrentStage() {

        }
        private GeoMethods geoMethods;
        private int stage;

        @Override
        protected void onPreExecute() {
            geoMethods = new GeoMethods(getActivity());
        }

        @Override
        protected Void doInBackground(Void... params) {
            Location gps = new Location("");
            if (mPrefs != null) {
                gps.setLatitude(mPrefs.getFloat("lat", 0));
                gps.setLongitude(mPrefs.getFloat("lng", 0));
            } else {
                gps.setLatitude(0);
                gps.setLongitude(0);
            }
            try {
                stage = geoMethods.onWhichStageSimple(gps);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
//            markCurrentStage(stage);
            if (stage != 0) {
                adapter.getItem(stage-1).current = true;
                adapter.notifyDataSetChanged();
            }
        }
    }



//    public void setCommunicater(Communicater communicater) {
//        this.communicater = communicater;
//    }
//
//    public interface Communicater {
//        public void respond(int index, String title);
//    }


//    class MyTask extends AsyncTask<Void, StageListItem, Void> {
//        public MyTask() {
//
//        }
//        private StagesAdapter adapter;
//        private String[] stageNames = getResources().getStringArray(R.array.stage_names);
//        @Override
//        protected void onPreExecute() {
//             adapter = (StagesAdapter) listView.getAdapter();
//        }
//
//        @Override
//        protected Void doInBackground(Void... params) {
//            StageListItem newStage;
//            int i=1;
//            for (String item : stageNames) {
//
//                newStage = new StageListItem(i, item);
//                publishProgress(newStage);
//                i++;
//            }
//
//            return null;
//        }
//
//        @Override
//        protected void onProgressUpdate(StageListItem... values) {
//                adapter.add(values[0]);
//
//        }
//
//        @Override
//        protected void onPostExecute(Void aVoid) {
//            super.onPostExecute(aVoid);
//        }
//    }

    private void fillListView() {
        String[] stageNames = getResources().getStringArray(R.array.stage_names);
        adapter = (StagesAdapter) listView.getAdapter();
        int i=1;
        for (String item : stageNames) {

            adapter.add(new StageListItem(i, item, false));

            i++;
        }
    }
}
