// Created by plusminus on 00:23:14 - 03.10.2008
package org.osmdroid.map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.osmdroid.NavigationDrawerLayout;
import org.osmdroid.R;


/**
 * Default map view activity.
 *
 * @author Manuel Stahl
 *
 */
public class MapActivity extends ActionBarActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{

    private static final int DIALOG_ABOUT_ID = 1;
	private static final String MAP_FRAGMENT_TAG = "org.osmdroid.MAP_FRAGMENT_TAG";
    public TextView geoOutTextViewLon;
    public TextView geoOutTextViewLat;
    public TextView geoOutTextViewTime;
    SharedPreferences prefs;

    private Toolbar toolbar;

    // ===========================================================
    // Constructors
    // ===========================================================
    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);//get the preferences that are allowed to be given
        //set the listener to listen for changes in the preferences
        prefs.registerOnSharedPreferenceChangeListener(this);
        this.setContentView(R.layout.activity_map);

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
			OSMFragment mapFragment = OSMFragment.newInstance();
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
            OSMFragment mapFragment = (OSMFragment)fm.findFragmentByTag(MAP_FRAGMENT_TAG);
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
        FragmentManager fm = this.getSupportFragmentManager();

            OSMFragment mapFragment = (OSMFragment)fm.findFragmentByTag(MAP_FRAGMENT_TAG);

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_hide_route) {
            mapFragment.routeOverlay.setEnabled(!mapFragment.routeOverlay.isEnabled());
            mapFragment.mMapView.invalidate();
            return true;
        }
        if (id == R.id.action_hide_markers) {
            mapFragment.albMarkersOverlay.setEnabled(!mapFragment.albMarkersOverlay.isEnabled());
            mapFragment.cityMarkersOverlay.setEnabled(!mapFragment.cityMarkersOverlay.isEnabled());
            mapFragment.mMapView.invalidate();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_key_info_geo")) {
            Log.d("Prefs", "" + prefs.getBoolean(key, true));
            geoOutTextViewLon.setVisibility(View.VISIBLE);
            geoOutTextViewLat.setVisibility(View.VISIBLE);
            geoOutTextViewTime.setVisibility(View.VISIBLE);
        }
        else {
            Log.d("Prefs", ""+prefs.getBoolean(key, true));
            geoOutTextViewLon.setVisibility(View.GONE);
            geoOutTextViewLat.setVisibility(View.GONE);
            geoOutTextViewTime.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }
}
