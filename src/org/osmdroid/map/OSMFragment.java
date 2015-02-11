// Created by plusminus on 00:23:14 - 03.10.2008
package org.osmdroid.map;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.R;
import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.constants.AppConstants;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.utils.JsonFilesHandler;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

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
    private MinimapOverlay mMinimapOverlay;
    private ScaleBarOverlay mScaleBarOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;
    private ResourceProxy mResourceProxy;
    public FolderOverlay albMarkersOverlay;
    public FolderOverlay cityMarkersOverlay;
    public Polyline routeOverlay;


    private ImageButton mZoomIn;
    private ImageButton mZoomOut;


    // GMS
    public Location mCurrentLocation;
    public String mLastUpdateTime;
    public Boolean mFollowUserLocation;
    private Intent mServiceIntent;

    public BroadcastReceiver br;
    SharedPreferences mSettings;
    private JsonFilesHandler jfh;

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

        // -->

        br = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                mLastUpdateTime = intent.getStringExtra(KEY_LAST_UPD_TIME);
                mCurrentLocation = intent.getParcelableExtra(KEY_CURRENT_LOCATION);
                if(mCurrentLocation != null) {
                    updateUI();

                }

                if (mFollowUserLocation != false) {
                    followUser();
                }
            }
        };
        IntentFilter intFilt = new IntentFilter(KEY_SERVICE_ACTION);
        getActivity().registerReceiver(br, intFilt);


        return inflater.inflate(R.layout.fragment_osm_map, container, false);
//        mMapView = new MapView(inflater.getContext(), 256);
//        setHardwareAccelerationOff();
//        return mMapView;
    }

    private void setUpMapView() {
        mMapView.setTileSource(TileSourceFactory.MAPQUESTOSM);
        mMapView.setUseDataConnection(false); //optional, but a good way to prevent loading from the network and test your zip loading.
        mMapView.getController().setZoom(9);
        mMapView.setMinZoomLevel(9);
        mMapView.setMaxZoomLevel(18);
        GeoPoint startPoint = new GeoPoint(42.4167413, -2.7294623);
        mMapView.getController().setCenter(startPoint);
        mMapView.setScrollableAreaLimit(areaLimitSpain);

        mMapView.setMapListener(new MapListener() {

            @Override
            public boolean onScroll(ScrollEvent scrollEvent) {
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent zoomEvent) {

                // Control overlays visibility
                if (mMapView.getZoomLevel() >= 15) {
                    albMarkersOverlay.setEnabled(true);
                    mMapView.invalidate();
                } else if (mMapView.getZoomLevel() >= 11) {
                    cityMarkersOverlay.setEnabled(true);
                    mMapView.invalidate();
                } else if (mMapView.getZoomLevel() < 15) {
                    albMarkersOverlay.setEnabled(false);
                    mMapView.invalidate();
                } else if (mMapView.getZoomLevel() < 11) {
                    cityMarkersOverlay.setEnabled(false);
                    mMapView.invalidate();
                }

                return false;
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

        MapTouchOverlay mapTouchOverlay = new MapTouchOverlay(getActivity());
//        mMapView.getOverlays().add(mapTouchOverlay);
//        if (mSettings.getBoolean("pref_key_stop_follow", true)) {
//            mapTouchOverlay.setEnabled(true);
//        }
//        else {
//            mapTouchOverlay.setEnabled(false);
//        }

        try {
            drawMarkers();
            drawRoute(0);
            mMapView.invalidate();
        } catch (JSONException e) {
            e.printStackTrace();

        }
        albMarkersOverlay.setEnabled(false);
        cityMarkersOverlay.setEnabled(false);
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
        mMapView = (MapView) getActivity().findViewById(R.id.osm_mapview);
        setUpMapView();
        mZoomIn = (ImageButton)getActivity().findViewById(R.id.zoomInBtn);
        mZoomOut = (ImageButton)getActivity().findViewById(R.id.zoomOutBtn);
        jfh = new JsonFilesHandler(getActivity());
        final int[] zoomLevels = {9, 10, 11, 13, 15, 18};
        mZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = mMapView.getZoomLevel();
                for (int i = 0; i < zoomLevels.length; i++) {
                    if (zoomLevels[i] == mMapView.getZoomLevel()) {
                        id = i;
                    }
                }
                if ((id + 1 >= 0) && (id + 1 < zoomLevels.length)) {
//                    mMapView.mI
                            mMapView.getController().setZoom(zoomLevels[id + 1]);
                    Toast.makeText(getActivity(), "Zoomed in to: " + zoomLevels[id + 1], Toast.LENGTH_SHORT).show();
                }
            }
        });
        mZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = mMapView.getZoomLevel();
                for (int i = 0; i < zoomLevels.length; i++) {
                    if (zoomLevels[i] == mMapView.getZoomLevel()) {
                        id = i;
                    }
                }
                if ((id-1 >= 0) && (id-1 < zoomLevels.length)) {
                    mMapView.getController().setZoom(zoomLevels[id-1]);
                  Toast.makeText(getActivity(), "Zoomed out to: " + zoomLevels[id - 1], Toast.LENGTH_SHORT).show();
                }
            }
        });

        setMapViewPreferences();
        setHasOptionsMenu(true);
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
//        edit.putString(PREFS_TILE_SOURCE, mMapView.getTileProvider().getTileSource().name());
        edit.putInt(PREFS_SCROLL_X, mMapView.getScrollX());
        edit.putInt(PREFS_SCROLL_Y, mMapView.getScrollY());
        edit.putInt(PREFS_ZOOM_LEVEL, mMapView.getZoomLevel());
        edit.putBoolean(PREFS_SHOW_LOCATION, mLocationOverlay.isMyLocationEnabled());
        edit.putBoolean(PREFS_SHOW_COMPASS, mCompassOverlay.isCompassEnabled());
        if (mCurrentLocation != null) {
            edit.putFloat("lat", (float)mCurrentLocation.getLatitude());
            edit.putFloat("lng", (float)mCurrentLocation.getLongitude());
        }
        edit.commit();

        this.mLocationOverlay.disableMyLocation();
        this.mCompassOverlay.disableCompass();
