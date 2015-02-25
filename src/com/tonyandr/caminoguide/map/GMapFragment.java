package com.tonyandr.caminoguide.map;


import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.tonyandr.caminoguide.R;
import com.tonyandr.caminoguide.constants.AppConstants;
import com.tonyandr.caminoguide.stages.StageActivity;
import com.tonyandr.caminoguide.utils.GMapReturnObject;
import com.tonyandr.caminoguide.utils.GeoMethods;
import com.tonyandr.caminoguide.utils.MarkerDataObject;

import org.json.JSONException;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class GMapFragment extends MapFragment implements AppConstants, OnMapReadyCallback {


    public GMapFragment() {
        // Required empty public constructor
    }

    private GoogleMap map;
    private BroadcastReceiver br;
    public Location mCurrentLocation;
    public Location mFinishLocation;
    public String mLastUpdateTime;
    public Boolean mFollowUserLocation;
    private Intent mServiceIntent;
    private GeoMethods geoMethods;
    private DrawingMethods drawingMethods;
    private Bundle bundle;
    private boolean mFirstCameraMove;
    SharedPreferences settings;

    // Tasks
    private CalculateDistanceTask calculateDistanceTask;
    private DrawAllRouteTask drawAllRouteTask;
    private DrawAlbMarkersTask drawAlbMarkersTask;
    private DrawCityMarkersTask drawCityMarkersTask;

    private SharedPreferences mPrefs;

    private ArrayList<Marker> markers = new ArrayList<>();

    public static GMapFragment newInstance() {
        GMapFragment fragment = new GMapFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFollowUserLocation = false;
        mLastUpdateTime = "";

        geoMethods = new GeoMethods(getActivity());
        drawingMethods = new DrawingMethods(getActivity());

//        mCurrentLocation = new Location("");
        mFirstCameraMove = false;
        mPrefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (mPrefs.contains(LOCATION_KEY_LAT)) {
            mCurrentLocation = new Location("");
            mCurrentLocation.setLatitude(mPrefs.getFloat(LOCATION_KEY_LAT, 43.1f));
            mCurrentLocation.setLongitude(mPrefs.getFloat(LOCATION_KEY_LNG, -2.9f));
        }
        if (mPrefs.contains(KEY_LAST_UPD_TIME)) {
            mLastUpdateTime = mPrefs.getString(KEY_LAST_UPD_TIME, "Not available");
        }
        updateValuesFromBundle(savedInstanceState);
        Log.d(DEBUGTAG, "onActivityCreated bundle updated");

        map = getMap();
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.setMyLocationEnabled(true);
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        MapsInitializer.initialize(this.getActivity());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(43.1, -2.9), MIN_ZOOM_LEVEL);
        map.animateCamera(cameraUpdate);

        ImageButton followMyLocation = (ImageButton) getActivity().findViewById(R.id.getMyLocBtn);
        ImageButton zoomInBtn = (ImageButton) getActivity().findViewById(R.id.zoomInBtn);
        ImageButton zoomOutBtn = (ImageButton) getActivity().findViewById(R.id.zoomOutBtn);
        zoomInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                map.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });
        zoomOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                map.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });


        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mLastUpdateTime = intent.getStringExtra(KEY_LAST_UPD_TIME);
                mCurrentLocation = intent.getParcelableExtra(KEY_CURRENT_LOCATION);
                Log.d(DEBUGTAG, "Location broadcast recieved");
                if (mCurrentLocation != null) {
                    updateUI();
                }
                if (mFollowUserLocation != false) {
                    followUser();
                }

            }
        };
        followMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mFollowUserLocation) {
                    mFollowUserLocation = true;
                    Toast toast = Toast.makeText(getActivity(), "GPS Tracking On", Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    mFollowUserLocation = false;
                    Toast toast = Toast.makeText(getActivity(), "GPS Tracking Off", Toast.LENGTH_SHORT);
                    toast.show();
                }
                if (mCurrentLocation != null) {
                    LatLng point = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                    CameraUpdate cameraUpdate;
                    if (map.getCameraPosition().zoom < TRACK_ZOOM_LEVEL) {
                        cameraUpdate = CameraUpdateFactory.newLatLngZoom(point, TRACK_ZOOM_LEVEL);
                    } else {
                        cameraUpdate = CameraUpdateFactory.newLatLng(point);
                    }
                    map.animateCamera(cameraUpdate);
                }
            }
        });

        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (cameraPosition.zoom < SHOW_MARKERS_ZOOM_LEVEL) {
                    for (Marker item : markers) {
                        item.setVisible(false);
                    }
                } else {
                    for (Marker item : markers) {
                        item.setVisible(true);
                    }
                }
            }
        });

        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                getActivity().findViewById(R.id.zoomInBtn).setVisibility(View.VISIBLE);
                getActivity().findViewById(R.id.zoomOutBtn).setVisibility(View.VISIBLE);
                getActivity().findViewById(R.id.getMyLocBtn).setVisibility(View.VISIBLE);
            }
        });

        IntentFilter intFilt = new IntentFilter(KEY_SERVICE_ACTION);
        getActivity().registerReceiver(br, intFilt);

    }

    private void updateUI() {
        String lon = "Long: " + String.valueOf(mCurrentLocation.getLongitude());
        String lat = " Lat: " + String.valueOf(mCurrentLocation.getLatitude());
        String tim = " Time: " + mLastUpdateTime;

        if (getActivity() instanceof MapActivity) {
            ((MapActivity) getActivity()).geoOutTextViewLon.setText(lon);
            ((MapActivity) getActivity()).geoOutTextViewLat.setText(lat);
            ((MapActivity) getActivity()).geoOutTextViewTime.setText(tim);
        }
        if (getActivity() instanceof StageActivity) {
            ((StageActivity) getActivity()).geoOutTextViewLon.setText(lon);
            ((StageActivity) getActivity()).geoOutTextViewLat.setText(lat);
            ((StageActivity) getActivity()).geoOutTextViewTime.setText(tim);
        }
        if (getActivity() instanceof MapActivity) {
            if (!mFirstCameraMove) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), FIRST_SHOW_ZOOM_LEVEL));
                mFirstCameraMove = true;
            }
        }

    }

    private void followUser() {
        LatLng point = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        CameraUpdate cameraUpdate;
        if (map.getCameraPosition().zoom < TRACK_ZOOM_LEVEL) {
            cameraUpdate = CameraUpdateFactory.newLatLngZoom(point, TRACK_ZOOM_LEVEL);
        } else {
            cameraUpdate = CameraUpdateFactory.newLatLng(point);
        }
        map.animateCamera(cameraUpdate);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() instanceof StageActivity) {
            if (settings.getBoolean("pref_key_info_geo", false)) {
                ((StageActivity) getActivity()).geoOutTextViewLon.setVisibility(View.VISIBLE);
                ((StageActivity) getActivity()).geoOutTextViewLat.setVisibility(View.VISIBLE);
                ((StageActivity) getActivity()).geoOutTextViewTime.setVisibility(View.VISIBLE);
            } else {
                ((StageActivity) getActivity()).geoOutTextViewLon.setVisibility(View.GONE);
                ((StageActivity) getActivity()).geoOutTextViewLat.setVisibility(View.GONE);
                ((StageActivity) getActivity()).geoOutTextViewTime.setVisibility(View.GONE);
            }
        }
        if (mFollowUserLocation) {
            Toast.makeText(getActivity(), "GPS Tracking On", Toast.LENGTH_SHORT).show();
        }

        mServiceIntent = new Intent(getActivity(), GeoService.class);
        getActivity().startService(mServiceIntent);

        try {
            drawLogic();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().findViewById(R.id.zoomInBtn).setVisibility(View.GONE);
        getActivity().findViewById(R.id.zoomOutBtn).setVisibility(View.GONE);
        getActivity().findViewById(R.id.getMyLocBtn).setVisibility(View.GONE);

        if (getActivity() instanceof StageActivity) {
            ((StageActivity) getActivity()).geoOutTextViewLon.setVisibility(View.GONE);
            ((StageActivity) getActivity()).geoOutTextViewLat.setVisibility(View.GONE);
            ((StageActivity) getActivity()).geoOutTextViewTime.setVisibility(View.GONE);
        }
        (getActivity().findViewById(R.id.progress_drawing_id)).setVisibility(View.GONE);

        final SharedPreferences.Editor edit = mPrefs.edit();
        if (mCurrentLocation != null) {
            edit.putFloat("lat", (float) mCurrentLocation.getLatitude());
            edit.putFloat("lng", (float) mCurrentLocation.getLongitude());
        }
        edit.commit();

        finishAllProcesses();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        finishAllProcesses();
    }

    private void finishAllProcesses() {
        if (br != null) {
            getActivity().unregisterReceiver(br);
            br = null;
        }
        if (mServiceIntent != null) {
            getActivity().stopService(mServiceIntent);
            mServiceIntent = null;
        }
        if (calculateDistanceTask != null) {
            calculateDistanceTask.cancel(true);
            Log.d(DEBUGTAG, "calculateTask cancelled");
        }
        if (drawAllRouteTask != null) {
            drawAllRouteTask.cancel(true);
            Log.d(DEBUGTAG, "drawStageTask cancelled");
        }
        if (drawAlbMarkersTask != null) {
            drawAlbMarkersTask.cancel(true);
            Log.d(DEBUGTAG, "drawAlbTask cancelled");
        }
        if (drawCityMarkersTask != null) {
            drawCityMarkersTask.cancel(true);
            Log.d(DEBUGTAG, "drawCityTask cancelled");
        }
    }
    // GMS Block
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            if (savedInstanceState.keySet().contains(KEY_FOLLOW_USER)) {
                mFollowUserLocation = savedInstanceState.getBoolean(KEY_FOLLOW_USER);
            }
            if (savedInstanceState.keySet().contains(KEY_FIRST_CAMERA_MOVE)) {
                mFirstCameraMove = savedInstanceState.getBoolean(KEY_FIRST_CAMERA_MOVE);
            }

        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        savedInstanceState.putBoolean(KEY_FOLLOW_USER, mFollowUserLocation);
        savedInstanceState.putBoolean(KEY_FIRST_CAMERA_MOVE, mFirstCameraMove);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(getActivity(), "Ready", Toast.LENGTH_SHORT).show();
    }


    private class CalculateDistanceTask extends AsyncTask<Void, Void, Void> {
        public CalculateDistanceTask() {

        }

        private GMapReturnObject gMapReturnObject;
        private double distanceToFinish;

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                gMapReturnObject = drawingMethods.drawDistanceRouteGMAP(mCurrentLocation, mFinishLocation);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            distanceToFinish = gMapReturnObject.length;
            Polyline line = map.addPolyline(gMapReturnObject.polylineOptions);
            drawAlbMarkersTask = new DrawAlbMarkersTask();
            drawAlbMarkersTask.execute();
            drawCityMarkersTask = new DrawCityMarkersTask();
            drawCityMarkersTask.execute();
            Toast.makeText(getActivity(), String.format("%.1f", distanceToFinish) + " km left", Toast.LENGTH_LONG).show();
        }
    }

    class DrawAllRouteTask extends AsyncTask<Void, Void, Void> {
        public DrawAllRouteTask() {

        }

        private PolylineOptions polylineOptions;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                polylineOptions = drawingMethods.drawAllRouteGMAP();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Polyline line = map.addPolyline(polylineOptions);

            drawAlbMarkersTask = new DrawAlbMarkersTask();
            drawAlbMarkersTask.execute();
            drawCityMarkersTask = new DrawCityMarkersTask();
            drawCityMarkersTask.execute();
        }
    }

    class DrawAlbMarkersTask extends AsyncTask<Void, Void, Void> {
        public DrawAlbMarkersTask() {

        }

        private ArrayList<MarkerDataObject> markersData;

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                markersData = drawingMethods.drawAlbMarkersGMAP(mFinishLocation);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            (getActivity().findViewById(R.id.progress_drawing_id)).setVisibility(View.GONE);

            for (MarkerDataObject item : markersData) {

                markers.add(map.addMarker(getMarkerOptions(item)));
            }
            if (map.getCameraPosition().zoom < SHOW_MARKERS_ZOOM_LEVEL) {
                for (Marker item : markers) {
                    item.setVisible(false);
                }
            } else {
                for (Marker item : markers) {
                    item.setVisible(true);
                }
            }
        }
    }

    private MarkerOptions getMarkerOptions(MarkerDataObject object) {
        return new MarkerOptions()
                .position(object.pointLocation)
                .title(object.title)
                .snippet(object.snippet)
                .icon(BitmapDescriptorFactory.fromResource(object.iconId));
    }

    class DrawCityMarkersTask extends AsyncTask<Void, Void, Void> {
        public DrawCityMarkersTask() {

        }

        private ArrayList<MarkerDataObject> markersData;

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                markersData = drawingMethods.drawCityMarkersGMAP();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            for (MarkerDataObject item : markersData) {
                map.addMarker(getMarkerOptions(item));
            }
            (getActivity().findViewById(R.id.progress_drawing_id)).setVisibility(View.GONE);
        }
    }

    private void drawLogic() throws JSONException {
        Log.d("drawlogic", "Calculating logic...");
        (getActivity().findViewById(R.id.progress_drawing_id)).setVisibility(View.VISIBLE);

        bundle = getArguments();
        if (bundle != null) {
            Log.d(DEBUGTAG, "We have bundle");
            mFinishLocation = new Location("");
            mFinishLocation.setLatitude(bundle.getDouble("lat"));
            mFinishLocation.setLongitude(bundle.getDouble("lng"));
            getActivity().setTitle(bundle.getString("title"));
            if (mCurrentLocation != null) {
                calculateDistanceTask = new CalculateDistanceTask();
                calculateDistanceTask.execute();
                Log.d(DEBUGTAG, "We have location");
                if (bundle.getBoolean("globe", false)) {
                    Log.d(DEBUGTAG, "From globe");
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), SHOW_STAGE_ZOOM_LEVEL));
                } else {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mFinishLocation.getLatitude(), mFinishLocation.getLongitude()), TRACK_ZOOM_LEVEL));
                }

            } else {
                drawAllRouteTask = new DrawAllRouteTask();
                drawAllRouteTask.execute();
                Log.d(DEBUGTAG, "No location, center to " + mFinishLocation.getLatitude() + ", " + mFinishLocation.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mFinishLocation.getLatitude(), mFinishLocation.getLongitude()), TRACK_ZOOM_LEVEL));
            }

        } else {
            drawAllRouteTask = new DrawAllRouteTask();
            drawAllRouteTask.execute();
            if (mCurrentLocation != null) {
                Log.d(DEBUGTAG, "We have location and no bundle recieved");
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), SHOW_STAGE_ZOOM_LEVEL));
                Log.d(DEBUGTAG, "Center to " +mCurrentLocation.getLatitude() + " " + mCurrentLocation.getLongitude());
            } else {
                Log.d(DEBUGTAG, "No location, no intent. Draw all.");
                LatLng startPoint = new LatLng(42.4167413, -2.7294623);
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(startPoint, MIN_ZOOM_LEVEL));
            }
        }
    }
}
