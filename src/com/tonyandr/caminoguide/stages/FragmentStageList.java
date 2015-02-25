package com.tonyandr.caminoguide.stages;


import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.tonyandr.caminoguide.R;
import com.tonyandr.caminoguide.constants.AppConstants;
import com.tonyandr.caminoguide.utils.JsonFilesHandler;

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
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        jfh = new JsonFilesHandler(getActivity());
        mPrefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Construct the data source
        ArrayList<StageListItem> arrayOfStages = new ArrayList<StageListItem>();
        // Create the adapter to convert the array to views
        StagesAdapter adapter = new StagesAdapter(getActivity(), arrayOfStages);
        // Attach the adapter to a ListView
        listView = (ListView) getActivity().findViewById(R.id.stageslistview);
        listView.setAdapter(adapter);

        MyTask myTask = new MyTask();
        myTask.execute();
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
    }

//    public void setCommunicater(Communicater communicater) {
//        this.communicater = communicater;
//    }
//
//    public interface Communicater {
//        public void respond(int index, String title);
//    }


    class MyTask extends AsyncTask<Void, StageListItem, Void> {
        public MyTask() {

        }
        private StagesAdapter adapter;
        private String[] stageNames = getResources().getStringArray(R.array.stage_names);
        @Override
        protected void onPreExecute() {
             adapter = (StagesAdapter) listView.getAdapter();
        }

        @Override
        protected Void doInBackground(Void... params) {
            StageListItem newStage;
            int i=1;
            for (String item : stageNames) {

                newStage = new StageListItem(i, item);
                publishProgress(newStage);
                i++;
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(StageListItem... values) {
                adapter.add(values[0]);

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }
}
