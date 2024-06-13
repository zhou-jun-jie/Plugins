package com.maxvision.plugin.wifi;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.maxvision.plugin.R;
import com.maxvision.wifi.WifiHelper;


public class WifiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
    }

    // 连接
    public void connect(View view) {
        WifiHelper.connectWifi(this,"ROBOT_TEST","maxrobot");
    }

    // 断开
    public void disconnect(View view) {

    }

    // 保存
    public void save(View view) {

    }

    // 忘记
    public void forget(View view) {

    }

}