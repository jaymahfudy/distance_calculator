package com.ipo.advert.client;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.ipo.advert.client.app.Config;
import com.ipo.advert.client.helper.BaseApiService;
import com.ipo.advert.client.helper.SessionManager;
import com.ipo.advert.client.helper.UtilsApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

public class UbahDataActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = UbahDataActivity.class.getSimpleName();
    Button btnChooseKtp,btnUploadKtp, btnChooseSim, btnUploadSim, btnChooseStnk, btnUploadStnk, btnEditData;
    TextInputEditText inputEmail, inputName, inputAddress, inputPhone1, inputPhone2, inputNoKtp, inputNoSim, inputNopol, inputDocKtp, inputDocSim, inputDocStnk;
    SessionManager session;
    BaseApiService mApiService;
    Bitmap bitmap;
    ProgressDialog loading;
    int PICK_IMAGE_REQUEST = 1;
    String param;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        toProfilActivity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ubah_data);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);

        session = new SessionManager(this);
        mApiService = UtilsApi.getAPIService();

        inputEmail = findViewById(R.id.input_edit_email);
        inputName = findViewById(R.id.input_edit_name);
        inputAddress = findViewById(R.id.input_edit_address);
        inputPhone1 = findViewById(R.id.input_edit_phone_1);
        inputPhone2 = findViewById(R.id.input_edit_phone_2);
        inputNoKtp = findViewById(R.id.input_edit_no_ktp);
        inputNoSim = findViewById(R.id.input_edit_no_sim);
        inputNopol = findViewById(R.id.input_edit_nopol);
        inputDocKtp = findViewById(R.id.input_doc_ktp);
        inputDocSim = findViewById(R.id.input_doc_sim);
        inputDocStnk = findViewById(R.id.input_doc_stnk);

        btnEditData = findViewById(R.id.btn_edit_data);
        btnChooseKtp = findViewById(R.id.btn_choose_ktp);
        btnUploadKtp = findViewById(R.id.btn_upload_ktp);
        btnChooseSim = findViewById(R.id.btn_choose_sim);
        btnUploadSim = findViewById(R.id.btn_upload_sim);
        btnChooseStnk = findViewById(R.id.btn_choose_stnk);
        btnUploadStnk = findViewById(R.id.btn_upload_stnk);

        inputEmail.requestFocus();

        inputEmail.setText(session.getEmail());
        inputName.setText(session.getName());
        inputAddress.setText(session.getAddress());
        inputPhone1.setText(session.getPhone1());
        inputPhone2.setText(session.getPhone2());
        inputNoKtp.setText(session.getNoKtp());
        inputNoSim.setText(session.getNoSim());
        inputNopol.setText(session.getNopol());

        btnEditData.setOnClickListener(this);
        btnChooseKtp.setOnClickListener(this);
        btnUploadKtp.setOnClickListener(this);
        btnChooseSim.setOnClickListener(this);
        btnUploadSim.setOnClickListener(this);
        btnChooseStnk.setOnClickListener(this);
        btnUploadStnk.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == btnChooseKtp){
            param = "ktp";
            showFileChooser();
        }

        if (v == btnChooseSim){
            param = "sim";
            showFileChooser();
        }

        if (v == btnChooseStnk){
            param = "stnk";
            showFileChooser();
        }

        if (v == btnUploadKtp){
            if(inputDocKtp.getText().toString().isEmpty()){
                alertDialog("Info.","Anda belum memilih Dokumen.",false);
            } else {
                showSpinner();
                uploadDoc();
            }
        }

        if (v == btnUploadSim){
            if(inputDocSim.getText().toString().isEmpty()){
                alertDialog("Info.","Anda belum memilih Dokumen.",false);
            } else {
                showSpinner();
                uploadDoc();
            }
        }

        if (v == btnUploadStnk){
            if(inputDocStnk.getText().toString().isEmpty()){
                alertDialog("Info.","Anda belum memilih Dokumen.",false);
            } else {
                showSpinner();
                uploadDoc();
            }
        }

        if (v==btnEditData){
            String email, nama, address, phone1, phone2, noKtp, noSim, nopol;
            email = inputEmail.getText().toString().trim();
            nama = inputName.getText().toString().trim();
            address = inputAddress.getText().toString().trim();
            phone1 = inputPhone1.getText().toString().trim();
            phone2 = inputPhone2.getText().toString().trim();
            noKtp = inputNoKtp.getText().toString().trim();
            noSim = inputNoSim.getText().toString().trim();
            nopol = inputNopol.getText().toString().trim();

            if(email.isEmpty()){inputEmail.setError(Html.fromHtml("<font color='red'>Mohon isi bagian ini.</font>"));}
            else if(nama.isEmpty()){inputName.setError(Html.fromHtml("<font color='red'>Mohon isi bagian ini.</font>"));}
            else if(address.isEmpty()){inputAddress.setError(Html.fromHtml("<font color='red'>Mohon isi bagian ini.</font>"));}
            else if(phone1.isEmpty()){inputPhone1.setError(Html.fromHtml("<font color='red'>Mohon isi bagian ini.</font>"));}
            else if(phone2.isEmpty()){inputPhone2.setError(Html.fromHtml("<font color='red'>Mohon isi bagian ini.</font>"));}
            else if(noKtp.isEmpty()){inputNoKtp.setError(Html.fromHtml("<font color='red'>Mohon isi bagian ini.</font>"));}
            else if(noSim.isEmpty()){inputNoSim.setError(Html.fromHtml("<font color='red'>Mohon isi bagian ini.</font>"));}
            else if(nopol.isEmpty()){inputNopol.setError(Html.fromHtml("<font color='red'>Mohon isi bagian ini.</font>"));}
            else {
                showSpinner();
                changeData();
            }
        }
    }

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Pilih Gambar"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri filePath = data.getData();
            String uriString = filePath.toString();
            File file = new File(uriString);
            if(uriString.startsWith("content://")){
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(filePath,null,null,null,null);
                    if (cursor != null && cursor.moveToFirst()){
                        switch (param) {
                            case "ktp":
                                inputDocKtp.setText(cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)));
                                break;
                            case "sim":
                                inputDocSim.setText(cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)));
                                break;
                            case "stnk":
                                inputDocStnk.setText(cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)));
                                break;
                        }
                    }
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    assert cursor != null;
                    cursor.close();
                }
            } else if(uriString.startsWith("file://")) {
                switch (param){
                    case "ktp":
                        inputDocKtp.setText(file.getName());
                        break;
                    case "sim":
                        inputDocSim.setText(file.getName());
                        break;
                    case "stnk":
                        inputDocStnk.setText(file.getName());
                        break;
                }
            }
        }

    }

    private void uploadDoc(){
        mApiService.uploadDocument(getStringImage(bitmap),session.getEmail(), param)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()){
                            loading.dismiss();
                            Log.d(TAG, "Get data driver response at profil: " + response);
                            try {
                                JSONObject jObj = new JSONObject(response.body().string());
                                if (jObj.getString("error").equals("false")){
                                    String errorMsg = jObj.getString("error_msg");
                                    alertDialog("Info",errorMsg,true);
                                    getDataDriver(session.getEmail());
                                } else {
                                    String errorMsg = jObj.getString("error_msg");
                                    Log.e(TAG, errorMsg);
                                }
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            alertDialog("Info","Gagal mengunggah Dokumen mohon cek koneksi anda",false);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e(TAG, "onFailure upload foto: ERROR > " + t.toString());
                    }
                });
    }

    public String getStringImage(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private void showSpinner() {
        loading = ProgressDialog.show(this, "Mengunggah Dokumen", "Mohon menunggu...", true, false);
    }

    private void changeData(){
        mApiService.changeDataRequest(session.getEmail(), inputName.getText().toString(), inputAddress.getText().toString(), inputPhone1.getText().toString(), inputPhone2.getText().toString(), inputNoKtp.getText().toString(), inputNoSim.getText().toString(), inputNopol.getText().toString())
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()){
                            loading.dismiss();
                            try {
                                JSONObject jObj = new JSONObject(response.body().string());
                                if (jObj.getString("error").equals("false")){
                                    String errorMsg = jObj.getString("error_msg");
                                    alertDialog("Info.", errorMsg, true);
                                    getDataDriver(session.getEmail());
                                } else {
                                    String errorMsg = jObj.getString("error_msg");
                                    alertDialog("Info.", errorMsg, true);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void alertDialog(String title, String messages, final boolean toProfile){
        AlertDialog.Builder builder = new AlertDialog.Builder(UbahDataActivity.this);
        builder.setTitle(title);
        builder.setMessage(messages);
        String positiveText = getString(android.R.string.ok);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (toProfile){
                            toProfilActivity();
                        }
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void getDataDriver(String email){
        mApiService.getDataByEmail(email)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()){
                            Log.d(TAG, "Get data driver response at ubah data: " + response);
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
                                    Log.i(TAG, errorMsg);
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

    private void toProfilActivity(){
        Intent i = new Intent(UbahDataActivity.this, ProfilActivity.class);
        startActivity(i);
        this.finish();
    }
}
