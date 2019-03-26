package com.kowah.habit.fragment;

import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.kowah.habit.MainActivity;
import com.kowah.habit.R;

import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class AlertFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("user_data", MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        View view = inflater.inflate(R.layout.fragment_alert, container, false);
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
                        Toast.makeText(getContext(), "设置成功", Toast.LENGTH_SHORT).show();

                        editor.putString("alertTime", time);
                        editor.apply();
                    }
                }, 0, 0, true).show();
            }
        });
        return view;
    }
}
