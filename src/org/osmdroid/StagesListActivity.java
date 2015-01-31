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

import org.osmdroid.views.util.StagesAdapter;

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
        StageListItem newStage = new StageListItem(1,"Burgos", "Logrono");
        adapter.add(newStage);
        adapter.add(newStage);
        adapter.add(newStage);
        adapter.add(newStage);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView c = (TextView) view.findViewById(R.id.tv_stagenumber);
                int stageId = Integer.parseInt(c.getText().toString().substring(6));
//                Toast.makeText(getApplicationContext(), c.getText().toString().substring(6), Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getApplicationContext(), StageActivity.class);
                intent.putExtra("stageId", stageId);
                startActivity(intent);
            }
        });
//        JSONArray jsonArray = ...;
//        ArrayList<Stage> newStages = Stage.fromJson(jsonArray);
//        adapter.addAll(newStages);


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
