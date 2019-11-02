package com.szwangel.habit.application;

import android.app.Application;

public class HabitApplication extends Application {
    private String domain = "http://118.89.16.252/habit/"; // 正式服

    public String getDomain() {
        return domain;
    }
}
