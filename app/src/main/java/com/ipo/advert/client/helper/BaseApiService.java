package com.ipo.advert.client.helper;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface BaseApiService {

    // Fungsi ini untuk memanggil API http://ipo.co.id/api/login_d.php
    @FormUrlEncoded
    @POST("login_d.php")
    Call<ResponseBody> loginRequest(@Field("email") String email,
                                    @Field("password") String password,
                                    @Field("android_id") String androidId);

    @FormUrlEncoded
    @POST("register.php")
    Call<ResponseBody> registerRequest(@Field("name") String nama,
                                       @Field("email") String email,
                                       @Field("pass") String password,
                                       @Field("address") String address,
                                       @Field("gender") String gender,
                                       @Field("no_sim") String noSim,
                                       @Field("no_ktp") String noKtp,
                                       @Field("nopol") String nopol);

    @FormUrlEncoded
    @POST("change_data.php")
    Call<ResponseBody> changeDataRequest(@Field("email") String email,
                                         @Field("name") String name,
                                         @Field("address") String address,
                                         @Field("phone_1") String phone1,
                                         @Field("phone_2") String phone2,
                                         @Field("no_ktp") String noKtp,
                                         @Field("no_sim") String noSim,
                                         @Field("nopol") String nopol);

    @FormUrlEncoded
    @POST("change_pass_d.php")
    Call<ResponseBody> changePassRequest(@Field("email") String email,
                                         @Field("old_pass") String oldPass,
                                         @Field("new_pass") String newPass,
                                         @Field("conf_new_pass") String conf_new_pass);

    @FormUrlEncoded
    @POST("change_photo.php")
    Call<ResponseBody> changePhotoRequest(@Field("img_drv") String imgDrv,
                                          @Field("email") String email);

    @FormUrlEncoded
    @POST("get_data_by_email.php")
    Call<ResponseBody> getDataByEmail(@Field("email") String email);

    @FormUrlEncoded
    @POST("get_drv_progress.php")
    Call<ResponseBody> getDrvProgress(@Field("id_vacant") String idVacant);

    @FormUrlEncoded
    @POST("init_job.php")
    Call<ResponseBody> initJob(@Field("id_job") int idJob,
                               @Field("id_drv") int idDrv);

    @FormUrlEncoded
    @POST("save_daily_progress.php")
    Call<ResponseBody> saveDailyProgress(@Field("id_vacant") String idVacant,
                                         @Field("ttl_today") double ttlToday);

    @FormUrlEncoded
    @POST("save_log_route.php")
    Call<ResponseBody> saveLogRoute(@Field("id_vacant") String idVacant,
                                    @Field("latitude") double latitude,
                                    @Field("longitude") double longitude,
                                    @Field("nm_lokasi") String nm_lokasi);

    @FormUrlEncoded
    @POST("upload_document.php")
    Call<ResponseBody> uploadDocument(@Field("foto") String foto,
                                      @Field("email") String email,
                                      @Field("param") String param);

    @FormUrlEncoded
    @POST("logout.php")
    Call<ResponseBody> logout(@Field("email") String email,
                              @Field("password") String pass,
                              @Field("android_id") String androidId);
}