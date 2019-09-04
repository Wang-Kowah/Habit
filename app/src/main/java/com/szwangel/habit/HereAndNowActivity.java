package com.szwangel.habit;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.githang.statusbar.StatusBarCompat;
import com.szwangel.habit.service.RetrofitService;
import com.szwangel.habit.utils.DateUtils;
import com.szwangel.habit.utils.LocationUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class HereAndNowActivity extends AppCompatActivity implements View.OnClickListener {

    private Context mContext;

    SharedPreferences sharedPreferences;
    RetrofitService retrofitService;

    View close;
    AlertDialog.Builder alertDialog;
    RecyclerView recyclerView;
    TextView textView;
    MsgAdapter adapter;

    ArrayList<Integer> dateList;
    ArrayList<String> msgList;

    boolean permissionGranted;
    BigDecimal lat;
    BigDecimal lng;
    int uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_here_and_now);
        //设置状态栏的颜色
        StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.colorPrimary));
        mContext = this;

        initView();
        initListener();
        initPermission();

        sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE);
        uid = sharedPreferences.getInt("uid", -1);

        MyApplication application = (MyApplication) getApplication();
        String domain = application.getDomain();
        retrofitService = new Retrofit.Builder()
                .baseUrl(domain)
                .build()
                .create(RetrofitService.class);

        updateMsg();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.close_hereandnow:
                finish();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 777) {
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void initView() {
        close = findViewById(R.id.close_hereandnow);
        textView = findViewById(R.id.hereAndNowTextView);

        dateList = new ArrayList<>();
        msgList = new ArrayList<>();
        adapter = new MsgAdapter(this, dateList, msgList);
        recyclerView = findViewById(R.id.hereAndNowRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        alertDialog = new android.support.v7.app.AlertDialog.Builder(mContext)
                .setTitle("定位失败")
                .setMessage("本功能基于当前时间与地点来展示您过去在此时此地发送过的帖子，需要开启定位才能正常使用")
                .setPositiveButton("重新加载", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        recreate();
                    }
                })
                .setNegativeButton("手动开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (permissionGranted) {
                            openLocationSetting();
                        } else {
                            initPermission();
                        }
                    }
                })
                .setCancelable(false);
    }

    private void initListener() {
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
    }

    private void initPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //检查权限
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //请求权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 777);
            } else {
                permissionGranted = true;
            }
        } else {
            permissionGranted = true;
        }
    }

    void updateMsg() {
        if (permissionGranted) {
            getGPSLocation();
            getBestLocation();
        } else {
            alertDialog.show();
        }

        if (lat != null && lng != null) {
            Call<ResponseBody> call = retrofitService.hereAndNow(uid, lat, lng);
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
                            if (jsonObject.getIntValue("size") == 0) {
                                Toast toast = Toast.makeText(mContext, "暂无相关历史内容", Toast.LENGTH_LONG);
                                toast.setText("暂无相关历史内容");
                                toast.show();
                            } else {
                                recyclerView.setVisibility(View.VISIBLE);
                                textView.setVisibility(View.GONE);

                                JSONArray jsonArray = jsonObject.getJSONArray("noteList");
                                ArrayList<Integer> dates = new ArrayList<>();
                                ArrayList<String> msgs = new ArrayList<>();
                                for (int i = 0; i < jsonArray.size(); i++) {
                                    JSONObject object = jsonArray.getJSONObject(i);
                                    msgs.add(object.getString("content"));
                                    dates.add(object.getIntValue("createTime"));
                                }
                                adapter.addFooterItem(dates, msgs);
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
        } else {
            alertDialog.show();
        }
    }

    // 通过GPS获取定位信息
    public void getGPSLocation() {
        Location gps = LocationUtils.getGPSLocation(mContext);
        if (gps == null) {
            //设置定位监听，因为GPS定位，第一次进来可能获取不到，通过设置监听，可以在有效的时间范围内获取定位信息
            LocationUtils.addLocationListener(mContext, LocationManager.GPS_PROVIDER, new LocationUtils.ILocationListener() {
                @Override
                public void onSuccessLocation(Location location) {
                    if (location != null) {
                        lat = new BigDecimal(location.getLatitude());
                        lng = new BigDecimal(location.getLongitude());
                        LocationUtils.unRegisterListener(mContext);
//                        Toast.makeText(mContext, "gps onSuccessLocation location:  lat==" + location.getLatitude() + "     lng==" + location.getLongitude(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            lat = new BigDecimal(gps.getLatitude());
            lng = new BigDecimal(gps.getLongitude());
//            Toast.makeText(mContext, "gps location: lat==" + gps.getLatitude() + "  lng==" + gps.getLongitude(), Toast.LENGTH_SHORT).show();
        }
    }

    // 采用最好的方式获取定位信息
    private void getBestLocation() {
        Criteria c = new Criteria();//Criteria类是设置定位的标准信息（系统会根据你的要求，匹配最适合你的定位供应商），一个定位的辅助信息的类
        c.setPowerRequirement(Criteria.POWER_LOW);//设置低耗电
        c.setAltitudeRequired(true);//设置需要海拔
        c.setBearingAccuracy(Criteria.ACCURACY_COARSE);//设置COARSE精度标准
        c.setAccuracy(Criteria.ACCURACY_LOW);//设置低精度
        //... Criteria 还有其他属性，就不一一介绍了
        Location best = LocationUtils.getBestLocation(mContext, c);
        if (best != null) {
            lat = new BigDecimal(best.getLatitude());
            lng = new BigDecimal(best.getLongitude());
//            Toast.makeText(mContext, "best location: lat==" + best.getLatitude() + " lng==" + best.getLongitude(), Toast.LENGTH_SHORT).show();
        }
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
            View itemView = mInflater.inflate(R.layout.item_here_and_now, parent, false);
            return new MsgAdapter.ItemViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            MsgAdapter.ItemViewHolder itemViewHolder = (MsgAdapter.ItemViewHolder) holder;

            long date = dateList.get(position) * 1000L;
            itemViewHolder.hereAndNowDate.setText(DateUtils.formatDate(date, "yyyy年MM月dd日"));

            String msg = msgList.get(position);
            itemViewHolder.hereAndNowMsg.setText(msg);
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

        class ItemViewHolder extends RecyclerView.ViewHolder {
            private TextView hereAndNowDate;
            private TextView hereAndNowMsg;

            ItemViewHolder(View itemView) {
                super(itemView);
                hereAndNowDate = itemView.findViewById(R.id.hereAndNowDate);
                hereAndNowMsg = itemView.findViewById(R.id.hereAndNowMsg);
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

    // 跳转到设置开启定位
    public void openLocationSetting() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }
}
