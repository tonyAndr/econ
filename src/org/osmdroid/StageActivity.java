package org.osmdroid;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.XLabels;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.views.util.JsonFilesHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class StageActivity extends ActionBarActivity{

    private LineChart mChart;
    private TextView tvStagename;
    private TextView tvStagelength;
    private TextView tvKmDone;
    private TextView tvKmLeft;
    private JSONArray mJsonArr;
    private ArrayList<String> xValsList;
    private ArrayList<Entry> altPoints;
    private float mStageLength;
    private float mStageKmLeft;
    private String mStageName;
    private JsonFilesHandler jfh = new JsonFilesHandler(this);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stage);

        Intent intent = getIntent();
        int stageId = intent.getIntExtra("stageId", 1);
        String fromto = intent.getStringExtra("fromto");
        setTitle("Stage " + stageId);

        tvStagename = (TextView)findViewById(R.id.tv_stagename);
        tvStagelength = (TextView)findViewById(R.id.tv_stagelength);
        tvKmDone = (TextView)findViewById(R.id.tv_kmdone);
        tvKmLeft = (TextView)findViewById(R.id.tv_kmleft);


        tvStagename.setText(fromto);

        xValsList = new ArrayList<>();
        altPoints = new ArrayList<>();

        getJsonData(stageId);
        chartSetup();
        tvStagelength.setText(String.format("%.1f", mStageLength) + "km");
        tvKmLeft.setText("Left: " + String.format("%.1f", mStageKmLeft) + "km");
        tvKmDone.setText("Behind: " + String.format("%.1f", mStageLength-mStageKmLeft) + "km");
    }

    public static float distFrom(float lat1, float lng1, float lat2, float lng2) {
        double earthRadius = 6371; //kilometers
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        float dist = (float) (earthRadius * c);

        return dist;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stage, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Getting last known position
    private double[] getGPS() {
        LocationManager lm = (LocationManager) getSystemService(this.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);
        Location l = null;
        for (int i=providers.size()-1; i>=0; i--) {
            l = lm.getLastKnownLocation(providers.get(i));
            if (l != null) break;
        }
        double[] gps = new double[2];
        if (l != null) {
            gps[0] = l.getLatitude();
            gps[1] = l.getLongitude();
        }
        return gps;
    }

    // Getting id of chart's point to be highlighted (nearest point to our location)
    private int getHighlightId (JSONArray jarr, double[] lastGeo) {
        int id = -1; // -1 == can't find lastGeo
        boolean lat_included = false;
        boolean lng_included = false;
        if (lastGeo != null) {
            float minDist = 999;
            try {
                if (lastGeo[1] < jarr.getJSONObject(0).getDouble("-lon") &&
                        lastGeo[1] > jarr.getJSONObject(jarr.length()-1).getDouble("-lon")){
                    lng_included = true;
                }

                if (jarr.getJSONObject(0).getDouble("-lat") < jarr.getJSONObject(jarr.length()-1).getDouble("-lat")) {
                    if (lastGeo[0] < jarr.getJSONObject(jarr.length()-1).getDouble("-lat") &&
                            lastGeo[0] > jarr.getJSONObject(0).getDouble("-lat")){
                        lat_included = true;
                    }
                } else {
                    if (lastGeo[0] < jarr.getJSONObject(0).getDouble("-lat") &&
                            lastGeo[0] > jarr.getJSONObject(jarr.length()-1).getDouble("-lat")){
                        lat_included = true;
                    }
                }
                for (int h = 0; h < jarr.length(); h++) {
                    JSONObject trkpt = jarr.getJSONObject(h);
                    float lat = (float)trkpt.getDouble("-lat");
                    float lng = (float)trkpt.getDouble("-lon");
                    if (lng_included == true && lat_included == true) {
                        float newMin = distFrom((float)lastGeo[0],(float)lastGeo[1], lat, lng);
                        if (minDist > newMin) {
                            minDist = newMin;
                            id = h;
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return id;
    }

    private void chartSetup() {
        mChart = (LineChart) findViewById(R.id.chart);
        mChart.setStartAtZero(true);
        // disable the drawing of values into the chart
        mChart.setDrawYValues(false);
        mChart.setDrawBorder(false);
        mChart.setDrawLegend(false);
        // no description text
        mChart.setDescription("");
        // enable value highlighting
        mChart.setHighlightEnabled(true);
        // enable touch gestures
        mChart.setTouchEnabled(false);
        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);
        mChart.setDrawGridBackground(false);
        mChart.setDrawVerticalGrid(true);

        XLabels xl = mChart.getXLabels();
        xl.setPosition(XLabels.XLabelPosition.BOTTOM); // set the position

        ArrayList<String> xVals = new ArrayList<>();
        ArrayList<LineDataSet> dataSets = new ArrayList<>();

        LineDataSet setAltData = new LineDataSet(altPoints, mStageName);
        dataSets.add(0, setAltData);
        setAltData.setDrawCubic(true);
        setAltData.setCubicIntensity(0.2f);
        setAltData.setDrawFilled(true);
        setAltData.setDrawCircles(false);
        setAltData.setLineWidth(2f);
        setAltData.setCircleSize(5f);
        setAltData.setHighLightColor(Color.BLUE);
        setAltData.setColor(Color.GREEN);

        xVals.addAll(xValsList);

        LineData data = new LineData(xVals, dataSets);
        mChart.setData(data);
        if (getGPS() != null) {
            if (getHighlightId(mJsonArr, getGPS()) != -1) {
                mChart.highlightValue(getHighlightId(mJsonArr, getGPS()), 0); // Highlight current position
            }
        }
    }

    // Load stage info from json
    private void getJsonData(int stageId){
        JSONObject fileObj = jfh.parseJSONObj("json/stage" + stageId + ".json");
        JSONObject stageObj = null;
        try {
            stageObj = fileObj.getJSONObject("gpx").getJSONObject("trk");
            mStageName = stageObj.getString("name");
            mJsonArr = stageObj.getJSONObject("trkseg").getJSONArray("trkpt");

            JSONObject prev_trkpt = new JSONObject();
            float dist = 0;
            mStageKmLeft = 0;
            for (int h = 0; h < mJsonArr.length(); h++) {
                JSONObject trkpt = mJsonArr.getJSONObject(h);
                float lat = (float)trkpt.getDouble("-lat");
                float lng = (float)trkpt.getDouble("-lon");
                float alt = (float)trkpt.getDouble("ele");
                if (h == 0) {
                    dist = 0;
                } else {
                    float prev_lat = (float)prev_trkpt.getDouble("-lat");
                    float prev_lng = (float)prev_trkpt.getDouble("-lon");
                    dist = dist + distFrom(prev_lat, prev_lng, lat, lng);
                }
                prev_trkpt = trkpt;
                xValsList.add(String.format("%.1f", dist));
                altPoints.add(new Entry(alt, h));

                if (h == getHighlightId(mJsonArr, getGPS())) {
                    mStageKmLeft = mStageKmLeft + dist;
                }
            }
            mStageLength = dist;
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }
}
