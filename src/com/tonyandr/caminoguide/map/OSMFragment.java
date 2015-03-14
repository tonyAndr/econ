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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.tonyandr.caminoguide.R;
import com.tonyandr.caminoguide.constants.AppConstants;
import com.tonyandr.caminoguide.stages.StageActivity;
import com.tonyandr.caminoguide.utils.CustomNewLocationOverlay;
import com.tonyandr.caminoguide.utils.GeoMethods;

import org.json.JSONException;
import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.DateFormat;
import java.util.Date;

//import android.support.v4.app.Fragment;

public class OSMFragment extends Fragment implements AppConstants, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
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
    public Intent mServiceIntent;

    public BroadcastReceiver br;
    SharedPreferences mSettings;
    private GeoMethods geoMethods;
    private DrawingMethods drawingMethods;
    private Bundle bundle;
    private Location finish;
    private Boolean mDrawMarkers; // to not draw when recieved broadcast

    //GMS
    protected static final String TAG = "location-updates-sample";
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 8000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // GMS
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;

    static final String KEY_CURRENT_LOCATION = "mCurrentLocation";
    static final String KEY_LAST_UPD_TIME = "mLastUpdateTime";
    static final String KEY_SERVICE_ACTION = "GeoService";

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
        mDrawMarkers = true;
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
//        mServiceIntent = new Intent(getActivity(), GeoService.class); ,

        startGeoService();

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
            getActivity().setTitle(bundle.getString("title"));
            if (mCurrentLocation != null) {
                calculateDistanceTask = new CalculateDistanceTask();
                calculateDistanceTask.execute();
                Log.d(DEBUGTAG, "We have location");
                if (areaLimitSpain.contains(new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))) {
                    Log.d(DEBUGTAG, "We are on route");
                    if (bundle.getBoolean("globe", false) && bundle.getBoolean("near", false)) {
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
                drawAllRouteTask.execute(bundle.getInt("stage_id"));
                Log.d(DEBUGTAG, "No location.");
                mMapView.getController().setCenter(new GeoPoint(mFinishLocation.getLatitude(), mFinishLocation.getLongitude()));
                mMapView.getController().setZoom(TRACK_ZOOM_LEVEL);
            }

        } else {
            drawAllRouteTask = new DrawAllRouteTask();
            drawAllRouteTask.execute(0);
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
        protected void onPreExecute() {
            showLoadingBanner(getString(R.string.progress_drawing_route));
        }

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
            if (mDrawMarkers) {
                drawCityMarkersTask = new DrawCityMarkersTask();
                drawCityMarkersTask.execute();
            }

            mMapView.invalidate();
            Toast.makeText(getActivity(), String.format("%.1f", distanceToFinish) + " km left", Toast.LENGTH_LONG).show();
            hideLoadingBanner();
        }
    }

    class DrawAllRouteTask extends AsyncTask<Integer, Void, Void> {
        public DrawAllRouteTask() {

        }

        @Override
        protected void onPreExecute() {
            showLoadingBanner(getString(R.string.progress_drawing_route));
        }

        @Override
        protected Void doInBackground(Integer... params) {
            try {
                drawingMethods.drawAllRoute(params[0]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
//            stage_id = params[0];
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mDrawMarkers) {
                drawCityMarkersTask = new DrawCityMarkersTask();
                drawCityMarkersTask.execute();
                mMapView.getOverlayManager().remove(mLocationOverlay);
                mMapView.getOverlays().add(mLocationOverlay);
            }
            mMapView.invalidate();
            hideLoadingBanner();
        }
    }

    class DrawAlbMarkersTask extends AsyncTask<Void, Void, Void> {
        public DrawAlbMarkersTask() {

        }

        @Override
        protected void onPreExecute() {
            showLoadingBanner(getString(R.string.progress_drawing_markers));
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
            if (mMapView.getZoomLevel() < SHOW_MARKERS_ZOOM_LEVEL) {
                albMarkersOverlay.setEnabled(false);
            } else {
                albMarkersOverlay.setEnabled(true);
            }
            mDrawMarkers = false;
            mMapView.invalidate();
            hideLoadingBanner();
        }
    }

    class DrawCityMarkersTask extends AsyncTask<Void, Void, Void> {
        public DrawCityMarkersTask() {

        }

        @Override
        protected void onPreExecute() {
            showLoadingBanner(getString(R.string.progress_drawing_markers));
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
            drawAlbMarkersTask = new DrawAlbMarkersTask();
            drawAlbMarkersTask.execute();
            mMapView.getOverlays().add(cityMarkersOverlay);
            mMapView.invalidate();
            hideLoadingBanner();
        }
    }

    private void setUpMapView() {

        mMapView.setTileSource(new XYTileSource("MapQuest",
                ResourceProxy.string.mapquest_osm, 10, 18, 256, ".png", new String[]{
                "http://otile1.mqcdn.com/tiles/1.0.0/map/",
                "http://otile2.mqcdn.com/tiles/1.0.0/map/",
                "http://otile3.mqcdn.com/tiles/1.0.0/map/",
                "http://otile4.mqcdn.com/tiles/1.0.0/map/"}));
        mMapView.setUseDataConnection(false); //optional, but a good way to prevent loading from the network and test your zip loading.
//        final MapTileProviderBasic tileProvider = new MapTileProviderBasic(getActivity());
//        final ITileSource tileSource = new XYTileSource("Camino", ResourceProxy.string.mapnik, 10,18, 256, ".png", new String[] {""});
        //mapView.setTileSource((new XYTileSource("localMapnik", Resource, 0, 18, 256, ".png",
        //  "http://tile.openstreetmap.org/")));
//        tileProvider.setTileSource(tileSource);
//        final TilesOverlay tilesOverlay = new TilesOverlay(tileProvider, getActivity());
//        mMapView.getOverlays().add(tilesOverlay);


        // Interesting thing for dload tiles
//        CacheManager cacheManager = new CacheManager(mMapView);
//        cacheManager.cleanAreaAsync(getActivity(),areaLimitSpain,10,10);

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
        this.mLocationOverlay = new CustomNewLocationOverlay(context, new GpsMyLocationProvider(context),
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
        if (mGoogleApiClient.isConnected())
            stopLocationUpdates();
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

//        mServiceIntent = new Intent(getActivity(), GeoService.class);
//        getActivity().startService(mServiceIntent);

        if (mGoogleApiClient.isConnected())
            startLocationUpdates();

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

    private void startGeoService() {
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    private void stopGeoService() {
        Log.d(DEBUGTAG, "Stop Location service");
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
        if (!mGoogleApiClient.isConnected()) {
            Log.e(DEBUGTAG, "GApiClient switched off");
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
//            Intent intent = new Intent();
//            intent.setAction("GeoService");
//            intent.putExtra(KEY_CURRENT_LOCATION, mCurrentLocation);
//            sendBroadcast(intent);

//            saveToPrefs();
        }
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }


    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
//        saveToPrefs();

        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("location-string", mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude() + "," + mCurrentLocation.getTime());
        editor.commit();

        onLocationChangedFunction();
//        Intent intent = new Intent();
//        intent.setAction(KEY_SERVICE_ACTION);
//        intent.putExtra(KEY_CURRENT_LOCATION, mCurrentLocation);
//        intent.putExtra(KEY_LAST_UPD_TIME, mLastUpdateTime);
//        sendBroadcast(intent);
    }

    private void onLocationChangedFunction() {
        if (mCurrentLocation != null) {
            updateUI();

            if (bundle != null && calculateDistanceTask != null && settings.getBoolean("pref_key_realtime_calculation", false)) {
                if (calculateDistanceTask.getStatus() == AsyncTask.Status.FINISHED) {
                    calculateDistanceTask = new CalculateDistanceTask();
                    calculateDistanceTask.execute();
//                            Toast.makeText(getActivity(), "Re-calculation...", Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (mFollowUserLocation) {
            followUser();
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
            if (calculateDistanceTask.getStatus() != AsyncTask.Status.FINISHED) {
                calculateDistanceTask.cancel(true);
                Log.d(DEBUGTAG, "calculateTask cancelled");
            }
        }
        if (drawAllRouteTask != null) {
            if (drawAllRouteTask.getStatus() != AsyncTask.Status.FINISHED) {
                drawAllRouteTask.cancel(true);
                Log.d(DEBUGTAG, "drawStageTask cancelled");
            }
        }
        if ( drawAlbMarkersTask != null) {
            if (drawAlbMarkersTask.getStatus() != AsyncTask.Status.FINISHED) {
                drawAlbMarkersTask.cancel(true);
                Log.d(DEBUGTAG, "drawAlbTask cancelled");
            }
        }
        if (drawCityMarkersTask != null) {
            if (drawCityMarkersTask.getStatus() != AsyncTask.Status.FINISHED) {
                drawCityMarkersTask.cancel(true);
                Log.d(DEBUGTAG, "drawCityTask cancelled");
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        finishAllProcesses();
        stopGeoService();
    }
//
//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        // Put overlay items first
//        mMapView.getOverlayManager().onCreateOptionsMenu(menu, MENU_LAST_ID, mMapView);
//
//        super.onCreateOptionsMenu(menu, inflater);
//    }
//
//    @Override
//    public void onPrepareOptionsMenu(final Menu pMenu) {
//        mMapView.getOverlayManager().onPrepareOptionsMenu(pMenu, MENU_LAST_ID, mMapView);
//        super.onPrepareOptionsMenu(pMenu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (mMapView.getOverlayManager().onOptionsItemSelected(item, MENU_LAST_ID, mMapView))
//            return true;
//
//        switch (item.getItemId()) {
//            case MENU_ABOUT:
//                getActivity().showDialog(DIALOG_ABOUT_ID);
//                return true;
//            case android.R.id.home:
//                getActivity().onBackPressed();
//                return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

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

    private void showLoadingBanner(String resource) {
        ((TextView)(getActivity().findViewById(R.id.progress_drawing_id)).findViewById(R.id.progress_drawing_text)).setText(resource);
        (getActivity().findViewById(R.id.progress_drawing_id)).setVisibility(View.VISIBLE);
    }
    private void hideLoadingBanner(){
        (getActivity().findViewById(R.id.progress_drawing_id)).setVisibility(View.GONE);
    }

}
