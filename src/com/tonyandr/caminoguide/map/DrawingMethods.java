package com.tonyandr.caminoguide.map;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.tonyandr.caminoguide.R;
import com.tonyandr.caminoguide.constants.AppConstants;
import com.tonyandr.caminoguide.utils.DBControllerAdapter;
import com.tonyandr.caminoguide.utils.GMapReturnObject;
import com.tonyandr.caminoguide.utils.GeoMethods;
import com.tonyandr.caminoguide.utils.JsonFilesHandler;
import com.tonyandr.caminoguide.utils.MarkerDataObject;
import com.tonyandr.caminoguide.utils.OnStageLocationData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;

/**
 * Created by Tony on 11-Feb-15.
 */
public class DrawingMethods implements AppConstants {
    private MapView mapView;
    private Context context;
    JsonFilesHandler jfh;
    GeoMethods geoMethods;
    DBControllerAdapter dbController;

    public DrawingMethods(MapView mapView, Context context) {
        this.context = context;
        this.mapView = mapView;
        jfh = new JsonFilesHandler(context);
        dbController = new DBControllerAdapter(context);
        geoMethods = new GeoMethods(context);
    }

    public DrawingMethods(Context context) {
        this.context = context;
        jfh = new JsonFilesHandler(context);
        dbController = new DBControllerAdapter(context);
        geoMethods = new GeoMethods(context);
    }


    private Marker drawAlbMarker(Double lat, Double lng, String title, String desc, String snippet, boolean highlighted) {
        GeoPoint mGeoP = new GeoPoint(lat, lng);
        // build a new marker pin
        Marker mPin = new Marker(mapView);
        mPin.setPosition(mGeoP);
        mPin.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        if (highlighted) {
            mPin.setIcon(context.getResources().getDrawable(R.drawable.to_albergue_stage_view));
        } else {
            mPin.setIcon(context.getResources().getDrawable(R.drawable.ic_alb_marker));
        }
        mPin.setTitle("Albergue " + title);
        mPin.setSubDescription(desc);
        mPin.setSnippet(snippet);

        // add new marker pin to map
        return mPin;
    }


    private Marker drawCityMarker(Double lat, Double lng, String title) {

        GeoPoint mGeoP = new GeoPoint(lat, lng);

        // build a new marker pin
        Marker mPin = new Marker(mapView);
        mPin.setPosition(mGeoP);
        mPin.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        mPin.setIcon(context.getResources().getDrawable(R.drawable.ic_city_marker2));
        mPin.setTitle(title);
//        mPin.setSubDescription(desc);
//        mPin.setSnippet(snippet);

        // add new marker pin to map
        return mPin;
    }

