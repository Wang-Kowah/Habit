package com.kowah.habit.service;

import java.io.IOException;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
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
    Call<ResponseBody> sendNote(@Field("uid") int uid, @Field("type") int type, @Field("msg") String msg);

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

}

class Test {
    public static void main(String[] args) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://119.29.77.201/habit/")
                .build();

        RetrofitService retrofitService = retrofit.create(RetrofitService.class);
        Call<ResponseBody> call = retrofitService.info(2);

        // 发送网络请求(异步)
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String json = response.body().string();
                    System.out.println(json);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println("fail, " + t.getMessage());
            }
        });
    }
}
