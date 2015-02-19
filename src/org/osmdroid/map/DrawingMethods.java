package org.osmdroid.map;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.R;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.utils.DBControllerAdapter;
import org.osmdroid.utils.GeoMethods;
import org.osmdroid.utils.JsonFilesHandler;
import org.osmdroid.views.MapView;

import java.util.ArrayList;

/**
 * Created by Tony on 11-Feb-15.
 */
public class DrawingMethods {
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
    }

    ;

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
        mPin.setTitle("Albergue "+title);
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

    public double drawDistanceRoute(int stage_id, Location current, Location finish) throws JSONException {

        JSONObject fileObj;
        JSONArray geoArr;
        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        fileObj = jfh.parseJSONObj("json/stage" + stage_id + ".json");
        geoArr = fileObj.getJSONArray("geo");

        Polyline routeOverlay = new Polyline(context);
        // Drawing route, each stage on it's own overlay
        JSONObject geopoint;
        JSONObject prev_geopoint = new JSONObject();
        double dist = 0;
        double distFromCurrToFin = 0;
        double minDistCurr = 999;
        double minDistFin = 999;
        boolean near;

        near = geoMethods.ifOnStage(geoArr, current);

        if (near) {
            waypoints.add(new GeoPoint(current.getLatitude(), current.getLongitude()));
            for (int h = 0; h < geoArr.length(); h++) {
                geopoint = geoArr.getJSONObject(h);

                dist = geoMethods.distFrom(geopoint.getDouble("-lat"), geopoint.getDouble("-lon"), current.getLatitude(), current.getLongitude());
                if (dist < minDistCurr) {
                    minDistCurr = dist;
                    prev_geopoint = geopoint;
                } else {
                    dist = geoMethods.distFrom(geopoint.getDouble("-lat"), geopoint.getDouble("-lon"), finish.getLatitude(), finish.getLongitude());
                    if (dist < minDistFin) {
                        minDistFin = dist;
                        waypoints.add(new GeoPoint(geopoint.getDouble("-lat"), geopoint.getDouble("-lon")));
                        distFromCurrToFin = distFromCurrToFin + geoMethods.distFrom(
                                prev_geopoint.getDouble("-lat"),
                                prev_geopoint.getDouble("-lon"),
                                geopoint.getDouble("-lat"),
                                geopoint.getDouble("-lon"));
                        prev_geopoint = geopoint;
                    } else {
                        distFromCurrToFin = distFromCurrToFin + geoMethods.distFrom(
                                finish.getLatitude(), finish.getLongitude(),
                                geopoint.getDouble("-lat"),
                                geopoint.getDouble("-lon"));
                        waypoints.add(new GeoPoint(finish.getLatitude(), finish.getLongitude()));
                    }
                }

            }
            routeOverlay.setColor(Color.argb(100, 0, 150, 136));
            routeOverlay.setWidth(5.0f);
            routeOverlay.setPoints(waypoints);
            mapView.getOverlays().add(routeOverlay);
        }

        return distFromCurrToFin;
    }


    public void drawStageRoute(Integer stage_id) throws JSONException {
        //stage_id == 0 => all route
        JSONObject fileObj, geo;
        JSONArray geoArr;
        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        GeoPoint newPoint;
        Polyline routeOverlay = new Polyline(context);
        // Drawing route, each stage on it's own overlay
        if (stage_id == 0) {
            for (int i = 1; i <= 32; i++) {
                fileObj = jfh.parseJSONObj("json/stage" + i + ".json");
                geoArr = fileObj.getJSONArray("geo");

                for (int h = 0; h < geoArr.length(); h++) {
                    geo = geoArr.getJSONObject(h);
                    Double lat = geo.getDouble("-lat");
                    Double lng = geo.getDouble("-lon");
                    newPoint = new GeoPoint(lat, lng);
                    waypoints.add(newPoint);
                }
            }
        } else {
            fileObj = jfh.parseJSONObj("json/stage" + stage_id + ".json");
            geoArr = fileObj.getJSONArray("geo");

            for (int h = 0; h < geoArr.length(); h++) {
                geo = geoArr.getJSONObject(h);
                Double lat = geo.getDouble("-lat");
                Double lng = geo.getDouble("-lon");
                newPoint = new GeoPoint(lat, lng);
                waypoints.add(newPoint);
            }
        }

        routeOverlay.setColor(Color.CYAN);
        routeOverlay.setWidth(4.0f);
        routeOverlay.setPoints(waypoints);
        mapView.getOverlays().add(routeOverlay);
    }

    public FolderOverlay drawAlbMarkers(int stage_id, FolderOverlay albMarkersOverlay, Location finish) throws JSONException {
        boolean highlighted = false;
        JSONObject vh = null;
        JSONArray albJArr = dbController.getAlbergues(stage_id);
        for (int i = 0; i < albJArr.length(); i++) {
            JSONObject v = albJArr.getJSONObject(i);
            String title = v.getString("title");
            String desc = v.getString("locality") + " " + v.getString("address");
            String snippet = v.getString("tel");
            if (stage_id != 0) {
                if (stage_id == v.getInt("stage")) {
                    Double lat = v.getDouble("lat");
                    Double lng = v.getDouble("lng");

                    if (lat == finish.getLatitude() && lng == finish.getLongitude()) {
                        highlighted = true;
                        vh = v;
                    } else {
                        albMarkersOverlay.add(drawAlbMarker(lat, lng, title, desc, snippet, false));
                    }
                }
            } else {
                Double lat = v.getDouble("lat");
                Double lng = v.getDouble("lng");
                albMarkersOverlay.add(drawAlbMarker(lat, lng, title, desc, snippet, false));
            }
        }
        if (highlighted && vh != null) {
            albMarkersOverlay.add(drawAlbMarker(vh.getDouble("lat"), vh.getDouble("lng"), vh.getString("title"), vh.getString("locality") + " " + vh.getString("address"), vh.getString("tel"), true));
        }
        return albMarkersOverlay;
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
}
