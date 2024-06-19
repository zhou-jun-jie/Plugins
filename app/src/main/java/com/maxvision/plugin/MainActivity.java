package com.maxvision.plugin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.maxvision.plugin.mdns.MdnsActivity;
import com.maxvision.plugin.udp.UdpActivity;
import com.maxvision.plugin.wifi.WifiActivity;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void udp(View view) {
        startActivity(new Intent(this, UdpActivity.class));
    }

    public void mdns(View view) {
        startActivity(new Intent(this, MdnsActivity.class));
    }

    public void wifi(View view) {
        startActivity(new Intent(this, WifiActivity.class));
    }
}