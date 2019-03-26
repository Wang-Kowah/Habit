package com.kowah.habit;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class KeywordActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * 三个导航按钮
     */
    Button buttonOne;
    Button buttonTwo;
    Button buttonThree;

    View imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_keyword);

        buttonOne = findViewById(R.id.btn_one2);
        buttonTwo = findViewById(R.id.btn_two2);
        buttonThree = findViewById(R.id.btn_three2);
        imageView = findViewById(R.id.returnbutton);

        buttonOne.setOnClickListener(this);
        buttonTwo.setOnClickListener(this);
        buttonThree.setOnClickListener(this);
        imageView.setOnClickListener(this);
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
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_one2:
                updateActivity(buttonOne);
                break;
            case R.id.btn_two2:
                updateActivity(buttonTwo);
                break;
            case R.id.btn_three2:
                updateActivity(buttonThree);
                break;
            case R.id.returnbutton:
                finish();
                break;
            default:
                break;
        }
    }

    private void updateActivity(Button button) {
        buttonOne.setTextColor(getColor(R.color.colorText));
        buttonTwo.setTextColor(getColor(R.color.colorText));
        buttonThree.setTextColor(getColor(R.color.colorText));
        button.setTextColor(getColor(R.color.colorPrimary));


    }
}