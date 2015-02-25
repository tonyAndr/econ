// Created by plusminus on 00:23:14 - 03.10.2008
package com.tonyandr.caminoguide.map;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.tonyandr.caminoguide.R;
import com.tonyandr.caminoguide.constants.AppConstants;
import com.tonyandr.caminoguide.stages.StageActivity;
import com.tonyandr.caminoguide.utils.GeoMethods;

import org.json.JSONException;
import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

//import android.support.v4.app.Fragment;

public class OSMFragment extends Fragment implements AppConstants {
    // ===========================================================
    // Constants
    // ===========================================================

    // OSM
    private static final int DIALOG_ABOUT_ID = 1;
    private static final int MENU_SAMPLES = Menu.FIRST + 1;
    private static final int MENU_ABOUT = MENU_SAMPLES + 1;
    private static final int MENU_LAST_ID = MENU_ABOUT + 1; // Always set to last unused id

    //GMS

    // ===========================================================
    // Fields
    // ===========================================================

    // OSM
    private SharedPreferences mPrefs;
    public MapView mMapView;
    private MyLocationNewOverlay mLocationOverlay;
    private CompassOverlay mCompassOverlay;
    private ScaleBarOverlay mScaleBarOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;
    public FolderOverlay albMarkersOverlay;
    public FolderOverlay cityMarkersOverlay;
    private ImageButton mZoomIn;
    private ImageButton mZoomOut;
    private ResourceProxy mResourceProxy;
    private boolean mFirstCameraMove;
    SharedPreferences settings;
    // GMS
    public Location mCurrentLocation;
    public Location mFinishLocation;
    public String mLastUpdateTime;
    public Boolean mFollowUserLocation;
    private Intent mServiceIntent;

    public BroadcastReceiver br;
    SharedPreferences mSettings;
    private GeoMethods geoMethods;
    private DrawingMethods drawingMethods;
    private Bundle bundle;
    private Location finish;

    // Tasks
    private CalculateDistanceTask   calculateDistanceTask;
    private DrawAllRouteTask        drawAllRouteTask;
    private DrawAlbMarkersTask      drawAlbMarkersTask;
    private DrawCityMarkersTask     drawCityMarkersTask;


    public static OSMFragment newInstance() {
        OSMFragment fragment = new OSMFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // GMS
        mFollowUserLocation = false;
        mLastUpdateTime = "";
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
//        mServiceIntent = new Intent(getActivity(), GeoService.class);

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mLastUpdateTime = intent.getStringExtra(KEY_LAST_UPD_TIME);
                mCurrentLocation = intent.getParcelableExtra(KEY_CURRENT_LOCATION);
                if (mCurrentLocation != null) {
                    updateUI();
                }
                if (mFollowUserLocation != false) {
                    followUser();
                }
            }
        };
        IntentFilter intFilt = new IntentFilter(KEY_SERVICE_ACTION);
        getActivity().registerReceiver(br, intFilt);
        mResourceProxy = new ResourceProxyImpl(inflater.getContext().getApplicationContext());
        mMapView = new MapView(inflater.getContext(), 256, mResourceProxy);
