package org.osmdroid.Stages;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.R;
import org.osmdroid.views.util.JsonFilesHandler;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class FragmentStageList extends Fragment {

    Communicater communicater;
    private JsonFilesHandler jfh;
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        jfh = new JsonFilesHandler(getActivity());
        // Construct the data source
        ArrayList<StageListItem> arrayOfStages = new ArrayList<StageListItem>();
        // Create the adapter to convert the array to views
        StagesAdapter adapter = new StagesAdapter(getActivity(), arrayOfStages);
        // Attach the adapter to a ListView
        final ListView listView = (ListView) getActivity().findViewById(R.id.stageslistview);
        listView.setAdapter(adapter);
        String name = new String();
        for (int i = 1; i <= 33; i++) {
            JSONObject fileObj = jfh.parseJSONObj("json/stage" + i + ".json");
            JSONObject stageObj = null;
            try {
                stageObj = fileObj.getJSONObject("gpx").getJSONObject("trk");
                name = stageObj.getString("name");
                StageListItem newStage = new StageListItem(i, name);
                adapter.add(newStage);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView sn = (TextView) view.findViewById(R.id.tv_stagenumber);
                TextView ft = (TextView) view.findViewById(R.id.tv_from_to);
                int stageId = Integer.parseInt(sn.getText().toString().substring(6));
                String fromto = ft.getText().toString();
                communicater.respond(stageId, fromto);
//                Toast.makeText(getApplicationContext(), c.getText().toString().substring(6), Toast.LENGTH_SHORT).show();

//                Intent intent = new Intent(getApplicationContext(), StageActivity.class);
//                intent.putExtra("stageId", stageId);
//                intent.putExtra("fromto", fromto);
//                startActivity(intent);
            }
        });
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public void setCommunicater(Communicater communicater) {
        this.communicater = communicater;
    }

    public interface Communicater {
        public void respond(int index, String title);
    }
}
