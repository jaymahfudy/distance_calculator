package com.ipo.advert.client;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ipo.advert.client.app.Config;
import com.ipo.advert.client.helper.BaseApiService;
import com.ipo.advert.client.helper.SessionManager;
import com.ipo.advert.client.helper.Utils;
import com.ipo.advert.client.helper.UtilsApi;
import com.ipo.advert.client.service.LocationUpdatesService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    // Used in checking for runtime permissions.
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    // The BroadcastReceiver used to listen from broadcasts from the service.
    private MyReceiver myReceiver;
    // A reference to the service used to get location updates.
    private LocationUpdatesService mService = null;

    // UI elements.
    AppCompatButton btnStart, btnStop;
    TextView tvDistance, tvSpeed, tvTarget, tvAchievedTarget;
    BaseApiService mApiService;
    SessionManager session;
    LocationManager locationManager;
    ProgressDialog loading;

    // Tracks the bound state of the service.
    boolean mBound = false;
    double prog;
    double distance;

    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.profile){
            Intent i = new Intent(this, ProfilActivity.class);
            startActivity(i);
        } else if(id == R.id.change_pass){
            Intent i = new Intent(this, UbahSandiActivity.class);
            startActivity(i);
        } else if (id == R.id.change_id_job) {
            Intent i = new Intent(this, InitJobActivity.class);
            startActivity(i);
            session.setJobStatus(false);
            session.setJobInit(false);
            finish();
        } else if(id == R.id.logout){
            showSpinner();
            requestLogout();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myReceiver = new MyReceiver();
        mApiService = UtilsApi.getAPIService();
        session = new SessionManager(this);

        setContentView(R.layout.activity_main);

        android.support.v7.app.ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setTitle(upFirstLetter(session.getName()));

        // Check that the user hasn't revoked permissions by going to Settings.
        if (Utils.requestingLocationUpdates(this)) {
            if (!checkPermissions()) {
                requestPermissions();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        tvDistance = findViewById(R.id.distancetext);
        tvSpeed = findViewById(R.id.speedtext);
        tvTarget = findViewById(R.id.target);
        tvAchievedTarget = findViewById(R.id.achieved_target);

        Typeface tp1 = Typeface.createFromAsset(getAssets(),"fonts/JosefinSans-Bold.ttf");
        tvDistance.setTypeface(tp1);
        tvSpeed.setTypeface(tp1);
        tvTarget.setTypeface(tp1);
        tvAchievedTarget.setTypeface(tp1);

        tvTarget.setText(MessageFormat.format("Target :{0} Km", session.getTarDrv()));

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(session.isJobFinished()){
                    tvAchievedTarget.setText("Job selesai");
                    showToast("Job sudah selesai silahkan masukkan id job yang baru.");
                } else if (!checkPermissions()) {
                    requestPermissions();
                } else {
                    checkGps();
                    mService.requestLocationUpdates();
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.removeLocationUpdates();
            }
        });

        // Restore the state of the buttons when the activity (re)launches.
        setButtonsState(Utils.requestingLocationUpdates(this));

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(this, LocationUpdatesService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getDrvProgress(session.getIdvacant());
        getDataDriver(session.getEmail());
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver, new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection);
            mBound = false;
        }

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    /**
     * Returns the current state of the permissions needed.
     */
    public boolean checkPermissions() {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    findViewById(R.id.activity_main),
                    R.string.dasar_izin,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                mService.requestLocationUpdates();
            } else {
                // Permission denied.
                setButtonsState(false);
                Snackbar.make(
                        findViewById(R.id.activity_main),
                        R.string.izin_ditolak,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.pengaturan, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    /**
     * Receiver for broadcasts sent by {@link LocationUpdatesService}.
     */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
            double speed = intent.getDoubleExtra(LocationUpdatesService.EXTRA_SPEED, 0);
            distance = intent.getDoubleExtra(LocationUpdatesService.EXTRA_DISTANCE, 0);
            if (Double.parseDouble(session.getTarDrv())<= prog+distance){
                session.setJobStatus(true);
                mService.removeLocationUpdates();
                tvAchievedTarget.setText("Job selesai");
            }
            if (location != null) {
                if (speed > 0.0)
                    tvSpeed.setText(MessageFormat.format("Kecepatan: {0} km/jam", new DecimalFormat("#.###").format(speed)));
                else
                    tvSpeed.setText(".......");

                tvAchievedTarget.setText(MessageFormat.format("Progress : {0} Km", new DecimalFormat("#.###").format(prog + distance)));
                tvDistance.setText(MessageFormat.format("Jarak : {0} Km", new DecimalFormat("#.###").format(distance)));
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        // Update the buttons state depending on whether location updates are being requested.
        if (s.equals(Utils.KEY_REQUESTING_LOCATION_UPDATES)) {
            setButtonsState(sharedPreferences.getBoolean(Utils.KEY_REQUESTING_LOCATION_UPDATES,
                    false));
        }
    }

    private void setButtonsState(boolean requestingLocationUpdates) {
        if (requestingLocationUpdates) {
            btnStart.setVisibility(View.GONE);
            btnStop.setVisibility(View.VISIBLE);
        } else {
            btnStart.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.GONE);
        }
    }

    private void getDrvProgress(String idVacant){
        mApiService.getDrvProgress(idVacant)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()){
                            Log.i(TAG, "Driver progress response: " + response);
                            try {
                                JSONObject jObj = new JSONObject(response.body().string());
                                prog = jObj.getDouble(Config.TAG_PROGRESS_DRV);
                                if (jObj.getString("error").equals("false")){
                                    tvAchievedTarget.setText(MessageFormat.format("Progress : {0} Km", new DecimalFormat("#.###").format(prog+distance)));
                                    session.setProgress(jObj.getString(Config.TAG_PROGRESS_DRV));
                                } else {
                                    String errorMsg = jObj.getString("error_msg");
                                    Log.e(TAG, "Failed to get driver progress: " + errorMsg);
                                }
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.e(TAG, "Can't get driver progress please check connection: ");
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e(TAG, "onFailure get driver progress : ERROR > " + t.toString());
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    public void logout() {
        mService.removeLocationUpdates();
        session.setLogin(false);
        Intent i = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(i);
        finish();
    }

    void checkGps() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        assert locationManager != null;
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGPSDisabledAlertToUser();
        }
    }

    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Aktifkan GPS untuk menggunakan aplikasi")
                .setCancelable(false)
                .setPositiveButton("Aktifkan",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Batal",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void getDataDriver(String email){
        mApiService.getDataByEmail(email)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()){
                            Log.d(TAG, "Get data driver response: " + response);
                            try {
                                JSONObject jObj = new JSONObject(response.body().string());
                                String email = jObj.getString(Config.TAG_EMAIL);
                                String name = jObj.getString(Config.TAG_NAME);
                                String sex = jObj.getString(Config.TAG_GENDER);
                                String address = jObj.getString(Config.TAG_ADDRESS);
                                String phone1 = jObj.getString(Config.TAG_PHONE_1);
                                String phone2 = jObj.getString(Config.TAG_PHONE_2);
                                String noKTP = jObj.getString(Config.TAG_NO_KTP);
                                String imgKTP = jObj.getString(Config.TAG_IMG_KTP);
                                String noSim = jObj.getString(Config.TAG_NO_SIM);
                                String imgSim = jObj.getString(Config.TAG_IMG_SIM);
                                String nopol = jObj.getString(Config.TAG_NOPOL);
                                String imgStnk = jObj.getString(Config.TAG_IMG_STNK);
                                String imgDrv = jObj.getString(Config.TAG_IMG_DRV);
                                if (jObj.getString("error").equals("false") && (email != null && name!=null)){
                                    session.setEmail(email);
                                    session.setName(name);
                                    session.setSex(sex);
                                    session.setAddress(address);
                                    session.setPhone1(phone1);
                                    session.setPhone2(phone2);
                                    session.setNoKtp(noKTP);
                                    session.setImgKtp(imgKTP);
                                    session.setNoSim(noSim);
                                    session.setImgSim(imgSim);
                                    session.setNopol(nopol);
                                    session.setImgStnk(imgStnk);
                                    session.setImgDrv(imgDrv);
                                } else {
                                    String errorMsg = jObj.getString("error_msg");
                                    showToast(errorMsg);
                                }
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.e(TAG, "Gagal mengambil data driver: ");
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e(TAG, "onFailure get data driver: ERROR > " + t.toString());
                    }
                });
    }

    private void requestLogout(){
        mApiService.logout(session.getEmail(), session.getPassword(), session.getAndroidId())
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()){
                            loading.dismiss();
                            try {
                                JSONObject jObj = new JSONObject(response.body().string());
                                if (jObj.getString("error").equals("false")){
                                    String errorMsg = jObj.getString("error_msg");
                                    Log.i(TAG,errorMsg);
                                    logout();
                                } else {
                                    String errorMsg = jObj.getString("error_msg");
                                    Log.i(TAG,errorMsg);
                                }
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            loading.dismiss();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e(TAG, "onFailure ubah data: ERROR > " + t.toString());
                        loading.dismiss();
                    }
                });
    }

    private void showSpinner() {
        loading = ProgressDialog.show(this, "Menjalankan proses logout", "Harap Tunggu...", true, false);
    }

    private String upFirstLetter(String text){
        String[] strArray = text.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String s : strArray) {
            String cap = s.substring(0, 1).toUpperCase() + s.substring(1);
            builder.append(cap + " ");
        }
        return builder.toString();
    }
}
