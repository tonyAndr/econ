package org.osmdroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.osmdroid.constants.AppConstants;
import org.osmdroid.map.MapActivity;
import org.osmdroid.utils.DBUpdateService;
import org.osmdroid.utils.JsonFilesHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class SplashActivity extends ActionBarActivity implements AppConstants {

    private static int SPLASH_SCREEN_DELAY = 1000;
    private boolean mUserLearnedApp;
    private boolean mFromSavedInstanceState;
    private Date mLastDBUpdate;

    public BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("dbservice", "Recieved intent" + intent.getBooleanExtra("broadcast", false));
            startMapActivity();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        JsonFilesHandler jfh = new JsonFilesHandler(this);

        IntentFilter filter = new IntentFilter("org.osmdroid.utils.DBUpdateService");
        registerReceiver(br, filter);

        mUserLearnedApp = Boolean.valueOf(readFromPreferences(this, KEY_USER_LEARNED_DRAWER, "false"));
        if (savedInstanceState != null) {
            mFromSavedInstanceState = true;
        }
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
                            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                                Log.d("dbservice", "success arr");
                                Intent intent = new Intent(SplashActivity.this, DBUpdateService.class);
                                intent.putExtra("array", response.toString());
                                startService(intent);

                                if (!mUserLearnedApp) {
                                    mUserLearnedApp = true;
                                    saveToPreferences(SplashActivity.this, KEY_USER_LEARNED_DRAWER, mUserLearnedApp + "");
                                }
                            }
                        });
                    } else {
                        startMapActivity();
                    }
                }
            });
        } else {
            if (!mUserLearnedApp && !mFromSavedInstanceState) {
                Log.d("dbservice", "First start");
                if (!mUserLearnedApp) {
                    mUserLearnedApp = true;
                    saveToPreferences(this, KEY_USER_LEARNED_DRAWER, mUserLearnedApp + "");
                }
                // getfromfile
                String alb_path = "json/albergues.json";
                JSONArray albJArr = jfh.parseJSONArr(alb_path);
                //update
                Intent intent = new Intent(SplashActivity.this, DBUpdateService.class);
                intent.putExtra("array", albJArr.toString());
                startService(intent);
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
        return sharedPreferences.getString(preferenceName, defaultValue);
    }

}
