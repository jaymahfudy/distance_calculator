package com.ipo.advert.client;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.ipo.advert.client.helper.BaseApiService;
import com.ipo.advert.client.helper.SessionManager;
import com.ipo.advert.client.helper.UtilsApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

public class UbahSandiActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = UbahSandiActivity.class.getSimpleName();
    SessionManager sessionManager;
    AppCompatButton btnEditPass;
    TextInputEditText inputOldPass, inputNewPass, inputConfNewPass;
    ProgressDialog loading;
    BaseApiService mApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ubah_sandi);

        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        sessionManager = new SessionManager(this);
        mApiService = UtilsApi.getAPIService();

        inputOldPass = findViewById(R.id.input_old_pass);
        inputNewPass = findViewById(R.id.input_new_pass);
        inputConfNewPass = findViewById(R.id.input_new_pass_confirmation);
        btnEditPass = findViewById(R.id.btn_edit_pass);
        btnEditPass.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == btnEditPass){
            String oldPass = inputOldPass.getText().toString().trim();
            String newPass = inputNewPass.getText().toString().trim();
            String newPassConf = inputConfNewPass.getText().toString().trim();
            if (oldPass.isEmpty()){inputOldPass.setError(Html.fromHtml("<font color='red'>Mohon isi bagian ini.</font>"));}
            else if (newPass.isEmpty()){inputNewPass.setError(Html.fromHtml("<font color='red'>Mohon isi bagian ini.</font>"));}
            else if (newPassConf.isEmpty()){inputConfNewPass.setError(Html.fromHtml("<font color='red'>Mohon isi bagian ini.</font>"));}
            else if (inputNewPass.getText().toString().equals(inputConfNewPass.getText().toString())){
                showSpinner();
                changePass();
            }
            else {
                inputConfNewPass.setError(Html.fromHtml("<font color='red'>Isi tidak sesuai dengan Kata Sandi Baru.</font>"));
            }
        }
    }

    private void changePass(){
        mApiService.changePassRequest(sessionManager.getEmail(), inputOldPass.getText().toString(), inputNewPass.getText().toString(), inputConfNewPass.getText().toString())
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()){
                            loading.dismiss();
                            try {
                                JSONObject jObj = new JSONObject(response.body().string());
                                if (jObj.getString("error").equals("false")){
                                    String errorMsg = jObj.getString("error_msg");
                                    alertDialog(errorMsg);
                                } else {
                                    String errorMsg = jObj.getString("error_msg");
                                    alertDialog(errorMsg);
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

    private void showSpinner() {
        loading = ProgressDialog.show(this, "Mengubah kata sandi", "Harap Tunggu...", true, false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        toMainActivity();
    }

    private void alertDialog(String messages){
        AlertDialog.Builder builder = new AlertDialog.Builder(UbahSandiActivity.this);
        builder.setTitle("Info.");
        builder.setMessage(messages);
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

    private void toMainActivity(){
        Intent i = new Intent(UbahSandiActivity.this, MainActivity.class);
        startActivity(i);
    }
}