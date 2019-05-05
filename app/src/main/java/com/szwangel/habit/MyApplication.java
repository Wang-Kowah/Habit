package com.szwangel.habit;

import android.app.Application;

public class MyApplication extends Application {
    private String domain = "http://118.89.16.252/habit/"; // 正式服
//    private String domain = "http://119.29.77.201/habit/"; // 测试服

    public String getDomain() {
        return domain;
    }
}
