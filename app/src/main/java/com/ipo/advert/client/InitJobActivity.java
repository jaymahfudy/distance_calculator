package com.ipo.advert.client;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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

public class InitJobActivity extends AppCompatActivity {
    private static final String TAG = InitJobActivity.class.getSimpleName();
    TextInputEditText inputIdJob;
    TextView tvInputkanId;
    AppCompatButton btnSubmitIdJob;
    ProgressDialog loading;
    SessionManager session;
    Context mContext;
    BaseApiService mApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init_job);
        session = new SessionManager(this);
        if (session.isJobInitiated()){
            startMainActivity();
        }
        mContext = this;
        mApiService = UtilsApi.getAPIService();
        initComponents();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        logout();
    }

    private void initComponents(){
        Typeface tp1 = Typeface.createFromAsset(getAssets(),"fonts/JosefinSans-Bold.ttf");
        inputIdJob = findViewById(R.id.input_id_job);
        tvInputkanId = findViewById(R.id.tv_inputkan_id);
        tvInputkanId.setTypeface(tp1);
        btnSubmitIdJob = findViewById(R.id.btn_submit_id_job);
        btnSubmitIdJob.setOnClickListener(btnSubmitIdListener);
    }

    private View.OnClickListener btnSubmitIdListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showSpinner();
            initJob();
        }
    };

    private void initJob(){
        mApiService.initJob(Integer.parseInt(inputIdJob.getText().toString().trim()), Integer.parseInt(session.getIdDriver()))
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()){
                            loading.dismiss();
                            try {
                                JSONObject jObj = new JSONObject(response.body().string());
                                if (jObj.getString("error").equals("false")){
                                    String idVacant = jObj.getString(Config.TAG_ID_VACANT);
                                    String target = jObj.getString(Config.TAG_TARGET);
                                    session.setJobId(inputIdJob.getText().toString());
                                    session.setIdVacant(idVacant);
                                    session.setTarDrv(target);
                                    session.setJobInit(true);
                                    startMainActivity();
                                } else {
                                    String errorMsg = jObj.getString("error_msg");
                                    Toast.makeText(mContext, errorMsg, Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            loading.dismiss();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        Log.e(TAG, "onFailure Init Job: ERROR > " + t.toString());
                        loading.dismiss();
                    }
                });
    }

    private void startMainActivity(){
        Intent intent = new Intent(InitJobActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void alertDialog(String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(InitJobActivity.this);
        builder.setTitle(title);
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

    private void showSpinner() {
        loading = ProgressDialog.show(mContext, "Mengecek id job.", "Harap Tunggu...", true, false);
    }

    public void logout() {
        session.setLogin(false);
        Intent i = new Intent(InitJobActivity.this, LoginActivity.class);
        startActivity(i);
        finish();
    }
}
