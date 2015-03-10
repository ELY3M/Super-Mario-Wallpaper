package own.supermariowallpaper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

import android.text.format.Time;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.e175.klaus.solarpositioning.*;


public class OpenActivity extends ActionBarActivity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private static final String TAG = "mario openactivity";

    private static OpenActivity main;
	
	//you need to get your own api key from wunderground
    String url = "http://api.wunderground.com/api/YOURAPIKEY/conditions/q/";
    String finalurl = "setup";
    private handlejson obj;

    private TextView mygps;
    double mylat = 0.0;
    double mylon = 0.0;
    String mytemp = "103";
    String myicon = "sunny";
    String myiconurl;
    String finalicon = "sunny";
    String myweather = "sunny";
    private int updatecount = 0;
    private TextView temp;
    private TextView weather;
    private ImageView icon;
    private TextView checkicon;
    private TextView checkiconurl;
    private TextView suntimes;
    private TextView dayornight;
    private TextView lastupdatetime;
    private Button getupdates;
    private Button startupdates;
    private Button stopupdates;
    private GoogleApiClient GoogleApiClient;

    private PendingIntent pi;
    private AlarmManager manager;

    ///int interval = 10000; // 10 seconds
    ////int interval = 1800000; //every 30 mins
    ////public static final long WEATHERUPDATE_INTERVAL_IN_MILLISECONDS = 70000;
    public static final long WEATHERUPDATE_INTERVAL_IN_MILLISECONDS = 1860000;
    public static final long GPSUPDATE_INTERVAL_IN_MILLISECONDS = 1800000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = GPSUPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private final static String REQUESTING_UPDATES_KEY = "requesting-updates-key";
    private final static String LOCATION_KEY = "location-key";
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private Boolean mRequestingUpdates;


    @Override
        protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        main = this;
        initializeGoogleAPI();
        setContentView(R.layout.openactivity);
        getupdates = (Button) findViewById(R.id.getupdates);
        startupdates = (Button) findViewById(R.id.startupdates);
        stopupdates = (Button) findViewById(R.id.stopupdates);
        mygps = (TextView) findViewById(R.id.gpstext);
        temp = (TextView) findViewById(R.id.temp);
        icon = (ImageView) findViewById(R.id.icon);
        weather = (TextView) findViewById(R.id.weather);
        checkicon = (TextView) findViewById(R.id.checkicon);
        checkiconurl = (TextView) findViewById(R.id.checkiconurl);
        suntimes = (TextView) findViewById(R.id.suntimes);
        dayornight = (TextView) findViewById(R.id.dayornight);
        lastupdatetime = (TextView) findViewById(R.id.lastupdatetime);

        mRequestingUpdates = false;
        updateValuesFromBundle(savedInstanceState);
        Intent myintent = new Intent(this, updates.class);
        pi = PendingIntent.getService(this, 103002, myintent, PendingIntent.FLAG_CANCEL_CURRENT);


    }

    public static OpenActivity  getMain()
    {
        return main;
    }

////gps stufff///
private void updateValuesFromBundle(Bundle savedInstanceState) {
    Log.i(TAG, "Updating values from bundle");
    if (savedInstanceState != null) {
        if (savedInstanceState.keySet().contains(REQUESTING_UPDATES_KEY)) {
            mRequestingUpdates = savedInstanceState.getBoolean(REQUESTING_UPDATES_KEY);
            setButtonsEnabledState();
        }
        if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
            mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
        }
        mylat = mCurrentLocation.getLatitude();
        mylon = mCurrentLocation.getLongitude();
        mygps.setText("lat: " + mylat + " lon:" + mylon);
        //make sure to setup latlon with weather url
        finalurl = url + mylat + "," + mylon + ".json";
        Log.i(TAG, "updateValuesFromBundle lat: " + mylat + " lon: " + mylon);

    }
}

    private void initializeGoogleAPI() {
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
                == ConnectionResult.SUCCESS) {
            GoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
                    createLocationRequest();
        }
    }



    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(GPSUPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(GoogleApiClient, mLocationRequest, this);
        mRequestingUpdates = true;
        setButtonsEnabledState();
        manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), WEATHERUPDATE_INTERVAL_IN_MILLISECONDS, pi);
        Toast.makeText(this, "updates started", Toast.LENGTH_SHORT).show();

    }

    private void stopUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(GoogleApiClient, this);
        mRequestingUpdates = false;
        setButtonsEnabledState();
        if (manager != null) {
            manager.cancel(pi);
            Toast.makeText(this, "updates stopped", Toast.LENGTH_SHORT).show();
        }

    }


