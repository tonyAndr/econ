package org.osmdroid;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Tony on 20-Jan-15.
 */
public class StageListItem {
    public Integer number;
    public String fromTo;


    public StageListItem(Integer number, String fromTo) {
        this.number = number;
        this.fromTo = fromTo;

    }

    public StageListItem(JSONObject object){
        try {
            this.number = object.getInt("number");
            this.fromTo = object.getString("fromTo");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Factory method to convert an array of JSON objects into a list of objects
    // User.fromJson(jsonArray);
    public static ArrayList<StageListItem> fromJson(JSONArray jsonObjects) {
        ArrayList<StageListItem> stages = new ArrayList<StageListItem>();
        for (int i = 0; i < jsonObjects.length(); i++) {
            try {
                stages.add(new StageListItem(jsonObjects.getJSONObject(i)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return stages;
    }
}