    public JSONArray getRouteArray(Location current, Location finish) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        int current_stage, current_point, finish_stage, finish_point;
        OnStageLocationData onStageLocationData;
        onStageLocationData = geoMethods.onWhichStage(finish);
        if (onStageLocationData != null) {
            Log.w(DEBUGTAG, "getArray: finish != null");
            finish_stage = onStageLocationData.stageId;
            finish_point = onStageLocationData.pointId;
            onStageLocationData = geoMethods.onWhichStage(current);

            if (onStageLocationData != null) {
                Log.w(DEBUGTAG, "getArray: current != null");
                current_stage = onStageLocationData.stageId;
                current_point = onStageLocationData.pointId;
                if (current_stage == finish_stage) { // Odinakoviy stage
                    Log.w(DEBUGTAG, "getArray: current_stage == finish_stage");
                    JSONObject jsonObject = jfh.parseJSONObj("json/stage" + current_stage + ".json");
                    Log.w(DEBUGTAG, "getArray: curr_point = " + current_point + " finish_point = " + finish_point);
                    if (current_point > finish_point) { // Dvijenie nazad
                        Log.w(DEBUGTAG, "getArray: Dvijenie nazad");
                        JSONArray geoArray = jsonObject.getJSONArray("geo");
                        for (int i = current_point; i >= finish_point; i--) {
                            jsonArray.put(geoArray.getJSONObject(i));
                            Log.w(DEBUGTAG, "getArray: object added");
                        }
                    } else if (current_point < finish_point) { // Dvijenie vpered
                        Log.w(DEBUGTAG, "getArray: Dvijenie vpered");
                        JSONArray geoArray = jsonObject.getJSONArray("geo");
                        for (int i = current_point; i <= finish_point; i++) {
                            jsonArray.put(geoArray.getJSONObject(i));
                            Log.w(DEBUGTAG, "getArray: object added");
                        }
                    } else { // oni ravni => ne risuem
                        Log.w(DEBUGTAG, "getArray: points ravni => ne risuem");
                    }
                } else { // Razniy stage
                    Log.w(DEBUGTAG, "getArray: current_stage != finish_stage");
                    if (current_stage > finish_stage) { // Nash stage dalwe chem tochka, dvijenie nazad
                        Log.w(DEBUGTAG, "getArray: Nash stage dalwe chem tochka, dvijenie nazad");
                        Log.w(DEBUGTAG, "getArray: cur_stage = " + current_stage + " fin_stage = " + finish_stage);

                        for (int i = current_stage; i >= finish_stage; i--) {
                            JSONObject jsonObject = jfh.parseJSONObj("json/stage" + i + ".json");
                            JSONArray geoArray = jsonObject.getJSONArray("geo");
                            if (i == current_stage) {
                                for (int j = current_point; j >= 0; j--) {
                                    jsonArray.put(geoArray.getJSONObject(j));
                                    Log.w(DEBUGTAG, "getArray: object added");
                                }
                            } else if (i == finish_stage) {
                                for (int j = geoArray.length() - 1; j >= finish_point; j--) {
                                    jsonArray.put(geoArray.getJSONObject(j));
                                    Log.w(DEBUGTAG, "getArray: object added");
                                }
                            } else {
                                for (int j = geoArray.length() - 1; j >= 0; j--) {
                                    jsonArray.put(geoArray.getJSONObject(j));
                                    Log.w(DEBUGTAG, "getArray: object added");
                                }
                            }

                        }
                    } else {
                        Log.w(DEBUGTAG, "getArray: Tochka dalwee chem curr_stage, dvij forward");
                        Log.w(DEBUGTAG, "getArray: cur_stage = " + current_stage + " fin_stage = " + finish_stage);
                        for (int i = current_stage; i <= finish_stage; i++) {
                            JSONObject jsonObject = jfh.parseJSONObj("json/stage" + i + ".json");
                            JSONArray geoArray = jsonObject.getJSONArray("geo");
                            if (i == current_stage) {
                                for (int j = current_point; j < geoArray.length(); j++) {
                                    jsonArray.put(geoArray.getJSONObject(j));
                                    Log.w(DEBUGTAG, "getArray: object added");
                                }
                            } else if (i == finish_stage) {
                                for (int j = 0; j <= finish_point; j++) {
                                    jsonArray.put(geoArray.getJSONObject(j));
                                    Log.w(DEBUGTAG, "getArray: object added");
                                }
                            } else {
                                for (int j = 0; j < geoArray.length(); j++) {
                                    jsonArray.put(geoArray.getJSONObject(j));
                                    Log.w(DEBUGTAG, "getArray: object added");
                                }
                            }
                        }
                    }
                }
            } else {
                Log.w(DEBUGTAG, "getArray: current == null");
                Log.w(DEBUGTAG, "getArray: fin_stg = " + finish_stage + " fin_pnt = " + finish_point);
                JSONObject jsonObject = jfh.parseJSONObj("json/stage" + finish_stage + ".json");
                JSONArray geoArray = jsonObject.getJSONArray("geo");
                for (int j = 0; j <= finish_point; j++) {
                    jsonArray.put(geoArray.getJSONObject(j));
                    Log.w(DEBUGTAG, "getArray: object added");
                }
            }
        } else {
            Log.e(DEBUGTAG, "Finish location == null, WTF?!");
        }
        Log.w(DEBUGTAG, "getArray: array's total length = " + jsonArray.length());
        return jsonArray;
    }

    public double drawDistanceRoute(Location current, Location finish) throws JSONException {
        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        JSONArray geoArr = getRouteArray(current, finish);
        Polyline routeOverlay = new Polyline(context);
        JSONObject geopoint;
        JSONObject prev_geopoint = new JSONObject();
        double distFromCurrToFin = 0;
        if (current != null) {
            if (areaLimitSpain.contains(new GeoPoint(current.getLatitude(), current.getLongitude())))
                waypoints.add(new GeoPoint(current.getLatitude(), current.getLongitude()));
        }
        if (geoArr.length() > 0 ) {
            for (int h = 0; h < geoArr.length(); h++) {
                geopoint = geoArr.getJSONObject(h);
                if (h == 0) {
                    if (current != null)
                        distFromCurrToFin = distFromCurrToFin + geoMethods.distance(new LatLng(geopoint.getDouble("lat"), geopoint.getDouble("lng")), new LatLng(current.getLatitude(), current.getLongitude()));
                    waypoints.add(new GeoPoint(geopoint.getDouble("lat"), geopoint.getDouble("lng")));
                } else {
                    distFromCurrToFin = distFromCurrToFin + geoMethods.distance(
                            new LatLng(prev_geopoint.getDouble("lat"), prev_geopoint.getDouble("lng")),
                            new LatLng(geopoint.getDouble("lat"), geopoint.getDouble("lng")));
                    waypoints.add(new GeoPoint(geopoint.getDouble("lat"), geopoint.getDouble("lng")));
                }
                prev_geopoint = geopoint;
            }
            distFromCurrToFin = distFromCurrToFin + geoMethods.distance(new LatLng(prev_geopoint.getDouble("lat"), prev_geopoint.getDouble("lng")), new LatLng(finish.getLatitude(), finish.getLongitude()));
        }

        waypoints.add(new GeoPoint(finish.getLatitude(), finish.getLongitude()));
        routeOverlay.setColor(Color.argb(200, 0, 150, 136));
        routeOverlay.setWidth(5.0f);
        routeOverlay.setPoints(waypoints);
        mapView.getOverlays().add(routeOverlay);

        return distFromCurrToFin;
    }


    public void drawAllRoute() throws JSONException {
        JSONObject fileObj, geo;
        JSONArray geoArr;
        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        GeoPoint newPoint;
        Polyline routeOverlay = new Polyline(context);
        for (int i = 1; i <= 32; i++) {
            fileObj = jfh.parseJSONObj("json/stage" + i + ".json");
            geoArr = fileObj.getJSONArray("geo");
            for (int h = 0; h < geoArr.length(); h++) {
                geo = geoArr.getJSONObject(h);
                Double lat = geo.getDouble("lat");
                Double lng = geo.getDouble("lng");
                newPoint = new GeoPoint(lat, lng);
                waypoints.add(newPoint);
            }
        }

        routeOverlay.setColor(Color.rgb(0, 150, 136));
        routeOverlay.setWidth(5.0f);
        routeOverlay.setPoints(waypoints);
        mapView.getOverlays().add(routeOverlay);
    }

    public FolderOverlay drawAlbMarkers(FolderOverlay albMarkersOverlay, Location finish) throws JSONException {
        boolean highlighted = false;
        JSONObject vh = null;
        JSONArray albJArr = dbController.getAlbergues(0);
        for (int i = 0; i < albJArr.length(); i++) {
            JSONObject v = albJArr.getJSONObject(i);
            String title = v.getString("title");
            String desc = v.getString("locality") + " " + v.getString("address");
            String snippet = v.getString("tel");
            Double lat = v.getDouble("lat");
            Double lng = v.getDouble("lng");
            if (finish != null) {
                if (lat == finish.getLatitude() && lng == finish.getLongitude()) {
                    highlighted = true;
                    vh = v;
                } else {
                    albMarkersOverlay.add(drawAlbMarker(lat, lng, title, desc, snippet, false));
                }
            } else {
                albMarkersOverlay.add(drawAlbMarker(lat, lng, title, desc, snippet, false));
            }
        }
        if (highlighted && vh != null) {
            albMarkersOverlay.add(drawAlbMarker(vh.getDouble("lat"), vh.getDouble("lng"), vh.getString("title"), vh.getString("locality") + " " + vh.getString("address"), vh.getString("tel"), true));
        }
        return albMarkersOverlay;
    }

    public ArrayList<MarkerDataObject> drawAlbMarkersGMAP(Location finish) throws JSONException {
        ArrayList<MarkerDataObject> markers = new ArrayList<>();
        boolean highlighted = false;
        JSONObject vh = null;
        JSONArray albJArr = dbController.getAlbergues(0);
        for (int i = 0; i < albJArr.length(); i++) {
            JSONObject v = albJArr.getJSONObject(i);
            String title = v.getString("title");
            String snippet = v.getString("tel");
            Double lat = v.getDouble("lat");
            Double lng = v.getDouble("lng");
            if (finish != null) {
                if (lat == finish.getLatitude() && lng == finish.getLongitude()) {
                    highlighted = true;
                    vh = v;
                } else {
                    markers.add(new MarkerDataObject(new LatLng(lat, lng), title, snippet, R.drawable.ic_alb_marker));
                }
            } else {
                markers.add(new MarkerDataObject(new LatLng(lat, lng), title, snippet, R.drawable.ic_alb_marker));

            }
        }
        if (highlighted && vh != null) {
            Double lat = vh.getDouble("lat");
            Double lng = vh.getDouble("lng");
            String title = vh.getString("title");
            String snippet = vh.getString("tel");
            markers.add(new MarkerDataObject(new LatLng(lat, lng), title, snippet, R.drawable.to_albergue_stage_view));
        }

        return markers;
    }

    public void drawCityMarkers(FolderOverlay cityMarkersOverlay) throws JSONException {

        JSONArray routeJArr = dbController.getLocalities();
        for (int i = 0; i < routeJArr.length(); i++) {
            JSONObject v = routeJArr.getJSONObject(i);
            if (v != null) {
                Double lat = v.getDouble("lat");
                Double lng = v.getDouble("lng");
                String title = v.getString("title");
                cityMarkersOverlay.add(drawCityMarker(lat, lng, title));
            }
        }
        mapView.getOverlays().add(cityMarkersOverlay);
    }

    public ArrayList<MarkerDataObject> drawCityMarkersGMAP() throws JSONException {
        ArrayList<MarkerDataObject> markers = new ArrayList<>();
        JSONArray routeJArr = dbController.getLocalities();
        for (int i = 0; i < routeJArr.length(); i++) {
            JSONObject v = routeJArr.getJSONObject(i);
            if (v != null) {
                Double lat = v.getDouble("lat");
                Double lng = v.getDouble("lng");
                String title = "Albergue " + v.getString("title");
                markers.add(new MarkerDataObject(new LatLng(lat, lng), title, null, R.drawable.ic_city_marker2));

            }
        }
        return markers;
    }

    public PolylineOptions drawAllRouteGMAP() throws JSONException {
        PolylineOptions rectOptions = new PolylineOptions();

        //stage_id == 0 => all route
        JSONObject fileObj, geo;
        JSONArray geoArr;
        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        LatLng newPoint;
        // Drawing route, each stage on it's own overlay
        for (int i = 1; i <= 32; i++) {
            fileObj = jfh.parseJSONObj("json/stage" + i + ".json");
            geoArr = fileObj.getJSONArray("geo");

            for (int h = 0; h < geoArr.length(); h++) {
                geo = geoArr.getJSONObject(h);
                Double lat = geo.getDouble("lat");
                Double lng = geo.getDouble("lng");
                newPoint = new LatLng(lat, lng);
                rectOptions.add(newPoint);
            }
        }
        rectOptions.color(Color.argb(200, 0, 150, 136)).width(6).geodesic(true);
        return rectOptions;
    }


    public GMapReturnObject drawDistanceRouteGMAP(Location current, Location finish) throws JSONException {
        PolylineOptions rectOptions = new PolylineOptions();

        JSONArray geoArr = getRouteArray(current, finish);
        JSONObject geopoint;
        JSONObject prev_geopoint = new JSONObject();
        double distFromCurrToFin = 0;
        if (current != null) {
            rectOptions.add(new LatLng(current.getLatitude(), current.getLongitude()));
        }
        if(geoArr.length() > 0) {
            for (int h = 0; h < geoArr.length(); h++) {
                geopoint = geoArr.getJSONObject(h);
                if (h == 0) {
                    if (current != null)
                        distFromCurrToFin = distFromCurrToFin + geoMethods.distance(new LatLng(geopoint.getDouble("lat"), geopoint.getDouble("lng")), new LatLng(current.getLatitude(), current.getLongitude()));
                    rectOptions.add(new LatLng(geopoint.getDouble("lat"), geopoint.getDouble("lng")));
                } else {
                    distFromCurrToFin = distFromCurrToFin + geoMethods.distance(
                            new LatLng(prev_geopoint.getDouble("lat"), prev_geopoint.getDouble("lng")),
                            new LatLng(geopoint.getDouble("lat"), geopoint.getDouble("lng")));
                    rectOptions.add(new LatLng(geopoint.getDouble("lat"), geopoint.getDouble("lng")));

                }
                prev_geopoint = geopoint;
            }
            distFromCurrToFin = distFromCurrToFin + geoMethods.distance(new LatLng(prev_geopoint.getDouble("lat"), prev_geopoint.getDouble("lng")), new LatLng(finish.getLatitude(), finish.getLongitude()));
        }


        rectOptions.add(new LatLng(finish.getLatitude(), finish.getLongitude()));
        rectOptions.color(Color.argb(200, 0, 150, 136)).width(6).geodesic(true);


        return new GMapReturnObject(distFromCurrToFin, rectOptions);
    }
}
