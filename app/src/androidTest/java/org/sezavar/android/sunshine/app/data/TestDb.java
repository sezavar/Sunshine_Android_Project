package org.sezavar.android.sunshine.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import java.util.HashSet;

/**
 * Created by amir on 6/1/15.
 */
public class TestDb extends AndroidTestCase {
    private static final String TEST_LOCATION_1 = "94040";
    private static final String TEST_LOCATION_2 = "94041";

    void deleteTheDataBase() {
        mContext.deleteDatabase(WeatherDbHelper.DATA_BASE_NAME);
    }

    public void setUp() {
        deleteTheDataBase();
    }

    public void testCreateDb() throws Throwable {
        final HashSet<String> tableNameHashSet = new HashSet<>();
        tableNameHashSet.add(WeatherContract.LocationEntry.TABLE_NAME);
        tableNameHashSet.add(WeatherContract.WeatherEntry.TABLE_NAME);

        mContext.deleteDatabase(WeatherDbHelper.DATA_BASE_NAME);
        WeatherDbHelper dbHelper=new WeatherDbHelper(this.mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        assertEquals(true, db.isOpen());

        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        do {
            tableNameHashSet.remove(c.getString(0));
        } while (c.moveToNext());

        c = db.rawQuery("PRAGMA table_info(" + WeatherContract.LocationEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        final HashSet<String> locationColumnHashSet = new HashSet<String>();
        locationColumnHashSet.add(WeatherContract.LocationEntry._ID);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_CITY_NAME);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LAT);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LONG);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            locationColumnHashSet.remove(columnName);
        } while (c.moveToNext());

        assertTrue("Error: The database doesn't contain all of the required location entry columns",
                locationColumnHashSet.isEmpty());
        c.close();
        dbHelper.close();

    }


    public void testLocationTable() {

        WeatherDbHelper dbHelper=new WeatherDbHelper(this.mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, TEST_LOCATION_1);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, "North Pole");
        contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, 64.74);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, -147.35);

        long locationId = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, contentValues);
        assertTrue(locationId != -1);
        Cursor c = db.query(WeatherContract.LocationEntry.TABLE_NAME, null, null, null, null, null, null);
        assertTrue("Error: This means the location record has not been inserted correctly",
                c.moveToFirst());
        TestUtilities.validateCurrentRecord("Error: Location Query Validation Failed", c, contentValues);
        assertFalse("Error: More tahn one record returned from location query", c.moveToNext());

        c.close();
        dbHelper.close();
    }


    public void testWeatherTable() {

        WeatherDbHelper dbHelper=new WeatherDbHelper(this.mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, TEST_LOCATION_2);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, "North");
        contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, 64.70);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, -147.30);
        long rowId = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, contentValues);

        ContentValues weatherValues = new ContentValues();
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, rowId);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, System.currentTimeMillis());
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, 10);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, 65);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, 15);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, 10);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, 30);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, "This is a short description");
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, 4);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, 34);

        long weatherId = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, weatherValues);
        assertTrue(weatherId != -1);
        Cursor c = db.query(WeatherContract.WeatherEntry.TABLE_NAME, null, null, null, null, null, null);
        assertTrue("Error: This means the weather record has not been inserted correctly", c.moveToFirst());
        TestUtilities.validateCurrentRecord("Error: Weather Query Validation Failed", c, weatherValues);
        assertFalse("Error: More than one record returned from location query", c.moveToNext());
        c.close();
        dbHelper.close();
    }

}
