package com.kowah.habit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving

        int tab = intent.getIntExtra("tab", -1);

        // 跳转回app
        Intent intent1 = new Intent(context, MainActivity.class);
        // extra传递失败，猜测与flags以及manifest的launchMode有关
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("tab", tab);
        context.startActivity(intent1);
    }
}