package com.tonyandr.caminoguide.utils;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.tonyandr.caminoguide.constants.AppConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Tony on 11-Feb-15.
 */
public class GeoMethods implements AppConstants {
    Context context;
    public GeoMethods(Context context) {
        this.context = context;
    }
    JsonFilesHandler jfh;
    // Location near our stage?
    public OnStageLocationData onWhichStage(Location location) throws JSONException {
        if (location != null) {
            JSONObject fileObj, max, min;
            ArrayList<Integer> stages = new ArrayList<>();

            jfh = new JsonFilesHandler(context);
            int stage_id = 0;
            JSONArray jsonArray = jfh.parseJSONArr("json/bounds.json");
            for (int i = 0; i < jsonArray.length(); i++) {
                fileObj = jsonArray.getJSONObject(i);
                max = fileObj.getJSONObject("maxlatlng");
                min = fileObj.getJSONObject("minlatlng");
                LatLngBounds box = new LatLngBounds((new LatLng(min.getDouble("lat"), min.getDouble("lng"))),(new LatLng(max.getDouble("lat"), max.getDouble("lng"))));
                if (box.contains(new LatLng(location.getLatitude(), location.getLongitude()))) {
                    stages.add(fileObj.getInt("stageid"));
                    Log.w(DEBUGTAG, "Close to stage #"+fileObj.getInt("stageid"));
                }
            }
            if (stages.size() > 0) {
                Log.w(DEBUGTAG, "Close to stages count #"+stages.size());

                double localMin = 9999;
                int localPointId = 0;
                for (int id:stages) {
                    fileObj = jfh.parseJSONObj("json/stage" + id + ".json");
                    double thisMin = getMinFromArray(fileObj, location).localMin;
                    int thisPointId = getMinFromArray(fileObj, location).pointId;
                    if(localMin > thisMin) {
                        localMin = thisMin;
                        stage_id = id;
                        localPointId = thisPointId;
                    }
                }
                return new OnStageLocationData(stage_id, localPointId);
            }
        }
        return null;
    }

    private OnStageLocationData getMinFromArray (JSONObject jsonObject, Location location) throws JSONException {
        JSONArray jsonArray = jsonObject.getJSONArray("geo");
        double localMin = 9999;
        int id = 0;
        for (int i=0; i<jsonArray.length(); i++) {
            double lat = Double.parseDouble(jsonArray.getJSONObject(i).getString("lat"));
            double lng = Double.parseDouble(jsonArray.getJSONObject(i).getString("lng"));
            double dist = distance(new LatLng(lat, lng), new LatLng(location.getLatitude(), location.getLongitude()));
            if ( localMin > dist) {
                localMin = dist;
                id = i;
            }
        }
        return new OnStageLocationData(localMin, id);
    }


    public double distance(LatLng current, LatLng last){
        if(last==null)
            return 0;
        Location cL = new Location("");
        cL.setLatitude(current.latitude);
        cL.setLongitude(current.longitude);

        Location lL = new Location("");
        lL.setLatitude(last.latitude);
        lL.setLongitude(last.longitude);

        return lL.distanceTo(cL)/1000;
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
