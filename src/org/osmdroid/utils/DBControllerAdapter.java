package org.osmdroid.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Tony on 14-Feb-15.
 */
public class DBControllerAdapter {

    DBController dbController;

    public DBControllerAdapter(Context context) {
        dbController = new DBController(context);
    }

    public long insertAlbergue(String title, String locality, String tel, String beds, int stage, double lat, double lng, String type, String address) {
        SQLiteDatabase db = dbController.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(dbController.albergues.TITLE, title);
        cv.put(dbController.albergues.LOCALITY, locality);
        cv.put(dbController.albergues.TEL, tel);
        cv.put(dbController.albergues.BEDS, beds);
        cv.put(dbController.albergues.LAT, lat);
        cv.put(dbController.albergues.LNG, lng);
        cv.put(dbController.albergues.STAGE, stage);
        cv.put(dbController.albergues.TYPE, type);
        cv.put(dbController.albergues.ADDRESS, address);

        long id = db.insert(dbController.albergues.TABLE_NAME, null, cv);
        return id;
    }

    public long insertLocality(String title, double lat, double lng) {
        SQLiteDatabase db = dbController.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(dbController.locality.TITLE, title);
        cv.put(dbController.locality.LAT, lat);
        cv.put(dbController.locality.LNG, lng);

        long id = db.insert(dbController.locality.TABLE_NAME, null, cv);
        return id;
    }

    public int eraseLocality() {
        SQLiteDatabase db = dbController.getWritableDatabase();
        int count = db.delete(dbController.locality.TABLE_NAME, null, null);

        return count;
    }

    public int eraseAlbergues() {
        SQLiteDatabase db = dbController.getWritableDatabase();
        int count = db.delete(dbController.albergues.TABLE_NAME, null, null);

        return count;
    }

    public JSONArray getAlbergues(int stage_id) throws JSONException {
        int title, tel, beds, lat, lng, loc, stage, type, address;
        SQLiteDatabase db = dbController.getWritableDatabase();
        String[] columns = {dbController.albergues.TITLE, dbController.albergues.TEL, dbController.albergues.BEDS, dbController.albergues.LAT,
                dbController.albergues.LNG, dbController.albergues.LOCALITY, dbController.albergues.STAGE, dbController.albergues.TYPE, dbController.albergues.ADDRESS};
        Cursor cursor;
        if (stage_id != 0) {
            cursor = db.query(dbController.albergues.TABLE_NAME, columns, dbController.albergues.STAGE + " = '"+stage_id+"'", null, null, null, null);
        } else {
            cursor = db.query(dbController.albergues.TABLE_NAME, columns, null, null, null, null, null);
        }
        JSONArray array = new JSONArray();
        while (cursor.moveToNext()) {
            title = cursor.getColumnIndex(dbController.albergues.TITLE);
            tel = cursor.getColumnIndex(dbController.albergues.TEL);
            beds = cursor.getColumnIndex(dbController.albergues.BEDS);
            lat = cursor.getColumnIndex(dbController.albergues.LAT);
            lng = cursor.getColumnIndex(dbController.albergues.LNG);
            loc = cursor.getColumnIndex(dbController.albergues.LOCALITY);
            stage = cursor.getColumnIndex(dbController.albergues.STAGE);
            type = cursor.getColumnIndex(dbController.albergues.TYPE);
            address = cursor.getColumnIndex(dbController.albergues.ADDRESS);

            array.put(new JSONObject("{\""+dbController.albergues.TITLE+"\": \""+cursor.getString(title)+"\"," +
                    "\""+dbController.albergues.TEL+"\": \""+cursor.getString(tel)+"\"," +
                    "\""+dbController.albergues.BEDS+"\": \""+cursor.getString(beds)+"\"," +
                    "\""+dbController.albergues.LNG+"\": \""+cursor.getDouble(lng)+"\"," +
                    "\""+dbController.albergues.LOCALITY+"\": \""+cursor.getString(loc)+"\"," +
                    "\""+dbController.albergues.STAGE+"\": \""+cursor.getInt(stage)+"\"," +
                    "\""+dbController.albergues.TYPE+"\": \""+cursor.getString(type)+"\"," +
                    "\""+dbController.albergues.ADDRESS+"\": \""+cursor.getString(address)+"\"," +
                    "\""+dbController.albergues.LAT+"\": \""+cursor.getDouble(lat)+"\"}"));
        }
        return array;
    }
    public JSONArray getLocalities() throws JSONException {
        int title, lat, lng;
        SQLiteDatabase db = dbController.getWritableDatabase();
        String[] columns = {dbController.locality.TITLE, dbController.locality.LAT, dbController.locality.LNG};
        Cursor cursor = db.query(dbController.locality.TABLE_NAME, columns, null, null, null, null, null);
        JSONArray array = new JSONArray();
        while (cursor.moveToNext()) {
            title = cursor.getColumnIndex(dbController.locality.TITLE);
            lat = cursor.getColumnIndex(dbController.locality.LAT);
            lng = cursor.getColumnIndex(dbController.locality.LNG);

            array.put(new JSONObject("{\""+dbController.locality.TITLE+"\": \""+cursor.getString(title)+"\"," +
                    "\""+dbController.locality.LNG+"\": \""+cursor.getDouble(lng)+"\"," +
                    "\""+dbController.locality.LAT+"\": \""+cursor.getDouble(lat)+"\"}"));
        }
        return array;
    }

