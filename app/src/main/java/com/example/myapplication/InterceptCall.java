package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.example.myapplication.MainActivity;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Utils;

public class InterceptCall extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.d("Receivemessage", "asd");
            MainActivity.u.sendData();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
