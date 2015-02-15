package org.osmdroid.utils;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Tony on 14-Feb-15.
 */
public class DBUpdateService extends IntentService {
    DBControllerAdapter dbController;

    public DBUpdateService() {
        super("DBUpdateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("dbservice", "start service");
        dbController = new DBControllerAdapter(this);
        eraseAlbergues();
        try {
            JSONArray array = new JSONArray(intent.getStringExtra("array"));
            addAlbergues(array);


        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendThisBroadcast();
        Log.d("dbservice", "stop service");

    }

    private void sendThisBroadcast() {
        Intent intent = new Intent();
        intent.setAction("org.osmdroid.utils.DBUpdateService");
        intent.putExtra("broadcast", true);
        sendBroadcast(intent);
    }

    private void addAlbergues (JSONArray array) {
        JSONObject obj;
        long id;
        String[] location;
        int count = 0;
        try {
            for (int i = 0; i < array.length(); i++) {
                obj = array.getJSONObject(i);
                location = new String[2];

                if (obj.getString("alb_geo").contains(", ")) {
                    location = obj.getString("alb_geo").split(", ");
                } else if (obj.getString("alb_geo").contains(",")) {
                    location = obj.getString("alb_geo").split(",");
                } else {
                    location = null;
                }
                if (location !=null) {
                    id = dbController.insertAlbergue(obj.getString("albergue"), obj.getString("locality"), obj.getString("telephone"), obj.getString("beds"), obj.getInt("stage"), Double.parseDouble(location[0]), Double.parseDouble(location[1]), obj.getString("type"));
                    count++;
                } else {
                    Log.d("dbservice", "Null at " + obj.getInt("id"));
                }

            }
            Log.d("dbservice", "Inserted " + count);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void eraseAlbergues () {
        int count = dbController.eraseAlbergues();
        Log.d("dbservice", "deleted " +count);
    }

}
