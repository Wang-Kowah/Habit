package com.szwangel.habit.service;

import java.math.BigDecimal;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface RetrofitService {

    @FormUrlEncoded
    @POST("user/getVerifyCode")
    Call<ResponseBody> getVerifyCode(@Field("mobile") String mobile);

    @FormUrlEncoded
    @POST("user/checkVerifyCode")
    Call<ResponseBody> checkVerifyCode(@Field("mobile") String mobile, @Field("code") String code);

    @FormUrlEncoded
    @POST("user/signUp")
    Call<ResponseBody> signUp(@Field("mobile") String mobile, @Field("name") String name);

    @GET("user/info")
    Call<ResponseBody> info(@Query("uid") int uid);

    @FormUrlEncoded
    @POST("user/logIn")
    Call<ResponseBody> logIn(@Field("mobile") String mobile);

    @FormUrlEncoded
    @POST("user/sendNote")
    Call<ResponseBody> sendNote(@Field("uid") int uid, @Field("type") int type, @Field("msg") String msg, @Field("lat") BigDecimal lat, @Field("lng") BigDecimal lng);

    @GET("user/noteList")
    Call<ResponseBody> noteList(@Query("uid") int uid, @Query("type") int type, @Query("pageNum") int pageNum, @Query("pageSize") int pageSize);

    @Multipart
    @POST("user/uploadProfile")
    Call<ResponseBody> uploadProfile(@Query("uid") int uid, @Part MultipartBody.Part pic);

    @GET("user/profile")
    Call<ResponseBody> profile(@Query("uid") int uid);

    @GET("user/dayKeyword")
    Call<ResponseBody> dayKeyword(@Query("uid") int uid, @Query("pageNum") int pageNum, @Query("pageSize") int pageSize);

    @GET("user/keyword")
    Call<ResponseBody> keyword(@Query("uid") int uid, @Query("type") int type, @Query("pageNum") int pageNum, @Query("pageSize") int pageSize);

    @FormUrlEncoded
    @POST("user/search")
    Call<ResponseBody> search(@Field("uid") int uid, @Field("key") String key, @Field("pageNum") int pageNum, @Field("pageSize") int pageSize);

    @Multipart
    @POST("user/sendPic")
    Call<ResponseBody> sendPic(@Query("uid") int uid, @Query("type") int type, @Query("lat") BigDecimal lat, @Query("lng") BigDecimal lng, @Part MultipartBody.Part pic);

    @GET("user/pic")
    Call<ResponseBody> pic(@Query("picName") String picName);

    @FormUrlEncoded
    @POST("user/hereAndNow")
    Call<ResponseBody> hereAndNow(@Field("uid") int uid, @Field("lat") BigDecimal lat, @Field("lng") BigDecimal lng);

    @FormUrlEncoded
    @POST("user/extractVoiceText")
    Call<ResponseBody> extractVoiceText(@Field("uid") int uid,@Field("text") String text);
}
