package com.szwangel.habit.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.githang.statusbar.StatusBarCompat;
import com.szwangel.habit.R;
import com.szwangel.habit.application.HabitApplication;
import com.szwangel.habit.service.RetrofitService;
import com.szwangel.habit.utils.DateUtils;
import com.szwangel.habit.utils.FileUtils;
import com.szwangel.habit.utils.VoiceRecognitionUtils;

import java.io.File;
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
    View voiceRefresh;
    View voiceRecord;
    ImageView voiceRefreshImage;
    TextView voiceRefreshText;
    TextView voiceCountDown;
    RecyclerView recyclerView;
    PopupWindow popupWindow;

    CountDownTimer countDownTimer;
    VoiceRecognitionUtils voiceRecognition;
    SharedPreferences sharedPreferences;
    RetrofitService retrofitService;
    Call<ResponseBody> call;
    MsgAdapter adapter;

    ArrayList<Integer> dateList;
    ArrayList<String> msgList;

    String finalResult;
    String[] keywords;
    boolean permissionGranted;
    boolean recording;
    boolean acceptPic = true;
    int uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_voice_assistant);
        // 设置状态栏的颜色
        StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.colorPrimary));
        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mContext = this;

        initView();
        initListener();

        // 进入就开始
        recording = true;
        voiceRecognition.start();
        countDownTimer.start();
    }

    @Override
    protected void onDestroy() {
        // 避免内存泄漏
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.close_assistant:
                finish();
                break;
            case R.id.voiceRefresh:
                recording = true;
                voiceRecognition.stop();
                process();
                countDownTimer.cancel();
                countDownTimer.start();
                voiceRecognition.start();
                break;
            case R.id.voiceRecord:
                if (recording) {
                    voiceRecognition.stop();
                    countDownTimer.cancel();
                    voiceCountDown.setText("录音已暂停");
                    voiceRefresh.setClickable(false);
                    voiceRefreshText.setTextColor(getColor(R.color.refreshDisable));
                    voiceRefreshImage.setColorFilter(getColor(R.color.refreshDisable));
                } else {
                    voiceRecognition.start();
                    countDownTimer.start();
                    voiceRefresh.setClickable(true);
                    voiceRefreshText.setTextColor(getColor(R.color.black));
                    voiceRefreshImage.setColorFilter(getColor(R.color.black));
                }
                switchRecordingStatus();
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
        voiceRefreshImage = findViewById(R.id.voiceRefreshImage);
        voiceRefreshText = findViewById(R.id.voiceRefreshText);
        voiceRefresh = findViewById(R.id.voiceRefresh);
        voiceRecord = findViewById(R.id.voiceRecord);
        voiceCountDown = findViewById(R.id.voiceCountDown);

        dateList = new ArrayList<>();
        msgList = new ArrayList<>();
        adapter = new MsgAdapter(this, dateList, msgList);
        recyclerView = findViewById(R.id.voiceRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        recyclerView.setAdapter(adapter);

        sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE);
        uid = sharedPreferences.getInt("uid", -1);

        HabitApplication application = (HabitApplication) getApplication();
        String domain = application.getDomain();
        retrofitService = new Retrofit.Builder()
                .baseUrl(domain)
                .build()
                .create(RetrofitService.class);

        countDownTimer = new CountDownTimerUtils(voiceCountDown, 7000, 1000);
        voiceRecognition = new VoiceRecognitionUtils(mContext, new VoiceRecognitionUtils.OnLineCallBack() {
            @Override
            public void onSuccess(String result) {
                finalResult = result;
            }
        });
    }

    void initListener() {
        close.setOnClickListener(this);
        close.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    close.setBackground(getDrawable(R.color.colorText));
                    close.setAlpha(0.3F);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    close.setBackground(getDrawable(R.color.colorPrimary));
                    close.setAlpha(1);
                }
                return false;
            }
        });
        voiceRefresh.setOnClickListener(this);
        voiceRecord.setOnClickListener(this);
    }

    void process() {
        call = retrofitService.extractVoiceText(uid, finalResult,acceptPic);
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
                        keywords = jsonObject.getJSONArray("keywords").toArray(new String[0]);
                        if (keywords.length != 0) {
                            if (jsonObject.getInteger("size").equals(0)) {
                                Toast toast = Toast.makeText(mContext, "没有搜索到相关的内容", Toast.LENGTH_LONG);
                                toast.setText("没有搜索到相关的内容");
                                toast.show();
                            } else {
                                updateMsg(jsonObject.getJSONArray("result"));
                            }
                        } else {
                            Toast toast = Toast.makeText(mContext, "未提取出关键词", Toast.LENGTH_LONG);
                            toast.setText("未提取出关键词");
                            toast.show();
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

    void updateMsg(JSONArray jsonArray) {
        // 清除上一轮的搜索结果
        adapter.clearItem();

        ArrayList<Integer> dates = new ArrayList<>();
        ArrayList<String> msgs = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            msgs.add(object.getString("content"));
            dates.add(object.getIntValue("createTime"));
        }
        adapter.addFooterItem(dates, msgs);
    }

    // list去重避免搜索结果重复
    List removeDuplicate(List list) {
        HashSet h = new HashSet(list);
        list.clear();
        list.addAll(h);
        return list;
    }

    void switchRecordingStatus() {
        recording = !recording;
    }

    public class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.ViewHolder> {
        private LayoutInflater mInflater;
        private List<Integer> dateList;
        private List<String> msgList;

        MsgAdapter(Context context, List<Integer> dateList, List<String> msgList) {
            this.mInflater = LayoutInflater.from(context);
            this.dateList = dateList;
            this.msgList = msgList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.item_here_and_now, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            final String msg = msgList.get(position);
            if (!msg.startsWith("_PIC:")) {
                holder.hereAndNowMsg.setVisibility(View.VISIBLE);

                // 高亮搜索词
                SpannableString spannableString = new SpannableString(msg);
                for (String key : keywords) {
                    // 放在循环内部才能实现多处span
                    ForegroundColorSpan span = new ForegroundColorSpan(getColor(R.color.colorPrimary));
                    // 避免大小写不一致导致找不到key的位置
                    int start = msg.toLowerCase().indexOf(key.toLowerCase());
                    if (start != -1) {
                        spannableString.setSpan(span, start, start + key.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                }
                holder.hereAndNowMsg.setText(spannableString);
            } else {
                holder.hereAndNowPic.setVisibility(View.VISIBLE);
                loadPic(msg, holder.hereAndNowPic);
                holder.hereAndNowPic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        View popupView = View.inflate(mContext, R.layout.popupwindow_preview_image, null);
                        ImageView preview = popupView.findViewById(R.id.preview_image);

                        popupView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // 显示状态栏，防止画面抖动
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        popupWindow.dismiss();
                                    }
                                }, 80);
                            }
                        });

                        popupWindow = new PopupWindow(popupView,
                                WindowManager.LayoutParams.MATCH_PARENT,
                                WindowManager.LayoutParams.MATCH_PARENT,
                                true);
                        popupWindow.setAnimationStyle(R.style.AppTheme);
                        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                            @Override
                            public void onDismiss() {
                                // 对返回时的处理
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                            }
                        });
                        popupWindow.showAtLocation(popupView, Gravity.BOTTOM, 0, 0);
                        // 隐藏状态栏，防止画面抖动
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                            }
                        }, 50);

                        String picPath = FileUtils.dirPath + msg.replace("_PIC:" + uid, "");
                        File pic = new File(picPath);
                        if (pic.exists()) {
                            Glide.with(mContext)
                                    .load(pic)
                                    .centerInside()
                                    .into(preview);
                        }
                    }
                });
            }

            long date = dateList.get(position) * 1000L;
            holder.hereAndNowDate.setText(DateUtils.formatDate(date, "yyyy年MM月dd日"));
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            // 清理glide避免RecyclerView回收复用出现图片文字混合
            if (holder.hereAndNowPic != null) {
                Glide.with(mContext).clear(holder.hereAndNowPic);
            }
            holder.hereAndNowMsg.setVisibility(View.GONE);
            holder.hereAndNowPic.setVisibility(View.GONE);
            super.onViewRecycled(holder);
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

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView hereAndNowDate;
            private TextView hereAndNowMsg;
            private ImageView hereAndNowPic;

            ViewHolder(View itemView) {
                super(itemView);
                hereAndNowDate = itemView.findViewById(R.id.hereAndNowDate);
                hereAndNowMsg = itemView.findViewById(R.id.hereAndNowMsg);
                hereAndNowPic = itemView.findViewById(R.id.hereAndNowPic);
                initListener(itemView);
            }

            private void initListener(View itemView) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        System.out.println("touching " + getAdapterPosition());
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
        CountDownTimerUtils(TextView textView, long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
            this.mTextView = textView;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            //设置倒计时时间
            mTextView.setText("录音中：" + (millisUntilFinished / 1000 + 1) + "s");
            SpannableString spannableString = new SpannableString(mTextView.getText().toString());
            ForegroundColorSpan span = new ForegroundColorSpan(getColor(R.color.colorPrimary));
            spannableString.setSpan(span, 4, 5, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            mTextView.setText(spannableString);
        }

        @Override
        public void onFinish() {
            voiceRecognition.stop();
            process();
            this.start();
            voiceRecognition.start();
        }
    }

    // 加载图片
    private void loadPic(String picName, final ImageView picItem) {
        // 这句放到限制宽高后面会导致限制失效。。原因未知
        String picPath = FileUtils.dirPath + picName.replace("_PIC:" + uid, "");
        // 获取屏幕宽度来确定ImageView的最大宽高
        int maxPixels = getResources().getDisplayMetrics().widthPixels * 5 / 9;
        // 限制ImageView的最大宽高
        int picWidth = getImageWidthHeight(picPath)[0];
        int picHeight = getImageWidthHeight(picPath)[1];
        if (picWidth >= picHeight && picWidth > maxPixels) {
            ViewGroup.LayoutParams layoutParams = picItem.getLayoutParams();
            layoutParams.width = maxPixels;
            picItem.setLayoutParams(layoutParams);
        }
        if (picWidth < picHeight && picHeight > maxPixels) {
            ViewGroup.LayoutParams layoutParams = picItem.getLayoutParams();
            layoutParams.height = maxPixels;
            picItem.setLayoutParams(layoutParams);
        }

        final RequestOptions options = RequestOptions.bitmapTransform(new RoundedCorners(15));
        File pic = new File(picPath);
        if (pic.exists()) {
            Glide.with(this)
                    .load(pic)
                    .centerInside()
                    .apply(options)
                    .into(picItem);
        } else {
            Call<ResponseBody> responseBodyCall = retrofitService.pic(picName);

            responseBodyCall.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                    String fileName = response.raw().headers().get("Content-Disposition");
                    if (fileName != null && fileName.contains("fileName=")) {
                        fileName = fileName.substring(fileName.indexOf("=") + 1).replace("_PIC:" + uid + "/", "");

                        //建立一个文件
                        final File file = FileUtils.createFile(mContext, fileName);
                        if (file.length() == 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    FileUtils.writeFile2Disk(response, file);
                                    if (file.exists()) {
                                        Glide.with(mContext)
                                                .load(file)
                                                .centerInside()
                                                .apply(options)
                                                .into(picItem);
                                    }
                                }
                            });
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    t.printStackTrace();
                }
            });
        }
    }

    // 获取图片宽高
    public static int[] getImageWidthHeight(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        /**
         * 最关键在此，把options.inJustDecodeBounds = true;
         * 这里再decodeFile()，返回的bitmap为空，但此时调用options.outHeight时，已经包含了图片的高了
         */
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options); // 此时返回的bitmap为null
        /**
         *options.outHeight为原始图片的高
         */
        return new int[]{options.outWidth, options.outHeight};
    }
}
