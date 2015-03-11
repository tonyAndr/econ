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
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tony on 11-Feb-15.
 */
public class DrawingMethods implements AppConstants {
    private MapView mapView;
    private Context context;
    JsonFilesHandler jfh;
    GeoMethods geoMethods;
    DBControllerAdapter dbController;
    private OnStageLocationData currentData;
    private OnStageLocationData finishData;
    private JSONArray returnArray;

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

    // *** Get array of geopoints to osm and gmap

    public JSONArray getRouteArrayOld(Location current, Location finish) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        int current_stage, current_point, finish_stage, finish_point;
        finishData = geoMethods.onWhichStage(finish);
        if (finishData != null) {
            Log.w(DEBUGTAG, "getArray: finish != null");
            finish_stage = finishData.stageId;
            finish_point = finishData.pointId;
            currentData = geoMethods.onWhichStage(current);

            if (currentData != null) {
                Log.w(DEBUGTAG, "getArray: current != null");
                current_stage = currentData.stageId;
                current_point = currentData.pointId;
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
                for (int j = 0; j < geoArray.length(); j++) {
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

    public JSONArray getRouteArray(Location current, Location finish) throws JSONException {
        returnArray = new JSONArray();
        String current_type, finish_type;
        finishData = geoMethods.onWhichStage(finish);
        if (finishData != null) {
            finish_type = (finishData.alt ? "alt" : "main");
            Log.w(DEBUGTAG, "getArray: finish != null");
            currentData = geoMethods.onWhichStage(current);
            if (currentData != null) {
                current_type = (currentData.alt ? "alt" : "main");
                if (currentData.stageId == finishData.stageId) {
                    JSONObject jsonObject = jfh.parseJSONObj("json/stage" + currentData.stageId + ".json");
                    if (currentData.partId == finishData.partId) {
                        if (currentData.alt == finishData.alt) {
                            pointSamePartSameAlt(jsonObject, current_type);
                        } else {
                            pointSamePartDiffAlt(jsonObject, current_type, finish_type);
                        }
                    } else if (currentData.partId > finishData.partId) {
                        pointDiffPartBackward(jsonObject, current_type, finish_type);
                    } else if (currentData.partId < finishData.partId) {
                        pointDiffPartForward(jsonObject, current_type, finish_type);
                    }
                } else if (currentData.stageId > finishData.stageId) {
                    pointDiffStageBackward(current_type, finish_type);
                } else if (currentData.stageId < finishData.stageId) {
                    pointDiffStageForward(current_type, finish_type);
                }
            } else {
                JSONObject jsonObject = jfh.parseJSONObj("json/stage" + finishData.stageId + ".json");
                for (int i = 0; i < jsonObject.getInt("parts"); i++) {
                    if (i == finishData.partId) {
                        returnArray = jsonObject.getJSONObject(finish_type).getJSONArray(finishData.partId + "");
                    } else {
                        returnArray = jsonObject.getJSONObject("main").getJSONArray(i + "");
                    }
                }
            }
        }
        return returnArray;
    }

    private void pointSamePartSameAlt(JSONObject obj, String type) throws JSONException {
        JSONArray arr = obj.getJSONObject(type).getJSONArray(currentData.partId + "");
        if (currentData.pointId > finishData.pointId) {
            for (int i = currentData.pointId; i >= finishData.pointId; i--) {
                returnArray.put(arr.getJSONObject(i));
            }
        } else if (currentData.pointId < finishData.pointId) {
            for (int i = currentData.pointId; i <= finishData.pointId; i++) {
                returnArray.put(arr.getJSONObject(i));
            }
        } else {

        }
    }

    private void pointSamePartDiffAlt(JSONObject obj, String cur_type, String fin_type) throws JSONException {
        JSONArray cur = obj.getJSONObject(cur_type).getJSONArray(currentData.partId + "");
        JSONArray fin = obj.getJSONObject(fin_type).getJSONArray(finishData.partId + "");
        JSONArray backward = new JSONArray();
        JSONArray forward = new JSONArray();
        double back_dist = 0, forw_dist = 0;
        JSONObject p_ob = new JSONObject(); // previos point

        if (currentData.partId == 0) {
            for (int i = currentData.pointId; i < cur.length(); i++) {
                returnArray.put(cur.getJSONObject(i));
            }
            for (int i = fin.length() - 1; i >= finishData.pointId; i--) {
                returnArray.put(fin.getJSONObject(i));
            }
        } else if (currentData.partId == obj.getInt("parts") - 1) {
            for (int i = currentData.pointId; i >= 0; i--) {
                returnArray.put(cur.getJSONObject(i));
            }
            for (int i = 0; i <= finishData.pointId; i++) {
                returnArray.put(fin.getJSONObject(i));
            }
        } else {
            for (int i = currentData.pointId; i >= 0; i--) {
                JSONObject ob = cur.getJSONObject(i);
                backward.put(ob);
                if (i != currentData.pointId) {
                    back_dist = back_dist + geoMethods.distance(new LatLng(ob.getDouble("lat"), ob.getDouble("lng")), new LatLng(p_ob.getDouble("lat"), p_ob.getDouble("lng")));
                } else {
                    p_ob = ob;
                }
            }
            for (int i = 0; i <= finishData.pointId; i++) {
                JSONObject ob = fin.getJSONObject(i);
                backward.put(ob);
                if (i != 0) {
                    back_dist = back_dist + geoMethods.distance(new LatLng(ob.getDouble("lat"), ob.getDouble("lng")), new LatLng(p_ob.getDouble("lat"), p_ob.getDouble("lng")));
                } else {
                    p_ob = ob;
                }
            }

            for (int i = currentData.pointId; i < cur.length(); i++) {
                JSONObject ob = cur.getJSONObject(i);
                forward.put(ob);
                if (i != currentData.pointId) {
                    forw_dist = forw_dist + geoMethods.distance(new LatLng(ob.getDouble("lat"), ob.getDouble("lng")), new LatLng(p_ob.getDouble("lat"), p_ob.getDouble("lng")));
                } else {
                    p_ob = ob;
                }
            }
            for (int i = fin.length() - 1; i >= finishData.pointId; i--) {
                JSONObject ob = fin.getJSONObject(i);
                forward.put(ob);
                if (i != fin.length() - 1) {
                    forw_dist = forw_dist + geoMethods.distance(new LatLng(ob.getDouble("lat"), ob.getDouble("lng")), new LatLng(p_ob.getDouble("lat"), p_ob.getDouble("lng")));
                } else {
                    p_ob = ob;
                }
            }

            if (back_dist < forw_dist) {
                for (int i = 0; i < backward.length(); i++) {
                    returnArray.put(backward.getJSONObject(i));
                }
            } else {
                for (int i = 0; i < forward.length(); i++) {
                    returnArray.put(forward.getJSONObject(i));
                }
            }
        }
    }

    private void pointDiffPartBackward(JSONObject obj, String cur_type, String fin_type) throws JSONException {
        ArrayList<JSONArray> list = new ArrayList<>();
        list.add(obj.getJSONObject(cur_type).getJSONArray(currentData.partId + ""));
        if ((currentData.partId - finishData.partId) > 1) {
            for (int i = currentData.partId - 1; i > finishData.partId; i--) {
                list.add(obj.getJSONObject("main").getJSONArray(i + ""));
            }
        }

        list.add(obj.getJSONObject(fin_type).getJSONArray(finishData.partId + ""));
        for (int i = 0; i < list.size(); i++) {
            JSONArray ar = list.get(i);
            if (i == 0) {
                for (int j = currentData.pointId; j >= 0; j--) {
                    returnArray.put(ar.getJSONObject(j));
                }
            } else if (i == list.size() - 1) {
                for (int j = ar.length() - 1; j >= finishData.pointId; j--) {
                    returnArray.put(ar.getJSONObject(j));
                }
            } else {
                for (int j = ar.length() - 1; j >= 0; j--) {
                    returnArray.put(ar.getJSONObject(j));
                }
            }
        }
    }

    private void pointDiffPartForward(JSONObject obj, String cur_type, String fin_type) throws JSONException {
        ArrayList<JSONArray> list = new ArrayList<>();
        list.add(obj.getJSONObject(cur_type).getJSONArray(currentData.partId + ""));
        if ((finishData.partId - currentData.partId) > 1) {
            for (int i = (currentData.partId + 1); i < finishData.partId; i++) {
                list.add(obj.getJSONObject("main").getJSONArray(i + ""));
            }
        }
        list.add(obj.getJSONObject(fin_type).getJSONArray(finishData.partId + ""));
        for (int i = 0; i < list.size(); i++) {
            JSONArray ar = list.get(i);
            if (i == 0) {
                for (int j = currentData.pointId; j < ar.length(); j++) {
                    returnArray.put(ar.getJSONObject(j));
                }
            } else if (i == list.size() - 1) {
                for (int j = 0; j <= finishData.pointId; j++) {
                    returnArray.put(ar.getJSONObject(j));
                }
            } else {
                for (int j = 0; j < ar.length(); j++) {
                    returnArray.put(ar.getJSONObject(j));
                }
            }
        }
    }

    private boolean stageHasAltPart(JSONObject obj, int index) throws JSONException {
        boolean hasAlt = false;
        boolean hasPart = false;
        hasAlt = obj.has("alt");
        if (hasAlt) {
            hasPart = obj.getJSONObject("alt").has(index+"");
        }
        return hasPart;
    }

    private void pointDiffStageBackward(String cur_type, String fin_type) throws JSONException {
        ArrayList<JSONArray> list = new ArrayList<>();
        boolean diff_alts = false;
        for (int i = currentData.stageId; i >= finishData.stageId; i--) {
            JSONObject obj = jfh.parseJSONObj("json/stage" + i + ".json");
            if (i == currentData.stageId) {
                list.add(obj.getJSONObject(cur_type).getJSONArray(currentData.partId + ""));
                if (currentData.partId != 0) {
                    for (int j = currentData.partId - 1; j >= 0; j--) {
                        list.add(obj.getJSONObject("main").getJSONArray(j + ""));
                    }
                }
            } else if (i == finishData.stageId) {
                if (obj.getInt("parts") > 1) {
                    if (finishData.partId != (obj.getInt("parts") - 1)) {
                        for (int j = obj.getInt("parts") - 1; j > finishData.partId; j--) {
                            if (j == (obj.getInt("parts") - 1) && currentData.alt && (i == (currentData.stageId - 1)) && stageHasAltPart(obj, j)) {
                                list.add(obj.getJSONObject("alt").getJSONArray(j + ""));
                            } else {
                                list.add(obj.getJSONObject("main").getJSONArray(j + ""));
                            }
                        }
                        list.add(obj.getJSONObject(fin_type).getJSONArray(finishData.partId + ""));
                    } else {
                        if ((!cur_type.equals(fin_type) && (i == (currentData.stageId - 1)))) {
                            list.add(obj.getJSONObject(cur_type).getJSONArray(finishData.partId + ""));
                            list.add(obj.getJSONObject(fin_type).getJSONArray(finishData.partId + ""));
                            diff_alts = true;
                        } else {
                            list.add(obj.getJSONObject(fin_type).getJSONArray(finishData.partId + ""));
                        }
                    }

                } else {
                    list.add(obj.getJSONObject(fin_type).getJSONArray(finishData.partId + ""));
                }
            } else {
                for (int j = obj.getInt("parts") - 1; j >= 0; j--) {
                    if (j == (obj.getInt("parts") - 1) && currentData.alt && (i == (currentData.stageId - 1)) && stageHasAltPart(obj, j)) {
                        list.add(obj.getJSONObject("alt").getJSONArray(j + ""));
                    } else {
                        list.add(obj.getJSONObject("main").getJSONArray(j + ""));
                    }
                }

            }
        }
        for (int i = 0; i < list.size(); i++) {
            JSONArray ar = list.get(i);
            if (i == 0) {
                for (int j = currentData.pointId; j >= 0; j--) {
                    returnArray.put(ar.getJSONObject(j));
                }
            } else if (i == list.size() - 1) {
                if (diff_alts) {
                    for (int j = 0; j <= finishData.pointId; j++) {
                        returnArray.put(ar.getJSONObject(j));
                    }
                } else {
                    for (int j = ar.length() - 1; j >= finishData.pointId; j--) {
                        returnArray.put(ar.getJSONObject(j));
                    }
                }
            } else {
                for (int j = ar.length() - 1; j >= 0; j--) {
                    returnArray.put(ar.getJSONObject(j));
                }
            }
        }
    }

    private void pointDiffStageForward(String cur_type, String fin_type) throws JSONException {
        ArrayList<JSONArray> list = new ArrayList<>();
        boolean diff_alts = false;
        for (int i = currentData.stageId; i <= finishData.stageId; i++) {
            JSONObject obj = jfh.parseJSONObj("json/stage" + i + ".json");
            if (i == currentData.stageId) {
                list.add(obj.getJSONObject(cur_type).getJSONArray(currentData.partId + ""));
                if (currentData.partId != (obj.getInt("parts") - 1)) {
                    for (int j = currentData.partId + 1; j < obj.getInt("parts"); j++) {
                        list.add(obj.getJSONObject("main").getJSONArray(j + ""));
                    }
                }
            } else if (i == finishData.stageId) {
                if (obj.getInt("parts") > 1) {
                    if (finishData.partId != 0) {
                        for (int j = 0; j < finishData.partId; j++) {
                            if (j == 0 && currentData.alt && (i == (currentData.stageId + 1)) && stageHasAltPart(obj, j)) {
                                list.add(obj.getJSONObject("alt").getJSONArray(j + ""));
                            } else {
                                list.add(obj.getJSONObject("main").getJSONArray(j + ""));
                            }
                        }
                        list.add(obj.getJSONObject(fin_type).getJSONArray(finishData.partId + ""));
                    } else {
                        if ((!cur_type.equals(fin_type) && (i == (currentData.stageId + 1)))) {
                            list.add(obj.getJSONObject(cur_type).getJSONArray(finishData.partId + ""));
                            list.add(obj.getJSONObject(fin_type).getJSONArray(finishData.partId + ""));
                            diff_alts = true;
                        } else {
                            list.add(obj.getJSONObject(fin_type).getJSONArray(finishData.partId + ""));
                        }
                    }
                } else {
                    list.add(obj.getJSONObject(fin_type).getJSONArray(finishData.partId + ""));
                }
            } else {
                for (int j = 0; j < obj.getInt("parts"); j++) {
                    if (j == 0 && currentData.alt && (i == (currentData.stageId + 1)) && stageHasAltPart(obj, j)) {
                        list.add(obj.getJSONObject("alt").getJSONArray(j + ""));
                    } else {
                        list.add(obj.getJSONObject("main").getJSONArray(j + ""));
                    }
                }
            }
        }
        for (int i = 0; i < list.size(); i++) {
            JSONArray ar = list.get(i);
            if (i == 0) {
                for (int j = currentData.pointId; j < ar.length(); j++) {
                    returnArray.put(ar.getJSONObject(j));
                }
            } else if (i == list.size() - 1) {
                if (diff_alts) {
                    for (int j = ar.length() - 1; j >= finishData.pointId; j--) {
                        returnArray.put(ar.getJSONObject(j));
                    }
                } else {
                    for (int j = 0; j <= finishData.pointId; j++) {
                        returnArray.put(ar.getJSONObject(j));
                    }
                }
            } else {
                for (int j = 0; j < ar.length(); j++) {
                    returnArray.put(ar.getJSONObject(j));
                }
            }
        }
    }

    // *** OSM Functions ***

    private Marker drawAlbMarker(Double lat, Double lng, String title, String desc, String snippet, boolean highlighted) {
        GeoPoint mGeoP = new GeoPoint(lat, lng);
        // build a new marker pin
        Marker mPin = new Marker(mapView);
        mPin.setPosition(mGeoP);
        mPin.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        if (highlighted) {
            mPin.setIcon(context.getResources().getDrawable(R.drawable.ic_albergue_marker_red));
        } else {
            mPin.setIcon(context.getResources().getDrawable(R.drawable.ic_albergue_marker_green));
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
        mPin.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mPin.setIcon(context.getResources().getDrawable(R.drawable.ic_locality_marker));
        mPin.setTitle(title);
//        mPin.setSubDescription(desc);
//        mPin.setSnippet(snippet);

        // add new marker pin to map
        return mPin;
    }

    public double drawDistanceRoute(Location current, Location finish) throws JSONException {
        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        JSONArray geoArr = getRouteArray(current, finish);
        Polyline routeOverlay = new Polyline(context);
        JSONObject geopoint;
        JSONObject prev_geopoint = new JSONObject();
        double distFromCurrToFin = 0;
        if (currentData != null) {
//            if (areaLimitSpain.contains(new GeoPoint(current.getLatitude(), current.getLongitude())))
            waypoints.add(new GeoPoint(current.getLatitude(), current.getLongitude()));
        }
        if (geoArr.length() > 0) {
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
        if (currentData != null) {
            waypoints.add(new GeoPoint(finish.getLatitude(), finish.getLongitude()));
        }
        routeOverlay.setColor(Color.argb(230, 244, 68, 68));
        routeOverlay.setWidth(6.0f);
        routeOverlay.setPoints(waypoints);
        List<Overlay> x = mapView.getOverlays();
        int i = 0;
        for (Overlay item : x) {
            if (item instanceof Polyline) {
                mapView.getOverlayManager().remove(i);
            }
            i++;
        }

        mapView.getOverlays().add(routeOverlay);

        return distFromCurrToFin;
    }

    public void drawAllRoute(int stage) throws JSONException {
        JSONObject fileObj, geo;
        JSONArray geoArr;
        ArrayList<GeoPoint> waypointsStart = new ArrayList<>();
        ArrayList<GeoPoint> waypointsHighlight = new ArrayList<>();
        ArrayList<GeoPoint> waypointsFinish = new ArrayList<>();
        ArrayList<GeoPoint> waypointsAlt = new ArrayList<>();

        GeoPoint newPoint;
        Polyline routeOverlayStart = new Polyline(context);
        Polyline routeOverlayHighlight = new Polyline(context);
        Polyline routeOverlayFinish = new Polyline(context);


        boolean finish = false;
        for (int i = 1; i < 32; i++) {
            fileObj = jfh.parseJSONObj("json/stage" + i + ".json");
            if (fileObj.getInt("parts") > 1) {
                for (int j = 0; j < fileObj.getInt("parts"); j++) {
                    JSONArray ar = fileObj.getJSONObject("main").getJSONArray(j + "");
                    for (int h = 0; h < ar.length(); h++) {
                        geo = ar.getJSONObject(h);
                        Double lat = geo.getDouble("lat");
                        Double lng = geo.getDouble("lng");
                        newPoint = new GeoPoint(lat, lng);
                        if (stage == i) {
                            waypointsHighlight.add(newPoint);
                            finish = true;
                        } else {
                            if (finish) {
                                waypointsFinish.add(newPoint);
                            } else {
                                waypointsStart.add(newPoint);
                            }
                        }
                    }
                    if (fileObj.getJSONObject("alt").has(j + "")) {
                        waypointsAlt = new ArrayList<>();
                        JSONArray ar_alt = fileObj.getJSONObject("alt").getJSONArray(j + "");
                        for (int h = 0; h < ar_alt.length(); h++) {
                            geo = ar_alt.getJSONObject(h);
                            Double lat = geo.getDouble("lat");
                            Double lng = geo.getDouble("lng");
                            newPoint = new GeoPoint(lat, lng);
                            waypointsAlt.add(newPoint);
                        }
                        Polyline routeOverlayAlt = new Polyline(context);
                        routeOverlayAlt.setWidth(3.0f);
                        routeOverlayAlt.setColor(Color.rgb(255, 255, 0));
                        routeOverlayAlt.setPoints(waypointsAlt);
                        mapView.getOverlays().add(routeOverlayAlt);
                    }
                }
            } else {
                JSONArray ar = fileObj.getJSONObject("main").getJSONArray("0");
                for (int h = 0; h < ar.length(); h++) {
                    geo = ar.getJSONObject(h);
                    Double lat = geo.getDouble("lat");
                    Double lng = geo.getDouble("lng");
                    newPoint = new GeoPoint(lat, lng);
                    if (stage == i) {
                        waypointsHighlight.add(newPoint);
                        finish = true;
                    } else {
                        if (finish) {
                            waypointsFinish.add(newPoint);
                        } else {
                            waypointsStart.add(newPoint);
                        }
                    }
                }
            }
        }
        routeOverlayStart.setColor(Color.rgb(0, 150, 136));
        routeOverlayStart.setWidth(5.0f);
        routeOverlayStart.setPoints(waypointsStart);
        routeOverlayFinish.setColor(Color.rgb(0, 150, 136));
        routeOverlayFinish.setWidth(5.0f);
        routeOverlayFinish.setPoints(waypointsFinish);
        routeOverlayHighlight.setColor(Color.rgb(244, 68, 68));
        routeOverlayHighlight.setWidth(5.0f);
        routeOverlayHighlight.setPoints(waypointsHighlight);
        mapView.getOverlays().add(routeOverlayStart);
        mapView.getOverlays().add(routeOverlayHighlight);
        mapView.getOverlays().add(routeOverlayFinish);
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

    // *** GMAP Functions

    public ArrayList<MarkerDataObject> drawAlbMarkersGMAP(Location finish) throws JSONException {
        ArrayList<MarkerDataObject> markers = new ArrayList<>();
        boolean highlighted = false;
        JSONObject vh = null;
        JSONArray albJArr = dbController.getAlbergues(0);
        for (int i = 0; i < albJArr.length(); i++) {
            JSONObject v = albJArr.getJSONObject(i);
            String title = "Albergue " + v.getString("title");
            String snippet = v.getString("tel");
            Double lat = v.getDouble("lat");
            Double lng = v.getDouble("lng");
            if (finish != null) {
                if (lat == finish.getLatitude() && lng == finish.getLongitude()) {
                    highlighted = true;
                    vh = v;
                } else {
                    markers.add(new MarkerDataObject(new LatLng(lat, lng), title, snippet, R.drawable.ic_albergue_marker_green));
                }
            } else {
                markers.add(new MarkerDataObject(new LatLng(lat, lng), title, snippet, R.drawable.ic_albergue_marker_green));

            }
        }
        if (highlighted && vh != null) {
            Double lat = vh.getDouble("lat");
            Double lng = vh.getDouble("lng");
            String title = vh.getString("title");
            String snippet = vh.getString("tel");
            markers.add(new MarkerDataObject(new LatLng(lat, lng), title, snippet, R.drawable.ic_albergue_marker_red));
        }

        return markers;
    }

    public ArrayList<MarkerDataObject> drawCityMarkersGMAP() throws JSONException {
        ArrayList<MarkerDataObject> markers = new ArrayList<>();
        JSONArray routeJArr = dbController.getLocalities();
        for (int i = 0; i < routeJArr.length(); i++) {
            JSONObject v = routeJArr.getJSONObject(i);
            if (v != null) {
                Double lat = v.getDouble("lat");
                Double lng = v.getDouble("lng");
                String title = v.getString("title");
                markers.add(new MarkerDataObject(new LatLng(lat, lng), title, null, R.drawable.ic_locality_marker));
            }
        }
        return markers;
    }

    public ArrayList<PolylineOptions> drawAllRouteGMAP(int stage) throws JSONException {
        PolylineOptions rectOptionsStart = new PolylineOptions();
        PolylineOptions rectOptionsFinish = new PolylineOptions();
        PolylineOptions rectOptionsHighlight = new PolylineOptions();
        PolylineOptions rectOptionsAlt = new PolylineOptions();
        ArrayList<PolylineOptions> returnList = new ArrayList<PolylineOptions>();
        boolean finish = false;

        //stage == 0 => all route w/o highlight
        JSONObject fileObj, geo;
        JSONArray geoArr;
        LatLng newPoint;
        for (int i = 1; i < 32; i++) {
            fileObj = jfh.parseJSONObj("json/stage" + i + ".json");
            if (fileObj.getInt("parts") > 1) {
                for (int j = 0; j < fileObj.getInt("parts"); j++) {
                    JSONArray ar = fileObj.getJSONObject("main").getJSONArray(j + "");
                    for (int h = 0; h < ar.length(); h++) {
                        geo = ar.getJSONObject(h);
                        Double lat = geo.getDouble("lat");
                        Double lng = geo.getDouble("lng");
                        newPoint = new LatLng(lat, lng);
                        if (i == stage) {
                            rectOptionsHighlight.add(newPoint);
                            finish = true;
                        } else {
                            if (finish) {
                                rectOptionsFinish.add(newPoint);
                            } else {
                                rectOptionsStart.add(newPoint);
                            }
                        }
                    }
                    if (fileObj.getJSONObject("alt").has(j + "")) {
                        rectOptionsAlt = new PolylineOptions();
                        JSONArray ar_alt = fileObj.getJSONObject("alt").getJSONArray(j + "");
                        for (int h = 0; h < ar_alt.length(); h++) {
                            geo = ar_alt.getJSONObject(h);
                            Double lat = geo.getDouble("lat");
                            Double lng = geo.getDouble("lng");
                            newPoint = new LatLng(lat, lng);
                            rectOptionsAlt.add(newPoint);
                        }
                        rectOptionsAlt.color(Color.argb(200, 255, 255, 0)).width(5).geodesic(true);
                        returnList.add(rectOptionsAlt);
                    }
                }
            } else {
                JSONArray ar = fileObj.getJSONObject("main").getJSONArray("0");
                for (int h = 0; h < ar.length(); h++) {
                    geo = ar.getJSONObject(h);
                    Double lat = geo.getDouble("lat");
                    Double lng = geo.getDouble("lng");
                    newPoint = new LatLng(lat, lng);
                    if (i == stage) {
                        rectOptionsHighlight.add(newPoint);
                        finish = true;
                    } else {
                        if (finish) {
                            rectOptionsFinish.add(newPoint);
                        } else {
                            rectOptionsStart.add(newPoint);
                        }
                    }
                }
            }
        }
        rectOptionsStart.color(Color.argb(200, 0, 150, 136)).width(6).geodesic(true);
        rectOptionsFinish.color(Color.argb(200, 0, 150, 136)).width(6).geodesic(true);
        rectOptionsHighlight.color(Color.argb(255, 244, 68, 68)).width(6).geodesic(true);
        returnList.add(rectOptionsStart);
        returnList.add(rectOptionsHighlight);
        returnList.add(rectOptionsFinish);
        return returnList;
    }

    public GMapReturnObject drawDistanceRouteGMAP(Location current, Location finish) throws JSONException {
        PolylineOptions rectOptions = new PolylineOptions();

        JSONArray geoArr = getRouteArray(current, finish);
        JSONObject geopoint;
        JSONObject prev_geopoint = new JSONObject();
        double distFromCurrToFin = 0;
        if (currentData != null) {
//            if (areaLimitSpainGMap.contains(new LatLng(current.getLatitude(), current.getLongitude())))
            rectOptions.add(new LatLng(current.getLatitude(), current.getLongitude()));
        }
        if (geoArr.length() > 0) {
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
        if (currentData != null) {
            rectOptions.add(new LatLng(finish.getLatitude(), finish.getLongitude()));
        }
        rectOptions.color(Color.argb(230, 244, 68, 68)).width(6).geodesic(true);

        return new GMapReturnObject(distFromCurrToFin, rectOptions);
    }
}
