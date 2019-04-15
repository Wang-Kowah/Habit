package com.kowah.habit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import com.kowah.habit.utils.DateUtils;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_start);

        SharedPreferences sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        long mLastOpenApp = sharedPreferences.getLong("mLastOpenApp", -1);
        long now = System.currentTimeMillis();

        TextView skipButton = findViewById(R.id.skip_button);

        // 首次安装时打开，否则只在每周一出现一次
        if (mLastOpenApp == -1 || (DateUtils.isMonday(now) && mLastOpenApp < DateUtils.getDayBeginTimestamp(now, 0))) {

            final CountDownTimer countDownTimer = new CountDownTimerUtils(skipButton, 5000, 1000);
            countDownTimer.start();

            skipButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    countDownTimer.cancel();
                    getHome();
                }
            });

            editor.putLong("mLastOpenApp", now);
            editor.apply();
        } else {
            skipButton.setVisibility(View.GONE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getHome();
                }
            }, 1000);
        }
    }

    private void getHome() {
        Intent intent = new Intent(StartActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
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
            mTextView.setText(millisUntilFinished / 1000 + "秒跳过"); //设置倒计时时间
            SpannableString spannableString = new SpannableString(mTextView.getText().toString());
            ForegroundColorSpan span = new ForegroundColorSpan(Color.WHITE);
            spannableString.setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            mTextView.setText(spannableString);
        }

        @Override
        public void onFinish() {
            getHome();
        }
    }
}
