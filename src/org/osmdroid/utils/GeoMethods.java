package org.osmdroid.utils;

import android.content.Context;
import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Tony on 11-Feb-15.
 */
public class GeoMethods {
    Context context;
    public GeoMethods(Context context) {
        this.context = context;
    }
    JsonFilesHandler jfh;
    // Location near our stage?
    public int onWhichStage(Location location) throws JSONException {
        JSONObject fileObj;
        JSONArray geoArr;
        jfh = new JsonFilesHandler(context);
        int stage_id = 0;
        for (int i = 1; i <= 32; i++) {
            fileObj = jfh.parseJSONObj("json/stage" + i + ".json");
            geoArr = fileObj.getJSONArray("geo");
            if (ifOnStage(geoArr,location)) {
                stage_id = i;
                i = 32;
            } else {
                stage_id = 0; // means not found
            }
        }
        return stage_id;
    }

    public Boolean ifOnStage(JSONArray jarr, Location lastGeo) throws JSONException {
        int id = -1; // -1 == can't find lastGeo
        boolean lat_included = false;
        boolean lng_included = false;
        if (lastGeo != null) {
                if (lastGeo.getLongitude() < jarr.getJSONObject(0).getDouble("-lon") &&
                        lastGeo.getLongitude() > jarr.getJSONObject(jarr.length() - 1).getDouble("-lon")) {
                    lng_included = true;
                }

                if (jarr.getJSONObject(0).getDouble("-lat") < jarr.getJSONObject(jarr.length() - 1).getDouble("-lat")) {
                    if (lastGeo.getLatitude() < jarr.getJSONObject(jarr.length() - 1).getDouble("-lat") &&
                            lastGeo.getLatitude() > jarr.getJSONObject(0).getDouble("-lat")) {
                        lat_included = true;
                    }
                } else {
                    if (lastGeo.getLatitude() < jarr.getJSONObject(0).getDouble("-lat") &&
                            lastGeo.getLatitude() > jarr.getJSONObject(jarr.length() - 1).getDouble("-lat")) {
                        lat_included = true;
                    }
                }
                if (lng_included == true && lat_included == true) {
                    return true;
                }
        }
        return false;
    }

    public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371; //kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = (earthRadius * c);

        return dist;
    }

}
