package Helper;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sezavar.android.sunshine.app.R;
import org.sezavar.android.sunshine.app.data.WeatherContract;
import org.sezavar.android.sunshine.app.data.WeatherContract.WeatherEntry;

import java.text.SimpleDateFormat;
import java.util.Vector;

/**
 * Created by amir on 5/27/15.
 */
public class WeatherDataParser {
    private static final String LOG_TAG = WeatherDataParser.class.getSimpleName();

    /* The date/time conversion code is going to be moved outside the asynctask later,
            * so for convenience we're breaking it out into its own method now.
            */
    private final static String getReadableDateString(long time) {
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private final static String formatHighLows(double high, double low, Context context) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String unitType = sharedPreferences.getString(context.getString(R.string.pref_units_key), context.getString(R.string.pref_units_metric));

        if (unitType.equals(context.getString(R.string.pref_units_imperial))) {
            high = (high * 1.8) + 32;
            low = (low * 1.8) + 32;
        } else if (!unitType.equals(context.getString(R.string.pref_units_metric))) {
            Log.d(LOG_TAG, "Unit type not found: " + unitType);
        }

        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p/>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    public final static String[] getWeatherDataFromJson(String forecastJsonStr, String locationSetting, Context context)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";
        final String OWM_WEATHER_ID = "id";
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
        String cityName = cityJson.getString(OWM_CITY_NAME);

        JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
        double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
        double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

        long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude, context);

        Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());
        // OWM returns daily forecasts based upon the local time of the city that is being
        // asked for, which means that we need to know the GMT offset to translate this data
        // properly.

        // Since this data is also sent in-order and the first day is always the
        // current day, we're going to take advantage of that to get a nice
        // normalized UTC date for all of our weather.

        Time dayTime = new Time();
        dayTime.setToNow();

        // we start at the day returned by local time. Otherwise this is a mess.
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        // now we work exclusively in UTC
        dayTime = new Time();


        for (int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            long dateTime;
            double pressure;
            int humidity;
            double windSpeed;
            double windDirection;
            double high;
            double low;
            int weatherId;
            String description;


            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);


            // Cheating to convert this to UTC time, which is what we want anyhow
            dateTime = dayTime.setJulianDay(julianStartDay + i);

            pressure = dayForecast.getDouble(OWM_PRESSURE);
            humidity = dayForecast.getInt(OWM_HUMIDITY);
            windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
            windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);


            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);
            weatherId = weatherObject.getInt(OWM_WEATHER_ID);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            high = temperatureObject.getDouble(OWM_MAX);
            low = temperatureObject.getDouble(OWM_MIN);

            ContentValues weatherValues = new ContentValues();

            weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationId);
            weatherValues.put(WeatherEntry.COLUMN_DATE, dateTime);
            weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, humidity);
            weatherValues.put(WeatherEntry.COLUMN_PRESSURE, pressure);
            weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(WeatherEntry.COLUMN_DEGREES, windDirection);
            weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, description);
            weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weatherId);

            cVVector.add(weatherValues);
        }
        if (cVVector.size() > 0) {
            ContentValues[] cvArray=new ContentValues[cVVector.size()];
            context.getContentResolver().bulkInsert(WeatherEntry.CONTENT_URI, cVVector.toArray(cvArray ));
        }

        String sortOrder = WeatherEntry.COLUMN_DATE + " ASC";


        // Students: Uncomment the next lines to display what what you stored in the bulkInsert

            Cursor cur = context.getContentResolver().query(WeatherEntry.CONTENT_URI,
                    null, null, null, sortOrder);

            cVVector = new Vector<ContentValues>(cur.getCount());
            if ( cur.moveToFirst() ) {
                do {
                    ContentValues cv = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(cur, cv);
                    cVVector.add(cv);
                } while (cur.moveToNext());
            }

        Log.d(LOG_TAG, "FetchWeatherTask Complete. " + cVVector.size() + " Inserted");

        String[] resultStrs = convertContentValuesToUXFormat(cVVector, context);


        return resultStrs;

    }

    public static String[] convertContentValuesToUXFormat(Vector<ContentValues> cvv, Context mContext) {
        String[] resultStrs = new String[cvv.size()];
        for (int i = 0; i < cvv.size(); i++) {
            ContentValues weatherValues = cvv.elementAt(i);
            String highAndLow = formatHighLows(
                    weatherValues.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP),
                    weatherValues.getAsDouble(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP),
                    mContext);
            resultStrs[i] = getReadableDateString(
                    weatherValues.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE)) +
                    "-" + weatherValues.getAsString(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC) +
                    "-" + highAndLow;
        }
        return resultStrs;
    }


    private final static long addLocation(String locationSetting, String cityName, double lat, double lon, Context mContext) {
        long locId;

        Cursor locationCursor = mContext.getContentResolver().query(WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ",
                new String[]{locationSetting},
                null);
        if (locationCursor.moveToFirst()) {
            int locIdIndex = locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locId = locationCursor.getLong(locIdIndex);
        } else {
            ContentValues locValues = new ContentValues();
            locValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            Uri insertedUri = mContext.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, locValues);
            locId = ContentUris.parseId(insertedUri);

        }

        locationCursor.close();

        return locId;
    }

}
