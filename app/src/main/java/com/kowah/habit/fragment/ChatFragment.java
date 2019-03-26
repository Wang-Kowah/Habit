package com.kowah.habit.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.kowah.habit.LoginActivity;
import com.kowah.habit.R;
import com.kowah.habit.service.CommonService;

import java.io.IOException;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.content.Context.MODE_PRIVATE;

public class ChatFragment extends Fragment {

    // 当前页面
    int currentTab = -1;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, final Bundle savedInstanceState) {

        final SharedPreferences sharedPreferences = getActivity().getSharedPreferences("user_data", MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        final View view = inflater.inflate(R.layout.fragment_chat, container, false);
        final TextView timeButton = view.findViewById(R.id.timeButton);

        if (getArguments() != null && getArguments().getInt("tab", -1) == 0) {
            view.findViewById(R.id.week).setVisibility(View.GONE);

            timeButton.setText(sharedPreferences.getString("time1", "22:30"));
            timeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {

                        @Override
                        public void onTimeSet(TimePicker view, int hour, int minute) {
                            String time = String.format(Locale.CHINA, "%02d:%02d", hour, minute);
                            timeButton.setText(time);
                            Toast.makeText(getContext(), "设置成功", Toast.LENGTH_SHORT).show();

                            editor.putString("time1", time);
                            editor.apply();
                        }
                    }, 0, 0, true).show();
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
                    builder.setItems(dayInWeek, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int selected) {
                            final String daySelected = dayInWeek[selected].substring(1);
                            new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {

                                @Override
                                public void onTimeSet(TimePicker view, int hour, int minute) {
                                    String time = String.format(Locale.CHINA, "%02d:%02d", hour, minute);
                                    timeButton.setText(time);
                                    dateTextView.setText(daySelected);
                                    Toast.makeText(getContext(), "设置成功", Toast.LENGTH_SHORT).show();

                                    editor.putString("dayInWeek", daySelected);
                                    editor.putString("time2", time);
                                    editor.apply();
                                }
                            }, 0, 0, true).show();
                        }
                    }).setTitle("选择要做总结的时间").show();
                }
            });

            currentTab = 2;
            System.out.println("week fragment created");
        }

        final EditText editText = view.findViewById(R.id.input_node);
        // 动态设置行数来避免EditText在多行状态下对ImeOptions的强制修改
        editText.setMaxLines(5);
        editText.setHorizontallyScrolling(false);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && KeyEvent.KEYCODE_ENTER == event.getKeyCode() && KeyEvent.ACTION_DOWN == event.getAction())) {
                    String input = v.getText().toString();
                    if (!input.equals("")) {
                        v.setText("");

                        CommonService commonService = new Retrofit.Builder()
                                .baseUrl("http://119.29.77.201/habit/")
                                .build()
                                .create(CommonService.class);

                        Call<ResponseBody> call = null;
                        int uid = sharedPreferences.getInt("uid", -1);
                        switch (currentTab) {
                            case 0:
                                call = commonService.sendNote(uid, 0, input);
                                break;
                            case 2:
                                call = commonService.sendNote(uid, 1, input);
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
                                        Toast.makeText(getContext(), jsonObject.getString("msg"), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (IOException e) {
                                    Toast.makeText(getContext(), "网络异常，请稍后重试", Toast.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Toast.makeText(getContext(), "网络异常，请稍后重试", Toast.LENGTH_SHORT).show();
                                t.printStackTrace();
                            }
                        });
                    }
                }
                return true;
            }
        });



        return view;
    }

}