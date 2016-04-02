package own.supermariowallpaper;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.BoundCamera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.Entity;
import org.andengine.entity.particle.BatchedPseudoSpriteParticleSystem;
import org.andengine.entity.particle.emitter.RectangleParticleEmitter;
import org.andengine.entity.particle.initializer.ScaleParticleInitializer;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.entity.scene.background.AutoParallaxBackground;
import org.andengine.entity.scene.background.ParallaxBackground.ParallaxEntity;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.particle.SpriteParticleSystem;
import org.andengine.entity.particle.emitter.PointParticleEmitter;
import org.andengine.entity.particle.initializer.AccelerationParticleInitializer;
import org.andengine.entity.particle.initializer.RotationParticleInitializer;
import org.andengine.entity.particle.initializer.VelocityParticleInitializer;
import org.andengine.entity.particle.modifier.ExpireParticleInitializer;
import org.andengine.entity.particle.modifier.ScaleParticleModifier;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.ui.livewallpaper.BaseLiveWallpaperService;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.view.ConfigChooser;
import org.andengine.opengl.view.EngineRenderer;
import org.andengine.opengl.view.IRendererListener;
import org.andengine.opengl.util.GLState;
import org.andengine.engine.Engine.EngineLock;
import org.andengine.util.debug.Debug;

import android.app.Service;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import android.location.Location;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.e175.klaus.solarpositioning.*;

public class wallpaper extends BaseLiveWallpaperService implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {


    private static final String TAG = "mario wallpaper";
    static GoogleApiClient GoogleApiClient;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 90000;
    ///public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1800000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    public LocationRequest mLocationRequest;
    public double ZENITH = 0.0;

    public Location mCurrentLocation;
    public double mylat = 0.0;
    public double mylon = 0.0;

    public String url = "http://forecast.weather.gov/MapClick.php?";
    public String finalurl = "setup";
    public handlejson obj;

    public static String mytemp = "0";
    public static String myicon = "unknown";
    public static String finalicon = "unknown";
    public static String myweather = "Light Rain";
    ///public static String myweather = "unknown";
    public static int updatecount = 0;



    /////gps stuff////
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
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(GoogleApiClient, mLocationRequest, this);
        getApplicationContext().startService(new Intent(getApplicationContext(), WeatherService.class));
        //Toast.makeText(this, "GPS update started", Toast.LENGTH_SHORT).show();

    }

    private void stopUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(GoogleApiClient, this);
        getApplicationContext().stopService(new Intent(getApplicationContext(), WeatherService.class));
        //Toast.makeText(this, "GPS update stopped", Toast.LENGTH_SHORT).show();

    }
