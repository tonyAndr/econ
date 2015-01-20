package org.osmdroid;

import android.app.ListActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class StagesListActivity extends ActionBarActivity {


    private ArrayAdapter<String> adapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String[] stagesArr = new String[] {
                "Stage #1",
                "Stage #2",
                "Stage #3",
                "Stage #4",
                "Stage #5",
                "Stage #5",
                "Stage #5",
                "Stage #5",
                "Stage #5",
                "Stage #5",
                "Stage #5",
                "Stage #5",
        };

//        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, stages);
        //setListAdapter(adapter);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.stageslist);
        ListView lv = (ListView) findViewById(R.id.stageslistview);
        lv.setAdapter(new ArrayAdapter<String>(this, R.layout.list_item, R.id.stage_name, stagesArr));

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
