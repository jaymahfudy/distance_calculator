package com.ipo.advert.client.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by Mark_28 on 5/15/2017.
 * @author Jay Mahfudy
 * @version 1.0
 */

public class SessionManager {
    private static String TAG = SessionManager.class.getSimpleName();

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    private static final String PREF_NAME = "ipoDriverSharedPref";
    private static final String KEY_IS_LOGGEDIN = "isLoggedIn";
    private static final String KEY_IS_JOB_INITIATED = "isJobInitiated";
    private static final String KEY_IS_JOB_FINISHED = "isJobFinished";
    private static final String KEY_ID_DRIVER = "id_drv";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_NAME = "name"   ;
    private static final String KEY_SEX = "sex";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_PHONE_1 = "phone_1";
    private static final String KEY_PHONE_2 = "phone_2";
    private static final String KEY_NO_KTP = "no_ktp";
    private static final String KEY_IMG_KTP = "img_ktp";
    private static final String KEY_NO_SIM = "no_sim";
    private static final String KEY_IMG_SIM = "img_sim";
    private static final String KEY_NOPOL = "nopol";
    private static final String KEY_IMG_STNK = "img_stnk";
    private static final String KEY_IMG_DRV = "img_drv";
    private static final String KEY_JOB_ID = "job_id";
    private static final String KEY_ID_VACANT = "id_vacant";
    private static final String KEY_TAR_DRV = "tar_drv";
    private static final String KEY_ANDROID_ID = "android_id";
    private static final String KEY_PROGRESS = "progress";

    public SessionManager(Context context) {
        int PRIVATE_MODE = 0;
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void setLogin(boolean isLoggedIn) {
        editor.putBoolean(KEY_IS_LOGGEDIN, isLoggedIn);
        editor.commit();
        Log.d(TAG, "User login session modified!");
    }

    public boolean isLoggedIn(){
        return pref.getBoolean(KEY_IS_LOGGEDIN, false);
    }

    public void setJobInit(boolean isJobInitiated){
        editor.putBoolean(KEY_IS_JOB_INITIATED, isJobInitiated);
        editor.commit();
        Log.d(TAG, "Id job session modified!");
    }

    public boolean isJobInitiated(){
        return pref.getBoolean(KEY_IS_JOB_INITIATED, false);
    }

    public void setJobStatus(boolean isJobFinished){
        editor.putBoolean(KEY_IS_JOB_FINISHED, isJobFinished);
        editor.commit();
        Log.d(TAG, "Job status is modified!");
    }

    public boolean isJobFinished(){
        return  pref.getBoolean(KEY_IS_JOB_FINISHED, false);
    }

    public void setIdDriver(String idDriver){
        editor.putString(KEY_ID_DRIVER, idDriver);
        editor.commit();
    }

    public String getIdDriver(){
        return pref.getString(KEY_ID_DRIVER, " ");
    }

    public  void setEmail(String email){
        editor.putString(KEY_EMAIL, email);
        editor.commit();
    }

    public String getEmail(){
        return pref.getString(KEY_EMAIL, "  ");
    }

    public void setPassword(String password){
        editor.putString(KEY_PASSWORD, password);
        editor.commit();
    }

    public String getPassword(){
        return pref.getString(KEY_PASSWORD, " ");
    }

    public void setName(String name){
        editor.putString(KEY_NAME, name);
        editor.commit();
    }

    public String getName(){
        return pref.getString(KEY_NAME, "  ");
    }

    public void setSex(String sex){
        editor.putString(KEY_SEX, sex);
        editor.commit();
    }

    public String getSex(){
        return pref.getString(KEY_SEX, "  ");
    }

    public void setAddress(String address){
        editor.putString(KEY_ADDRESS, address);
        editor.commit();
    }

    public String getAddress(){
        return pref.getString(KEY_ADDRESS, "  ");
    }

    public void setPhone1(String phone1){
        editor.putString(KEY_PHONE_1, phone1);
        editor.commit();
    }

    public String getPhone1(){
        return pref.getString(KEY_PHONE_1, "  ");
    }

    public void setPhone2(String phone){
        editor.putString(KEY_PHONE_2, phone);
        editor.commit();
    }

    public String getPhone2(){
        return pref.getString(KEY_PHONE_2, "  ");
    }

    public void setNoKtp(String noKtp){
        editor.putString(KEY_NO_KTP, noKtp);
        editor.commit();
    }

    public String getNoKtp(){
        return pref.getString(KEY_NO_KTP, "  ");
    }

    public void setImgKtp(String imgKtp){
        editor.putString(KEY_IMG_KTP, imgKtp);
        editor.commit();
    }

    public String getImgKtp(){
        return pref.getString(KEY_IMG_KTP, "  ");
    }

    public void setNoSim(String noSim){
        editor.putString(KEY_NO_SIM, noSim);
        editor.commit();
    }

    public String getNoSim(){
        return pref.getString(KEY_NO_SIM, "  ");
    }

    public void setImgSim(String imgSim){
        editor.putString(KEY_IMG_SIM, imgSim);
        editor.commit();
    }

    public String getImgSim(){
        return pref.getString(KEY_IMG_SIM, "  ");
    }

    public void setNopol(String nopol){
        editor.putString(KEY_NOPOL, nopol);
        editor.commit();
    }

    public String getNopol(){
        return pref.getString(KEY_NOPOL, "  ");
    }

    public void setImgStnk(String imgStnk){
        editor.putString(KEY_IMG_STNK, imgStnk);
        editor.commit();
    }

    public String getImgStnk(){
        return pref.getString(KEY_IMG_STNK, "  ");
    }

    public void setImgDrv(String imgDrv){
        editor.putString(KEY_IMG_DRV, imgDrv);
        editor.commit();
    }

    public String getImgDrv(){
        return pref.getString(KEY_IMG_DRV, "  ");
    }

    public void setIdVacant(String idVacant){
        editor.putString(KEY_ID_VACANT, idVacant);
        editor.commit();
    }

    public String getIdvacant(){
        return pref.getString(KEY_ID_VACANT, " ");
    }

    public void setJobId(String jobId){
        editor.putString(KEY_JOB_ID, jobId);
        editor.commit();
    }

    public String getJobId(){
        return pref.getString(KEY_JOB_ID, " ");
    }

    public void setTarDrv(String tarDrv){
        editor.putString(KEY_TAR_DRV, tarDrv);
        editor.commit();
    }

    public String getTarDrv(){
        return pref.getString(KEY_TAR_DRV, " ");
    }

    public void setAndroidId(String androidId){
        editor.putString(KEY_ANDROID_ID, androidId);
        editor.commit();
    }

    public String getAndroidId(){
        return pref.getString(KEY_ANDROID_ID, " ");
    }

    public void setProgress(String progress){
        editor.putString(KEY_PROGRESS, progress);
        editor.commit();
    }

    public String getProgress(){
        return pref.getString(KEY_PROGRESS, "0");
    }
}