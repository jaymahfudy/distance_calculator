package com.ipo.advert.client;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.ipo.advert.client.helper.BaseApiService;
import com.ipo.advert.client.helper.UtilsApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;


public class RegisActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    static final String TAG = RegisActivity.class.getSimpleName();
    ProgressDialog loading;
    BaseApiService mApiService;
    TextInputEditText inputName, inputEmail, inputPass, inputRepass, inputAddress, inputSimNumber, inputNoKtp, inputNopol;
    AppCompatButton btnDaftar;
    Spinner spinGender;
    TextView tvLinkLogin;
    String gender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regis);
        mApiService = UtilsApi.getAPIService();

        inputName = findViewById(R.id.input_name);
        inputEmail = findViewById(R.id.input_email);
        inputPass = findViewById(R.id.input_password);
        inputRepass = findViewById(R.id.input_conf_password);
        inputAddress = findViewById(R.id.input_address);
        inputSimNumber = findViewById(R.id.input_sim_number);
        inputNoKtp = findViewById(R.id.input_ktp_number);
        inputNopol = findViewById(R.id.input_nopol);
        spinGender = findViewById(R.id.spinner_gender);
        btnDaftar = findViewById(R.id.btn_daftar);
        tvLinkLogin = findViewById(R.id.link_login);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.gender_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinGender.setAdapter(adapter);
        spinGender.setOnItemSelectedListener(this);
        btnDaftar.setOnClickListener(btnRegisterListener);
        tvLinkLogin.setOnClickListener(tvLinkLoginListener);
    }

    private void registerDriver(String name, String email, String pass, String address, String gender, String simNumber, String noKtp, String nopol){
        mApiService.registerRequest(name, email, pass, address, gender, simNumber, noKtp, nopol)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()){
                            Log.d(TAG, "Register Response: " + response);
                            loading.dismiss();
                            try {
                                JSONObject jObj = new JSONObject(response.body().string());
                                if (jObj.getString("error").equals("false")){
                                    alertDialog("Pendaftaran berhasil.","Silahkan cek email anda untuk mengaktifkan akun anda.",true);
                                } else {
                                    String errorMsg = jObj.getString("error_msg");
                                    alertDialog("Info.",errorMsg,false);
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

    private boolean isEmailValid(String email){
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void showSpinner() {
        loading = ProgressDialog.show(this, "Mengecek validasi data", "Harap Tunggu...", true, false);
    }

    private View.OnClickListener tvLinkLoginListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(RegisActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    };

    private View.OnClickListener btnRegisterListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String name = inputName.getText().toString().trim();
            String email = inputEmail.getText().toString().trim();
            String pass = inputPass.getText().toString().trim();
            String repass = inputRepass.getText().toString().trim();
            String address = inputAddress.getText().toString().trim();
            String nomorSim = inputSimNumber.getText().toString().trim();
            String noKtp = inputNoKtp.getText().toString().trim();
            String nopol = inputNopol.getText().toString().trim();

            if(name.isEmpty()){inputName.setError("Masukkan nama anda sesuai KTP");}
            else if(email.isEmpty()){inputEmail.setError("Masukkan email anda");}
            else if (pass.isEmpty()){inputPass.setError("Masukkan kata sandi anda");}
            else if (!pass.equals(repass)){
                inputRepass.setError("Passwordd yang diinputkan tidak sama");
                inputPass.setError("Passowrd yang diinputkan tidak sama");
            }
            else if(address.isEmpty()){inputAddress.setError("Masukkan alamat anda");}
            else if(nomorSim.isEmpty()){inputSimNumber.setError("Masukkan nomor SIM anda");}
            else if(noKtp.isEmpty()){inputAddress.setError("Masukkan kota kelahiran anda");}
            else if(nopol.isEmpty()){
                inputNopol.setError("Masukkan tanggal lahir anda");}
            else if(!isEmailValid(email)){inputEmail.setError("Email tidak valid");}
            else{
                showSpinner();
                registerDriver(name,email,pass,address,gender,nomorSim,noKtp,nopol);
            }
        }
    };

    private void alertDialog(String title, String message, final boolean success){
        AlertDialog.Builder builder = new AlertDialog.Builder(RegisActivity.this);
        builder.setTitle(title);
        builder.setMessage(message);
        String positiveText = getString(android.R.string.ok);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (success){
                            Intent i = new Intent(getApplicationContext(), LoginActivity.class);
                            startActivity(i);
                        }
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        gender = (position == 0) ? "L" : "P";
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