public void update() {
    ///get weather////
    LocationServices.FusedLocationApi.requestLocationUpdates(GoogleApiClient, mLocationRequest, this);
    mRequestingUpdates = true;
    setButtonsEnabledState();
    if (isOnline(getApplicationContext())) {
            finalurl = url + mylat + "," + mylon + ".json";
            Log.i(TAG, "finalurl: " + finalurl);
            obj = new handlejson(finalurl);
            obj.fetchJSON();
            while (obj.parsingComplete) ;
            mytemp = obj.getTemp();
            myicon = obj.getIcon();
            myiconurl = obj.getIconurl();
            myweather = obj.getWeather();

            //wunderground have fucking bug in icon... do not use use night icon as they should.
            Pattern pattern = Pattern.compile("http://icons.wxug.com/i/c/k/(.*?).gif");
            Matcher geticon = pattern.matcher(myiconurl);
            while (geticon.find()) {
                finalicon = geticon.group(1);
            }
            double finaltemp = Math.ceil(Double.valueOf(mytemp));
            mytemp = String.valueOf((int) finaltemp) + "°F";

            temp.setText(mytemp);
            weather.setText(myweather);
            int res = getResources().getIdentifier(finalicon, "drawable", getApplicationContext().getPackageName());
            icon.setImageResource(res);
            checkicon.setText(myicon);
            checkiconurl.setText(finalicon);

            getsuntimes();

        SimpleDateFormat timestamp = new SimpleDateFormat("EEE M-d-yy h:mm:ss a");
            Calendar c = Calendar.getInstance();
            String mytimestamp = timestamp.format(c.getTime());
            updatecount++;
            lastupdatetime.setTextSize(13);
            lastupdatetime.setText("Last Update: " + mytimestamp + "\nUpdate Count: " + updatecount);
            ///Toast.makeText(getApplicationContext(), "Weather Update: " + mytimestamp + "  Update Count: " + updatecount, Toast.LENGTH_SHORT).show();

    } else { //internet check
        SimpleDateFormat timestamp = new SimpleDateFormat("EEE M-d-yy h:mm:ss a");
        Calendar c = Calendar.getInstance();
        String mytimestamp = timestamp.format(c.getTime());
        lastupdatetime.setTextSize(13);
        lastupdatetime.setText("Failed Update (Offline): " + mytimestamp + "\nUpdate Count: " + updatecount);
    }
}


    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    }



    @Override
    protected void onStart() {
        super.onStart();
        GoogleApiClient.connect();
    }



    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(GoogleApiClient);
            mylat = mCurrentLocation.getLatitude();
            mylon = mCurrentLocation.getLongitude();
            mygps.setText("lat: " + mylat + " lon:" + mylon);
            finalurl = url + mylat + "," + mylon + ".json";
            Log.i(TAG, "onConnected lat: " + mylat + " lon: " + mylon);
        }
        if (mRequestingUpdates) {
            startUpdates();
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mylat = mCurrentLocation.getLatitude();
        mylon = mCurrentLocation.getLongitude();
        mygps.setText("lat: " + mylat + " lon:" + mylon);
        finalurl = url + mylat + "," + mylon + ".json";
        Log.i(TAG, "onLocationChanged lat: " + mylat + " lon: " + mylon);

    }


/////end of gps stuff//////

public void getweather(View view) {
    update();
}