//        return inflater.inflate(R.layout.fragment_osm_map, container, false);

        return mMapView;
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setHardwareAccelerationOff() {
        // Turn off hardware acceleration here, or in manifest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            mMapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        mMapView = (MapView) getActivity().findViewById(R.id.osm_mapview);
        mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mZoomIn = (ImageButton) getActivity().findViewById(R.id.zoomInBtn);
        mZoomOut = (ImageButton) getActivity().findViewById(R.id.zoomOutBtn);
        geoMethods = new GeoMethods(getActivity());
        drawingMethods = new DrawingMethods(mMapView, getActivity());

        albMarkersOverlay = new FolderOverlay(getActivity());
        cityMarkersOverlay = new FolderOverlay(getActivity());

        ImageButton followMyLocation = (ImageButton) getActivity().findViewById(R.id.getMyLocBtn);
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
                    GeoPoint point = new GeoPoint(mCurrentLocation);
                    mMapView.getController().animateTo(point);
                    if (mMapView.getZoomLevel() < TRACK_ZOOM_LEVEL) {
                        mMapView.getController().setZoom(TRACK_ZOOM_LEVEL);
                    }
                }
            }
        });

        setUpMapView();
        setMapViewPreferences();
        setHasOptionsMenu(true);
    }

    private void drawLogic() throws JSONException {
        Log.d(DEBUGTAG, "Calculating logic...");
        bundle = getArguments();
        if (bundle != null) {
            Log.d(DEBUGTAG, "We have bundle");
            mFinishLocation = new Location("");
            mFinishLocation.setLatitude(bundle.getDouble("lat"));
            mFinishLocation.setLongitude(bundle.getDouble("lng"));

            if (mCurrentLocation != null) {
                calculateDistanceTask = new CalculateDistanceTask();
                calculateDistanceTask.execute();
                Log.d(DEBUGTAG, "We have location");
                if (areaLimitSpain.contains(new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))) {
                    Log.d(DEBUGTAG, "We are on route");
                    if (bundle.getBoolean("globe", false)) {
                        Log.d(DEBUGTAG, "From globe");
                        mMapView.getController().setCenter(new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                        mMapView.getController().setZoom(SHOW_STAGE_ZOOM_LEVEL);
                    } else {
                        mMapView.getController().setCenter(new GeoPoint(mFinishLocation.getLatitude(), mFinishLocation.getLongitude()));
                        mMapView.getController().setZoom(TRACK_ZOOM_LEVEL);
                    }
                } else {
                    Log.d(DEBUGTAG, "We are not on route");
                    if (bundle.getBoolean("globe", false)) {
                        Log.d(DEBUGTAG, "From globe");
                        mMapView.getController().setCenter(new GeoPoint(mFinishLocation.getLatitude(), mFinishLocation.getLongitude()));
                        mMapView.getController().setZoom(SHOW_STAGE_ZOOM_LEVEL);
                    } else {
                        mMapView.getController().setCenter(new GeoPoint(mFinishLocation.getLatitude(), mFinishLocation.getLongitude()));
                        mMapView.getController().setZoom(TRACK_ZOOM_LEVEL);
                    }
                }
            } else {
                drawAllRouteTask = new DrawAllRouteTask();
                drawAllRouteTask.execute();
                Log.d(DEBUGTAG, "No location.");
                mMapView.getController().setCenter(new GeoPoint(mFinishLocation.getLatitude(), mFinishLocation.getLongitude()));
                mMapView.getController().setZoom(TRACK_ZOOM_LEVEL);
            }

        } else {
            drawAllRouteTask = new DrawAllRouteTask();
            drawAllRouteTask.execute();
            if (mCurrentLocation != null) {
                Log.d(DEBUGTAG, "We have location and no bundle recieved");
                if (areaLimitSpain.contains(new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))) {
                    Log.d(DEBUGTAG, "We are on route");
                    mMapView.getController().setCenter(new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                    mMapView.getController().setZoom(SHOW_STAGE_ZOOM_LEVEL);
                } else {
                    Log.d(DEBUGTAG, "We are not on route");
                    GeoPoint startPoint = new GeoPoint(42.4167413, -2.7294623);
                    mMapView.getController().setCenter(startPoint);
                    mMapView.getController().setZoom(SHOW_STAGE_ZOOM_LEVEL);
                }
            } else {
                Log.d(DEBUGTAG, "No location, no intent. Draw all.");
                GeoPoint startPoint = new GeoPoint(42.4167413, -2.7294623);
                mMapView.getController().setCenter(startPoint);
                mMapView.getController().setZoom(SHOW_STAGE_ZOOM_LEVEL);
            }
        }
    }

    private class CalculateDistanceTask extends AsyncTask<Void, Void, Void> {
        public CalculateDistanceTask() {        }
        private double distanceToFinish;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                distanceToFinish = drawingMethods.drawDistanceRoute(mCurrentLocation, mFinishLocation);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            drawAlbMarkersTask = new DrawAlbMarkersTask();
            drawAlbMarkersTask.execute();
            drawCityMarkersTask = new DrawCityMarkersTask();
            drawCityMarkersTask.execute();
            mMapView.invalidate();
            Toast.makeText(getActivity(), String.format("%.1f", distanceToFinish) + " km left", Toast.LENGTH_LONG).show();
        }
    }

    class DrawAllRouteTask extends AsyncTask<Void, Void, Void> {
        public DrawAllRouteTask() {

        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                drawingMethods.drawAllRoute();
            } catch (JSONException e) {
                e.printStackTrace();
            }
//            stage_id = params[0];
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            drawAlbMarkersTask = new DrawAlbMarkersTask();
            drawAlbMarkersTask.execute();
            drawCityMarkersTask = new DrawCityMarkersTask();
            drawCityMarkersTask.execute();
            mMapView.invalidate();
        }
    }

    class DrawAlbMarkersTask extends AsyncTask<Void, Void, Void> {
        public DrawAlbMarkersTask() {

        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                drawingMethods.drawAlbMarkers(albMarkersOverlay, mFinishLocation);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mMapView.getOverlays().add(albMarkersOverlay);
            if (mMapView.getZoomLevel() < 16) {
                albMarkersOverlay.setEnabled(false);
            } else {
                albMarkersOverlay.setEnabled(true);
            }

            mMapView.invalidate();
        }
    }

    class DrawCityMarkersTask extends AsyncTask<Void, Void, Void> {
        public DrawCityMarkersTask() {

        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                drawingMethods.drawCityMarkers(cityMarkersOverlay);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mMapView.getOverlays().add(cityMarkersOverlay);
            mMapView.invalidate();
        }
    }

    private void setUpMapView() {

        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.setUseDataConnection(false); //optional, but a good way to prevent loading from the network and test your zip loading.
        mMapView.getController().setZoom(MIN_ZOOM_LEVEL);
        mMapView.setMinZoomLevel(MIN_ZOOM_LEVEL);
        mMapView.setMaxZoomLevel(MAX_ZOOM_LEVEL);
        mMapView.setScrollableAreaLimit(areaLimitSpain);
        mMapView.setMapListener(new MapListener() {

            @Override
            public boolean onScroll(ScrollEvent scrollEvent) {
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent zoomEvent) {
                // Control overlays visibility
                if (mMapView.getZoomLevel() >= SHOW_MARKERS_ZOOM_LEVEL) {
                    albMarkersOverlay.setEnabled(true);
                    mMapView.invalidate();
                } else {
                    albMarkersOverlay.setEnabled(false);
                    mMapView.invalidate();
                }
                return true;
            }
        });
        mZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapView.getController().zoomIn();
            }
        });
        mZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapView.getController().zoomOut();
            }
        });
    }


    private void setMapViewPreferences() {
        final Context context = this.getActivity();

        final DisplayMetrics dm = context.getResources().getDisplayMetrics();

        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        this.mCompassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context),
                mMapView);
        this.mLocationOverlay = new MyLocationNewOverlay(context, new GpsMyLocationProvider(context),
                mMapView);

        mScaleBarOverlay = new ScaleBarOverlay(context);
        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);

        mRotationGestureOverlay = new RotationGestureOverlay(context, mMapView);
        mRotationGestureOverlay.setEnabled(false);

        mMapView.setBuiltInZoomControls(false);
        mMapView.setMultiTouchControls(true);
        mMapView.getOverlays().add(this.mLocationOverlay);
        mMapView.getOverlays().add(this.mCompassOverlay);
        mMapView.getOverlays().add(this.mScaleBarOverlay);
        mMapView.getOverlays().add(this.mRotationGestureOverlay);

