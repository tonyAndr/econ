package org.osmdroid.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

/**
 * Created by Tony on 14-Feb-15.
 */
public class DBControllerAdapter  {

    DBController dbController;

    public DBControllerAdapter(Context context) {
        dbController = new DBController(context);
    }

    public long insertAlbergue (String title, String locality, String tel, String beds, int stage, double lat, double lng, String type) {
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

        long id = db.insert(dbController.albergues.TABLE_NAME, null, cv);
        return id;
    }
    public long insertLocality (String title, double lat, double lng) {
        SQLiteDatabase db = dbController.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(dbController.locality.TITLE, title);
        cv.put(dbController.locality.LAT, lat);
        cv.put(dbController.locality.LNG, lng);

        long id = db.insert(dbController.locality.TABLE_NAME, null, cv);
        return id;
    }

    public int eraseAlbergues() {
        SQLiteDatabase db = dbController.getWritableDatabase();
        int count = db.delete(dbController.albergues.TABLE_NAME, null, null);

        return count;
    }

    static class DBController extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "caminodatabase";
        private static final int DATABASE_VERSION = 3;
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
            query = "CREATE TABLE "+albergues.TABLE_NAME+" ( "+albergues.UID+" INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " "+albergues.TITLE+" TEXT, "+albergues.TYPE+" TEXT," +
                    " "+albergues.LOCALITY+" TEXT, "+albergues.BEDS+" VARCHAR(10)," +
                    " "+albergues.LAT+" DOUBLE," + " " +albergues.STAGE +" INTEGER," +
                    " "+albergues.LNG+" DOUBLE, "+albergues.TEL+" TEXT)";
            database.execSQL(query);
            query = "CREATE TABLE "+locality.TABLE_NAME+" ( "+locality.UID+" INTEGER," +
                    " "+locality.TITLE+" TEXT," +" "+locality.LAT+" DOUBLE," +
                    " "+locality.LNG+" DOUBLE)";
            database.execSQL(query);
            Toast.makeText(context, "DB Created", Toast.LENGTH_LONG).show();
        }
        @Override
        public void onUpgrade(SQLiteDatabase database, int version_old, int current_version) {
            String query;
            query = "DROP TABLE IF EXISTS " +albergues.TABLE_NAME;
            database.execSQL(query);
            query = "DROP TABLE IF EXISTS " +locality.TABLE_NAME;
            database.execSQL(query);
            onCreate(database);
            Toast.makeText(context, "DB Upgraded", Toast.LENGTH_LONG).show();
        }
    }

}
