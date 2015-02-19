// Created by plusminus on 00:23:14 - 03.10.2008
package org.osmdroid.map;

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

import org.json.JSONException;
import org.osmdroid.R;
import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.constants.AppConstants;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.stages.StageActivity;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.utils.GeoMethods;
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
    private static final int TRACK_ZOOM_LEVEL = 13;
    private static final int DEFAULT_ZOOM_LEVEL = 13;
    private static final BoundingBoxE6 areaLimitSpain;

    //GMS
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    static final String KEY_CURRENT_LOCATION = "mCurrentLocation";
    static final String KEY_LAST_UPD_TIME = "mLastUpdateTime";
    static final String KEY_SERVICE_ACTION = "org.osmdroid.map.GeoService";
    static final String KEY_FOLLOW_USER = "mFollowUserLocation";


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
    public Polyline routeOverlay;
    private ImageButton mZoomIn;
    private ImageButton mZoomOut;
    private ResourceProxy mResourceProxy;
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
    private Intent mIntentFromStage;
    private Bundle bundle;

    static {
        areaLimitSpain = new BoundingBoxE6(43.78,
                -0.54, 41.0, -9.338);
//        sPaint = new Paint();
//        sPaint.setColor(Color.argb(50, 255, 0, 0));
    }

    public static OSMFragment newInstance() {
        OSMFragment fragment = new OSMFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // GMS
        mFollowUserLocation = false;
        mLastUpdateTime = "";
        updateValuesFromBundle(savedInstanceState);
        mServiceIntent = new Intent(getActivity(), GeoService.class);

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

        return  mMapView;
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


        setUpMapView();
        setMapViewPreferences();
        new OnWhichStageTask().execute();
        setHasOptionsMenu(true);
    }

    private void drawLogic(int current_stage) throws JSONException {
        Log.d("drawlogic", "Calculating logic...");
        // Where we are?
//        int current_stage = 0; // 0 = not found
            bundle = getArguments();
            if (bundle != null) {
                Log.d("drawlogic", "We have bundle");
                Location finish = new Location("");
                finish.setLatitude(bundle.getDouble("lat"));
                finish.setLongitude(bundle.getDouble("lng"));
                int show_stage = bundle.getInt("stage_id");
//                Toast.makeText(getActivity(), "Don't dorget about stage_id", Toast.LENGTH_SHORT).show();
                if (mCurrentLocation != null) {
                    Log.d("drawlogic", "We have location");
                    if (current_stage == show_stage) {
                        Log.d("drawlogic", "We are on show stage!");
                        new CalculateDistanceTask().execute(show_stage);
                        mMapView.getController().setCenter(new GeoPoint(finish.getLatitude(), finish.getLongitude()));
                        mMapView.getController().setZoom(16);
                        albMarkersOverlay.setEnabled(true);
                        mMapView.invalidate();
                    } else {
                        Log.d("drawlogic", "We are not on show stage!");
                        new DrawStageTask().execute(show_stage);
                        mMapView.getController().setCenter(new GeoPoint(finish.getLatitude(), finish.getLongitude())); // Center to albergue or start of stage if globus
                        mMapView.getController().setZoom(16);
                        albMarkersOverlay.setEnabled(true);
                        mMapView.invalidate();
                    }
                } else {
                    Log.d("drawlogic", "No location, draw show stage, center to stagestart or albergue");
                    new DrawStageTask().execute(show_stage);
                    mMapView.getController().setZoom(16);
                    mMapView.getController().setCenter(new GeoPoint(finish.getLatitude(), finish.getLongitude()));
                }

            } else {
                if (mCurrentLocation != null) {
                    Log.d("drawlogic", "We have location and no bundle recieved");
                    new DrawStageTask().execute(current_stage);
                    mMapView.getController().setCenter(new GeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                    mMapView.getController().setZoom(14);
                } else {
                    Log.d("drawlogic", "No location, no intent. Draw all.");
                    new DrawStageTask().execute(current_stage);
                    GeoPoint startPoint = new GeoPoint(42.4167413, -2.7294623);
                    mMapView.getController().setCenter(startPoint);
                    mMapView.getController().setZoom(10);
                }
            }
    }
    private class OnWhichStageTask extends AsyncTask<Void, Void, Void> {
        public OnWhichStageTask() {
        }
        private int current;
        @Override
        protected void onPreExecute() {
//            geoMethods = new GeoMethods();
            current = 0; // means not found
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                current = geoMethods.onWhichStage(mCurrentLocation);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            try {
                drawLogic(current);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    private class CalculateDistanceTask extends AsyncTask<Integer, Void, Void> {
        public CalculateDistanceTask() {

        }
        private double distanceToFinish;
        private int stage_id;

        @Override
        protected Void doInBackground(Integer... params) {
            try {
                distanceToFinish = drawingMethods.drawDistanceRoute(params[0], mCurrentLocation, mFinishLocation);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            stage_id = params[0];
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new DrawAlbMarkersTask().execute(stage_id);
            new DrawCityMarkersTask().execute();
            mMapView.invalidate();
            Toast.makeText(getActivity(), distanceToFinish+ " km left", Toast.LENGTH_LONG).show();
        }
    }
    class DrawStageTask extends AsyncTask<Integer, Void, Void> {
        public DrawStageTask() {

        }
        private int stage_id;

        @Override
        protected void onPreExecute() {
            drawingMethods = new DrawingMethods(mMapView, getActivity());

        }

        @Override
        protected Void doInBackground(Integer... params) {
            try {
                drawingMethods.drawStageRoute(params[0]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            stage_id = params[0];
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new DrawAlbMarkersTask().execute(stage_id);
            new DrawCityMarkersTask().execute();
            mMapView.invalidate();
        }
    }
    class DrawAlbMarkersTask extends AsyncTask<Integer, Void, Void> {
        public DrawAlbMarkersTask() {

        }
        private FolderOverlay folderOverlay;
        @Override
        protected void onPreExecute() {
            drawingMethods = new DrawingMethods(mMapView, getActivity());
            folderOverlay = new FolderOverlay(getActivity());
        }

        @Override
        protected Void doInBackground(Integer... params) {
            try {
                drawingMethods.drawAlbMarkers(params[0], folderOverlay);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            albMarkersOverlay = folderOverlay;
            mMapView.getOverlays().add(albMarkersOverlay);
            if (mMapView.getZoomLevel() <16) {
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
        private FolderOverlay folderOverlay;
        @Override
        protected void onPreExecute() {
            drawingMethods = new DrawingMethods(mMapView, getActivity());
            folderOverlay = new FolderOverlay(getActivity());
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                drawingMethods.drawCityMarkers(folderOverlay);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            cityMarkersOverlay = folderOverlay;
            mMapView.getOverlays().add(cityMarkersOverlay);
            mMapView.invalidate();
        }
    }

    private void setUpMapView() {

        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.setUseDataConnection(false); //optional, but a good way to prevent loading from the network and test your zip loading.
        mMapView.getController().setZoom(10);
        mMapView.setMinZoomLevel(10);
        mMapView.setMaxZoomLevel(18);
        mMapView.setScrollableAreaLimit(areaLimitSpain);
        final int[] zoomLevels = {10, 12, 14, 16, 18};
        mMapView.setMapListener(new MapListener() {

            @Override
            public boolean onScroll(ScrollEvent scrollEvent) {
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent zoomEvent) {
                // Control overlays visibility
                if (mMapView.getZoomLevel() >= 16) {
                    albMarkersOverlay.setEnabled(true);
                    mMapView.invalidate();
                }
                if (mMapView.getZoomLevel() >= 10) {
                    cityMarkersOverlay.setEnabled(true);
                    mMapView.invalidate();
                }
                if (mMapView.getZoomLevel() < 16) {
                    albMarkersOverlay.setEnabled(false);
                    mMapView.invalidate();
                }
                if (mMapView.getZoomLevel() < 10) {
                    cityMarkersOverlay.setEnabled(false);
                    mMapView.invalidate();
                }
                return false;
            }
        });
        mZoomIn.setOnClickListener(new View.OnClickListener() {
            public int getNextZoomId() {
                int curr = mMapView.getZoomLevel();
                int id = 0;
                for (int i = 0; i < zoomLevels.length; i++) {
                    if (zoomLevels[i] == curr) {
                        if (i != zoomLevels.length - 1) {
                            id = i + 1;
                        } else {
                            id = i;
                        }

                    } else {
                        if (i != zoomLevels.length - 1) {
                            if (curr > zoomLevels[i] && curr < zoomLevels[i + 1]) {
                                id = i + 1;
                            }
                        }
                    }
                }
                return id;
            }

            @Override
            public void onClick(View v) {
                int id = getNextZoomId();
                mMapView.getController().setZoom(zoomLevels[id]);
                Toast.makeText(getActivity(), "Zoomed in to: " + zoomLevels[id], Toast.LENGTH_SHORT).show();
            }

        });
        mZoomOut.setOnClickListener(new View.OnClickListener() {
            public int getPrevZoomId() {
                int curr = mMapView.getZoomLevel();
                int id = 0;
                for (int i = 0; i < zoomLevels.length; i++) {
                    if (zoomLevels[i] == curr) {
                        if (i != 0) {
                            id = i - 1;
                        } else {
                            id = i;
                        }

                    } else {
                        if (i != 0) {
                            if (curr > zoomLevels[i - 1] && curr < zoomLevels[i]) {
                                id = i - 1;
                            }
                        }
                    }
                }
                return id;
            }

            @Override
            public void onClick(View v) {
                mMapView.getController().setZoom(zoomLevels[getPrevZoomId()]);
                Toast.makeText(getActivity(), "Zoomed out to: " + zoomLevels[getPrevZoomId()], Toast.LENGTH_SHORT).show();

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
        mMapView.setMultiTouchControls(false);
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
        edit.putInt(PREFS_SCROLL_X, mMapView.getScrollX());
        edit.putInt(PREFS_SCROLL_Y, mMapView.getScrollY());
        edit.putInt(PREFS_ZOOM_LEVEL, mMapView.getZoomLevel());
        edit.putBoolean(PREFS_SHOW_LOCATION, mLocationOverlay.isMyLocationEnabled());
        edit.putBoolean(PREFS_SHOW_COMPASS, mCompassOverlay.isCompassEnabled());
        if (mCurrentLocation != null) {
            edit.putFloat("lat", (float) mCurrentLocation.getLatitude());
            edit.putFloat("lng", (float) mCurrentLocation.getLongitude());
        }
        edit.commit();

        this.mLocationOverlay.disableMyLocation();
        this.mCompassOverlay.disableCompass();
//        prefs.unregisterOnSharedPreferenceChangeListener(this);
        getActivity().stopService(mServiceIntent);
        if(getActivity() instanceof StageActivity) {
            getActivity().findViewById(R.id.zoomInBtn).setVisibility(View.GONE);
            getActivity().findViewById(R.id.zoomOutBtn).setVisibility(View.GONE);
            getActivity().findViewById(R.id.getMyLocBtn).setVisibility(View.GONE);
        }

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

        getActivity().startService(mServiceIntent);

//        mIntentFromStage = getActivity().getIntent();
        bundle = getArguments();
        if (bundle != null && getActivity() instanceof StageActivity) {
            getActivity().findViewById(R.id.zoomInBtn).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.zoomOutBtn).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.getMyLocBtn).setVisibility(View.VISIBLE);
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(br);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Put overlay items first
        mMapView.getOverlayManager().onCreateOptionsMenu(menu, MENU_LAST_ID, mMapView);

        // Put "About" menu item last
        menu.add(0, MENU_ABOUT, Menu.CATEGORY_SECONDARY, R.string.about).setIcon(
                android.R.drawable.ic_menu_info_details);

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

        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        savedInstanceState.putBoolean(KEY_FOLLOW_USER, mFollowUserLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onUseMapActivityBtnHandler() {
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

    private void updateUI() {
        String lon = "Long: " + String.valueOf(mCurrentLocation.getLongitude());
        String lat = " Lat: " + String.valueOf(mCurrentLocation.getLatitude());
        String tim = " Time: " + mLastUpdateTime;

        ((MapActivity) getActivity()).geoOutTextViewLon.setText(lon);
        ((MapActivity) getActivity()).geoOutTextViewLat.setText(lat);
        ((MapActivity) getActivity()).geoOutTextViewTime.setText(tim);
    }

    private void followUser() {
        GeoPoint point = new GeoPoint(mCurrentLocation);
        mMapView.getController().animateTo(point);
        if (mMapView.getZoomLevel() < TRACK_ZOOM_LEVEL) {
            mMapView.getController().setZoom(TRACK_ZOOM_LEVEL);
        }
    }

}
