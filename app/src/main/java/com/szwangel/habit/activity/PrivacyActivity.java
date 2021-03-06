package com.szwangel.habit.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.githang.statusbar.StatusBarCompat;
import com.szwangel.habit.R;

public class PrivacyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_privacy);
        //设置状态栏的颜色
        StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.colorPrimary));

        final View imageView = findViewById(R.id.returnbutton3);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    imageView.setBackground(getDrawable(R.color.colorText));
                    imageView.setAlpha(0.3F);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    imageView.setBackground(getDrawable(R.color.colorPrimary));
                    imageView.setAlpha(1);
                }
                return false;
            }
        });

        WebView terms = findViewById(R.id.privacy);
        terms.loadUrl("file:///android_asset/privacy.html");
        WebSettings webSettings = terms.getSettings();
        webSettings.setJavaScriptEnabled(false);
        webSettings.setDisplayZoomControls(false);
    }
}