/*
    @Override
    protected void onStart() {
        super.onStart();
        GoogleApiClient.connect();
    }
*/


    @Override
    public void onConnected(Bundle connectionHint) {
        Debug.i(TAG, "Connected to GoogleApiClient");
        if (mCurrentLocation == null) {
            Debug.i(TAG, "mCurrentLocation == null");
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(GoogleApiClient);
        } else {
            Debug.i(TAG, "mCurrentLocation not null");
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(GoogleApiClient);
            mylat = mCurrentLocation.getLatitude();
            mylon = mCurrentLocation.getLongitude();
            //make sure to setup latlon with weather url
            //String finalurl = url + mylat + "," + mylon + ".json";
            Debug.i(TAG, "mario onConnected lat: " + mylat + " lon: " + mylon);

        }

        startUpdates();
    }


    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mylat = mCurrentLocation.getLatitude();
        mylon = mCurrentLocation.getLongitude();
        //make sure to setup latlon with weather url
        //String finalurl = url + mylat + "," + mylon + ".json";
        Debug.i(TAG, "mario onLocationChanged lat: " + mylat + " lon: " + mylon);

    }

    @Override
    public void onConnectionSuspended(int cause) {
        Debug.i(TAG, "mario Connection suspended");
        GoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Debug.i(TAG, "mario Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    //google gps service
    public static class WeatherService extends Service implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {
        private GoogleApiClient GoogleApiClient;
        public static final long GPSUPDATE_INTERVAL_IN_MILLISECONDS = 1800000;
        //public static final long GPSUPDATE_INTERVAL_IN_MILLISECONDS = 60000;
        public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = GPSUPDATE_INTERVAL_IN_MILLISECONDS / 2;
        public final static String REQUESTING_UPDATES_KEY = "requesting-updates-key";
        public final static String LOCATION_KEY = "location-key";
        public LocationRequest mLocationRequest;
        public Location mCurrentLocation;

        public String url = "http://forecast.weather.gov/MapClick.php?";
        public String finalurl = "setup";
        public handlejson obj;
        public double lat = 0.0;
        public double lon = 0.0;
        public String mylat = "0.0";
        public String mylon = "0.0";
        //public static String mytemp = "0";
        //public static String myicon = "unknown";
        //public static String finalicon = "unknown";
        //public static String myweather = "unknown";
        //public int updatecount = 0;


        private Handler handler = new Handler();
        private Runnable runn = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "update service ran");
                //You can do the processing here update the widget/remote views.
                weatherupdate();
                handler.postDelayed(runn, UPDATE_INTERVAL_IN_MILLISECONDS);
            }
        };

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }


        @Override
        public void onStart(Intent intent, int startId) {
            super.onStart(intent, startId);
            Log.i(TAG, "onStart()");
            GoogleApiClient.connect();
        }

        /*
        @Override
        public void onDestroy() {
            stopUpdates();
            Log.i(TAG, "Service.onDestroy()");
            super.onDestroy();
        }
        */

        public void onDestroy() {
            super.onDestroy();
            stopUpdates();
            if(handler!=null){
                handler.removeCallbacks(runn);
            }
            handler = null;
        }


        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.i(TAG, "Service.onStartCommand()");
            initializeGoogleAPI();
            handler.post(runn);
            return super.onStartCommand(intent, flags, startId);
        }


        private void initializeGoogleAPI() {
            Log.i(TAG, "initializeGoogleAPI()");
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
            Log.i(TAG, "startUpdates()");
            LocationServices.FusedLocationApi.requestLocationUpdates(GoogleApiClient, mLocationRequest, this);

        }

        private void stopUpdates() {
            Log.i(TAG, "stopUpdates()");
            LocationServices.FusedLocationApi.removeLocationUpdates(GoogleApiClient, this);

        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Log.i(TAG, "Connected to GoogleApiClient");
            if (mCurrentLocation == null) {
                Log.i(TAG, "mCurrentLocation == null");
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(GoogleApiClient);
            } else {
                Log.i(TAG, "mCurrentLocation not null");
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(GoogleApiClient);
                lat = mCurrentLocation.getLatitude();
                lon = mCurrentLocation.getLongitude();
                mylat = String.valueOf(lat);
                mylon = String.valueOf(lon);
                //make sure to setup latlon with weather url
                finalurl = url + "lat=" + mylat + "&lon=" + mylon + "&FcstType=json";
                Log.i(TAG, "onConnected finalurl: " + finalurl);
                Log.i(TAG, "onConnected lat: " + mylat + " lon: " + mylon);
                ///weatherupdate();
            }

            startUpdates();
        }


        @Override
        public void onLocationChanged(Location location) {
            mCurrentLocation = location;
            lat = mCurrentLocation.getLatitude();
            lon = mCurrentLocation.getLongitude();
            mylat = String.valueOf(lat);
            mylon = String.valueOf(lon);
            //make sure to setup latlon with weather url
            finalurl = url + "lat=" + mylat + "&lon=" + mylon + "&FcstType=json";
            Log.i(TAG, "onLocationChanged finalurl: " + finalurl);
            Log.i(TAG, "onLocationChanged lat: " + mylat + " lon: " + mylon);
            ///weatherupdate();
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


        public void weatherupdate() {
            ///get weather////
            Log.i(TAG, "weatherupdate() started");
            if (isOnline(getApplicationContext())) {
                //if (checknet()) {
                if (GoogleApiClient.isConnected()) {
                    ////if (check for location) {
                    finalurl = url + "lat=" + mylat + "&lon=" + mylon + "&FcstType=json";
                    Log.i(TAG, "finalurl: " + finalurl);
                    obj = new handlejson(finalurl);
                    obj.fetchJSON();
                    while (obj.parsingComplete) ;
                    mytemp = obj.getTemp();
                    myicon = obj.getIcon();
                    myweather = obj.getWeather();
                    if (myweather.isEmpty()) {
                        Log.i(TAG, "myweather is null!");
                        Log.i(TAG, "setting myweather to unknown");
                        myweather = "unknown";
                    }
                    Log.i(TAG, "mytemp: " + mytemp);
                    Log.i(TAG, "myicon: " + myicon);
                    Log.i(TAG, "myweather: " + myweather);
                    Pattern pattern = Pattern.compile("(.*?)(.png|.jpg|.gif)");
                    Matcher geticon = pattern.matcher(myicon);
                    while (geticon.find()) {
                        finalicon = geticon.group(1);
                    }
                    double finaltemp = Math.ceil(Double.valueOf(mytemp));
                    mytemp = String.valueOf((int) finaltemp) + "°F";

                    Log.i(TAG, "finalicon: " + finalicon);
                    myicon = finalicon;
                    Log.i(TAG, "myicon (final): " + myicon);
                    SimpleDateFormat timestamp = new SimpleDateFormat("EEE M-d-yy h:mm:ss a");
                    Calendar c = Calendar.getInstance();
                    String mytimestamp = timestamp.format(c.getTime());
                    updatecount++;
                    Log.i(TAG, "Last Update: " + mytimestamp + "\nUpdate Count: " + updatecount);
                    ///Toast.makeText(getApplicationContext(), "Weather Update: " + mytimestamp + "  Update Count: " + updatecount, Toast.LENGTH_SHORT).show();
                    ///`getcityname(lat,lon);
                } else { ///internet connection
                    SimpleDateFormat timestamp = new SimpleDateFormat("EEE M-d-yy h:mm:ss a");
                    Calendar c = Calendar.getInstance();
                    String mytimestamp = timestamp.format(c.getTime());
                    updatecount++;
                    Log.i(TAG, "Failed Update (Google): " + mytimestamp + "\nUpdate Count: " + updatecount);
                }

            } else { //internet check
                SimpleDateFormat timestamp = new SimpleDateFormat("EEE M-d-yy h:mm:ss a");
                Calendar c = Calendar.getInstance();
                String mytimestamp = timestamp.format(c.getTime());
                updatecount++;
                Log.i(TAG, "Failed Update (Offline): " + mytimestamp + "\nUpdate Count: " + updatecount);
            }

        }


        public static boolean isOnline(Context context) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()) {
                Log.i(TAG, "network state = true");
                return true;
            }
            return false;
        }



    }