//        mMapView.getController().setZoom(mPrefs.getInt(PREFS_ZOOM_LEVEL, 8));
        mMapView.scrollTo(mPrefs.getInt(PREFS_SCROLL_X, 0), mPrefs.getInt(PREFS_SCROLL_Y, 0));

        mLocationOverlay.enableMyLocation();
        mCompassOverlay.enableCompass();

//        albMarkersOverlay.setEnabled(false);

        MapTouchOverlay mapTouchOverlay = new MapTouchOverlay(getActivity());
        mMapView.getOverlays().add(mapTouchOverlay);
        if (mSettings.getBoolean(PREFS_STOP_FOLLOW_GESTURES, true)) {
            mapTouchOverlay.setEnabled(true);
        } else {
            mapTouchOverlay.setEnabled(false);
        }

    }

    public class MapTouchOverlay extends org.osmdroid.views.overlay.Overlay {

        public MapTouchOverlay(Context ctx) {
            super(ctx);
        }

        @Override
        protected void draw(Canvas canvas, MapView mapView, boolean b) {

        }

        @Override
        public boolean onTouchEvent(MotionEvent e, MapView mapView) {
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                if (mFollowUserLocation) {
                    mFollowUserLocation = false;
                    Toast toast = Toast.makeText(getActivity(), "GPS Tracking Off", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
            if (e.getAction() == MotionEvent.ACTION_SCROLL) {
                if (mFollowUserLocation) {
                    mFollowUserLocation = false;
                    Toast toast = Toast.makeText(getActivity(), "GPS Tracking Off", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
            return false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        final SharedPreferences.Editor edit = mPrefs.edit();
//        edit.putInt(PREFS_SCROLL_X, mMapView.getScrollX());
//        edit.putInt(PREFS_SCROLL_Y, mMapView.getScrollY());
//        edit.putInt(PREFS_ZOOM_LEVEL, mMapView.getZoomLevel());
        edit.putBoolean(PREFS_SHOW_LOCATION, mLocationOverlay.isMyLocationEnabled());
        if (mCurrentLocation != null) {
            edit.putFloat("lat", (float) mCurrentLocation.getLatitude());
            edit.putFloat("lng", (float) mCurrentLocation.getLongitude());
        }
        edit.commit();

        this.mLocationOverlay.disableMyLocation();
        this.mCompassOverlay.disableCompass();

        if (getActivity() instanceof StageActivity) {
            getActivity().findViewById(R.id.zoomInBtn).setVisibility(View.GONE);
            getActivity().findViewById(R.id.zoomOutBtn).setVisibility(View.GONE);
            getActivity().findViewById(R.id.getMyLocBtn).setVisibility(View.GONE);
            ((StageActivity) getActivity()).geoOutTextViewLon.setVisibility(View.GONE);
            ((StageActivity) getActivity()).geoOutTextViewLat.setVisibility(View.GONE);
            ((StageActivity) getActivity()).geoOutTextViewTime.setVisibility(View.GONE);
        }
        (getActivity().findViewById(R.id.progress_drawing_id)).setVisibility(View.GONE);

        finishAllProcesses();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPrefs.getBoolean(PREFS_SHOW_LOCATION, false)) {
            this.mLocationOverlay.enableMyLocation();
        }
        if (mPrefs.getBoolean(PREFS_SHOW_COMPASS, false)) {
            this.mCompassOverlay.enableCompass();
        }
        if (mFollowUserLocation) {
            Toast.makeText(getActivity(), "GPS Tracking On", Toast.LENGTH_SHORT).show();
        }

        mServiceIntent = new Intent(getActivity(), GeoService.class);
        getActivity().startService(mServiceIntent);

//        mIntentFromStage = getActivity().getIntent();
        bundle = getArguments();
        if ((bundle != null && getActivity() instanceof StageActivity) || getActivity() instanceof MapActivity) {
            getActivity().findViewById(R.id.zoomInBtn).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.zoomOutBtn).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.getMyLocBtn).setVisibility(View.VISIBLE);
        }

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

        try {
            drawLogic();
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        finishAllProcesses();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Put overlay items first
        mMapView.getOverlayManager().onCreateOptionsMenu(menu, MENU_LAST_ID, mMapView);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu pMenu) {
        mMapView.getOverlayManager().onPrepareOptionsMenu(pMenu, MENU_LAST_ID, mMapView);
        super.onPrepareOptionsMenu(pMenu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mMapView.getOverlayManager().onOptionsItemSelected(item, MENU_LAST_ID, mMapView))
            return true;

        switch (item.getItemId()) {
            case MENU_ABOUT:
                getActivity().showDialog(DIALOG_ABOUT_ID);
                return true;
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
                mMapView.getController().setCenter(new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                mMapView.getController().setZoom(FIRST_SHOW_ZOOM_LEVEL);
                mFirstCameraMove = true;
            }
        }
    }

    private void followUser() {
        GeoPoint point = new GeoPoint(mCurrentLocation);
        mMapView.getController().animateTo(point);
        if (mMapView.getZoomLevel() < TRACK_ZOOM_LEVEL) {
            mMapView.getController().setZoom(TRACK_ZOOM_LEVEL);
        }
    }

}
