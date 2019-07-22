package com.szwangel.habit;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.githang.statusbar.StatusBarCompat;
import com.szwangel.habit.service.RetrofitService;
import com.szwangel.habit.utils.DateUtils;
import com.szwangel.habit.utils.LocationUtils;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SearchActivity extends AppCompatActivity implements View.OnClickListener {
    private boolean flag;

    Context mContext;
    EditText searchText;
    TextView cancel;
    RecyclerView recyclerView;
    RefreshAdapter adapter;

    SharedPreferences sharedPreferences;
    RetrofitService retrofitService;

    ArrayList<Integer> dateList;
    ArrayList<String> msgList;

    int pageSize = 10;
    int pageNum;
    int uid;
    String key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_search);
        //设置状态栏的颜色
        StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.colorPrimary));
        mContext = SearchActivity.this;

        initView();
        initListener();

        sharedPreferences = getSharedPreferences("user_data", MODE_PRIVATE);
        uid = sharedPreferences.getInt("uid", -1);

        MyApplication application = (MyApplication) getApplication();
        String domain = application.getDomain();
        retrofitService = new Retrofit.Builder()
                .baseUrl(domain)
                .build()
                .create(RetrofitService.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initPermission();//针对6.0以上版本做权限适配
    }

    private void initView() {
        searchText = findViewById(R.id.search_edittext);
        searchText.setHintTextColor(getResources().getColor(R.color.halfWhite));
        cancel = findViewById(R.id.cancel_search);

        dateList = new ArrayList<>();
        msgList = new ArrayList<>();
        adapter = new RefreshAdapter(this, dateList, msgList);
        recyclerView = findViewById(R.id.searchRecyclerView);
        recyclerView.setVisibility(View.GONE);
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
                    updateMsg();
                }
            }
        });
    }

    private void initListener() {
        cancel.setOnClickListener(this);
        cancel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    cancel.setBackground(getDrawable(R.color.colorText));
                    cancel.setAlpha(0.3F);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    cancel.setBackground(getDrawable(R.color.colorPrimary));
                    cancel.setAlpha(1);
                }
                return false;
            }
        });
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && KeyEvent.KEYCODE_ENTER == event.getKeyCode() && KeyEvent.ACTION_DOWN == event.getAction())) {
                    key = v.getText().toString();
                    if (TextUtils.isEmpty(key)) {
                        Toast.makeText(mContext, "您还未输入要搜索的内容", Toast.LENGTH_LONG).show();
                    } else {
                        dateList.clear();
                        msgList.clear();
                        adapter.notifyDataSetChanged();
                        recyclerView.setVisibility(View.VISIBLE);
                        pageNum = 1;
                        updateMsg();
                        hideSoftInput(v.getWindowToken());
                    }
                }
                return true;
            }
        });
    }

    private void initPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //检查权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //请求权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                flag = true;
            }
        } else {
            flag = true;
        }
    }

    /**
     * 权限的结果回调函数
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            flag = grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel_search:
                finish();
                break;
        }
    }

    void updateMsg() {
        Call<ResponseBody> call = retrofitService.search(uid, key, pageNum++, pageSize);
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
                        if (result.getIntValue("total") == 0) {
                            Toast toast = Toast.makeText(mContext, "没有搜索到相关的内容", Toast.LENGTH_LONG);
                            toast.setText("没有搜索到相关的内容");
                            toast.show();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.setFooterVisibility(View.GONE);
                                }
                            }, 1200);
                        } else if (result.getIntValue("total") == dateList.size()) {
                            Toast toast = Toast.makeText(mContext, "没有更多消息啦", Toast.LENGTH_SHORT);
                            toast.setText("没有更多消息啦");
                            toast.show();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.setFooterVisibility(View.GONE);
                                }
                            }, 1200);
                        } else {
                            JSONArray jsonArray = result.getJSONArray("list");
                            ArrayList<Integer> dates = new ArrayList<>();
                            ArrayList<String> msgs = new ArrayList<>();
                            for (int i = 0; i < jsonArray.size(); i++) {
                                JSONObject object = jsonArray.getJSONObject(i);
                                msgs.add(object.getString("content"));
                                dates.add(object.getIntValue("createTime"));
                            }
                            adapter.addFooterItem(dates, msgs);
                            adapter.setFooterVisibility(View.GONE);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                adapter.setFooterVisibility(View.GONE);
                Toast toast = Toast.makeText(mContext, "网络异常，请稍后重试", Toast.LENGTH_SHORT);
                toast.setText("网络异常，请稍后重试");
                toast.show();
                t.printStackTrace();
            }
        });
    }

    public class RefreshAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private Context mContext;
        private LayoutInflater mInflater;
        private List<Integer> dateList;
        private List<String> msgList;
        private RefreshAdapter.FooterViewHolder footerViewHolder;

        private static final int TYPE_ITEM = 0;
        private static final int TYPE_FOOTER = 1;

        RefreshAdapter(Context context, List<Integer> dateList, List<String> msgList) {
            this.mContext = context;
            this.mInflater = LayoutInflater.from(context);
            this.dateList = dateList;
            this.msgList = msgList;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_ITEM) {
                View itemView = mInflater.inflate(R.layout.item_search, parent, false);
                return new RefreshAdapter.ItemViewHolder(itemView);
            } else if (viewType == TYPE_FOOTER) {
                View itemView = mInflater.inflate(R.layout.footer_load_more, parent, false);
                return new RefreshAdapter.FooterViewHolder(itemView);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof RefreshAdapter.ItemViewHolder) {
                RefreshAdapter.ItemViewHolder itemViewHolder = (RefreshAdapter.ItemViewHolder) holder;

                long date = dateList.get(position) * 1000L;
                itemViewHolder.searchDate.setText(DateUtils.formatDate(date, "yyyy年MM月dd日"));

                String msg = msgList.get(position);
                itemViewHolder.searchMsg.setText(msg);
            } else if (holder instanceof RefreshAdapter.FooterViewHolder) {
                footerViewHolder = (RefreshAdapter.FooterViewHolder) holder;
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
            msgList.addAll(0, keys);
            notifyDataSetChanged();
        }

        void addFooterItem(List<Integer> dates, List<String> keys) {
            dateList.addAll(dates);
            msgList.addAll(keys);
            notifyDataSetChanged();
        }

        // 显示/隐藏加载条
        void setFooterVisibility(int id) {
            footerViewHolder.itemView.setVisibility(id);
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

        class FooterViewHolder extends RecyclerView.ViewHolder {
            FooterViewHolder(View itemView) {
                super(itemView);
            }
        }
    }

    // 隐藏软键盘
    private void hideSoftInput(IBinder token) {
        if (token != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * 通过GPS获取定位信息
     */
    public void getGPSLocation() {
        Location gps = LocationUtils.getGPSLocation(this);
        if (gps == null) {
            //设置定位监听，因为GPS定位，第一次进来可能获取不到，通过设置监听，可以在有效的时间范围内获取定位信息
            LocationUtils.addLocationListener(mContext, LocationManager.GPS_PROVIDER, new LocationUtils.ILocationListener() {
                @Override
                public void onSuccessLocation(Location location) {
                    if (location != null) {
                        Toast.makeText(SearchActivity.this, "gps onSuccessLocation location:  lat==" + location.getLatitude() + "     lng==" + location.getLongitude(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SearchActivity.this, "gps location is null", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Toast.makeText(this, "gps location: lat==" + gps.getLatitude() + "  lng==" + gps.getLongitude(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 通过网络等获取定位信息
     */
    private void getNetworkLocation() {
        Location net = LocationUtils.getNetWorkLocation(this);
        if (net == null) {
            Toast.makeText(this, "net location is null", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "network location: lat==" + net.getLatitude() + "  lng==" + net.getLongitude(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 采用最好的方式获取定位信息
     */
    private void getBestLocation() {
        Criteria c = new Criteria();//Criteria类是设置定位的标准信息（系统会根据你的要求，匹配最适合你的定位供应商），一个定位的辅助信息的类
        c.setPowerRequirement(Criteria.POWER_LOW);//设置低耗电
        c.setAltitudeRequired(true);//设置需要海拔
        c.setBearingAccuracy(Criteria.ACCURACY_COARSE);//设置COARSE精度标准
        c.setAccuracy(Criteria.ACCURACY_LOW);//设置低精度
        //... Criteria 还有其他属性，就不一一介绍了
        Location best = LocationUtils.getBestLocation(this, c);
        if (best == null) {
            Toast.makeText(this, " best location is null", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "best location: lat==" + best.getLatitude() + " lng==" + best.getLongitude(), Toast.LENGTH_SHORT).show();
        }
    }
}
