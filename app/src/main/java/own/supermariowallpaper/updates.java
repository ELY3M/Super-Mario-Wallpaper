package own.supermariowallpaper;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class updates extends Service {
    private static final String TAG = updates.class.getSimpleName();
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "updater started");
        OpenActivity.getMain().update();
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

}