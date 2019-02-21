package com.ipo.advert.client.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.ipo.advert.client.MainActivity;
import com.ipo.advert.client.R;
import com.ipo.advert.client.app.Config;
import com.ipo.advert.client.helper.BaseApiService;
import com.ipo.advert.client.helper.Constants;
import com.ipo.advert.client.helper.SessionManager;
import com.ipo.advert.client.helper.Utils;
import com.ipo.advert.client.helper.UtilsApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

public class LocationUpdatesService extends Service {

    private static final String PACKAGE_NAME = "com.ipo.advert.locationupdatesforegroundservice";
    private static final String TAG = LocationUpdatesService.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();

    private static final int ONE_MINUTES = 1000 * 60;

    /**
     * The name of the channel for notifications.
     */
    private static final String CHANNEL_ID = "channel_01";
    public static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";
    public static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
    public static final String EXTRA_DISTANCE = PACKAGE_NAME + ".distance";
    public static final String EXTRA_SPEED = PACKAGE_NAME + ".speed";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME + ".started_from_notification";

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000 * 10;

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * The identifier for the notification displayed for the foreground service.
     */
    private static final int NOTIFICATION_ID = 12345678;

    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */
    private boolean mChangingConfiguration = false;

    private NotificationManager mNotificationManager;

    private LocationRequest mLocationRequest;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Callback for changes in location.
     */
    private LocationCallback mLocationCallback;

    private Handler mServiceHandler;

    private PendingIntent mPendingIntent;
    private ActivityRecognitionClient mActivityRecognitionClient;
    BroadcastReceiver broadcastReceiver;
    boolean isActiveAndAccurate = false;
    /**
     * The current location.
     */
    private Location mLocation;
    private Location lStart;
    private Location prevBestLoc;
    private BaseApiService mApiService;
    private double distance = 0;
    private SessionManager sessionManager;

    public LocationUpdatesService() {

    }

