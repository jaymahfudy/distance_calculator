package com.ipo.advert.client;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import com.ipo.advert.client.app.Config;
import com.ipo.advert.client.helper.BaseApiService;
import com.ipo.advert.client.helper.SessionManager;
import com.ipo.advert.client.helper.UtilsApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();
    TextInputEditText inputEmail, inputPasword;
    AppCompatButton btnLogin, btnRegis;
    ProgressDialog loading;
    SessionManager session;
    Context mContext;
    BaseApiService mApiService;
    String androidId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        reqPermission();
        androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.i(TAG, androidId);
        session = new SessionManager(this);
        if (session.isLoggedIn()) { startMainActivity(); }
        mContext = this;
        mApiService = UtilsApi.getAPIService();
        initComponents();
    }

    private void initComponents(){
        inputPasword = findViewById(R.id.input_password);
        inputEmail = findViewById(R.id.input_email);
        btnLogin = findViewById(R.id.btn_login);
        btnRegis = findViewById(R.id.btn_daftar);
        btnLogin.setOnClickListener(btnLoginListener);
        btnRegis.setOnClickListener(btnRegisListener);
    }

    private void requestLogin(){
        mApiService.loginRequest(inputEmail.getText().toString(), inputPasword.getText().toString(), androidId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()){
                            loading.dismiss();
                            try {
                                JSONObject jObj = new JSONObject(response.body().string());
                                if (jObj.getString("error").equals("false")){
                                    Log.i(TAG, "Login berhasil");
                                    String idDriver = jObj.getString(Config.TAG_ID_DRV);
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

                                    session.setIdDriver(idDriver);
                                    session.setEmail(email);
                                    session.setPassword(inputPasword.getText().toString().trim());
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
                                    session.setAndroidId(androidId);
                                    session.setLogin(true);
                                    startMainActivity();
                                } else {
                                    String errorMsg = jObj.getString("error_msg");
                                    alertDialog(errorMsg);
                                    Log.i(TAG, errorMsg);
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
                        Log.e(TAG, "onFailure Login: ERROR > " + t.toString());
                        loading.dismiss();
                    }
                });
    }

    private View.OnClickListener btnLoginListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String email, pass;
            email = inputEmail.getText().toString().trim();
            pass = inputPasword.getText().toString().trim();
            if(!email.isEmpty() && !pass.isEmpty() && isEmailValid(email)){
                showSpinner();
                requestLogin();
            }
            else if(email.isEmpty()){
                inputEmail.setError("Mohon masukkan email anda");
            }
            else if(pass.isEmpty()){
                inputPasword.setError("Mohon masukkan kata sandi anda");
            }
            else if(!isEmailValid(email)){
                inputEmail.setError("Email tidak valid");
            }
        }
    };

    private View.OnClickListener btnRegisListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(LoginActivity.this, RegisActivity.class));
            finish();
        }
    };

    private void reqPermission(){
        if (!(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void startMainActivity(){
        Intent intent = new Intent(LoginActivity.this, InitJobActivity.class);
        startActivity(intent);
        finish();
    }

    private void showSpinner() {
        loading = ProgressDialog.show(mContext, "Mengecek email dan kata Sandi", "Harap Tunggu...", true, false);
    }

    private void alertDialog(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("Info.");
        builder.setMessage(message);
        String positiveText = getString(android.R.string.ok);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean isEmailValid(String email){
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
