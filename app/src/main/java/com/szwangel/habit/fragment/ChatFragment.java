package com.szwangel.habit.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.daasuu.bl.BubbleLayout;
import com.githang.statusbar.StatusBarCompat;
import com.szwangel.habit.MyApplication;
import com.szwangel.habit.R;
import com.szwangel.habit.RingReceiver;
import com.szwangel.habit.service.RetrofitService;
import com.szwangel.habit.utils.DateUtils;
import com.szwangel.habit.utils.FileUtils;
import com.szwangel.habit.utils.LocationUtils;
import com.xiasuhuei321.loadingdialog.view.LoadingDialog;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

public class ChatFragment extends Fragment {

    private static final int REQUEST_GALLERY_IMAGE = 10;// 图库选取图片标识请求码
    private static final int REQUEST_CAMERA_IMAGE = 11;// 拍照标识请求码

    SharedPreferences sharedPreferences;
    RetrofitService retrofitService;
    AlarmManager alarmManager;
    MsgAdapter adapter;
    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;
    PopupWindow popupWindow;

    LinkedList<String> msgList;
    LinkedList<Long> dateList;

    // 当前页面
    int currentTab = -1;
    // 每次更新的消息条数
    int pageSize = 10;
    int pageNum;
    int uid;
    int sentMsgNum;
    String picPath;

