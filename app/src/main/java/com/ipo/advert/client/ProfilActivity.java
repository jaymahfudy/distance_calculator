package com.ipo.advert.client;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ipo.advert.client.app.Config;
import com.ipo.advert.client.helper.BaseApiService;
import com.ipo.advert.client.helper.SessionManager;
import com.ipo.advert.client.helper.UtilsApi;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

public class ProfilActivity extends AppCompatActivity {
    private static final String TAG = ProfilActivity.class.getSimpleName();
    private static final int CAMERA_REQUEST = 1888, PICK_IMAGE_REQUEST = 1;
    private Bitmap bitmap;
    SessionManager session;
    ProgressDialog loading;
    BaseApiService mApiService;
    CircleImageView ivProfil;
    TextView tvEmail, tvNama, tvAlamat, tvNoTelp_1, tvNoTelp_2, tvNoKtp, tvNoSim, tvNoPolisi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil);
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        session = new SessionManager(this);
        mApiService = UtilsApi.getAPIService();

        tvEmail = findViewById(R.id.tv_email);
        tvNama = findViewById(R.id.tv_nama);
        tvAlamat = findViewById(R.id.tv_alamat);
        tvNoTelp_1 = findViewById(R.id.tv_no_telp_1);
        tvNoTelp_2 = findViewById(R.id.tv_no_telp_2);
        tvNoKtp = findViewById(R.id.tv_no_ktp);
        tvNoSim = findViewById(R.id.tv_no_sim);
        tvNoPolisi = findViewById(R.id.tv_nopol);
        ivProfil = findViewById(R.id.profile_photo);

        tvEmail.setText(session.getEmail().trim());
        tvNama.setText(session.getName().trim());
        tvAlamat.setText(session.getAddress().trim());
        tvNoTelp_1.setText(session.getPhone1().trim());
        tvNoTelp_2.setText(session.getPhone2().trim());
        tvNoKtp.setText(session.getNoKtp().trim());
        tvNoSim.setText(session.getNoSim().trim());
        tvNoPolisi.setText(session.getNopol().trim());

        if (!session.getImgDrv().isEmpty()){
            Picasso.with(this)
                    .load(session.getImgDrv())
                    .placeholder(R.drawable.avatar)
                    .error(android.R.drawable.ic_dialog_alert)
                    .into(ivProfil);
        }

        ivProfil.setOnClickListener(ivProvilListener);
    }

    private View.OnClickListener ivProvilListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showFileChooser();
        }
    };

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
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                ivProfil.setImageBitmap(bitmap);
                showSpinner();
                uploadImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            bitmap = (Bitmap) data.getExtras().get("data");
            ivProfil.setImageBitmap(bitmap);
            showFileChooser();
            uploadImage();
        }
    }

    public String getStringImage(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    public void showToast(String messages){
        Toast.makeText(this, messages, Toast.LENGTH_LONG).show();
    }

    private void alertDialog(String messages){
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfilActivity.this);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        toMainActivity();
        this.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.change_data){
            Intent i = new Intent(this, UbahDataActivity.class);
            startActivity(i);
        }

        if(id == android.R.id.home){
            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }

    private void toMainActivity(){
        Intent i = new Intent(ProfilActivity.this, MainActivity.class);
        startActivity(i);
    }

    private void getDataDriver(String email){
        mApiService.getDataByEmail(email)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()){
                            Log.d(TAG, "Get data driver response at profil: " + response);
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

    private void uploadImage(){
        mApiService.changePhotoRequest(getStringImage(bitmap),session.getEmail())
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
                                    alertDialog(errorMsg);
                                    getDataDriver(session.getEmail());
                                } else {
                                    String errorMsg = jObj.getString("error_msg");
                                    showToast(errorMsg);
                                }
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.e(TAG, "Gagal mengunggah foto: ");
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e(TAG, "onFailure upload foto: ERROR > " + t.toString());
                    }
                });
    }

    private void showSpinner() {
        loading = ProgressDialog.show(this, "Mengunggah foto profil.", "Harap Tunggu...", true, false);
    }
}
