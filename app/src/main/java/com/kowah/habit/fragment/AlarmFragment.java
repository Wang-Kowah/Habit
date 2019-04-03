package com.kowah.habit.fragment;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TimePickerDialog;
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
import android.widget.TimePicker;
import android.widget.Toast;

import com.kowah.habit.R;
import com.kowah.habit.RingReceiver;

import java.util.ArrayList;
import java.util.Calendar;
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
                new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hour, int minute) {
                        String time = String.format(Locale.CHINA, "%02d:%02d", hour, minute);
                        alertTime.setText(time);
                        Toast toast = Toast.makeText(getContext(), "闹钟设置成功", Toast.LENGTH_SHORT);
                        toast.setText("闹钟设置成功");
                        if (!time.equals(sharedPreferences.getString("alertTime", "07:50"))){
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
                }, 0, 0, true).show();
            }
        });

//        createAlarm("早上复习", 7,50);
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
