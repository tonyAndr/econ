// Created by plusminus on 00:23:14 - 03.10.2008
package org.osmdroid.map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.osmdroid.NavigationDrawerLayout;
import org.osmdroid.R;
import org.osmdroid.settings.SettingsActivity;
import org.osmdroid.stages.StageActivity;


/**
 * Default map view activity.
 *
 * @author Manuel Stahl
 *
 */
public class MapActivity extends ActionBarActivity
{

    private static final int DIALOG_ABOUT_ID = 1;
	private static final String MAP_FRAGMENT_TAG = "org.osmdroid.MAP_FRAGMENT_TAG";
    public TextView geoOutTextViewLon;
    public TextView geoOutTextViewLat;
    public TextView geoOutTextViewTime;

    private Toolbar toolbar;

    // ===========================================================
    // Constructors
    // ===========================================================
    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.map_activity_appbar);

        toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        NavigationDrawerLayout drawerFragment = (NavigationDrawerLayout) getSupportFragmentManager().findFragmentById(R.id.fragment_nav_drawer);
        drawerFragment.setUp(R.id.fragment_nav_drawer,(DrawerLayout)findViewById(R.id.drawer_layout), toolbar);

        geoOutTextViewLon = (TextView)findViewById(R.id.debugGeoOutputLon);
        geoOutTextViewLat = (TextView)findViewById(R.id.debugGeoOutputLat);
        geoOutTextViewTime = (TextView)findViewById(R.id.debugGeoOutputTime);
        FragmentManager fm = this.getSupportFragmentManager();

		if (fm.findFragmentByTag(MAP_FRAGMENT_TAG) == null) {
			MapFragment mapFragment = MapFragment.newInstance();
			fm.beginTransaction().add(R.id.map_container, mapFragment, MAP_FRAGMENT_TAG).commit();
		}
    }
    @Override
    protected Dialog onCreateDialog(final int id)
    {
        Dialog dialog;

        switch (id) {
            case DIALOG_ABOUT_ID:
                return new AlertDialog.Builder(MapActivity.this).setIcon(R.drawable.icon)
                        .setTitle(R.string.app_name).setMessage(R.string.about_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int whichButton)
                            {
                                //
                            }
                        }).create();

            default:
                dialog = null;
                break;
        }
        return dialog;
    }

    public void getMyLocationBtnHandler(View view) {
        FragmentManager fm = this.getSupportFragmentManager();

        if (fm.findFragmentByTag(MAP_FRAGMENT_TAG) != null) {
            MapFragment mapFragment = (MapFragment)fm.findFragmentByTag(MAP_FRAGMENT_TAG);
            mapFragment.onUseMapActivityBtnHandler();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_stages) {
            Intent intent = new Intent(MapActivity.this, StageActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MapActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
