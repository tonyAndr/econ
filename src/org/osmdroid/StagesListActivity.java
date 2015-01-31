package org.osmdroid;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.views.util.StagesAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


public class StagesListActivity extends ActionBarActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stageslist);
// Construct the data source
        ArrayList<StageListItem> arrayOfStages = new ArrayList<StageListItem>();
// Create the adapter to convert the array to views
        StagesAdapter adapter = new StagesAdapter(this, arrayOfStages);
// Attach the adapter to a ListView
        final ListView listView = (ListView) findViewById(R.id.stageslistview);
        listView.setAdapter(adapter);
        String name = new String();
        for (int i = 1; i <= 33; i++) {
            JSONObject fileObj = parseJSONObj("json/stage" + i + ".json");
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
//                Toast.makeText(getApplicationContext(), c.getText().toString().substring(6), Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getApplicationContext(), StageActivity.class);
                intent.putExtra("stageId", stageId);
                intent.putExtra("fromto", fromto);
                startActivity(intent);
            }
        });
//        JSONArray jsonArray = ...;
//        ArrayList<Stage> newStages = Stage.fromJson(jsonArray);
//        adapter.addAll(newStages);


    }
    public JSONObject parseJSONObj(String filename) {
        String JSONString = null;
        JSONObject JSONObject = null;
        try {

            //open the inputStream to the file
            InputStream inputStream = this.getAssets().open(filename);

            int sizeOfJSONFile = inputStream.available();

            //array that will store all the data
            byte[] bytes = new byte[sizeOfJSONFile];

            //reading data into the array from the file
            inputStream.read(bytes);

            //close the input stream
            inputStream.close();

            JSONString = new String(bytes, "UTF-8");
            JSONObject = new JSONObject(JSONString);

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        catch (JSONException x) {
            x.printStackTrace();
            return null;
        }
        return JSONObject;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stages_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
