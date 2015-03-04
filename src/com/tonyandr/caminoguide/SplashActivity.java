package com.tonyandr.caminoguide;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.tonyandr.caminoguide.constants.AppConstants;
import com.tonyandr.caminoguide.map.MapActivity;
import com.tonyandr.caminoguide.utils.DBUpdateService;
import com.tonyandr.caminoguide.utils.JsonFilesHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class SplashActivity extends ActionBarActivity implements AppConstants {

    private static int SPLASH_SCREEN_DELAY = 400;
    private boolean mUserLearnedApp;
    private boolean mFromSavedInstanceState;
    private Date mLastDBUpdate;
    private GoogleApiClient mGoogleApiClient;
    private AddAlberguesTask addAlberguesTask;
    private AddLocalitiesTask addLocalitiesTask;
    private ProgressBar progressBar;
    private TextView textView;
    private int mAlberguesArrayLength;
//    private JSONArray albergues;
//    private JSONArray localities;

    public BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("dbservice", "Recieved intent " + intent.getIntExtra("progress", 0));

            progressBar.setProgress(intent.getIntExtra("progress", 0));
            textView.setText("Albergues... " + intent.getIntExtra("count", 0));
            if (intent.getBooleanExtra("finished", false)) {
                startMapActivity();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = (ProgressBar) findViewById(R.id.progressBar2);
        progressBar.setMax(100);
        textView = (TextView) findViewById(R.id.splash_textview);

        JsonFilesHandler jfh = new JsonFilesHandler(this);

        IntentFilter filter = new IntentFilter("DBUpdateService");
        registerReceiver(br, filter);

        mUserLearnedApp = Boolean.valueOf(readFromPreferences(this, KEY_USER_LEARNED_APP, "false"));
        if (savedInstanceState != null) {
            mFromSavedInstanceState = true;
        }
        if (!mUserLearnedApp && !mFromSavedInstanceState) {
            Log.d("dbservice", "First start");
            if (!mUserLearnedApp) {
                mUserLearnedApp = true;
                saveToPreferences(this, KEY_USER_LEARNED_APP, mUserLearnedApp + "");
            }
            // getfromfile
            String alb_path = "json/albergues.json";
            JSONObject fileObj = jfh.parseJSONObj(alb_path);
            try {
                JSONArray albergues = fileObj.getJSONArray("albergues");
                JSONArray localities = fileObj.getJSONArray("localities");
                mAlberguesArrayLength = albergues.length();
                //update
                Intent intent = new Intent(SplashActivity.this, DBUpdateService.class);
                intent.putExtra("albergues", albergues.toString());
                intent.putExtra("localities", localities.toString());
                startService(intent);
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, "Can't update database!", Toast.LENGTH_LONG).show();
            }

        } else {
            if (isNetworkAvailable()) {
                final AsyncHttpClient client = new AsyncHttpClient();
                client.get("http://alberguenajera.es/projects/gms/get_update_date.php", new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                        boolean updated = false;
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        try {
                            mLastDBUpdate = df.parse(readFromPreferences(SplashActivity.this, "db_update_date", "2010-02-13 15:48:02"));
                            for (int i = 0; i < response.length(); i++) {
                                if (df.parse(response.getString(i)).after(mLastDBUpdate)) {
                                    updated = true;
                                    mLastDBUpdate = df.parse(response.getString(i));
                                }
                            }
                            saveToPreferences(SplashActivity.this, "db_update_date", df.format(mLastDBUpdate));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        if (updated == true) {
                            client.get("http://alberguenajera.es/projects/gms/get_albergues_json.php", new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                    Log.d("dbservice", "success obj");

                                    Intent intent = new Intent(SplashActivity.this, DBUpdateService.class);
                                    try {
                                        JSONArray albergues = response.getJSONArray("albergues");
                                        mAlberguesArrayLength = albergues.length();
                                        JSONArray localities = response.getJSONArray("localities");
                                        intent.putExtra("albergues", albergues.toString());
                                        intent.putExtra("localities", localities.toString());
                                        startService(intent);

                                        if (!mUserLearnedApp) {
                                            mUserLearnedApp = true;
                                            saveToPreferences(SplashActivity.this, KEY_USER_LEARNED_APP, mUserLearnedApp + "");
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        Toast.makeText(SplashActivity.this, "Error while updating :/", Toast.LENGTH_LONG).show();
                                    }


//                                    try {
//                                        albergues = response.getJSONArray("albergues");
//                                        localities = response.getJSONArray("localities");
//
//
//                                        if (!mUserLearnedApp) {
//                                            mUserLearnedApp = true;
//                                            saveToPreferences(SplashActivity.this, KEY_USER_LEARNED_APP, mUserLearnedApp + "");
//                                        }
//                                    } catch (JSONException e) {
//                                        e.printStackTrace();
//                                        Toast.makeText(SplashActivity.this, "Error while updating :/", Toast.LENGTH_LONG).show();
//                                    }


                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                                    startMapActivity();
                                }
                            });
                        } else {
                            startMapActivity();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        startMapActivity();
                    }
                });
            } else {
                startMapActivity();
            }
        }

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void startMapActivity() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MapActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_SCREEN_DELAY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(br);
    }

    public static void saveToPreferences(Context context, String preferenceName, String preferenceValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(preferenceName, preferenceValue);
        editor.apply();
    }

    public static String readFromPreferences(Context context, String preferenceName, String defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (sharedPreferences.contains(LOCATION_KEY_LAT)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(LOCATION_KEY_LAT).remove(LOCATION_KEY_LNG);
            editor.commit();
        }

        return sharedPreferences.getString(preferenceName, defaultValue);
    }

    class AddAlberguesTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            return null;
        }
    }
    class AddLocalitiesTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            return null;
        }
    }



}
