// Created by plusminus on 00:23:14 - 03.10.2008
package org.osmdroid;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.constants.OpenStreetMapConstants;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.util.JsonFilesHandler;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MapFragment extends Fragment implements OpenStreetMapConstants,
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener
{
    // ===========================================================
    // Constants
    // ===========================================================

    // OSM
    private static final int DIALOG_ABOUT_ID = 1;
    private static final int MENU_SAMPLES = Menu.FIRST + 1;
    private static final int MENU_ABOUT = MENU_SAMPLES + 1;
    private static final int MENU_LAST_ID = MENU_ABOUT + 1; // Always set to last unused id
    private static final int TRACK_ZOOM_LEVEL = 14;
    private static final int DEFAULT_ZOOM_LEVEL = 13;
    private static final BoundingBoxE6 areaLimitSpain;
    private static final Integer[] mAvailableZoomLevels = {7,11,14,17};

    //GMS
    protected static final String TAG = "location-updates-sample";
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";


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
    private FolderOverlay albMarkersOverlay;
    private FolderOverlay cityMarkersOverlay;


    // GMS
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    public Location mCurrentLocation;
    protected Boolean mRequestingLocationUpdates;
    protected String mLastUpdateTime;

    static {
        areaLimitSpain = new BoundingBoxE6(44.496505,
                3.691406, 35.012002, -10.766602 );
//        sPaint = new Paint();
//        sPaint.setColor(Color.argb(50, 255, 0, 0));
    }

	public static MapFragment newInstance() {
		MapFragment fragment = new MapFragment();
		return fragment;
	}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // GMS
        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";
        updateValuesFromBundle(savedInstanceState);
        // -->

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mResourceProxy = new ResourceProxyImpl(inflater.getContext().getApplicationContext());
        mMapView = new MapView(inflater.getContext(), 256, mResourceProxy);
        // Call this method to turn off hardware acceleration at the View level.
        setHardwareAccelerationOff();
        mMapView.setTileSource(TileSourceFactory.MAPQUESTOSM);
//        mMapView.setTileSource(new XYTileSource("MAPQUESTOSM",
//                ResourceProxy.string.mapquest_osm, 0, 18, 256, ".jpg", new String[] {
//                "http://otile1.mqcdn.com/tiles/1.0.0/map/",
//                "http://otile2.mqcdn.com/tiles/1.0.0/map/",
//                "http://otile3.mqcdn.com/tiles/1.0.0/map/",
//                "http://otile4.mqcdn.com/tiles/1.0.0/map/"}));
        mMapView.setUseDataConnection(false); //optional, but a good way to prevent loading from the network and test your zip loading.
        IMapController mapController = mMapView.getController();
        mapController.setZoom(8);
        mMapView.setMinZoomLevel(7);
        mMapView.setMaxZoomLevel(17);
        GeoPoint startPoint = new GeoPoint(42.4167413, -2.7294623999999885);
        mapController.setCenter(startPoint);
        mMapView.setScrollableAreaLimit(areaLimitSpain);

        mMapView.setMapListener(new MapListener() {

            @Override
            public boolean onScroll(ScrollEvent scrollEvent) {
                return false;
            }
            @Override
            public boolean onZoom(ZoomEvent zoomEvent) {
                // 7,11,14,17 - available zooms defined in array


                // Control overlays visibility
                if (mMapView.getZoomLevel() >= 16) {
                    albMarkersOverlay.setEnabled(true);
                    mMapView.invalidate();
                } else if (mMapView.getZoomLevel() >= 11){
                    cityMarkersOverlay.setEnabled(true);
                    mMapView.invalidate();
                } else {
                    albMarkersOverlay.setEnabled(false);
                    cityMarkersOverlay.setEnabled(false);
                    mMapView.invalidate();
                }
                return false;
            }
        });
        buildGoogleApiClient();
        return mMapView;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setHardwareAccelerationOff()
    {
        // Turn off hardware acceleration here, or in manifest
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            mMapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        final Context context = this.getActivity();
		final DisplayMetrics dm = context.getResources().getDisplayMetrics();

        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        this.mCompassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context),
                mMapView);
        this.mLocationOverlay = new MyLocationNewOverlay(context, new GpsMyLocationProvider(context),
                mMapView);

