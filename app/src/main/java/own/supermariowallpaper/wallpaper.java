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
import org.andengine.util.debug.Debug;

import android.app.WallpaperManager;
import android.os.Bundle;
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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import net.e175.klaus.solarpositioning.*;

public class wallpaper extends BaseLiveWallpaperService implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private static final String TAG = "mario wallpaper";
    private GoogleApiClient GoogleApiClient;
    public static final long GPSUPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = GPSUPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private Boolean mRequestingUpdates;
    double mylat = 0.0;
    double mylon = 0.0;
    double ZENITH = 0.0;

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
        mLocationRequest.setInterval(GPSUPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(GoogleApiClient, mLocationRequest, this);
        mRequestingUpdates = true;
        //Toast.makeText(this, "GPS update started", Toast.LENGTH_SHORT).show();

    }

    private void stopUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(GoogleApiClient, this);
        mRequestingUpdates = false;
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
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(GoogleApiClient);
            mylat = mCurrentLocation.getLatitude();
            mylon = mCurrentLocation.getLongitude();
            //make sure to setup latlon with weather url
            //String finalurl = url + mylat + "," + mylon + ".json";
            Debug.i(TAG, "mario onConnected lat: " + mylat + " lon: " + mylon);
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

    @Override
    public void onCreate() {
        ////gps stuff
        //start GPS
        initializeGoogleAPI();
        GoogleApiClient.connect();
        mRequestingUpdates = true;
        Debug.i(TAG, "mario gps started: " + mylat + " lon: " + mylon);
        super.onCreate();
    }

    private int CAMERA_WIDTH = 600;
    private int CAMERA_HEIGHT = 800;

    private Camera mCamera;
    private Scene mScene;

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
        this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
        ///return new EngineOptions(true, ScreenOrientation.PORTRAIT_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera);
        return new EngineOptions(true, ScreenOrientation.PORTRAIT_FIXED, new FillResolutionPolicy(), this.mCamera);

    }

    @Override
    public void onCreateResources(OnCreateResourcesCallback pOnCreateResourcesCallback) {
        Debug.i(TAG, "mario gps in res: " + mylat + " lon: " + mylon);
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
        mScene = new Scene();
        final AutoParallaxBackground autoScrollBackground = new AutoParallaxBackground(0, 0, 0, 5);


        mScene.registerUpdateHandler(new TimerHandler(10.0f, true, new ITimerCallback() {


            @Override
            public void onTimePassed(final TimerHandler pTimerHandler) {

                Debug.i(TAG, "mario gps in onupdate: " + mylat + " lon: " + mylon);
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




                    /*
                    //String sunrisetime;
                    //String sunsettime;

                    //final String tz = Time.getCurrentTimezone();
                    //final SunCalculator calculator = new SunCalculator(mCurrentLocation, tz);
                    //final Calendar now = Calendar.getInstance();

                    final double sunrisea = calculator.computeSunriseTime(SunCalculator.ZENITH_ASTRONOMICAL, now);
                    astronomicalrise = SunCalculator.timeToDayFraction(sunrisea);

                    final double sunrisen = calculator.computeSunriseTime(SunCalculator.ZENITH_NAUTICAL, now);
                    nauticalrise = SunCalculator.timeToDayFraction(sunrisen);

                    final double sunrisec = calculator.computeSunriseTime(SunCalculator.ZENITH_CIVIL, now);
                    civilrise = SunCalculator.timeToDayFraction(sunrisec);

                    final double sunriseo = calculator.computeSunriseTime(SunCalculator.ZENITH_OFFICIAL, now);
                    sunrise = SunCalculator.timeToDayFraction(sunriseo);

                    final double sunseto = calculator.computeSunsetTime(SunCalculator.ZENITH_OFFICIAL, now);
                    sunset = SunCalculator.timeToDayFraction(sunseto);

                    final double sunsetc = calculator.computeSunsetTime(SunCalculator.ZENITH_CIVIL, now);
                    civilset = SunCalculator.timeToDayFraction(sunsetc);

                    final double sunsetn = calculator.computeSunsetTime(SunCalculator.ZENITH_NAUTICAL, now);
                    nauticalset = SunCalculator.timeToDayFraction(sunsetn);

                    final double sunseta = calculator.computeSunsetTime(SunCalculator.ZENITH_ASTRONOMICAL, now);
                    astronomicalset = SunCalculator.timeToDayFraction(sunseta);


                    int currenthr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    int currentmin = Calendar.getInstance().get(Calendar.MINUTE);
                    currenttimeinfraction = timetofraction(currenthr, currentmin);

                    sunrisetime = SunCalculator.timeToString(sunriseo);
                    sunsettime = SunCalculator.timeToString(sunseto);
                    Debug.i(TAG, "mario current time: " + currenthr + ":" + currentmin);
                    //Debug.i(TAG, "mario sunrise/set in fraction: " + sunrise + "  " + sunset);
                    Debug.i(TAG, "mario sunrise/set: " + sunrisetime + "  " + sunsettime);
                    Debug.i(TAG, "mario current time in fraction: " + currenttimeinfraction);
                    Debug.i(TAG, "mario astronomicalrise in fraction: " + astronomicalrise);
                    Debug.i(TAG, "mario nauticalrise in fraction: " + nauticalrise);
                    Debug.i(TAG, "mario civilrise in fraction: " + civilrise);
                    Debug.i(TAG, "mario sunrise/set in fraction: " + sunrise + "  " + sunset);
                    Debug.i(TAG, "mario civilset in fraction: " + civilset);
                    Debug.i(TAG, "mario nauticalset in fraction: " + nauticalset);
                    Debug.i(TAG, "mario astronomicalset in fraction: " + astronomicalset);


                    if (sunrise <= currenttimeinfraction && sunset >= currenttimeinfraction) {
                        Debug.i(TAG, "mario day");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.skyRegion, wallpaper.this.getVertexBufferObjectManager())));
                    }
                    //else {
                    //    Debug.i(TAG, "mario night");
                    //    autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.nightRegion, wallpaper.this.getVertexBufferObjectManager())));
                    //    autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, -341, wallpaper.this.starsRegion, wallpaper.this.getVertexBufferObjectManager())));
                    //}

                    if (astronomicalrise <= currenttimeinfraction && nauticalrise >= currenttimeinfraction) {
                        Debug.i(TAG, "mario astronomicalrise");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.astronomicalRegion, wallpaper.this.getVertexBufferObjectManager())));
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, -341, wallpaper.this.starsRegion, wallpaper.this.getVertexBufferObjectManager())));

                    }

                    if (nauticalrise <= currenttimeinfraction && civilrise >= currenttimeinfraction) {
                        Debug.i(TAG, "mario nauticalrise");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.nauticalRegion, wallpaper.this.getVertexBufferObjectManager())));
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, -341, wallpaper.this.starsnauticalRegion, wallpaper.this.getVertexBufferObjectManager())));

                    }

                    if (civilrise <= currenttimeinfraction && sunrise >= currenttimeinfraction) {
                        Debug.i(TAG, "mario civilrise");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.civilRegion, wallpaper.this.getVertexBufferObjectManager())));
                        //autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, -341, wallpaper.this.starsRegion, wallpaper.this.getVertexBufferObjectManager())));

                    }


                    if (sunset <= currenttimeinfraction && civilset >= currenttimeinfraction) {
                        Debug.i(TAG, "mario civilset");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.civilRegion, wallpaper.this.getVertexBufferObjectManager())));
                        //autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, -341, wallpaper.this.starsRegion, wallpaper.this.getVertexBufferObjectManager())));
                    }

                    if (civilset <= currenttimeinfraction && nauticalset >= currenttimeinfraction) {
                        Debug.i(TAG, "mario nauticalset");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.nauticalRegion, wallpaper.this.getVertexBufferObjectManager())));
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, -341, wallpaper.this.starsnauticalRegion, wallpaper.this.getVertexBufferObjectManager())));

                    }

                    if (nauticalset <= currenttimeinfraction && astronomicalset >= currenttimeinfraction) {
                        Debug.i(TAG, "mario astronomicalset");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.astronomicalRegion, wallpaper.this.getVertexBufferObjectManager())));
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, -341, wallpaper.this.starsRegion, wallpaper.this.getVertexBufferObjectManager())));

                    }

                    if (astronomicalset <= currenttimeinfraction) {
                        Debug.i(TAG, "mario night");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.nightRegion, wallpaper.this.getVertexBufferObjectManager())));
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, -341, wallpaper.this.starsRegion, wallpaper.this.getVertexBufferObjectManager())));

                    }
                    if (currenttimeinfraction >= 0 && astronomicalrise >= currenttimeinfraction) {
                        Debug.i(TAG, "mario night");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.nightRegion, wallpaper.this.getVertexBufferObjectManager())));
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, -341, wallpaper.this.starsRegion, wallpaper.this.getVertexBufferObjectManager())));

                    }
                    */

                    /*
                    //sunset after midnight - common in alaska
                    if (sunset >= 0 && currenttimeinfraction <= 0) {
                        Debug.i(TAG, "mario day - before midnight sunset");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.skyRegion, wallpaper.this.getVertexBufferObjectManager())));
                    }
                    if (sunset >= 0 && currenttimeinfraction >= 0) {
                        Debug.i(TAG, "mario night - after midnight sunset");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.nightRegion, wallpaper.this.getVertexBufferObjectManager())));
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, -341, wallpaper.this.starsRegion, wallpaper.this.getVertexBufferObjectManager())));
                    }
                    */


                    /*
                    TimeZone timeZone = TimeZone.getTimeZone(now.getTimeZone().getID());
                    //SunriseSunset ss = new SunriseSunset(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), now.getTime(), timeZone.getOffset(now.getTimeInMillis()) / 1000 / 60 / 60);
                    SunriseSunset ss = new SunriseSunset(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), now.getTime(), 0);
                    Debug.i(TAG, "mario - sun up: " + ss.isSunUp() + " sun down: " + ss.isSunDown());

                    if (ss.isSunUp()) {
                        Debug.i(TAG, "mario day - sun is up all day");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.skyRegion, wallpaper.this.getVertexBufferObjectManager())));
                    }

                    if (ss.isSunDown()) {
                        Debug.i(TAG, "mario night - sun is down all day");
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(0.0f, new Sprite(0, -341, wallpaper.this.nightRegion, wallpaper.this.getVertexBufferObjectManager())));
                        autoScrollBackground.attachParallaxEntity(new ParallaxEntity(-5.0f, new Sprite(0, -341, wallpaper.this.starsRegion, wallpaper.this.getVertexBufferObjectManager())));
                    }
                    */



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

        if ( (mEngine.getEngineOptions().getScreenOrientation() == ScreenOrientation.PORTRAIT_FIXED && pWidth > pHeight) ||
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

double timetofraction(int hour, int minute) {
    double dechr = (float)hour/24;
    double decmin = (float)minute/60/24;
    double dectime = dechr+decmin;
    return dectime;
}

float timetofrac(int hour, int minute) {
    return (hour * 60 + minute) / 1440.0f;
}


}