    static class DBController extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "caminodatabase";
        private static final int DATABASE_VERSION = 5;
        private final Albergues albergues = new Albergues();
        private final Locality locality = new Locality();
        Context context;


        final class Albergues {
            String TABLE_NAME = "ALBERGUE";
            String UID = "_id";
            String TITLE = "title";
            String TYPE = "type";
            String LAT = "lat";
            String LNG = "lng";
            String TEL = "tel";
            String BEDS = "beds";
            String STAGE = "stage";
            String LOCALITY = "locality";
            String ADDRESS = "address";
        }

        final class Locality {
            String TABLE_NAME = "LOCALITY";
            String UID = "_id";
            String TITLE = "title";
            String LAT = "lat";
            String LNG = "lng";
        }

        public DBController(Context applicationcontext) {
            super(applicationcontext, DATABASE_NAME, null, DATABASE_VERSION);
            this.context = applicationcontext;
        }

        //Creates Table
        @Override
        public void onCreate(SQLiteDatabase database) {
            String query;
            query = "CREATE TABLE " + albergues.TABLE_NAME + " ( " + albergues.UID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " " + albergues.TITLE + " TEXT, " + albergues.TYPE + " TEXT," + albergues.ADDRESS + " TEXT," +
                    " " + albergues.LOCALITY + " TEXT, " + albergues.BEDS + " VARCHAR(10)," +
                    " " + albergues.LAT + " DOUBLE," + " " + albergues.STAGE + " INTEGER," +
                    " " + albergues.LNG + " DOUBLE, " + albergues.TEL + " TEXT)";
            database.execSQL(query);
            query = "CREATE TABLE " + locality.TABLE_NAME + " ( " + locality.UID + " INTEGER," +
                    " " + locality.TITLE + " TEXT," + " " + locality.LAT + " DOUBLE," +
                    " " + locality.LNG + " DOUBLE)";
            database.execSQL(query);
//            Toast.makeText(context, "DB Created", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int version_old, int current_version) {
            String query;
            query = "DROP TABLE IF EXISTS " + albergues.TABLE_NAME;
            database.execSQL(query);
            query = "DROP TABLE IF EXISTS " + locality.TABLE_NAME;
            database.execSQL(query);
            onCreate(database);
//            Toast.makeText(context, "DB Upgraded", Toast.LENGTH_LONG).show();
        }
    }

}