//        prefs.unregisterOnSharedPreferenceChangeListener(this);
        getActivity().stopService(mServiceIntent);
    }

    @Override
    public void onResume() {
        super.onResume();

        //set the listener to listen for changes in the preferences
//        prefs.registerOnSharedPreferenceChangeListener(this);
//        final String tileSourceName = mPrefs.getString(PREFS_TILE_SOURCE,
//                TileSourceFactory.DEFAULT_TILE_SOURCE.name());
//        try {
//            final ITileSource tileSource = TileSourceFactory.getTileSource(tileSourceName);
//            mMapView.setTileSource(tileSource);
//        } catch (final IllegalArgumentException e) {
//            mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
//        }
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

        ((MapActivity)getActivity()).geoOutTextViewLon.setText(lon);
        ((MapActivity)getActivity()).geoOutTextViewLat.setText(lat);
        ((MapActivity)getActivity()).geoOutTextViewTime.setText(tim);
    }

    private void followUser() {
        GeoPoint point = new GeoPoint(mCurrentLocation);
        mMapView.getController().animateTo(point);
        if (mMapView.getZoomLevel() < TRACK_ZOOM_LEVEL) {
            mMapView.getController().setZoom(TRACK_ZOOM_LEVEL);
        }
    }

    public Marker drawAlbMarker(MapView mapView, Double lat, Double lng, String title, String desc) {
        GeoPoint mGeoP = new GeoPoint(lat, lng);
        // build a new marker pin
        Marker mPin = new Marker(mapView);
        mPin.setPosition(mGeoP);
        mPin.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mPin.setIcon(getResources().getDrawable(R.drawable.ic_alb_marker));
        mPin.setTitle(title);
        mPin.setSubDescription(desc);
        mPin.setSnippet("Snippet text");

        // add new marker pin to map
        return mPin;
    }

    public Marker drawCityMarker(MapView mapView, Double lat, Double lng, String title, String snippet) {

        GeoPoint mGeoP = new GeoPoint(lat, lng);

        // build a new marker pin
        Marker mPin = new Marker(mapView);
        mPin.setPosition(mGeoP);
        mPin.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mPin.setIcon(getResources().getDrawable(R.drawable.ic_city_marker));
        mPin.setTitle(title);
//        mPin.setSubDescription(desc);
        mPin.setSnippet(snippet);

        // add new marker pin to map
        return mPin;
    }

    public void drawRoute(Integer id) throws JSONException {
        JSONObject fileObj, geo;
        JSONArray geoArr;
        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        GeoPoint newPoint;
        routeOverlay = new Polyline(getActivity());
        // Drawing route, each stage on it's own overlay
        if (id == 0) {
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
            fileObj = jfh.parseJSONObj("json/stage" + id + ".json");
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
        mMapView.getOverlays().add(routeOverlay);
    }

    public void drawMarkers() throws JSONException {

        // Parent overlay for albergues markers
        albMarkersOverlay = new FolderOverlay(getActivity());

        // Parent overlay for city markers
        cityMarkersOverlay = new FolderOverlay(getActivity());

        //Get the JSON object from the data
        String alb_path = "json/albergues.json";
        String route_path = "json/route.json";
        JSONArray albJArr = jfh.parseJSONArr(alb_path);
        JSONArray routeJArr = jfh.parseJSONArr(route_path);

//        Log.v("TEST", parent.toString());
        for (int i = 0; i < albJArr.length(); i++) {
            JSONObject v = albJArr.getJSONObject(i);
            String alb_geo = v.getString("alb_geo");
            if (alb_geo != null && alb_geo.length() > 1) {
                String title = v.getString("albergue");
                String desc = v.getString("locality") + " " + v.getString("address");

                String[] location = new String[2];

                if (alb_geo.contains(", ") == true) {
                    location = alb_geo.split(", ");
                } else {
                    location = alb_geo.split(",");
                }

                if (location[0] == null || location[0] == "") {
                    Log.v("TEST", v.getString("id") + " " + location);
                } else {
                    Double lat = Double.parseDouble(location[0]);
                    Double lng = Double.parseDouble(location[1]);
                    albMarkersOverlay.add(drawAlbMarker(mMapView, lat, lng, title, desc));
                }
            }
        }
        mMapView.getOverlays().add(albMarkersOverlay);

        // Parent overlay for route
        for (int i = 0; i < routeJArr.length(); i++) {
            JSONObject v = routeJArr.getJSONObject(i);
            if (v != null) {
                Double lat = v.getDouble("Latitude");
                Double lng = v.getDouble("Longitude");
                if (v.getString("Symbol").contains("Pin, Red")) {
                    String title = v.getString("Description");
                    String snippet = v.getString("Name");
                    cityMarkersOverlay.add(drawCityMarker(mMapView, lat, lng, title, snippet));
                }
            }
        }
        mMapView.getOverlays().add(cityMarkersOverlay);

    }

}
