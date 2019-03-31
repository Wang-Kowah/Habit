package com.kowah.habit;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kowah.habit.service.CommonService;
import com.kowah.habit.utils.DateUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class KeywordActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * 三个导航按钮
     */
    Button buttonOne;
    Button buttonTwo;
    Button buttonThree;
    ArrayList<Button> buttonList;

    View returnButton;
    RecyclerView recyclerView;
    RefreshAdapter adapter;

    SharedPreferences sharedPreferences;
    CommonService commonService;

    ArrayList<Integer> dateList;
    ArrayList<String> keywords;
    // 当前页面
    int currentTab = -1;
    // 上次更新关键词列表的日期
    int mLastUpdateKeyword = 0;
    int uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_keyword);

        sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE);
        uid = sharedPreferences.getInt("uid", -1);
        mLastUpdateKeyword = Integer.parseInt(DateUtils.formatDate(System.currentTimeMillis(), "yyyyMMdd"));

        buttonOne = findViewById(R.id.btn_one2);
        buttonTwo = findViewById(R.id.btn_two2);
        buttonThree = findViewById(R.id.btn_three2);
        returnButton = findViewById(R.id.returnbutton);

        buttonOne.setOnClickListener(this);
        buttonTwo.setOnClickListener(this);
        buttonThree.setOnClickListener(this);
        returnButton.setOnClickListener(this);
        returnButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    returnButton.setBackground(getDrawable(R.color.colorText));
                    returnButton.setAlpha(0.3F);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    returnButton.setBackground(getDrawable(R.color.colorPrimary));
                    returnButton.setAlpha(1);
                }
                return false;
            }
        });

        buttonList = new ArrayList<>();
        buttonList.add(buttonOne);
        buttonList.add(buttonTwo);
        buttonList.add(buttonThree);

        commonService = new Retrofit.Builder()
                .baseUrl("http://119.29.77.201/habit/")
                .build()
                .create(CommonService.class);

        updateActivity(0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_one2:
                updateActivity(0);
                break;
            case R.id.btn_two2:
                updateActivity(1);
                break;
            case R.id.btn_three2:
                updateActivity(2);
                break;
            case R.id.returnbutton:
                finish();
                break;
            default:
                break;
        }
    }

    private void updateActivity(int tab) {
        currentTab = tab;
        buttonOne.setTextColor(getColor(R.color.colorText));
        buttonTwo.setTextColor(getColor(R.color.colorText));
        buttonThree.setTextColor(getColor(R.color.colorText));
        buttonList.get(tab).setTextColor(getColor(R.color.colorPrimary));

//        final ArrayList<String> dateList = new ArrayList<>(Arrays.asList("周一", "周二", "周三", "周四", "周五", "周六", "周日"));
//        final ArrayList<String> keywords = new ArrayList<>(Arrays.asList("1", "2", "3"));

        dateList = new ArrayList<>();
        keywords = new ArrayList<>();

        adapter = new RefreshAdapter(this, dateList, keywords);
        recyclerView = findViewById(R.id.keywordRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        // 重写上滑监听
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            int lastVisibleItem;

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                // 更新最后一个可见的ITEM
                lastVisibleItem = layoutManager.findLastVisibleItemPosition();
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                //判断RecyclerView的状态为空闲，同时是最后一个可见item时才加载
                if (newState == RecyclerView.SCROLL_STATE_IDLE && lastVisibleItem + 1 == adapter.getItemCount()) {
                    adapter.setFooterVisibility(View.VISIBLE);
                    updateMsg(false);
                }
            }
        });

        updateMsg(true);
    }

    // 刷新
    void updateMsg(final boolean firstLoad) {
        Call<ResponseBody> call;
        switch (currentTab) {
            case 0:
                call = commonService.dayKeyword(uid);
                break;
            default:
                call = commonService.keyword(uid, currentTab);
                break;
        }
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                String json;
                try {
                    json = response.body().string();
                    JSONObject jsonObject = JSONObject.parseObject(json);
                    if (!jsonObject.getInteger("retcode").equals(0)) {
                        Toast toast = Toast.makeText(KeywordActivity.this, jsonObject.getString("msg"), Toast.LENGTH_SHORT);
                        toast.setText(jsonObject.getString("msg"));
                        toast.show();
                    } else {
                        mLastUpdateKeyword = Integer.parseInt(DateUtils.formatDate(System.currentTimeMillis(), "yyyyMMdd"));

                        JSONArray jsonArray;
                        switch (currentTab) {
                            case 0:
                                jsonArray = jsonObject.getJSONArray("dayKeywordList");
                                break;
                            default:
                                jsonArray = jsonObject.getJSONArray("keywordList");
                                break;
                        }
                        int hasNewMsg = 0;
                        for (int i = 0; i < jsonArray.size(); i++) {
                            JSONObject object = jsonArray.getJSONObject(i);
                            ArrayList<Integer> dates = new ArrayList<>();
                            ArrayList<String> keys = new ArrayList<>();
                            if (firstLoad || object.getIntValue("date") > mLastUpdateKeyword) {
                                keys.add(object.getString("keywords"));
                                dates.add(object.getIntValue("date"));

                                adapter.addFooterItem(dates, keys);
                                hasNewMsg = 1;

//                                recyclerView.scrollToPosition(dateList.size() - 1);
                            }
                        }
                        if (hasNewMsg == 0) {
                            Toast toast = Toast.makeText(KeywordActivity.this, "没有更多消息啦", Toast.LENGTH_SHORT);
                            toast.setText("没有更多消息啦");
                            toast.show();
                        }
                    }
                    adapter.setFooterVisibility(View.GONE);
                } catch (IOException e) {
                    Toast toast = Toast.makeText(KeywordActivity.this, "网络异常，请稍后重试", Toast.LENGTH_SHORT);
                    toast.setText("网络异常，请稍后重试");
                    toast.show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast toast = Toast.makeText(KeywordActivity.this, "网络异常，请稍后重试", Toast.LENGTH_SHORT);
                toast.setText("网络异常，请稍后重试");
                toast.show();
                t.printStackTrace();
            }
        });
    }

    // 自定义上拉刷新Adapter
    public class RefreshAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private Context mContext;
        private LayoutInflater mInflater;
        private List<Integer> dateList;
        private List<String> keywords;
        private FooterViewHolder footerViewHolder;

        private static final int TYPE_ITEM = 0;
        private static final int TYPE_FOOTER = 1;

        RefreshAdapter(Context context, List<Integer> dateList, List<String> keywords) {
            this.mContext = context;
            this.mInflater = LayoutInflater.from(context);
            this.dateList = dateList;
            this.keywords = keywords;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_ITEM) {
                View itemView = mInflater.inflate(R.layout.item_keyword, parent, false);
                return new ItemViewHolder(itemView);
            } else if (viewType == TYPE_FOOTER) {
                View itemView = mInflater.inflate(R.layout.footer_load_more, parent, false);
                return new FooterViewHolder(itemView);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ItemViewHolder) {
                ItemViewHolder itemViewHolder = (ItemViewHolder) holder;

                int date = dateList.get(position);
                if (currentTab == 0 && mLastUpdateKeyword - date == 1) {
                    itemViewHolder.keywordDate.setText("昨天");
                } else if (currentTab == 1 && mLastUpdateKeyword - date < 7) {
                    itemViewHolder.keywordDate.setText("上周");
                } else if (currentTab == 2 && date > Integer.parseInt(DateUtils.formatDate(DateUtils.getMonthBeginTimestamp(System.currentTimeMillis(), 0), "yyyyMMdd"))) {
                    itemViewHolder.keywordDate.setText("上月");
                } else {
                    itemViewHolder.keywordDate.setText(date / 10000 + "年" + date % 10000 / 100 + "月" + date % 100 + "日");
                }

                String keyword = keywords.get(position);
                String keywordTop = keyword.split(",")[0];
                String keywordAll = keyword.replace(",", " ");

                itemViewHolder.keywordTop.setText(keywordTop);
                itemViewHolder.keywordAll.setText(keywordAll);

            } else if (holder instanceof FooterViewHolder) {
                footerViewHolder = (FooterViewHolder) holder;
            }
        }

        @Override
        public int getItemCount() {
            //RecyclerView的count设置为数据总条数+ 1（footerView）
            return dateList.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (position + 1 == getItemCount()) {
                //最后一个item设置为footerView
                return TYPE_FOOTER;
            } else {
                return TYPE_ITEM;
            }
        }

        void AddHeaderItem(List<Integer> dates, List<String> keys) {
            dateList.addAll(0, dates);
            keywords.addAll(0, keys);
            notifyDataSetChanged();
        }

        void addFooterItem(List<Integer> dates, List<String> keys) {
            dateList.addAll(dates);
            keywords.addAll(keys);
            notifyDataSetChanged();
        }

        // 显示/隐藏加载条
        void setFooterVisibility(int id) {
            footerViewHolder.itemView.setVisibility(id);
            notifyDataSetChanged();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            private TextView keywordDate;
            private TextView keywordTop;
            private TextView keywordAll;

            ItemViewHolder(View itemView) {
                super(itemView);
                keywordDate = itemView.findViewById(R.id.keywordDate);
                keywordTop = itemView.findViewById(R.id.keywordTop);
                keywordAll = itemView.findViewById(R.id.keywordAll);
                initListener(itemView);
            }

            private void initListener(View itemView) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        System.out.println("touching " + getAdapterPosition());
                    }
                });
            }
        }

        class FooterViewHolder extends RecyclerView.ViewHolder {
            FooterViewHolder(View itemView) {
                super(itemView);
            }
        }
    }

}