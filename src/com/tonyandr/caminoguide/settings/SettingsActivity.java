package com.tonyandr.caminoguide.settings;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import com.tonyandr.caminoguide.NavigationDrawerLayout;
import com.tonyandr.caminoguide.R;


public class SettingsActivity extends ActionBarActivity implements FragmentManager.OnBackStackChangedListener {

    private Toolbar toolbar;
    private FragmentManager fragmentManager;
    private int backstackCount;
    private NavigationDrawerLayout drawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        fragmentManager = getFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        drawerFragment = (NavigationDrawerLayout) getSupportFragmentManager().findFragmentById(R.id.fragment_nav_drawer);
        drawerFragment.setUp(R.id.fragment_nav_drawer,(DrawerLayout)findViewById(R.id.drawer_layout), toolbar);

        fragmentManager.beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() == 0) {
            this.finish();
        } else {
            fragmentManager.popBackStack();
        }
    }

    @Override
    public void onBackStackChanged() {
        backstackCount = fragmentManager.getBackStackEntryCount();
        if (backstackCount == 0) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        drawerFragment.setActionBarArrowDependingOnFragmentsBackStack(backstackCount);
        if (backstackCount != 0) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class SettingsFragment extends PreferenceFragment {

        public SettingsFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.settings);

            Preference dl_pref = findPreference("dl_key");
            dl_pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    //open browser or intent here
                    getActivity().startActivity(new Intent(getActivity(), MapManagerActivity.class));
//                    Toast.makeText(getActivity(), "Starting download process...", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
    }



}
