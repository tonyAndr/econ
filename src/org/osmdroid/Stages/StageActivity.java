package org.osmdroid.Stages;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.osmdroid.NavigationDrawerLayout;
import org.osmdroid.R;


public class StageActivity extends ActionBarActivity implements FragmentStageList.Communicater{

    FragmentStageList fragmentList;
    FragmentStageView fragmentView;
    FragmentManager fragmentManager;
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stage);
        fragmentManager = getFragmentManager();

        if (fragmentManager.findFragmentByTag("FragmentList") == null) {
            fragmentList = new FragmentStageList();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.stage_fragment_holder_id, fragmentList, "FragmentList");
//                    transaction.addToBackStack(FragmentStageList.class.getName());
                            transaction.commit();
        }
        fragmentList.setCommunicater(this);


        toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        NavigationDrawerLayout drawerFragment = (NavigationDrawerLayout) getSupportFragmentManager().findFragmentById(R.id.fragment_nav_drawer);
        drawerFragment.setUp(R.id.fragment_nav_drawer,(DrawerLayout)findViewById(R.id.drawer_layout), toolbar);



    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stage, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void respond(int index, String title) {
        fragmentView = new FragmentStageView();
        fragmentView.recieveData(index, title);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (fragmentManager.findFragmentByTag("FragmentList") != null) {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.replace(R.id.stage_fragment_holder_id, fragmentView, "FragmentView");
            transaction.addToBackStack(FragmentStageView.class.getName());
            transaction.commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() == 0) {
            this.finish();
        } else {
            fragmentManager.popBackStack();
        }
    }
}
