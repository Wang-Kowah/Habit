package com.szwangel.habit;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.githang.statusbar.StatusBarCompat;
import com.szwangel.habit.service.RetrofitService;
import com.xiasuhuei321.loadingdialog.view.LoadingDialog;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginActivity extends AppCompatActivity implements OnClickListener {

    SharedPreferences.Editor editor;

    EditText mobile;
    EditText code;
    Button button;
    TextView termsButton;
    TextView getCode;
    CountDownTimerUtils countDownTimer;
    RetrofitService retrofitService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_login);
        //设置状态栏的颜色
        StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.colorPrimary));

        // 动态申请权限
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, RESULT_FIRST_USER);

        mobile = findViewById(R.id.mobile);
        code = findViewById(R.id.code);
        button = findViewById(R.id.sign_in_button);
        termsButton = findViewById(R.id.termsbutton);
        getCode = findViewById(R.id.getCode);

        button.setOnClickListener(this);
        termsButton.setOnClickListener(this);
        getCode.setOnClickListener(this);

        countDownTimer = new CountDownTimerUtils(getCode, 60000, 1000);

        MyApplication application = (MyApplication) getApplication();
        String domain = application.getDomain();
        retrofitService = new Retrofit.Builder()
                .baseUrl(domain)
                .build()
                .create(RetrofitService.class);

        editor = getSharedPreferences("user_data", Context.MODE_PRIVATE).edit();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.getCode:
                getCode();
                break;
            case R.id.sign_in_button:
                signUp();
                break;
            case R.id.termsbutton:
                navigateTo(ProtocolActivity.class);
                break;
            default:
                break;
        }
    }

    public class CountDownTimerUtils extends CountDownTimer {
        private TextView mTextView;

        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        public CountDownTimerUtils(TextView textView, long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
            this.mTextView = textView;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mTextView.setClickable(false); //设置不可点击
            mTextView.setText(millisUntilFinished / 1000 + "秒"); //设置倒计时时间
            SpannableString spannableString = new SpannableString(mTextView.getText().toString());
            ForegroundColorSpan span = new ForegroundColorSpan(Color.RED);
            spannableString.setSpan(span, 0, 2, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            mTextView.setText(spannableString);
        }

        @Override
        public void onFinish() {
            mTextView.setText("重新获取");
            mTextView.setClickable(true);//重新获得点击
        }
    }

    /**
     * 获取验证码
     */
    private void getCode() {
        final String mobileStr = mobile.getText().toString();
        if (mobileStr.length() != 11) {
            Toast toast = Toast.makeText(LoginActivity.this, "请输入正确的手机号", Toast.LENGTH_SHORT);
            toast.setText("请输入正确的手机号");
            toast.show();
        } else {
            Call<ResponseBody> call = retrofitService.getVerifyCode(mobileStr);

            // 发送网络请求(异步)
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        String json = response.body().string();
                        JSONObject jsonObject = JSONObject.parseObject(json);
                        if (!jsonObject.get("retcode").equals(0)) {
                            Toast toast = Toast.makeText(LoginActivity.this, jsonObject.getString("msg"), Toast.LENGTH_SHORT);
                            toast.setText(jsonObject.getString("msg"));
                            toast.show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    System.out.println("fail, " + t.getMessage());
                }
            });
            countDownTimer.start();
        }
    }

    /**
     * 登录
     */
    private void signUp() {
        final String mobileStr1 = mobile.getText().toString();
        String codeStr = code.getText().toString();
        if (codeStr.length() != 4) {
            Toast toast = Toast.makeText(LoginActivity.this, "请输入完整的验证码", Toast.LENGTH_SHORT);
            toast.setText("请输入完整的验证码");
            toast.show();
        } else {

            final LoadingDialog loadingDialog = new LoadingDialog(this);
            loadingDialog.setLoadingText("正在登录，请稍等...")
                    .setSuccessText("您已成功登录")
                    .setFailedText("登录失败，请稍后再试！")
                    .setInterceptBack(true)
                    .setRepeatCount(0)
                    .show();

            Call<ResponseBody> call = retrofitService.checkVerifyCode(mobileStr1, codeStr);
//            Call<ResponseBody> call = retrofitService.info(11);

            // 发送网络请求(异步)
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        String json = response.body().string();
                        JSONObject jsonObject = JSONObject.parseObject(json);
                        if (!jsonObject.getInteger("retcode").equals(0)) {
                            loadingDialog.setFailedText(jsonObject.getString("msg"));
                            loadingDialog.loadFailed();
                        } else {
                            // 暂定格式："用户"+手机号后4位+10位时间戳
                            call = retrofitService.signUp(mobileStr1, "用户" + mobileStr1.substring(7) + System.currentTimeMillis() / 1000);
//                            call = retrofitService.logIn("13602676334");
                            call.enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                    String json = null;
                                    try {
                                        json = response.body().string();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    JSONObject jsonObject = JSONObject.parseObject(json);
                                    if (jsonObject.getInteger("retcode").equals(0) || jsonObject.getInteger("retcode").equals(4001)) {
                                        loadingDialog.loadSuccess();

                                        int uid = jsonObject.getInteger("uid");
                                        editor.putInt("uid", uid);
                                        editor.putString("mobile", mobileStr1);
                                        editor.commit();

                                        navigateTo(MainActivity.class);
                                        finish();
                                    }
                                }

                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {
                                    try {
                                        call.execute();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e("e", "onFailure: ", t);
                    Toast toast = Toast.makeText(LoginActivity.this, "网络异常，请稍后重试", Toast.LENGTH_SHORT);
                    toast.setText("网络异常，请稍后重试");
                    toast.show();
                }
            });
        }
    }

    private void navigateTo(Class activity) {
        Intent intent = new Intent(LoginActivity.this, activity);
        startActivity(intent);
    }

}