/*
    public void weatherupdate() {
        ///get weather////
        Log.i(TAG, "weatherupdate() started");
        if (isOnline(getApplicationContext())) {
            //if (checknet()) {
            if (GoogleApiClient.isConnected()) {
                ////if (check for location) {
                finalurl = url + "lat=" + mylat + "&lon=" + mylon + "&FcstType=json";
                Log.i(TAG, "finalurl: " + finalurl);
                obj = new handlejson(finalurl);
                obj.fetchJSON();
                while (obj.parsingComplete) ;
                mytemp = obj.getTemp();
                myicon = obj.getIcon();
                myweather = obj.getWeather();
                if (myweather.isEmpty()) {
                    Log.i(TAG, "myweather is null!");
                    Log.i(TAG, "setting myweather to unknown");
                    myweather = "unknown";
                }
                Log.i(TAG, "mytemp: " + mytemp);
                Log.i(TAG, "myicon: " + myicon);
                Log.i(TAG, "myweather: " + myweather);
                Pattern pattern = Pattern.compile("(.*?)(.png|.jpg|.gif)");
                Matcher geticon = pattern.matcher(myicon);
                while (geticon.find()) {
                    finalicon = geticon.group(1);
                }
                double finaltemp = Math.ceil(Double.valueOf(mytemp));
                mytemp = String.valueOf((int) finaltemp) + "°F";

                Log.i(TAG, "finalicon: " + finalicon);
                myicon = finalicon;
                Log.i(TAG, "myicon (final): " + myicon);
                SimpleDateFormat timestamp = new SimpleDateFormat("EEE M-d-yy h:mm:ss a");
                Calendar c = Calendar.getInstance();
                String mytimestamp = timestamp.format(c.getTime());
                updatecount++;
                Log.i(TAG, "Last Update: " + mytimestamp + "\nUpdate Count: " + updatecount);
                ///Toast.makeText(getApplicationContext(), "Weather Update: " + mytimestamp + "  Update Count: " + updatecount, Toast.LENGTH_SHORT).show();
                ///`getcityname(lat,lon);
            } else { ///internet connection
                SimpleDateFormat timestamp = new SimpleDateFormat("EEE M-d-yy h:mm:ss a");
                Calendar c = Calendar.getInstance();
                String mytimestamp = timestamp.format(c.getTime());
                updatecount++;
                Log.i(TAG, "Failed Update (Google): " + mytimestamp + "\nUpdate Count: " + updatecount);
            }

        } else { //internet check
            SimpleDateFormat timestamp = new SimpleDateFormat("EEE M-d-yy h:mm:ss a");
            Calendar c = Calendar.getInstance();
            String mytimestamp = timestamp.format(c.getTime());
            updatecount++;
            Log.i(TAG, "Failed Update (Offline): " + mytimestamp + "\nUpdate Count: " + updatecount);
        }

    }


    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            Log.i(TAG, "network state = true");
            return true;
        }
        return false;
    }
*/

    @Override
    public void onCreate() {
        ////gps stuff
        //start GPS
        initializeGoogleAPI();
        GoogleApiClient.connect();
        Debug.i(TAG, "mario gps started: " + mylat + " lon: " + mylon);
        super.onCreate();
    }

    private int CAMERA_WIDTH = 600;
    private int CAMERA_HEIGHT = 800;

    //private Camera mCamera;
    //private Scene mScene;

    BitmapTextureAtlas skyTexture;
    ITextureRegion skyRegion;
    BitmapTextureAtlas civilTexture;
    ITextureRegion civilRegion;
    BitmapTextureAtlas nauticalTexture;
    ITextureRegion nauticalRegion;
    BitmapTextureAtlas astronomicalTexture;
    ITextureRegion astronomicalRegion;
    BitmapTextureAtlas nightTexture;
    ITextureRegion nightRegion;
    BitmapTextureAtlas starsTexture;
    ITextureRegion starsRegion;
    BitmapTextureAtlas starsnauticalTexture;
    ITextureRegion starsnauticalRegion;
    BitmapTextureAtlas groundTexture;
    ITextureRegion groundRegion;

    //weather
    BitmapTextureAtlas cloud1Texture;
    ITextureRegion cloud1Region;
    BitmapTextureAtlas cloud2Texture;
    ITextureRegion cloud2Region;
    BitmapTextureAtlas cloud3Texture;
    ITextureRegion cloud3Region;

    //rain
    BitmapTextureAtlas rain1Texture;
    ITextureRegion rain1Region;
    BitmapTextureAtlas rain2Texture;
    ITextureRegion rain2Region;
    BitmapTextureAtlas rain3Texture;
    ITextureRegion rain3Region;
    BitmapTextureAtlas rain4Texture;
    ITextureRegion rain4Region;

    BitmapTextureAtlas drizzleTexture;
    ITextureRegion drizzleRegion;

    //snow
    BitmapTextureAtlas snow1Texture;
    ITextureRegion snow1Region;
    BitmapTextureAtlas snow2Texture;
    ITextureRegion snow2Region;
    BitmapTextureAtlas snow3Texture;
    ITextureRegion snow3Region;
    BitmapTextureAtlas snow4Texture;
    ITextureRegion snow4Region;
    BitmapTextureAtlas snow5Texture;
    ITextureRegion snow5Region;
    BitmapTextureAtlas snow6Texture;
    ITextureRegion snow6Region;
    BitmapTextureAtlas snow7Texture;
    ITextureRegion snow7Region;

    BitmapTextureAtlas icepelletTexture;
    ITextureRegion icepelletRegion;

    BitmapTextureAtlas hailTexture;
    ITextureRegion hailRegion;

    BitmapTextureAtlas windTexture;
    ITextureRegion windRegion;


    double astronomicalrise = 0;
    double nauticalrise = 0;
    double civilrise = 0;
    double sunrise = 0;
    double sunset = 0;
    double civilset = 0;
    double nauticalset = 0;
    double astronomicalset = 0;
    double currenttimeinfraction = 0;


    @Override
    public EngineOptions onCreateEngineOptions() {
        final Camera mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
        ///return new EngineOptions(true, ScreenOrientation.PORTRAIT_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera);
        return new EngineOptions(true, ScreenOrientation.PORTRAIT_FIXED, new FillResolutionPolicy(), mCamera);

    }

    @Override
    public void onCreateResources(OnCreateResourcesCallback pOnCreateResourcesCallback) {
        Debug.i(TAG, "mario gps in res: " + mylat + " lon: " + mylon);
        Debug.i(TAG, "mario weather in res: temp:" + mytemp + " icon: " + myicon + " weather: " + myweather);

        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        this.skyTexture = new BitmapTextureAtlas(this.getTextureManager(), 1080, 1920);
        this.skyRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.skyTexture, this, "sky.png", 0, 0);
        skyTexture.load();
        this.civilTexture = new BitmapTextureAtlas(this.getTextureManager(), 1080, 1920);
        this.civilRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.civilTexture, this, "civil.png", 0, 0);
        civilTexture.load();
        this.nauticalTexture = new BitmapTextureAtlas(this.getTextureManager(), 1080, 1920);
        this.nauticalRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.nauticalTexture, this, "nautical.png", 0, 0);
        nauticalTexture.load();
        this.starsnauticalTexture = new BitmapTextureAtlas(this.getTextureManager(), 1080, 1920);
        this.starsnauticalRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.starsnauticalTexture, this, "starsnautical.png", 0, 0);
        starsnauticalTexture.load();
        this.astronomicalTexture = new BitmapTextureAtlas(this.getTextureManager(), 1080, 1920);
        this.astronomicalRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.astronomicalTexture, this, "astronomical.png", 0, 0);
        astronomicalTexture.load();
        this.nightTexture = new BitmapTextureAtlas(this.getTextureManager(), 1080, 1920);
        this.nightRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.nightTexture, this, "night.png", 0, 0);
        nightTexture.load();
        this.starsTexture = new BitmapTextureAtlas(this.getTextureManager(), 1080, 1920);
        this.starsRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.starsTexture, this, "stars.png", 0, 0);
        starsTexture.load();

        //weather

        //clouds
        this.cloud1Texture = new BitmapTextureAtlas(this.getTextureManager(), 1080, 1920);
        this.cloud1Region = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.cloud1Texture, this, "cloud1.png", 0, 0);
        cloud1Texture.load();
        this.cloud2Texture = new BitmapTextureAtlas(this.getTextureManager(), 1080, 1920);
        this.cloud2Region = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.cloud2Texture, this, "cloud2.png", 0, 0);
        cloud2Texture.load();
        this.cloud3Texture = new BitmapTextureAtlas(this.getTextureManager(), 1080, 1920);
        this.cloud3Region = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.cloud3Texture, this, "cloud3.png", 0, 0);
        cloud3Texture.load();

        //rain
        this.rain1Texture = new BitmapTextureAtlas(this.getTextureManager(), 1080, 1920);
        this.rain1Region = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.rain1Texture, this, "rain1.png", 0, 0);
        rain1Texture.load();
        this.rain2Texture = new BitmapTextureAtlas(this.getTextureManager(), 1080, 1920);
        this.rain2Region = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.rain2Texture, this, "rain2.png", 0, 0);
        rain2Texture.load();
        this.rain3Texture = new BitmapTextureAtlas(this.getTextureManager(), 1080, 1920);
        this.rain3Region = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.rain3Texture, this, "rain3.png", 0, 0);
        rain3Texture.load();
        this.rain4Texture = new BitmapTextureAtlas(this.getTextureManager(), 1080, 1920);
        this.rain4Region = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.rain4Texture, this, "rain4.png", 0, 0);
        rain4Texture.load();


        /*
        this.groundTexture = new BitmapTextureAtlas(this.getTextureManager(), 1080, 1920);
        this.groundRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.groundTexture, this, "ground.png", 0, 0);
        groundTexture.load();
        */
        //test
        this.groundTexture = new BitmapTextureAtlas(this.getTextureManager(), 66, 118, TextureOptions.REPEATING_NEAREST);
        this.groundRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.groundTexture, this, "groundtitle.png", 0, 0);
		/* The following statement causes the BitmapTextureAtlas to be printed horizontally 10x on any Sprite that uses it.
		 * So we will later increase the width of such a sprite by the same factor to avoid distortion. */
        //this.groundRegion.setTextureWidth(2 * this.groundTexture.getWidth());
        this.groundTexture.load();

        pOnCreateResourcesCallback.onCreateResourcesFinished();
    }




    @Override
    public void onCreateScene(OnCreateSceneCallback pOnCreateSceneCallback) {
        this.mEngine.registerUpdateHandler(new FPSLogger());
        Debug.i(TAG, "mario gps in scene: " + mylat + " lon: " + mylon);
        Debug.i(TAG, "mario weather in scene: temp:" + mytemp + " icon: " + myicon + " weather: " + myweather);
        final Scene mScene = new Scene();
        final AutoParallaxBackground autoScrollBackground = new AutoParallaxBackground(0, 0, 0, 5);



        mScene.registerUpdateHandler(new TimerHandler(10.0f, true, new ITimerCallback() {


            @Override
            public void onTimePassed(final TimerHandler pTimerHandler) {

                Debug.i(TAG, "mario gps in onupdate: " + mylat + " lon: " + mylon);
                Debug.i(TAG, "mario weather in onupdate: temp:" + mytemp + " icon: " + myicon + " weather: " + myweather);

                //Debug.i(TAG, "mario mCurrentlocation in onupdate: " + mCurrentLocation);


                if (mCurrentLocation != null) {

                    GregorianCalendar dateTime = new GregorianCalendar();
                    AzimuthZenithAngle solarposition = PSA.calculateSolarPosition(dateTime, mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                    Debug.i(TAG, "mario PSA: " + solarposition.getZenithAngle());
                    AzimuthZenithAngle solarposition2 = SPA.calculateSolarPosition(dateTime, mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(),
                            190, // elevation
                            67, // delta T
                            1010, // avg. air pressure
                            11); // avg. air temperature
                    Debug.i(TAG, "mario SPA (more acc but slow): " + solarposition2.getZenithAngle());
                    ZENITH = solarposition2.getZenithAngle();


                    if (ZENITH < 90.8) {
                        Debug.i(TAG, "mario day");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.skyRegion, wallpaper.this.getVertexBufferObjectManager())));
                    }

                    if (ZENITH >= 90.8 && ZENITH < 96) {
                        Debug.i(TAG, "mario civil");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.civilRegion, wallpaper.this.getVertexBufferObjectManager())));
                    }

                    if (ZENITH >= 96 && ZENITH < 102) {
                        Debug.i(TAG, "mario nautical");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.nauticalRegion, wallpaper.this.getVertexBufferObjectManager())));
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, -341, wallpaper.this.starsnauticalRegion, wallpaper.this.getVertexBufferObjectManager())));
                    }

                    if (ZENITH >= 102 && ZENITH < 108) {
                        Debug.i(TAG, "mario astronomical");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.astronomicalRegion, wallpaper.this.getVertexBufferObjectManager())));
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, -341, wallpaper.this.starsRegion, wallpaper.this.getVertexBufferObjectManager())));

                    }
                    if (ZENITH >= 108) {
                        Debug.i(TAG, "mario night");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.nightRegion, wallpaper.this.getVertexBufferObjectManager())));
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, -341, wallpaper.this.starsRegion, wallpaper.this.getVertexBufferObjectManager())));
                    }


                    //rain
                    Pattern rainpattern = Pattern.compile("(.*?)(Rain)(.*?)");
                    Matcher getrain = rainpattern.matcher(myweather);
                    final BatchedPseudoSpriteParticleSystem rain1particle = new BatchedPseudoSpriteParticleSystem(new RectangleParticleEmitter(CAMERA_WIDTH / 2, -100, CAMERA_WIDTH, 1), 6, 10, 500, rain1Region, wallpaper.this.getVertexBufferObjectManager());
                    rain1particle.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
                    rain1particle.addParticleInitializer(new VelocityParticleInitializer(0, 0, 60, 90));
                    rain1particle.addParticleInitializer(new AccelerationParticleInitializer(0, 103));
                    ///particleSystem.addParticleInitializer(new RotationParticleInitializer(0.0f, 360.0f));
                    rain1particle.addParticleInitializer(new ExpireParticleInitializer<Entity>(11.5f));
                    //enlarge bit
                    rain1particle.addParticleInitializer(new ScaleParticleInitializer<Entity>(3.0f, 3.0f));


                    if (getrain.find()) {
                        Debug.i(TAG, "mario raining...");
                        mScene.attachChild(rain1particle);
                    } else {
                        Debug.i(TAG, "mario stopped raining...");
                        /*
                        BatchedPseudoSpriteParticleSystem rain1particle = new BatchedPseudoSpriteParticleSystem(new RectangleParticleEmitter(CAMERA_WIDTH / 2, -100, CAMERA_WIDTH, 1), 6, 10, 500, rain1Region, wallpaper.this.getVertexBufferObjectManager());
                        rain1particle.removeParticleInitializer(new VelocityParticleInitializer(0, 0, 60, 90));
                        rain1particle.removeParticleInitializer(new AccelerationParticleInitializer(0, 103));
                        ///particleSystem.addParticleInitializer(new RotationParticleInitializer(0.0f, 360.0f));
                        rain1particle.removeParticleInitializer(new ExpireParticleInitializer<Entity>(11.5f));
                        //enlarge bit
                        rain1particle.removeParticleInitializer(new ScaleParticleInitializer<Entity>(3.0f, 3.0f));
                        */
                        //rain1particle.setVisible(false);
                        //rain1particle.setIgnoreUpdate(true);

                        //mScene.attachChild(rain1particle);
                        //mScene.detachChild(rain1particle);
                        //rain1particle.dispose();
                        //rain1particle = null;
                        ///mScene.detachChild(rain1particle);

                        final EngineLock engineLock = mEngine.getEngineLock();
                        engineLock.lock();
                        mScene.detachChild(rain1particle);
                        rain1particle.detachSelf();
                        rain1particle.dispose();
                        //rain1particle = null;
                        engineLock.unlock();



                        mEngine.runOnUpdateThread(new Runnable() {
                            @Override
                                public void run () {
                                Debug.i(TAG, "trying to stop raining...");
                                    mScene.detachChild(rain1particle);
                                }
                        });


                    }



                }


            }

        }));









        //autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, this.skyRegion, this.getVertexBufferObjectManager())));
        ///autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, 80, this.mParallaxLayerMid, this.getVertexBufferObjectManager())));
        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-10.0f, new Sprite(0, CAMERA_HEIGHT - this.groundRegion.getHeight(), this.groundRegion, this.getVertexBufferObjectManager())));
        mScene.setBackground(autoScrollBackground);

        pOnCreateSceneCallback.onCreateSceneFinished(mScene);


    }

    @Override
    public void onPopulateScene(Scene pScene, OnPopulateSceneCallback pOnPopulateSceneCallback) {
        pOnPopulateSceneCallback.onPopulateSceneFinished();
    }

    @Override
    public void onSurfaceChanged(final GLState pGLState, final int pWidth, final int pHeight) {
        super.onSurfaceChanged(pGLState, pWidth, pHeight);

        if ((mEngine.getEngineOptions().getScreenOrientation() == ScreenOrientation.PORTRAIT_FIXED && pWidth > pHeight) ||
                (mEngine.getEngineOptions().getScreenOrientation() == ScreenOrientation.LANDSCAPE_FIXED && pHeight > pWidth)) {
            mEngine.getScene().setRotation(90f);
        } else {
            mEngine.getScene().setRotation(0f);
        }
    }

    @Override
    public Engine onCreateEngine() {
        return new MyBaseWallpaperGLEngine(this);
    }


    protected class MyBaseWallpaperGLEngine extends GLEngine {

        private EngineRenderer mEngineRenderer;
        private ConfigChooser mConfigChooser;


        public MyBaseWallpaperGLEngine(final IRendererListener pRendererListener) {

            if (this.mConfigChooser == null) {
                wallpaper.this.mEngine.getEngineOptions().getRenderOptions().setMultiSampling(false);
                this.mConfigChooser = new ConfigChooser(wallpaper.this.mEngine.getEngineOptions().getRenderOptions().isMultiSampling());
                //wallpaper.this.mEngine.getEngineOptions().getRenderOptions().setDithering(false);
                //this.mConfigChooser = new ConfigChooser(wallpaper.this.mEngine.getEngineOptions().getRenderOptions().getConfigChooserOptions());
            }
            this.setEGLConfigChooser(this.mConfigChooser);

            this.mEngineRenderer = new EngineRenderer(wallpaper.this.mEngine, this.mConfigChooser, pRendererListener);
            this.setRenderer(this.mEngineRenderer);
            this.setRenderMode(GLEngine.RENDERMODE_CONTINUOUSLY);
        }

        @Override
        public Bundle onCommand(final String pAction, final int pX,
                                final int pY, final int pZ, final Bundle pExtras,
                                final boolean pResultRequested) {
            if (pAction.equals(WallpaperManager.COMMAND_TAP)) {
                // LiveWallpaperService.this.onTap(pX, pY);
            } else if (pAction.equals(WallpaperManager.COMMAND_DROP)) {
                // LiveWallpaperService.this.onDrop(pX, pY);
            }

            return super.onCommand(pAction, pX, pY, pZ, pExtras,
                    pResultRequested);
        }

        @Override
        public void onResume() {
            super.onResume();
            wallpaper.this.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
            wallpaper.this.onPause();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            this.mEngineRenderer = null;
        }
    }

}
