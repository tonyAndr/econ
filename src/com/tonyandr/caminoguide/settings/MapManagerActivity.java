package com.tonyandr.caminoguide.settings;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import com.tonyandr.caminoguide.NavigationDrawerLayout;
import com.tonyandr.caminoguide.R;

public class MapManagerActivity extends ActionBarActivity {
    MapDownloadFragment fragmentDownload;
    MapDeleteFragment fragmentDelete;
    FragmentManager fragmentManager;



    private Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_manager);

        fragmentManager = getFragmentManager();

        if (fragmentManager.findFragmentByTag("MapDownloadFragment") == null) {
            fragmentDownload = new MapDownloadFragment();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.mapmanager_fragment_holder_id, fragmentDownload, "MapDownloadFragment");
            transaction.commit();
        }



        toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        NavigationDrawerLayout drawerFragment = (NavigationDrawerLayout) getSupportFragmentManager().findFragmentById(R.id.fragment_nav_drawer);
        drawerFragment.setUp(R.id.fragment_nav_drawer,(DrawerLayout)findViewById(R.id.drawer_layout), toolbar);

    }

//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_map_manager, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_delete) {
//            if (fragmentManager.findFragmentByTag("MapDownloadFragment") != null) {
//                fragmentDelete = new MapDeleteFragment();
//                FragmentTransaction transaction = fragmentManager.beginTransaction();
//                transaction.replace(R.id.mapmanager_fragment_holder_id, fragmentDelete, "MapDeleteFragment");
////                    transaction.addToBackStack(FragmentStageList.class.getName());
//                transaction.commit();
//            }
//
//            return true;
//        }
//
//
//        return super.onOptionsItemSelected(item);
//    }
    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() == 0) {
            this.finish();
        } else {
            fragmentManager.popBackStack();
        }
    }
}
