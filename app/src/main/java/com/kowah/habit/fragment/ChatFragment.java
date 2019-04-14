package com.kowah.habit.fragment;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.provider.AlarmClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.codbking.widget.DatePickDialog;
import com.codbking.widget.OnSureLisener;
import com.codbking.widget.bean.DateType;
import com.kowah.habit.R;
import com.kowah.habit.RingReceiver;
import com.kowah.habit.service.RetrofitService;
import com.kowah.habit.utils.DateUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.content.Context.MODE_PRIVATE;

public class ChatFragment extends Fragment {

    SharedPreferences sharedPreferences;
    RetrofitService retrofitService;
    AlarmManager alarmManager;
    MsgAdapter adapter;
    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;

    LinkedList<String> msgList;
    LinkedList<Long> dateList;

    // 当前页面
    int currentTab = -1;
    // 每次更新的消息条数
    int pageSize = 10;
    int pageNum;
    int uid;
    int sentMsgNum;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, final Bundle savedInstanceState) {
        pageNum = 1;
        sentMsgNum = 0;
        msgList = new LinkedList<>();
        dateList = new LinkedList<>();

        alarmManager = (AlarmManager) getActivity().getApplicationContext().getSystemService(Service.ALARM_SERVICE);

        sharedPreferences = getActivity().getSharedPreferences("user_data", MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        final View view = inflater.inflate(R.layout.fragment_chat, container, false);
        final TextView timeButton = view.findViewById(R.id.timeButton);

        if (getArguments() != null && getArguments().getInt("tab", -1) == 0) {
            view.findViewById(R.id.week).setVisibility(View.GONE);

            timeButton.setText(sharedPreferences.getString("time1", "22:30"));
            timeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DatePickDialog dialog = new DatePickDialog(getContext());
                    //设置标题
                    dialog.setTitle("选择闹钟时间");
                    //设置类型
                    dialog.setType(DateType.TYPE_HM);
                    //设置消息体的显示格式，日期格式
                    dialog.setMessageFormat("每天  HH:mm");
                    //设置选择回调
                    dialog.setOnChangeLisener(null);
                    //设置点击确定按钮回调
                    dialog.setOnSureLisener(new OnSureLisener() {
                        @Override
                        public void onSure(Date date) {
                            int hour = date.getHours(), minute = date.getMinutes();

                            String time = String.format(Locale.CHINA, "%02d:%02d", hour, minute);
                            timeButton.setText(time);
//                            Toast toast = Toast.makeText(getContext(), "闹钟设置成功", Toast.LENGTH_SHORT);
//                            toast.setText("闹钟设置成功");
//                            if (!time.equals(sharedPreferences.getString("time1", "22:30"))) {
//                                toast.setText("闹钟设置成功，请手动删除旧闹钟");
//                            }
//                            toast.show();

                            editor.putString("time1", time);
                            editor.apply();

                            createAlarm("【习惯APP】每天总结", hour, minute, -1);
                            new android.support.v7.app.AlertDialog.Builder(getContext())
                                    .setTitle("已为您设置新的闹钟")
                                    .setMessage("可去系统闹铃里手动关闭旧的闹钟")
                                    .setPositiveButton("我知道了", null)
                                    .setCancelable(false)
                                    .show();
//                            setBroadcastAlarm(hour, minute);
//                            new Handler().postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    showAlarm();
//                                }
//                            }, 300);

                        }
                    });
                    dialog.show();
                }
            });

            currentTab = 0;
            System.out.println("day fragment created");
        } else {
            final TextView dateTextView = view.findViewById(R.id.dateText);
            dateTextView.setText(sharedPreferences.getString("dayInWeek", "六"));
            timeButton.setText(sharedPreferences.getString("time2", "18:50"));

            view.findViewById(R.id.dateButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String[] dayInWeek = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    AlertDialog alertDialog = builder.setItems(dayInWeek, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, final int selected) {
                            final String daySelected = dayInWeek[selected].substring(1);
                            DatePickDialog weekDialog = new DatePickDialog(getContext());
                            //设置标题
                            weekDialog.setTitle("选择闹钟时间");
                            //设置类型
                            weekDialog.setType(DateType.TYPE_HM);
                            //设置消息体的显示格式，日期格式
                            weekDialog.setMessageFormat("每" + dayInWeek[selected] + "  HH:mm");
                            //设置选择回调
                            weekDialog.setOnChangeLisener(null);
                            //设置点击确定按钮回调
                            weekDialog.setOnSureLisener(new OnSureLisener() {
                                @Override
                                public void onSure(Date date) {
                                    int hour = date.getHours(), minute = date.getMinutes();

                                    String time = String.format(Locale.CHINA, "%02d:%02d", hour, minute);
                                    timeButton.setText(time);
                                    dateTextView.setText(daySelected);
//                                    Toast toast = Toast.makeText(getContext(), "闹钟设置成功", Toast.LENGTH_SHORT);
//                                    toast.setText("闹钟设置成功");
//                                    if (!time.equals(sharedPreferences.getString("time2", "18:50")) || !daySelected.equals(sharedPreferences.getString("dayInWeek", "六"))) {
//                                        toast.setText("闹钟设置成功，请手动删除旧闹钟");
//                                    }
//                                    toast.show();

                                    editor.putString("dayInWeek", daySelected);
                                    editor.putString("time2", time);
                                    editor.apply();

                                    createAlarm("【习惯APP】每周总结", hour, minute, selected);
                                    new android.support.v7.app.AlertDialog.Builder(getContext())
                                            .setTitle("已为您设置新的闹钟")
                                            .setMessage("可去系统闹铃里手动关闭旧的闹钟")
                                            .setPositiveButton("我知道了", null)
                                            .setCancelable(false)
                                            .show();
//                                    setBroadcastAlarm(hour, minute);
//                                    new Handler().postDelayed(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            showAlarm();
//                                        }
//                                    }, 300);
                                }
                            });
                            weekDialog.show();
                        }
                    }).setTitle("选择要做总结的时间").create();
                    alertDialog.show();

                    // 设置宽度
                    WindowManager.LayoutParams layoutParams = alertDialog.getWindow().getAttributes();
                    layoutParams.width = getResources().getDisplayMetrics().widthPixels * 5 / 6;
                    layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    alertDialog.getWindow().setAttributes(layoutParams);
                }
            });

            currentTab = 2;
            System.out.println("week fragment created");
        }

        uid = sharedPreferences.getInt("uid", -1);
        retrofitService = new Retrofit.Builder()
                .baseUrl("http://119.29.77.201/habit/")
                .build()
                .create(RetrofitService.class);

        final EditText editText = view.findViewById(R.id.input_node);
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
                        },100);
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
                    if (!input.equals("")) {
                        v.setText("");
                        // 手动更新消息列表
                        msgList.addFirst(input);
                        dateList.addFirst(System.currentTimeMillis() / 1000);
                        adapter.notifyItemInserted(0);
                        recyclerView.scrollToPosition(0);
                        sentMsgNum++;

                        Call<ResponseBody> call = null;
                        switch (currentTab) {
                            case 0:
                                call = retrofitService.sendNote(uid, 0, input);
                                break;
                            case 2:
                                call = retrofitService.sendNote(uid, 1, input);
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

        return view;
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
            holder.msg.setText(msgList.get(position));

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
                Bitmap bitmap = null; //从本地取图片
                try {
                    bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (bitmap != null) {
                    holder.profile.setImageBitmap(bitmap); //设置Bitmap为头像
                }
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.itemView.requestFocus();
                    holder.itemView.clearFocus();
                }
            });

        }

        @Override
        public int getItemCount() {
            return msgList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            private TextView msg;
            private TextView msgDate;
            private ImageView profile;

            ViewHolder(View itemView) {
                super(itemView);
                msg = itemView.findViewById(R.id.msg);
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

}