package com.kowah.habit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kowah.habit.fragment.ChatFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_DRAGGING;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_SETTLING;

public class KeywordActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * 三个导航按钮
     */
    Button buttonOne;
    Button buttonTwo;
    Button buttonThree;

    View imageView;

    int currentTab = -1;

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

        updateActivity(buttonOne);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_one2:
                currentTab = 0;
                updateActivity(buttonOne);
                break;
            case R.id.btn_two2:
                currentTab = 1;
                updateActivity(buttonTwo);
                break;
            case R.id.btn_three2:
                currentTab = 2;
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


        final ArrayList<String> date = new ArrayList<>(Arrays.asList("昨天", "周二", "周三", "周四", "周五", "周六", "周日"));
        final ArrayList<String> keywords = new ArrayList<>(Arrays.asList("昨天", "周二", "周三", "周四", "周五", "周六", "周日"));
        final RecyclerView recyclerView = findViewById(R.id.keywordRecyclerView);
        final KeywordAdapter adapter = new KeywordAdapter(this, date, keywords);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
//        recyclerView.setOnScrollChangeListener();
    }

    // 自定义RecyclerViewAdapter
    public class KeywordAdapter extends RecyclerView.Adapter<KeywordAdapter.ViewHolder> {

        private Context context;
        private List<String> date;
        private List<String> keywords;

        KeywordAdapter(Context context, List<String> date, List<String> keywords) {
            this.context = context;
            this.date = date;
            this.keywords = keywords;
        }

        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_keyword, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            holder.keywordDate.setText(date.get(position));
//            holder.keywordTop.setText(keywords.get(position).split(",")[0]);
//            holder.keywordAll.setText(keywords.get(position).replace(",", " "));
            holder.keywordAll.setSingleLine(true);
            holder.keywordAll.setHorizontallyScrolling(true);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println(position);
                }
            });

        }

        @Override
        public int getItemCount() {
            return date.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            private TextView keywordDate;
            private TextView keywordTop;
            private TextView keywordAll;

            public ViewHolder(View itemView) {
                super(itemView);
                keywordDate = itemView.findViewById(R.id.keywordDate);
                keywordTop = itemView.findViewById(R.id.keywordTop);
                keywordAll = itemView.findViewById(R.id.keywordAll);
            }
        }
    }

}