    @Override
    public void onCreate() {

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY)) {
                    int type = intent.getIntExtra("type", -1);
                    int confidence = intent.getIntExtra("confidence", 0);
                    isActiveAndAccurate = handleUserActivity(type, confidence);
                }
            }
        };

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        mApiService = UtilsApi.getAPIService();
        sessionManager = new SessionManager(this);

        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");
        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);

        // We got here because the user decided to remove location updates from the notification.
        if (startedFromNotification) {
            removeLocationUpdates();
            stopSelf();
        }
        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "in onBind()");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()");
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Last client unbound from service");

        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (!mChangingConfiguration && Utils.requestingLocationUpdates(this)) {
            Log.i(TAG, "Starting foreground service");
            /*
            // TODO(developer). If targeting O, use the following code.
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                mNotificationManager.startServiceInForeground(new Intent(this,
                        LocationUpdatesService.class), NOTIFICATION_ID, getNotification());
            } else {
                startForeground(NOTIFICATION_ID, getNotification());
            }
             */
            startForeground(NOTIFICATION_ID, getNotification());
        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onDestroy() {
        removeActivityUpdatesButtonHandler();
        mServiceHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void requestLocationUpdates() {
        //getDrvProgress(sessionManager.getIdvacant());
        Log.i(TAG, "Requesting location updates");
        Utils.setRequestingLocationUpdates(this, true);
        startService(new Intent(getApplicationContext(), LocationUpdatesService.class));
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            Utils.setRequestingLocationUpdates(this, false);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            Utils.setRequestingLocationUpdates(this, false);
            stopSelf();
            saveDailyProg(sessionManager.getIdvacant(), distance);
            distance = 0;
        } catch (SecurityException unlikely) {
            Utils.setRequestingLocationUpdates(this, true);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification() {
        Intent intent = new Intent(this, LocationUpdatesService.class);
        CharSequence text;
        if (Double.parseDouble(sessionManager.getTarDrv())<= Double.parseDouble(sessionManager.getProgress()) +distance){
            sessionManager.setJobStatus(true);
            removeLocationUpdates();
            text = "Job selesai";
        } else {
            text = "Jarak tempuh : " + new DecimalFormat("#.###").format(distance) + " Km";
        }

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // The PendingIntent to launch activity.
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_launch_black_24dp, getString(R.string.launch_activity),
                        activityPendingIntent)
                .addAction(R.drawable.ic_cancel_black_24dp, getString(R.string.hentikan),
                        servicePendingIntent)
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setContentTitle(Utils.getLocationTitle(this))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        return builder.build();
    }

    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                            } else {
                                Log.w(TAG, "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }



    private void onNewLocation(Location location) {
        Log.i(TAG, "New location: " + location);

        double speed = location.getSpeed() * 18 / 5;

        Intent i = new Intent(ACTION_BROADCAST);
        i.putExtra(EXTRA_SPEED, speed);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);

        if (isBetterLocation(location, prevBestLoc) && isActiveAndAccurate){
            mLocation = location;

            Location lEnd;
            if (lStart == null) {
                lStart = mLocation;
                lEnd = mLocation;
            } else{
                lEnd = mLocation;
            }

            prevBestLoc = location;
            //Calling the method below updates the  live values of distance and tvSpeed to the TextViews.
            logRoute(sessionManager.getIdvacant(), location.getLatitude(), location.getLongitude(), Utils.getLocationAddress(location, this));
            distance = distance + (lStart.distanceTo(lEnd) / 1000.00);

            // Notify anyone listening for broadcasts about the new location.
            Intent intent = new Intent(ACTION_BROADCAST);
            intent.putExtra(EXTRA_LOCATION, location);
            intent.putExtra(EXTRA_DISTANCE, distance);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            lStart = lEnd;
        }

        // Update notification content if running as a foreground service.
        if (serviceIsRunningInForeground(this)) {
            mNotificationManager.notify(NOTIFICATION_ID, getNotification());
        }
    }

    /**
     * Sets the location request parameters.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mActivityRecognitionClient = new ActivityRecognitionClient(this);
        Intent mIntentService = new Intent(this, DetectedActivitiesIntentService.class);
        mPendingIntent = PendingIntent.getService(this, 1, mIntentService, PendingIntent.FLAG_UPDATE_CURRENT);
        requestActivityUpdatesButtonHandler();
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public LocationUpdatesService getService() {
            return LocationUpdatesService.this;
        }
    }

    /**
     * Returns true if this is a foreground service.
     *
     * @param context The {@link Context}.
     */
    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

    private void logRoute(String idVacant, double latitude, double longitude, String address){
        mApiService.saveLogRoute(idVacant, latitude, longitude, address)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()){
                            try {
                                assert response.body() != null;
                                JSONObject jObj = new JSONObject(response.body().string());
                                if (jObj.getString("error").equals("false")){
                                    Log.i(TAG, "Location sent");
                                } else {
                                    String errorMsg = jObj.getString("error_msg");
                                    Log.e(TAG, "Location failed to send" + errorMsg);
                                }
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.e(TAG, "Tidak bisa mengirim lokasi... Cek koneksi anda!");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        Log.e(TAG, "Gagal menyimpan log route : ERROR > " + t.toString());
                    }
                });
    }

    private void saveDailyProg(String idVacant, double progress){
        mApiService.saveDailyProgress(idVacant, progress)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()){
                            try {
                                assert response.body() != null;
                                JSONObject jObj = new JSONObject(response.body().string());
                                if (jObj.getString("error").equals("false")){
                                    Log.i(TAG, "Daily progress tersimpan");
                                } else {
                                    String errorMsg = jObj.getString("error_msg");
                                    Log.e(TAG, "Daily progress gagal tersimpan" + errorMsg);
                                }
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.e(TAG, "Tidak bisa menyimpan daily progress cek koneksi anda.");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        Log.e(TAG, "Gagal menyimpan daily progress : ERROR > " + t.toString());
                    }
                });
    }

    private void getDrvProgress(String idVacant){
        mApiService.getDrvProgress(idVacant)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()){
                            Log.i(TAG, "Progress driver response : " + response);
                            try {
                                JSONObject jObj = new JSONObject(response.body().string());
                                double prog = jObj.getDouble(Config.TAG_PROGRESS_DRV);
                                if (jObj.getString("error").equals("false")){
                                } else {
                                    String errorMsg = jObj.getString("error_msg");
                                    Log.e(TAG, "Gagal mengambil progress driver :" + errorMsg);
                                }
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.e(TAG, "Tidak bisa mengambil progrss driver... Cek koneksi anda!");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        Log.e(TAG, "Gagal mengambil progress driver : ERROR > " + t.toString());
                    }
                });
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > ONE_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -ONE_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 1;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        return isMoreAccurate || isNewer && !isLessAccurate || isNewer && !isSignificantlyLessAccurate && isFromSameProvider;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private boolean handleUserActivity(int type, int confidence) {
        boolean isDriverActive = false;
        boolean isAccurate = false;
        String label = getString(R.string.activity_unknown);
        switch (type) {
            case DetectedActivity.IN_VEHICLE: {
                label = getString(R.string.activity_in_vehicle);
                isDriverActive = true;
                Toast.makeText(getApplicationContext(), label, Toast.LENGTH_SHORT).show();
                break;
            }
            case DetectedActivity.ON_BICYCLE: {
                label = getString(R.string.activity_on_bicycle);
                isDriverActive = true;
                Toast.makeText(getApplicationContext(), label, Toast.LENGTH_SHORT).show();
                break;
            }
            case DetectedActivity.ON_FOOT: {
                label = getString(R.string.activity_on_foot);
                isDriverActive = false;
                Toast.makeText(getApplicationContext(), label, Toast.LENGTH_SHORT).show();
                break;
            }
            case DetectedActivity.RUNNING: {
                label = getString(R.string.activity_running);
                isDriverActive = false;
                Toast.makeText(getApplicationContext(), label, Toast.LENGTH_SHORT).show();
                break;
            }
            case DetectedActivity.STILL: {
                label = getString(R.string.activity_still);
                isDriverActive = false;
                Toast.makeText(getApplicationContext(), label, Toast.LENGTH_SHORT).show();
                break;
            }
            case DetectedActivity.TILTING: {
                label = getString(R.string.activity_tilting);
                isDriverActive = false;
                Toast.makeText(getApplicationContext(), label, Toast.LENGTH_SHORT).show();
                break;
            }
            case DetectedActivity.WALKING: {
                label = getString(R.string.activity_walking);
                isDriverActive = false;
                Toast.makeText(getApplicationContext(), label, Toast.LENGTH_SHORT).show();
                break;
            }
            case DetectedActivity.UNKNOWN: {
                label = getString(R.string.activity_unknown);
                break;
            }
        }

        Log.e(TAG, "User activity: " + label + ", Confidence: " + confidence);

        if (confidence > Constants.CONFIDENCE) {
            isAccurate = true;
        }
        return isDriverActive && isAccurate;
    }

    public void requestActivityUpdatesButtonHandler() {
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                mPendingIntent);

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(getApplicationContext(), "Successfully requested activity updates", Toast.LENGTH_SHORT).show();
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Requesting activity updates failed to start", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void removeActivityUpdatesButtonHandler() {
        Task<Void> task = mActivityRecognitionClient.removeActivityUpdates(
                mPendingIntent);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(getApplicationContext(),
                        "Removed activity updates successfully!",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Failed to remove activity updates!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

}