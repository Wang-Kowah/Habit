package com.kowah.habit.fragment;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.provider.AlarmClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.codbking.widget.DatePickDialog;
import com.codbking.widget.OnSureLisener;
import com.codbking.widget.bean.DateType;
import com.kowah.habit.R;
import com.kowah.habit.RingReceiver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class AlarmFragment extends Fragment {

    private AlarmManager alarmManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        alarmManager = (AlarmManager) getActivity().getApplicationContext().getSystemService(Service.ALARM_SERVICE);

        final SharedPreferences sharedPreferences = getActivity().getSharedPreferences("user_data", MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        View view = inflater.inflate(R.layout.fragment_alarm, container, false);
        final TextView alertTime = view.findViewById(R.id.alertTime);

        alertTime.setText(sharedPreferences.getString("alertTime", "07:50"));
        alertTime.setOnClickListener(new View.OnClickListener() {
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
                        alertTime.setText(time);
                        Toast toast = Toast.makeText(getContext(), "闹钟设置成功", Toast.LENGTH_SHORT);
                        toast.setText("闹钟设置成功");
                        if (!time.equals(sharedPreferences.getString("alertTime", "07:50"))) {
                            toast.setText("闹钟设置成功，请手动删除旧闹钟");
                        }
                        toast.show();

                        editor.putString("alertTime", time);
                        editor.apply();

                        createAlarm("早上复习", hour, minute);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                showAlarm();
                            }
                        }, 300);
                    }
                });
                dialog.show();
            }
        });

        return view;
    }

    // 创建闹钟
    private void createAlarm(String message, int hour, int minutes) {
        ArrayList<Integer> testDays = new ArrayList<>();
        testDays.add(Calendar.MONDAY);
        testDays.add(Calendar.TUESDAY);
        testDays.add(Calendar.WEDNESDAY);
        testDays.add(Calendar.THURSDAY);
        testDays.add(Calendar.FRIDAY);
        testDays.add(Calendar.SATURDAY);
        testDays.add(Calendar.SUNDAY);

        //action为AlarmClock.ACTION_SET_ALARM
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                .putExtra(AlarmClock.EXTRA_MESSAGE, message)
                //用于指定该闹铃触发时是否振动
                .putExtra(AlarmClock.EXTRA_VIBRATE, false)
                //对于一次性闹铃，无需指定此 extra
                .putExtra(AlarmClock.EXTRA_DAYS, testDays)
                //如果为true，则调用startActivity()不会进入手机的闹钟设置界面
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void showAlarm() {
        Intent intent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void setAlarm(String message, int hour, int minutes) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minutes);

        Intent intent = new Intent(this.getActivity(), RingReceiver.class);
        intent.setAction("com.kowah.habit.Ring");
        intent.putExtra("tab", 1);
        intent.putExtra("msg", message);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getActivity(), 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(c.getTimeInMillis(),pendingIntent) ,pendingIntent);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
    }

}
