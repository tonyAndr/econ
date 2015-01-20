package org.osmdroid;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.osmdroid.views.util.StagesAdapter;

import java.util.ArrayList;


public class StagesListActivity extends ActionBarActivity {


    private ArrayAdapter<String> adapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stageslist);
// Construct the data source
        ArrayList<Stage> arrayOfUsers = new ArrayList<Stage>();
// Create the adapter to convert the array to views
        StagesAdapter adapter = new StagesAdapter(this, arrayOfUsers);
// Attach the adapter to a ListView
        ListView listView = (ListView) findViewById(R.id.stageslistview);
        listView.setAdapter(adapter);
        Stage newStage = new Stage(1,"Burgos", "Logrono");
        adapter.add(newStage);

//        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, stages);
        //setListAdapter(adapter);
//        getActionBar().setDisplayHomeAsUpEnabled(true);

//        ListView lv = (ListView) findViewById(R.id.stageslistview);
//        lv.setAdapter(new ArrayAdapter<String>(this, R.layout.stage_list_item, R.id.stage_name, stagesArr));

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