//      mMinimapOverlay = new MinimapOverlay(context, mMapView.getTileRequestCompleteHandler());
//		mMinimapOverlay.setWidth(dm.widthPixels / 5);
//		mMinimapOverlay.setHeight(dm.heightPixels / 5);

		mScaleBarOverlay = new ScaleBarOverlay(context);
		mScaleBarOverlay.setCentred(true);
		mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);

        mRotationGestureOverlay = new RotationGestureOverlay(context, mMapView);
		mRotationGestureOverlay.setEnabled(false);

        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(false);
        mMapView.getOverlays().add(this.mLocationOverlay);
        mMapView.getOverlays().add(this.mCompassOverlay);
//        mMapView.getOverlays().add(this.mMinimapOverlay);
		mMapView.getOverlays().add(this.mScaleBarOverlay);
        mMapView.getOverlays().add(this.mRotationGestureOverlay);

//        mMapView.getController().setZoom(mPrefs.getInt(PREFS_ZOOM_LEVEL, 8));
        mMapView.scrollTo(mPrefs.getInt(PREFS_SCROLL_X, 0), mPrefs.getInt(PREFS_SCROLL_Y, 0));

		mLocationOverlay.enableMyLocation();
		mCompassOverlay.enableCompass();

        MapTouchOverlay mapTouchOverlay = new MapTouchOverlay(getActivity());
        mMapView.getOverlays().add(mapTouchOverlay);
        try {
            drawRouteAndMarkers(mMapView);

            mMapView.invalidate();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        albMarkersOverlay.setEnabled(false);
        cityMarkersOverlay.setEnabled(false);

        setHasOptionsMenu(true);
    }


    public class MapTouchOverlay extends org.osmdroid.views.overlay.Overlay {

        public MapTouchOverlay(Context ctx) {super(ctx);}

        @Override
        protected void draw(Canvas canvas, MapView mapView, boolean b) {

        }

        @Override
        public boolean onTouchEvent(MotionEvent e, MapView mapView) {
            if(e.getAction() == MotionEvent.ACTION_MOVE) {
                if (mRequestingLocationUpdates) {
                    mRequestingLocationUpdates = false;
                    stopLocationUpdates();
                    Toast toast = Toast.makeText(getActivity(), "GPS Tracking Off", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
            if(e.getAction() == MotionEvent.ACTION_SCROLL) {
                if (mRequestingLocationUpdates) {
                    mRequestingLocationUpdates = false;
                    stopLocationUpdates();
                    Toast toast = Toast.makeText(getActivity(), "GPS Tracking Off", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
            return false;
        }
    }

    @Override
    public void onPause()
    {
        final SharedPreferences.Editor edit = mPrefs.edit();
        edit.putString(PREFS_TILE_SOURCE, mMapView.getTileProvider().getTileSource().name());
        edit.putInt(PREFS_SCROLL_X, mMapView.getScrollX());
        edit.putInt(PREFS_SCROLL_Y, mMapView.getScrollY());
        edit.putInt(PREFS_ZOOM_LEVEL, mMapView.getZoomLevel());
        edit.putBoolean(PREFS_SHOW_LOCATION, mLocationOverlay.isMyLocationEnabled());
        edit.putBoolean(PREFS_SHOW_COMPASS, mCompassOverlay.isCompassEnabled());
        edit.commit();

        this.mLocationOverlay.disableMyLocation();
        this.mCompassOverlay.disableCompass();

        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
        final String tileSourceName = mPrefs.getString(PREFS_TILE_SOURCE,
                TileSourceFactory.DEFAULT_TILE_SOURCE.name());
        try {
            final ITileSource tileSource = TileSourceFactory.getTileSource(tileSourceName);
            mMapView.setTileSource(tileSource);
        } catch (final IllegalArgumentException e) {
            mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        }
        if (mPrefs.getBoolean(PREFS_SHOW_LOCATION, false)) {
			this.mLocationOverlay.enableMyLocation();
        }
        if (mPrefs.getBoolean(PREFS_SHOW_COMPASS, false)) {
			this.mCompassOverlay.enableCompass();
        }



    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        // Put overlay items first
        mMapView.getOverlayManager().onCreateOptionsMenu(menu, MENU_LAST_ID, mMapView);

        // Put "About" menu item last
        menu.add(0, MENU_ABOUT, Menu.CATEGORY_SECONDARY, R.string.about).setIcon(
                android.R.drawable.ic_menu_info_details);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu pMenu)
    {
        mMapView.getOverlayManager().onPrepareOptionsMenu(pMenu, MENU_LAST_ID, mMapView);
        super.onPrepareOptionsMenu(pMenu);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (mMapView.getOverlayManager().onOptionsItemSelected(item, MENU_LAST_ID, mMapView))
			return true;

		switch (item.getItemId()) {
		case MENU_ABOUT:
			getActivity().showDialog(DIALOG_ABOUT_ID);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public MapView getMapView() {
		return mMapView;
	}

    // @Override
    // public boolean onTrackballEvent(final MotionEvent event) {
    // return this.mMapView.onTrackballEvent(event);
    // }

    // GMS Block
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
//                setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);

            }
//            updateUI();
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
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


    /**
     * Updates the latitude, the longitude, and the last location time in the UI.
     */

    private void updateUI() {
        if (mCurrentLocation != null) {
            String lon = "Long: " + String.valueOf(mCurrentLocation.getLongitude());
            String lat = " Lat: " + String.valueOf(mCurrentLocation.getLatitude());
            String tim = " Time: " + mLastUpdateTime;

            ((MapActivity)getActivity()).geoOutTextViewLon.setText(lon);
            ((MapActivity)getActivity()).geoOutTextViewLat.setText(lat);
            ((MapActivity)getActivity()).geoOutTextViewTime.setText(tim);

            GeoPoint point = new GeoPoint(mCurrentLocation);
            mMapView.getController().animateTo(point);
            if (mMapView.getZoomLevel() < TRACK_ZOOM_LEVEL) {
                mMapView.getController().setZoom(TRACK_ZOOM_LEVEL);
            }
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }
    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    @Override
    public void onConnected(Bundle connectionHint) {
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            //updateUI();
        }
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateUI();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }


    public void onUseMapActivityBtnHandler() {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            startLocationUpdates();
            Toast toast = Toast.makeText(getActivity(), "GPS Tracking On", Toast.LENGTH_LONG);
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
    public void drawRouteAndMarkers(MapView mapView) throws JSONException {

        JsonFilesHandler jfh = new JsonFilesHandler(getActivity());

        // Drawing route, each stage on it's own overlay
        for (int i = 1; i <= 32; i++) {
            JSONObject fileObj = jfh.parseJSONObj("json/stage" + i + ".json");
                JSONObject stageObj = fileObj.getJSONObject("gpx").getJSONObject("trk");
                    String name = stageObj.getString("name");

                    JSONArray geoArr = stageObj.getJSONObject("trkseg").getJSONArray("trkpt");

                    ArrayList<GeoPoint> waypoints = new ArrayList<>();
                    for (int h = 0; h < geoArr.length(); h++) {
                        JSONObject trkpt = geoArr.getJSONObject(h);
                        Double lat = trkpt.getDouble("-lat");
                        Double lng = trkpt.getDouble("-lon");
                        GeoPoint newPoint = new GeoPoint(lat, lng);
                        waypoints.add(newPoint);
                    }
                    Polyline p = new Polyline(getActivity());
                    p.setTitle(name);
                    p.setColor(Color.GREEN);
                    p.setWidth(8.0f);
                    p.setPoints(waypoints);
                    mapView.getOverlays().add(p);
        }


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

                if (alb_geo.contains(", ") == true){
                    location = alb_geo.split(", ");
                } else {
                    location = alb_geo.split(",");
                }

                if (location[0] == null || location[0] == "") {
                    Log.v("TEST", v.getString("id") + " " + location);
                } else {
                    Double lat = Double.parseDouble(location[0]);
                    Double lng = Double.parseDouble(location[1]);
                    albMarkersOverlay.add(drawAlbMarker(mapView, lat, lng, title, desc));
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
                    cityMarkersOverlay.add(drawCityMarker(mapView, lat, lng, title, snippet));
                }
            }
        }
        mMapView.getOverlays().add(cityMarkersOverlay);

    }

}
