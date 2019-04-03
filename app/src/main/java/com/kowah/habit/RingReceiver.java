package com.kowah.habit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class RingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving

        int tab = intent.getIntExtra("tab",-1);

        Intent intent1=new Intent(context,MainActivity.class);
        // an Intent broadcast.
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent1);

        Toast toast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
        switch (tab){
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            default:
                break;
        }
        toast.setText("闹钟时间到了");
        toast.show();
    }
}