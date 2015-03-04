// Created by plusminus on 00:23:14 - 03.10.2008
package com.tonyandr.caminoguide.map;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tonyandr.caminoguide.NavigationDrawerLayout;
import com.tonyandr.caminoguide.R;
import com.tonyandr.caminoguide.utils.DBControllerAdapter;


/**
 * Default map view activity.
 *
 * @author Manuel Stahl
 */
public class MapActivity extends ActionBarActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int DIALOG_ABOUT_ID = 1;
    private static final String OSM_FRAGMENT_TAG = "com.tonyandr.caminoguide.osm_tag";
    private static final String GMS_FRAGMENT_TAG = "com.tonyandr.caminoguide.gms_tag";
    public TextView geoOutTextViewLon;
    public TextView geoOutTextViewLat;
    public TextView geoOutTextViewTime;
    SharedPreferences settings;
    private Boolean doubleBackToExitPressedOnce = false;
    private FragmentManager fm;

    DBControllerAdapter dbController;
    private Toolbar toolbar;

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = PreferenceManager.getDefaultSharedPreferences(this);//get the preferences that are allowed to be given
        //set the listener to listen for changes in the preferences
        settings.registerOnSharedPreferenceChangeListener(this);
        this.setContentView(R.layout.activity_map);

        toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        NavigationDrawerLayout drawerFragment = (NavigationDrawerLayout) getSupportFragmentManager().findFragmentById(R.id.fragment_nav_drawer);
        drawerFragment.setUp(R.id.fragment_nav_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), toolbar);

        geoOutTextViewLon = (TextView) findViewById(R.id.debugGeoOutputLon);
        geoOutTextViewLat = (TextView) findViewById(R.id.debugGeoOutputLat);
        geoOutTextViewTime = (TextView) findViewById(R.id.debugGeoOutputTime);
        fm = this.getFragmentManager();

        if (settings.getBoolean("pref_key_offline_mode", true)) {
            if (fm.findFragmentByTag(OSM_FRAGMENT_TAG) == null) {
                OSMFragment mapFragment = OSMFragment.newInstance();
                fm.beginTransaction().add(R.id.map_container, mapFragment, OSM_FRAGMENT_TAG).commit();
            }
        } else {
            if (fm.findFragmentByTag(GMS_FRAGMENT_TAG) == null) {
                GMapFragment mapFragment = GMapFragment.newInstance();
                fm.beginTransaction().add(R.id.map_container, mapFragment, GMS_FRAGMENT_TAG).commit();
            }
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
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fm = this.getFragmentManager();
        if (settings.getBoolean("pref_key_offline_mode", true)) {
            if (fm.findFragmentByTag(OSM_FRAGMENT_TAG) == null) {
                OSMFragment mapFragment = OSMFragment.newInstance();
                fm.beginTransaction().replace(R.id.map_container, mapFragment, OSM_FRAGMENT_TAG).commit();
            }
        } else {
            if (fm.findFragmentByTag(GMS_FRAGMENT_TAG) == null) {
                GMapFragment mapFragment = GMapFragment.newInstance();
                fm.beginTransaction().replace(R.id.map_container, mapFragment, GMS_FRAGMENT_TAG).commit();
            }
        }
        if (settings.getBoolean("pref_key_info_geo", false)) {
            geoOutTextViewLon.setVisibility(View.VISIBLE);
            geoOutTextViewLat.setVisibility(View.VISIBLE);
            geoOutTextViewTime.setVisibility(View.VISIBLE);
        } else {
            geoOutTextViewLon.setVisibility(View.GONE);
            geoOutTextViewLat.setVisibility(View.GONE);
            geoOutTextViewTime.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        settings.unregisterOnSharedPreferenceChangeListener(this);
        stopService(new Intent(this, GeoService.class));
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopService(new Intent(this, GeoService.class));
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }
}