    public boolean permissionGranted;
    BigDecimal lat;
    BigDecimal lng;


    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, final Bundle savedInstanceState) {
        initPermission();

        pageNum = 1;
        sentMsgNum = 0;
        msgList = new LinkedList<>();
        dateList = new LinkedList<>();

//        alarmManager = (AlarmManager) getActivity().getApplicationContext().getSystemService(Service.ALARM_SERVICE);

        sharedPreferences = getActivity().getSharedPreferences("user_data", MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        final View view = inflater.inflate(R.layout.fragment_chat, container, false);
        final TextView timeButton = view.findViewById(R.id.timeButton);
        final EditText editText = view.findViewById(R.id.input_node);
        final ImageView plusButton = view.findViewById(R.id.plusButton);
        final View menuBar = view.findViewById(R.id.menuBar);

        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupWindow(menuBar);
            }
        });

        if (getArguments() != null && getArguments().getInt("tab", -1) == 0) {
            view.findViewById(R.id.week).setVisibility(View.GONE);
            editText.setHint("   随时记录自己的想法或总结");

//            timeButton.setText(sharedPreferences.getString("time1", "22:30"));
            timeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    DatePickDialog dialog = new DatePickDialog(getContext());
//                    //设置标题
//                    dialog.setTitle("选择闹钟时间");
//                    //设置类型
//                    dialog.setType(DateType.TYPE_HM);
//                    //设置消息体的显示格式，日期格式
//                    dialog.setMessageFormat("每天  HH:mm");
//                    //设置选择回调
//                    dialog.setOnChangeLisener(null);
//                    //设置点击确定按钮回调
//                    dialog.setOnSureLisener(new OnSureLisener() {
//                        @Override
//                        public void onSure(Date date) {
//                            int hour = date.getHours(), minute = date.getMinutes();
//
//                            String time = String.format(Locale.CHINA, "%02d:%02d", hour, minute);
//                            timeButton.setText(time);
////                            Toast toast = Toast.makeText(getContext(), "闹钟设置成功", Toast.LENGTH_SHORT);
////                            toast.setText("闹钟设置成功");
////                            if (!time.equals(sharedPreferences.getString("time1", "22:30"))) {
////                                toast.setText("闹钟设置成功，请手动删除旧闹钟");
////                            }
////                            toast.show();
//
//                            editor.putString("time1", time);
//                            editor.apply();

//                            createAlarm("【习惯APP】每天总结", hour, minute, -1);
                    new android.support.v7.app.AlertDialog.Builder(getContext())
                            .setTitle("温馨提示")
                            .setMessage("闹铃设置-每天总结，如有需要您可以自己去系统闹铃设置闹铃，建议文字提示是：【习惯】每天总结")
                            .setPositiveButton("确认", null)
                            .setCancelable(false)
                            .show();
//                            setBroadcastAlarm(hour, minute);
//                            new Handler().postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    showAlarm();
//                                }
//                            }, 300);

//                        }
//                    });
//                    dialog.show();
                }
            });

            currentTab = 0;
            System.out.println("day fragment created");
        } else {
//            final TextView dateTextView = view.findViewById(R.id.dateText);
//            dateTextView.setText(sharedPreferences.getString("dayInWeek", "六"));
//            timeButton.setText(sharedPreferences.getString("time2", "18:50"));

            view.findViewById(R.id.dateButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    final String[] dayInWeek = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
//
//                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
//                    AlertDialog alertDialog = builder.setItems(dayInWeek, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, final int selected) {
//                            final String daySelected = dayInWeek[selected].substring(1);
//                            DatePickDialog weekDialog = new DatePickDialog(getContext());
//                            //设置标题
//                            weekDialog.setTitle("选择闹钟时间");
//                            //设置类型
//                            weekDialog.setType(DateType.TYPE_HM);
//                            //设置消息体的显示格式，日期格式
//                            weekDialog.setMessageFormat("每" + dayInWeek[selected] + "  HH:mm");
//                            //设置选择回调
//                            weekDialog.setOnChangeLisener(null);
//                            //设置点击确定按钮回调
//                            weekDialog.setOnSureLisener(new OnSureLisener() {
//                                @Override
//                                public void onSure(Date date) {
//                                    int hour = date.getHours(), minute = date.getMinutes();
//
//                                    String time = String.format(Locale.CHINA, "%02d:%02d", hour, minute);
//                                    timeButton.setText(time);
//                                    dateTextView.setText(daySelected);
//                                    Toast toast = Toast.makeText(getContext(), "闹钟设置成功", Toast.LENGTH_SHORT);
//                                    toast.setText("闹钟设置成功");
//                                    if (!time.equals(sharedPreferences.getString("time2", "18:50")) || !daySelected.equals(sharedPreferences.getString("dayInWeek", "六"))) {
//                                        toast.setText("闹钟设置成功，请手动删除旧闹钟");
//                                    }
//                                    toast.show();

//                                    editor.putString("dayInWeek", daySelected);
//                                    editor.putString("time2", time);
//                                    editor.apply();

//                                    createAlarm("【习惯APP】每周总结", hour, minute, selected);
                    new android.support.v7.app.AlertDialog.Builder(getContext())
                            .setTitle("温馨提示")
                            .setMessage("闹铃设置-每周总结，如有需要您可以自己去系统闹铃设置闹铃，建议文字提示是：【习惯】每周总结")
                            .setPositiveButton("确认", null)
                            .setCancelable(false)
                            .show();
//                                    setBroadcastAlarm(hour, minute);
//                                    new Handler().postDelayed(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            showAlarm();
//                                        }
//                                    }, 300);
//                                }
//                            });
//                            weekDialog.show();
//                        }
//                    }).setTitle("选择要做总结的时间").create();
//                    alertDialog.show();

                    // 设置宽度
//                    WindowManager.LayoutParams layoutParams = alertDialog.getWindow().getAttributes();
//                    layoutParams.width = getResources().getDisplayMetrics().widthPixels * 5 / 6;
//                    layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
//                    alertDialog.getWindow().setAttributes(layoutParams);
                }
            });

            currentTab = 2;
            System.out.println("week fragment created");
        }

        uid = sharedPreferences.getInt("uid", -1);
        MyApplication application = (MyApplication) getActivity().getApplication();
        String domain = application.getDomain();
        retrofitService = new Retrofit.Builder()
                .baseUrl(domain)
                .build()
                .create(RetrofitService.class);

        // 动态设置行数来避免EditText在多行状态下对ImeOptions的强制设置导致回车键样式修改失败
        editText.setMaxLines(5);
        editText.setHorizontallyScrolling(false);
        editText.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            // 当键盘弹出隐藏的时候会 调用此方法。
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                // 获取当前界面可视部分
                FragmentActivity fragmentActivity = getActivity();
                if (fragmentActivity != null) {
                    fragmentActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
                    // 获取屏幕的高度
                    int screenHeight = fragmentActivity.getWindow().getDecorView().getRootView().getHeight();
                    // 此处就是用来获取键盘的高度的， 在键盘没有弹出的时候 此高度为0 键盘弹出的时候为一个正数
                    int heightDifference = screenHeight - r.bottom;
                    if (heightDifference != 0) {
                        // 点击EditText会跳回最后一行
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                recyclerView.scrollToPosition(0);
                            }
                        }, 100);
                    }
                }
            }
        });
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && KeyEvent.KEYCODE_ENTER == event.getKeyCode() && KeyEvent.ACTION_DOWN == event.getAction())) {
                    final String input = v.getText().toString();
                    if (!input.trim().equals("")) {
                        v.setText("");
                        if (permissionGranted) {
                            getGPSLocation();
                            getBestLocation();
                        }
                        // 手动更新消息列表
                        msgList.addFirst(input);
                        dateList.addFirst(System.currentTimeMillis() / 1000);
                        adapter.notifyItemInserted(0);
                        recyclerView.scrollToPosition(0);
                        sentMsgNum++;

                        Call<ResponseBody> call = null;
                        switch (currentTab) {
                            case 0:
                                call = retrofitService.sendNote(uid, 0, input, lat, lng);
                                break;
                            case 2:
                                call = retrofitService.sendNote(uid, 1, input, lat, lng);
                                break;
                            default:
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
                                        Toast toast = Toast.makeText(getContext(), jsonObject.getString("msg"), Toast.LENGTH_SHORT);
                                        toast.setText(jsonObject.getString("msg"));
                                        toast.show();
                                    }

//                                    updateMsg(false);
                                } catch (IOException e) {
                                    Toast toast = Toast.makeText(getContext(), "网络异常，请稍后重试", Toast.LENGTH_SHORT);
                                    toast.setText("网络异常，请稍后重试");
                                    toast.show();
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Toast toast = Toast.makeText(getContext(), "网络异常，请稍后重试", Toast.LENGTH_SHORT);
                                toast.setText("网络异常，请稍后重试");
                                toast.show();
                                t.printStackTrace();
                            }
                        });
                    }
                }
                return true;
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        adapter = new MsgAdapter(getContext(), msgList, dateList);
        recyclerView = view.findViewById(R.id.chatRecyclerView);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout = view.findViewById(R.id.swipe);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 可见状态下才进行刷新，防止误触
                if (getUserVisibleHint()) {
                    updateMsg();
                }
            }
        });

        updateMsg();

        // 获取定位
        if (permissionGranted) {
            getGPSLocation();
            getBestLocation();
        } else {
            initPermission();
        }

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 777) {
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_GALLERY_IMAGE && null != data) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                picPath = cursor.getString(columnIndex);
                cursor.close();
                sendPic(picPath);
            } else if (requestCode == REQUEST_CAMERA_IMAGE) {
                sendPic(picPath);
            }
        }
    }

    // 自定义RecyclerViewAdapter
    class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.ViewHolder> {
        private Context context;
        private List<String> msgList;
        private List<Long> dateList;

        MsgAdapter(Context context, List<String> msgList, List<Long> dateList) {
            this.context = context;
            this.msgList = msgList;
            this.dateList = dateList;
        }

        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            final String msg = msgList.get(position);
            if (!msg.startsWith("_PIC:")) {
                holder.msg.setVisibility(View.VISIBLE);
                holder.msg.setText(msg);
            } else {
                holder.bubbleLayout.setPadding(0, 0, 0, 0);
                holder.pic.setVisibility(View.VISIBLE);
                loadPic(msg, holder.pic);
                holder.pic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        View popupView = View.inflate(getContext(), R.layout.popupwindow_preview_image, null);
                        ImageView preview = popupView.findViewById(R.id.preview_image);

                        popupView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                popupWindow.dismiss();
                            }
                        });

                        //获取屏幕宽高
                        int weight = getResources().getDisplayMetrics().widthPixels;
                        int height = getResources().getDisplayMetrics().heightPixels;

                        popupWindow = new PopupWindow(popupView, weight, height);
                        popupWindow.setAnimationStyle(R.style.AppTheme);
                        popupWindow.setFocusable(true);
                        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                            @Override
                            public void onDismiss() {
                                StatusBarCompat.setStatusBarColor(getActivity(), getResources().getColor(R.color.colorPrimary));
                            }
                        });
                        popupWindow.showAtLocation(popupView, Gravity.BOTTOM, 0, 0);
                        StatusBarCompat.setStatusBarColor(getActivity(), getResources().getColor(R.color.black));

                        String picPath = FileUtils.dirPath + msg.replace("_PIC:" + uid, "");
                        File pic = new File(picPath);
                        if (pic.exists()) {
                            Glide.with(context)
                                    .load(pic)
                                    .centerInside()
                                    .into(preview);
                        }
                    }
                });
            }

            // 确定昨天跟今天的时间显示格式
            long date = dateList.get(position) * 1000L;
            long now = System.currentTimeMillis();
            if (date > DateUtils.getDayBeginTimestamp(now, 1) && date < DateUtils.getDayBeginTimestamp(now, 0)) {
                holder.msgDate.setText("昨天 " + DateUtils.formatDate(date, "HH:mm"));
            } else if (date > DateUtils.getDayBeginTimestamp(now, 0)) {
                holder.msgDate.setText(DateUtils.formatDate(date, "HH:mm"));
            } else {
                holder.msgDate.setText(DateUtils.formatDate(date));
            }

            File file = new File(sharedPreferences.getString("mLastProfilePath", ""));
            if (file.exists()) {
                Glide.with(context)
                        .load(file)
                        .into(holder.profile);
            }

