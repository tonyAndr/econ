package org.osmdroid;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


public class StageActivity extends ActionBarActivity {

    private LineChart chart;
    private TextView tvStagename;
    private TextView tvStagelength;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stage);

        tvStagename = (TextView)findViewById(R.id.tv_stagename);
        tvStagelength = (TextView)findViewById(R.id.tv_stagelength);
        Intent intent = getIntent();
        int stageId = intent.getIntExtra("stageId", 1);
        setTitle("Stage "+stageId);

        chart = (LineChart) findViewById(R.id.chart);


        ArrayList<String> xVals = new ArrayList<>();
        ArrayList<LineDataSet> dataSets = new ArrayList<>();
//        float max_dist = 0;
        JSONObject fileObj = parseJSONObj("json/stage"+stageId+".json");

        try {
            if (fileObj.getJSONObject("gpx").get("trk") instanceof JSONArray) {
                JSONArray stageArr = fileObj.getJSONObject("gpx").getJSONArray("trk");

                for (int j = 0; j < stageArr.length(); j++) {
                    ArrayList<Entry> altPoints = new ArrayList<Entry>();
                    JSONObject trk = stageArr.getJSONObject(j);
                    JSONArray geoArr = trk.getJSONObject("trkseg").getJSONArray("trkpt");
                    JSONObject prev_trkpt = new JSONObject();
                    float dist = 0;
                    for (int h = 0; h < geoArr.length(); h++) {
                        JSONObject trkpt = geoArr.getJSONObject(h);
                        float lat = (float)trkpt.getDouble("-lat");
                        float lng = (float)trkpt.getDouble("-lon");
                        float alt = (float)trkpt.getDouble("ele");
//                        String lng = trkpt.getString("-lon");
                        if (h == 0) {
                            dist = 0;
                        } else {
                            float prev_lat = (float)prev_trkpt.getDouble("-lat");
                            float prev_lng = (float)prev_trkpt.getDouble("-lon");
                            dist = dist + distFrom(prev_lat, prev_lng, lat, lng);
                        }
                        prev_trkpt = trkpt;
                        xVals.add(String.format("%.1f", dist));
                        altPoints.add(new Entry(alt, h));
                    }
//                    tvStagename.setText(trk.getString("name"));
                    tvStagelength.setText(String.format("%.1f", dist));
                    LineDataSet setAltData = new LineDataSet(altPoints, trk.getString("name"));
                    dataSets.add(setAltData);
                }



            } else if (fileObj.getJSONObject("gpx").get("trk") instanceof JSONObject) {
                JSONObject stageObj = fileObj.getJSONObject("gpx").getJSONObject("trk");
//                String name = stageObj.getString("name");
                ArrayList<Entry> altPoints = new ArrayList<Entry>();
                JSONArray geoArr = stageObj.getJSONObject("trkseg").getJSONArray("trkpt");
                JSONObject prev_trkpt = new JSONObject();
                float dist = 0;
                for (int h = 0; h < geoArr.length(); h++) {
                    JSONObject trkpt = geoArr.getJSONObject(h);
                    float lat = (float)trkpt.getDouble("-lat");
                    float lng = (float)trkpt.getDouble("-lon");
                    float alt = (float)trkpt.getDouble("ele");
//                        String lng = trkpt.getString("-lon");
                    if (h == 0) {
                        dist = 0;
                    } else {
                        float prev_lat = (float)prev_trkpt.getDouble("-lat");
                        float prev_lng = (float)prev_trkpt.getDouble("-lon");
                        dist = dist + distFrom(prev_lat, prev_lng, lat, lng);
                    }
                    prev_trkpt = trkpt;
                    xVals.add(String.format("%.1f", dist));
                    altPoints.add(new Entry(alt, h));
                }
//                tvStagename.setText(stageObj.getString("name"));
                tvStagelength.setText(String.format("%.1f",dist));
                LineDataSet setAltData = new LineDataSet(altPoints, stageObj.getString("name"));
                dataSets.add(setAltData);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }






        LineData data = new LineData(xVals, dataSets);
        chart.setData(data);
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

    public JSONArray parseJSONArr(String filename) {
        String JSONString = null;
        JSONArray JSONArray = null;
        try {

            //open the inputStream to the file
            InputStream inputStream = this.getAssets().open(filename);

            int sizeOfJSONFile = inputStream.available();

            //array that will store all the data
            byte[] bytes = new byte[sizeOfJSONFile];

            //reading data into the array from the file
            inputStream.read(bytes);

            //close the input stream
            inputStream.close();

            JSONString = new String(bytes, "UTF-8");
            JSONArray = new JSONArray(JSONString);

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        catch (JSONException x) {
            x.printStackTrace();
            return null;
        }
        return JSONArray;
    }
    public JSONObject parseJSONObj(String filename) {
        String JSONString = null;
        JSONObject JSONObject = null;
        try {

            //open the inputStream to the file
            InputStream inputStream = this.getAssets().open(filename);

            int sizeOfJSONFile = inputStream.available();

            //array that will store all the data
            byte[] bytes = new byte[sizeOfJSONFile];

            //reading data into the array from the file
            inputStream.read(bytes);

            //close the input stream
            inputStream.close();

            JSONString = new String(bytes, "UTF-8");
            JSONObject = new JSONObject(JSONString);

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        catch (JSONException x) {
            x.printStackTrace();
            return null;
        }
        return JSONObject;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stage, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
