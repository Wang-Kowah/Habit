package com.szwangel.habit;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.githang.statusbar.StatusBarCompat;
import com.szwangel.habit.service.RetrofitService;
import com.szwangel.habit.utils.DateUtils;
import com.szwangel.habit.utils.VoiceRecognitionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.view.View.OnClickListener;

public class VoiceAssistantActivity extends AppCompatActivity implements OnClickListener {
    Context mContext;

    View close;
    TextView voiceText;
    TextView voiceKeyword;
    TextView voiceCountDown;
    RecyclerView recyclerView;

    CountDownTimer countDownTimer;
    VoiceRecognitionUtils voiceRecognition;
    SharedPreferences sharedPreferences;
    RetrofitService retrofitService;
    Call<ResponseBody> call;
    MsgAdapter adapter;

    ArrayList<Integer> dateList;
    ArrayList<String> msgList;

    String[] keywords;
    boolean permissionGranted;
    int keywordNum = 2;
    int pageSize = 30;
    int pageNum = 1;
    int uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_voice_assistant);
        //设置状态栏的颜色
        StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.colorPrimary));
        mContext = this;

        initView();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.close_assistant:
                finish();
                break;
            case R.id.voiceText:
                voiceRecognition.start();
                countDownTimer.start();

//                Toast.makeText(mContext, "start", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        voiceRecognition.stop();
//                        Toast.makeText(mContext, "stop", Toast.LENGTH_SHORT).show();
                    }
                }, 5000);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 369) {
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    void initView() {
        close = findViewById(R.id.close_assistant);
        close.setOnClickListener(this);
        voiceCountDown = findViewById(R.id.voiceCountDown);
        voiceKeyword = findViewById(R.id.voiceKeyword);
        voiceText = findViewById(R.id.voiceText);
        voiceText.setOnClickListener(this);

        dateList = new ArrayList<>();
        msgList = new ArrayList<>();
        adapter = new MsgAdapter(this, dateList, msgList);
        recyclerView = findViewById(R.id.voiceRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        recyclerView.setAdapter(adapter);

        sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE);
        uid = sharedPreferences.getInt("uid", -1);

        MyApplication application = (MyApplication) getApplication();
        String domain = application.getDomain();
        retrofitService = new Retrofit.Builder()
                .baseUrl(domain)
                .build()
                .create(RetrofitService.class);

        voiceRecognition = new VoiceRecognitionUtils(mContext, new VoiceRecognitionUtils.OnLineCallBack() {
            @Override
            public void onSuccess(String result) {
                voiceText.setText(result);

                call = retrofitService.extractKeyword(result, keywordNum);
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        String json;
                        try {
                            json = response.body().string();
                            JSONObject jsonObject = JSONObject.parseObject(json);
                            if (!jsonObject.getInteger("retcode").equals(0)) {
                                Toast toast = Toast.makeText(mContext, jsonObject.getString("msg"), Toast.LENGTH_SHORT);
                                toast.setText(jsonObject.getString("msg"));
                                toast.show();
                            } else {
                                // 清除上一轮的搜索结果
                                adapter.clearItem();

                                keywords = jsonObject.getJSONArray("keywords").toArray(new String[0]);
                                if (keywords.length != 0) {
                                    StringBuilder stringBuilder = new StringBuilder();
                                    for (String keyword : keywords) {
                                        stringBuilder.append(keyword).append("，");
                                        updateMsg(keyword);
                                    }
                                    voiceKeyword.setText(stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString());

                                    if (msgList.isEmpty()) {
                                        Toast toast = Toast.makeText(mContext, "没有搜索到相关的内容", Toast.LENGTH_LONG);
                                        toast.setText("没有搜索到相关的内容");
                                        toast.show();
                                    } else {
                                        adapter.removeDuplicateItem();
                                    }
                                } else {
                                    voiceKeyword.setText("未提取出关键词");
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast toast = Toast.makeText(mContext, "网络异常，请稍后重试", Toast.LENGTH_SHORT);
                        toast.setText("网络异常，请稍后重试");
                        toast.show();
                        t.printStackTrace();
                    }
                });
            }
        });

        countDownTimer = new CountDownTimerUtils(voiceCountDown, 5000, 1000);
    }

    void updateMsg(String key) {
        Call<ResponseBody> call = retrofitService.search(uid, key, pageNum, pageSize);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                String json;
                try {
                    json = response.body().string();
                    JSONObject jsonObject = JSONObject.parseObject(json);
                    if (!jsonObject.getInteger("retcode").equals(0)) {
                        Toast toast = Toast.makeText(mContext, jsonObject.getString("msg"), Toast.LENGTH_SHORT);
                        toast.setText(jsonObject.getString("msg"));
                        toast.show();
                    } else {
                        JSONObject result = jsonObject.getJSONObject("result");
                        JSONArray jsonArray = result.getJSONArray("list");
                        ArrayList<Integer> dates = new ArrayList<>();
                        ArrayList<String> msgs = new ArrayList<>();
                        for (int i = 0; i < jsonArray.size(); i++) {
                            JSONObject object = jsonArray.getJSONObject(i);
                            msgs.add(object.getString("content"));
                            dates.add(object.getIntValue("createTime"));
                        }
                        adapter.addFooterItem(dates, msgs);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast toast = Toast.makeText(mContext, "网络异常，请稍后重试", Toast.LENGTH_SHORT);
                toast.setText("网络异常，请稍后重试");
                toast.show();
                t.printStackTrace();
            }
        });
    }

    // list去重避免搜索结果重复
    List removeDuplicate(List list) {
        HashSet h = new HashSet(list);
        list.clear();
        list.addAll(h);
        return list;
    }

    public class MsgAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private LayoutInflater mInflater;
        private List<Integer> dateList;
        private List<String> msgList;

        MsgAdapter(Context context, List<Integer> dateList, List<String> msgList) {
            this.mInflater = LayoutInflater.from(context);
            this.dateList = dateList;
            this.msgList = msgList;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.item_search, parent, false);
            return new MsgAdapter.ItemViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            MsgAdapter.ItemViewHolder itemViewHolder = (MsgAdapter.ItemViewHolder) holder;

            long date = dateList.get(position) * 1000L;
            itemViewHolder.searchDate.setText(DateUtils.formatDate(date));

            String msg = msgList.get(position);
            itemViewHolder.searchMsg.setText(msg);

            // 高亮搜索词
            SpannableString spannableString = new SpannableString(itemViewHolder.searchMsg.getText().toString());
            for (String key : keywords) {
                // 放在循环内部才能实现多处span
                ForegroundColorSpan span = new ForegroundColorSpan(getColor(R.color.colorPrimary));
                // 避免大小写不一致导致找不到key的位置
                int start = msg.toLowerCase().indexOf(key.toLowerCase());
                if (start != -1) {
                    spannableString.setSpan(span, start, start + key.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                }
            }
            itemViewHolder.searchMsg.setText(spannableString);
        }

        @Override
        public int getItemCount() {
            return dateList.size();
        }

        void AddHeaderItem(List<Integer> dates, List<String> keys) {
            dateList.addAll(0, dates);
            msgList.addAll(0, keys);
            notifyDataSetChanged();
        }

        void addFooterItem(List<Integer> dates, List<String> keys) {
            dateList.addAll(dates);
            msgList.addAll(keys);
            notifyDataSetChanged();
        }

        void removeDuplicateItem() {
            dateList = removeDuplicate(dateList);
            msgList = removeDuplicate(msgList);
            notifyDataSetChanged();
        }

        void clearItem() {
            dateList.clear();
            msgList.clear();
            notifyDataSetChanged();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            private TextView searchDate;
            private TextView searchMsg;

            ItemViewHolder(View itemView) {
                super(itemView);
                searchDate = itemView.findViewById(R.id.searchDate);
                searchMsg = itemView.findViewById(R.id.searchMsg);
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
            //设置倒计时时间
            mTextView.setText("录音倒计时：" + millisUntilFinished / 1000 + "秒");
            SpannableString spannableString = new SpannableString(mTextView.getText().toString());
            ForegroundColorSpan span = new ForegroundColorSpan(getColor(R.color.colorPrimary));
            spannableString.setSpan(span, 6, 7, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            mTextView.setText(spannableString);
        }

        @Override
        public void onFinish() {
            voiceRecognition.stop();
            this.start();
            voiceRecognition.start();
        }
    }
}