//            holder.itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    holder.itemView.requestFocus();
//                    holder.itemView.clearFocus();
//                }
//            });

        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            if (holder.pic != null) {
                Glide.with(context).clear(holder.pic);
            }
            super.onViewRecycled(holder);
        }

        @Override
        public int getItemCount() {
            return msgList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private BubbleLayout bubbleLayout;
            private TextView msg;
            private ImageView pic;
            private TextView msgDate;
            private ImageView profile;

            ViewHolder(View itemView) {
                super(itemView);
                bubbleLayout = itemView.findViewById(R.id.bubble);
                msg = itemView.findViewById(R.id.msg);
                pic = itemView.findViewById(R.id.pic);
                msgDate = itemView.findViewById(R.id.msgDate);
                profile = itemView.findViewById(R.id.chatProfile);
            }
        }
    }

    // 更新列表
    private void updateMsg() {
        Call<ResponseBody> call = null;
        switch (currentTab) {
            case 0:
                call = retrofitService.noteList(uid, 0, pageNum++, pageSize);
                break;
            case 2:
                call = retrofitService.noteList(uid, 1, pageNum++, pageSize);
                break;
            default:
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
                        Toast toast = Toast.makeText(getContext(), jsonObject.getString("msg"), Toast.LENGTH_SHORT);
                        toast.setText(jsonObject.getString("msg"));
                        toast.show();
                    } else {
                        if (jsonObject.getJSONObject("noteList").getIntValue("total") == msgList.size()) {
                            Toast toast = Toast.makeText(getContext(), "没有更多消息啦", Toast.LENGTH_SHORT);
                            toast.setText("没有更多消息啦");
                            if (getUserVisibleHint()) {
                                toast.show();
                            }
                        } else {
                            JSONArray jsonArray = jsonObject.getJSONObject("noteList").getJSONArray("list");
                            for (int i = 0; i < jsonArray.size(); i++) {
                                JSONObject object = jsonArray.getJSONObject(i);

                                // 排除发送新消息后拉取到的重复数据
                                if (!dateList.contains(object.getLongValue("createTime"))) {
                                    msgList.add(object.getString("content"));
                                    dateList.add(object.getLongValue("createTime"));

                                    //有消息刷新时显示，不在更新数据的同一线程调用会引发RecyclerView自身bug
                                    adapter.notifyItemInserted(msgList.size() - 1 + sentMsgNum);
                                    recyclerView.scrollToPosition(msgList.size() - jsonArray.size());
                                }
                            }
                        }
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }, 600);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                pageNum = 1;
                Toast toast = Toast.makeText(getActivity(), "网络异常，请稍后重试", Toast.LENGTH_SHORT);
                toast.setText("网络异常，请稍后重试");
                toast.show();
                t.printStackTrace();
            }
        });
    }

    // 创建闹钟
    private void createAlarm(String message, int hour, int minutes, int selected) {
        ArrayList<Integer> weekDays = new ArrayList<>();
        weekDays.add(Calendar.MONDAY);
        weekDays.add(Calendar.TUESDAY);
        weekDays.add(Calendar.WEDNESDAY);
        weekDays.add(Calendar.THURSDAY);
        weekDays.add(Calendar.FRIDAY);
        weekDays.add(Calendar.SATURDAY);
        weekDays.add(Calendar.SUNDAY);

//        String packageName = getActivity().getApplication().getPackageName();
//        Uri ringtoneUri = Uri.parse("android.resource://" + packageName + "/" + resId);

        //action为AlarmClock.ACTION_SET_ALARM
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                //闹钟的小时
                .putExtra(AlarmClock.EXTRA_HOUR, hour)
                //闹钟的分钟
                .putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                //响铃时提示的信息
                .putExtra(AlarmClock.EXTRA_MESSAGE, message)
                //用于指定该闹铃触发时是否振动
                .putExtra(AlarmClock.EXTRA_VIBRATE, false)
                //一个 content: URI，用于指定闹铃使用的铃声，也可指定 VALUE_RINGTONE_SILENT 以不使用铃声。
                //如需使用默认铃声，则无需指定此 extra。
//                .putExtra(AlarmClock.EXTRA_RINGTONE, ringtoneUri)
                //一个 ArrayList，其中包括应重复触发该闹铃的每个周日。
                // 每一天都必须使用 Calendar 类中的某个整型值（如 MONDAY）进行声明。
                //对于一次性闹铃，无需指定此 extra
                .putExtra(AlarmClock.EXTRA_DAYS, weekDays)
                //如果为true，则调用startActivity()不会进入手机的闹钟设置界面
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true);

        // 选定某一天
        if (selected != -1) {
            ArrayList<Integer> oneDay = new ArrayList<>();
            oneDay.add(weekDays.get(selected));
            intent.putExtra(AlarmClock.EXTRA_DAYS, oneDay);
        }

        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    // 显示闹钟
    private void showAlarm() {
        Intent intent = new Intent(AlarmClock.ACTION_SHOW_ALARMS)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, false);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    // 设置闹钟广播
    private void setBroadcastAlarm(int hour, int minutes) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minutes);
        c.set(Calendar.SECOND, 0);

        Intent intent = new Intent(this.getActivity(), RingReceiver.class);
        intent.setAction("com.kowah.habit.Ring");
        intent.putExtra("tab", currentTab);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getActivity(), currentTab, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(c.getTimeInMillis(),pendingIntent) ,pendingIntent);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
    }

    private void initPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //检查权限
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //请求权限
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 777);
            } else {
                permissionGranted = true;
            }
        } else {
            permissionGranted = true;
        }
    }

    // 通过GPS获取定位信息
    public void getGPSLocation() {
        Location gps = LocationUtils.getGPSLocation(getContext());
        if (gps == null) {
            //设置定位监听，因为GPS定位，第一次进来可能获取不到，通过设置监听，可以在有效的时间范围内获取定位信息
            LocationUtils.addLocationListener(getContext(), LocationManager.GPS_PROVIDER, new LocationUtils.ILocationListener() {
                @Override
                public void onSuccessLocation(Location location) {
                    if (location != null) {
                        lat = new BigDecimal(location.getLatitude());
                        lng = new BigDecimal(location.getLongitude());
                        LocationUtils.unRegisterListener(getContext());
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
        Location best = LocationUtils.getBestLocation(getContext(), c);
        if (best != null) {
            lat = new BigDecimal(best.getLatitude());
            lng = new BigDecimal(best.getLongitude());
//            Toast.makeText(mContext, "best location: lat==" + best.getLatitude() + " lng==" + best.getLongitude(), Toast.LENGTH_SHORT).show();
        }
    }

    // 弹窗选择相册或拍照
    private void showPopupWindow(final View v) {
        View popupView = View.inflate(getContext(), R.layout.popupwindow_plus_menu, null);
        View menuAlbum = popupView.findViewById(R.id.menuAlbum);
        View menuCamera = popupView.findViewById(R.id.menuCamera);

        menuAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, REQUEST_GALLERY_IMAGE);
                }
                popupWindow.dismiss();
            }
        });
        menuCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
                popupWindow.dismiss();
            }
        });

        //获取屏幕宽高
        int weight = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels / 3;

        popupWindow = new PopupWindow(popupView, weight, height);
        popupWindow.setAnimationStyle(R.style.Animation_Design_BottomSheetDialog);
        popupWindow.setFocusable(true);
        //点击外部popupWindow消失
        popupWindow.setOutsideTouchable(true);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) v.getLayoutParams();
                lp.bottomMargin = 0;
                v.setLayoutParams(lp);
            }
        });

        // 主动计算高度
        popupWindow.getContentView().measure(0, 0);
        popupWindow.showAtLocation(v, Gravity.BOTTOM, 0, 0);

        final RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) v.getLayoutParams();
        // 获取popupWindow高度
        lp.bottomMargin = popupWindow.getContentView().getMeasuredHeight();
        // 与动画同步
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                v.setLayoutParams(lp);
            }
        }, 200);
    }

    // 确认权限
    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(), new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
            }, 666);

            return false;
        }
        return true;
    }

    // 发送图片
    private void sendPic(String picPath) {
        if (permissionGranted) {
            getGPSLocation();
            getBestLocation();
        }

        final LoadingDialog loadingDialog = new LoadingDialog(getContext());
        loadingDialog.setLoadingText("发送中,请稍等...")
                .setSuccessText("发送成功")
                .setFailedText("发送失败，请稍后再试！")
                .setInterceptBack(true)
                .setRepeatCount(0)
                .show();

        if (getImageMIMEType(picPath).contains("gif")) {
            loadingDialog.setFailedText("暂不支持GIF格式的图片");
            loadingDialog.loadFailed();
            return;
        } else if (getImageMIMEType(picPath).contains("webp")) {
            loadingDialog.setFailedText("暂不支持WebP格式的图片");
            loadingDialog.loadFailed();
            return;
        }

        final File file = new File(picPath);
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part pic = MultipartBody.Part.createFormData("pic", file.getName(), requestFile);
        Call<ResponseBody> call = null;
        switch (currentTab) {
            case 0:
                call = retrofitService.sendPic(uid, 0, lat, lng, pic);
                break;
            case 2:
                call = retrofitService.sendPic(uid, 1, lat, lng, pic);
                break;
            default:
                break;
        }
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                String json;
                try {
                    json = response.body().string();
                    JSONObject jsonObject = JSONObject.parseObject(json);
                    if (jsonObject.getInteger("retcode").equals(0)) {
                        // 直接复制图片
                        String picName = jsonObject.getString("picName");
                        File pic = FileUtils.createFile(getContext(), picName.replace("_PIC:" + uid + "/", ""));
                        FileUtils.copyFile(file, pic);

                        // 手动更新消息列表
                        msgList.addFirst(picName);
                        dateList.addFirst(Long.parseLong(picName.substring(0, picName.indexOf(".")).replace("_PIC:" + uid + "/", "")));
                        sentMsgNum++;
                        adapter.notifyItemInserted(0);
                        recyclerView.scrollToPosition(0);

                        loadingDialog.loadSuccess();
                    } else {
                        loadingDialog.loadFailed();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    loadingDialog.loadFailed();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                loadingDialog.loadFailed();
                Toast toast = Toast.makeText(getContext(), "网络异常，请稍后重试", Toast.LENGTH_SHORT);
                toast.setText("网络异常，请稍后重试");
                toast.show();
            }
        });
    }

    // 调用摄像头
    private void takePhoto() {
        if (checkPermission()) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                // Create the File where the photo should go
                File photo = FileUtils.createImageFile();
                // Continue only if the File was successfully created
                if (photo != null) {
                    // api24后需要使用FileProvider
                    picPath = photo.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(getContext(), "com.szwangel.fileprovider", photo));
                }
            }
            // 跳转界面传回拍照所得数据
            startActivityForResult(takePictureIntent, REQUEST_CAMERA_IMAGE);
        }
    }

    // 加载图片
    private void loadPic(String picName, final ImageView picItem) {
        String picPath = FileUtils.dirPath + picName.replace("_PIC:" + uid, "");
        //获取屏幕宽度
        int widthPixels = getResources().getDisplayMetrics().widthPixels * 2 / 3;
        // 图片尺寸太大时，限制ImageView宽度为屏幕的2/3
        if (getImageWidthHeight(picPath)[0] > widthPixels) {
            ViewGroup.LayoutParams layoutParams = picItem.getLayoutParams();
            layoutParams.width = widthPixels;
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
                        final File file = FileUtils.createFile(getContext(), fileName);
                        if (file.length() == 0) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    FileUtils.writeFile2Disk(response, file);
                                    if (file.exists()) {
                                        Glide.with(getContext())
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

    // 获取图片类型
    public static String getImageMIMEType(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        // inJustDecodeBounds设置为true是为了让图片不加载到内存中
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        return options.outMimeType;
    }
}