///location and weather updates
    public void startweatherupdates(View view) {
    startUpdates();
}

    public void stopweatherupdates(View view) {
    stopUpdates();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection suspended");
        GoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_UPDATES_KEY, mRequestingUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void setButtonsEnabledState() {
        if (mRequestingUpdates) {
            startupdates.setEnabled(false);
            stopupdates.setEnabled(true);
        } else {
            startupdates.setEnabled(true);
            stopupdates.setEnabled(false);
        }
    }
////////////end of gps//////////////////////

    public void getupdates(View view) {
        update();
    }

    public void getsunupdates(View view) {
        getsuntimes();
    }

/*
    double timetofraction(int hour, int minute) {
        double dechr = (float)hour/24;
        double decmin = (float)minute/60/24;
        double dectime = dechr+decmin;
        return dectime;
    }
*/

    public void getsuntimes() {

        suntimes.setText("");

        double ZENITH = 0.0;

        double ASTRONOMICAL = 108;
        double NAUTICAL = 102;
        double CIVIL = 96;
        double OFFICAL = 90.8333;

        suntimes.append("ZENITH GUIDE:\n");
        suntimes.append("ASTRONOMICAL = 108\n");
        suntimes.append("NAUTICAL = 102\n");
        suntimes.append("CIVIL = 96\n");
        suntimes.append("OFFICAL = 90.8333\n");
        suntimes.append("\n");




        ///sun times///
        double astronomicalrise = 0;
        double nauticalrise = 0;
        double civilrise = 0;
        double sunrise = 0;
        double sunset = 0;
        double civilset = 0;
        double nauticalset = 0;
        double astronomicalset = 0;
        double currenttimeinfraction = 0;

        String asunrise = "0";
        String nsunrise = "0";
        String csunrise = "0";
        String osunrise = "0";
        String osunset = "0";
        String csunset = "0";
        String nsunset = "0";
        String asunset = "0";


        final String tz = Time.getCurrentTimezone();
        final SunCalculator calculator = new SunCalculator(mCurrentLocation, tz);
        final Calendar now = Calendar.getInstance();
        final GregorianCalendar dateTime = new GregorianCalendar();



        final double sunrisea = calculator.computeSunriseTime(SunCalculator.ZENITH_ASTRONOMICAL, now);
        astronomicalrise = SunCalculator.timeToDayFraction(sunrisea);

        final double sunrisen = calculator.computeSunriseTime(SunCalculator.ZENITH_NAUTICAL, now);
        nauticalrise = SunCalculator.timeToDayFraction(sunrisen);

        final double sunrisec = calculator.computeSunriseTime(SunCalculator.ZENITH_CIVIL, now);
        civilrise = SunCalculator.timeToDayFraction(sunrisec);

        final double sunriseo = calculator.computeSunriseTime(SunCalculator.ZENITH_OFFICIAL, now);
        sunrise = SunCalculator.timeToDayFraction(sunriseo);

        final double sunsetc = calculator.computeSunsetTime(SunCalculator.ZENITH_CIVIL, now);
        civilset = SunCalculator.timeToDayFraction(sunsetc);

        final double sunsetn = calculator.computeSunsetTime(SunCalculator.ZENITH_NAUTICAL, now);
        nauticalset = SunCalculator.timeToDayFraction(sunsetn);

        final double sunseta = calculator.computeSunsetTime(SunCalculator.ZENITH_ASTRONOMICAL, now);
        astronomicalset = SunCalculator.timeToDayFraction(sunseta);

        final double sunseto = calculator.computeSunsetTime(SunCalculator.ZENITH_OFFICIAL, now);
        sunset = SunCalculator.timeToDayFraction(sunseto);


        int currenthr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int currentmin = Calendar.getInstance().get(Calendar.MINUTE);
        //currenttimeinfraction = timetofraction(currenthr, currentmin);


        asunrise = SunCalculator.timeToString(sunrisea);
        nsunrise = SunCalculator.timeToString(sunrisen);
        csunrise = SunCalculator.timeToString(sunrisec);
        osunrise = SunCalculator.timeToString(sunriseo);
        osunset = SunCalculator.timeToString(sunseto);
        csunset = SunCalculator.timeToString(sunsetc);
        nsunset = SunCalculator.timeToString(sunsetn);
        asunset = SunCalculator.timeToString(sunseta);

        suntimes.setTextSize(14);
        suntimes.setText("sunrise and sunrise times:\n");

        ///human readable
        suntimes.append("mario current time: " + currenthr + ":" + currentmin + "\n");

        suntimes.append("mario astronomicalrise: " + asunrise + "\n");
        suntimes.append("mario nauticalrise: " + nsunrise + "\n");
        suntimes.append("mario civilrise: " + csunrise + "\n");
        suntimes.append("mario sunrise: " + osunrise + "\n");
        suntimes.append("mario sunset: " + osunset + "\n");
        suntimes.append("mario civilset: " + csunset + "\n");
        suntimes.append("mario nauticalset: " + nsunset + "\n");
        suntimes.append("mario astronomicalset: " + asunset + "\n");


        suntimes.append("\n");

        suntimes.append("Solar positioning\n");

        AzimuthZenithAngle solarposition = PSA.calculateSolarPosition(dateTime, mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        suntimes.append("PSA: " + solarposition.getZenithAngle() + "\n");
        AzimuthZenithAngle solarposition2 = SPA.calculateSolarPosition(dateTime, mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(),
                190, // elevation
                67, // delta T
                1010, // avg. air pressure
                11); // avg. air temperature
        suntimes.append("SPA (more acc but slow): " + solarposition2.getZenithAngle() + "\n");
        suntimes.append("\n");
        ZENITH = solarposition2.getZenithAngle();

        dayornight.setTextSize(14);
        dayornight.setText("mario day or night:\n");

/*
        double ASTRONOMICAL = 108;
        double NAUTICAL = 102;
        double CIVIL = 96;
        double OFFICAL = 90.8333;-
 */
        if (ZENITH < 90.8) {
            dayornight.append("mario day\n");
        }

        if (ZENITH >= 90.8 && ZENITH < 96) {
            dayornight.append("mario civil\n");
        }

        if (ZENITH >= 96 && ZENITH < 102) {
            dayornight.append("mario nautical\n");
        }

        if (ZENITH >= 102 && ZENITH < 108) {
            dayornight.append("mario astronomical\n");
        }
        if (ZENITH >= 108) {
            dayornight.append("mario night\n");
        }


/*
        ///fraction time
        suntimes.append("mario current time in fraction: " + currenttimeinfraction + "\n");
        suntimes.append("mario astronomicalrise in fraction: " + astronomicalrise + "\n");
        suntimes.append("mario nauticalrise in fraction: " + nauticalrise + "\n");
        suntimes.append("mario civilrise in fraction: " + civilrise + "\n");
        suntimes.append("mario sunrise in fraction: " + sunrise + "\n");
        suntimes.append("mario sunset in fraction: " + sunset + "\n");
        suntimes.append("mario civilset in fraction: " + civilset + "\n");
        suntimes.append("mario nauticalset in fraction: " + nauticalset + "\n");
        suntimes.append("mario astronomicalset in fraction: " + astronomicalset + "\n");




        dayornight.setTextSize(14);
        dayornight.setText("day or night:\n");

        if (sunrise <= currenttimeinfraction && sunset >= currenttimeinfraction) {
            dayornight.append("mario day\n");
        }


        if (astronomicalrise <= currenttimeinfraction && nauticalrise >= currenttimeinfraction) {
            dayornight.append("mario astronomicalrise\n");
        }

        if (nauticalrise <= currenttimeinfraction && civilrise >= currenttimeinfraction) {
            dayornight.append("mario nauticalrise\n");
        }

        if (civilrise <= currenttimeinfraction && sunrise >= currenttimeinfraction) {
            dayornight.append("mario civilrise\n");
        }


        if (sunset <= currenttimeinfraction && civilset >= currenttimeinfraction) {
            dayornight.append("mario civilset\n");
        }

        if (civilset <= currenttimeinfraction && nauticalset >= currenttimeinfraction) {
            dayornight.append("mario nauticalset\n");
        }

        if (nauticalset <= currenttimeinfraction && astronomicalset >= currenttimeinfraction) {
            dayornight.append("mario astronomicalset\n");
        }

        if (astronomicalset <= currenttimeinfraction) {
            dayornight.append("mario night\n");
        }
        if (currenttimeinfraction >= 0 && astronomicalrise >= currenttimeinfraction) {
            dayornight.append("mario night\n");

        }

        ///TODO get midnight sunset working... common in alaska.

        //sunset after midnight - common in alaska
        if (sunset >= 0 && currenttimeinfraction <= 0) {
            dayornight.append("mario day - before midnight sunset\n");
        }
        if (sunset >= 0 && currenttimeinfraction >= 0) {
            dayornight.append("mario night - after midnight sunset\n");
        }


        TimeZone timeZone = TimeZone.getTimeZone(now.getTimeZone().getID());
        //SunriseSunset ss = new SunriseSunset(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), now.getTime(), timeZone.getOffset(now.getTimeInMillis()) / 1000 / 60 / 60);
        SunriseSunset ss = new SunriseSunset(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), now.getTime(), 0);
        dayornight.append("mario - sun up: " + ss.isSunUp() + " sun down: " + ss.isSunDown() + "\n");

        if (ss.isSunUp()) {
            dayornight.append("mario day - sun is up all day\n");
        }

        if (ss.isSunDown()) {
            dayornight.append("mario night - sun is down all day\n");
        }

        if (ss.isDaytime()) {
            dayornight.append("isDaytime: " + ss.isDaytime() + "\n");
            dayornight.append("mario day - isDaytime = true\n");
        } else {
            dayornight.append("isDaytime: " + ss.isDaytime() + "\n");
            dayornight.append("mario night - isDaytime = false\n");
        }
*/




    }


}
