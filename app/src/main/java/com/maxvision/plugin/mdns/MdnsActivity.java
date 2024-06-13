package com.maxvision.plugin.mdns;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.maxvision.mdns.NsdBean;
import com.maxvision.mdns.NsdHelper;
import com.maxvision.plugin.R;

import java.util.List;

/**
 * user: zjj
 * date: 2024/6/7
 * desc: mdns测试类
 */
public class MdnsActivity extends AppCompatActivity implements NsdHelper.ResolveResultListener {

    private NsdHelper nsdHelper;

    private StringBuilder sb;
    private TextView tvMdns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mdns);
        tvMdns = findViewById(R.id.tv_mdns);
        nsdHelper = new NsdHelper(this);
        nsdHelper.setResultListener(this);
    }

    public void start(View view) {
        nsdHelper.discoverServices("_maxvision._tcp.");
    }

    public void stop(View view) {
        nsdHelper.stopDiscovery();
    }

    @Override
    public void onAllServicesResolved(List<NsdBean> services) {
        sb = new StringBuilder();
        for (NsdBean service : services) {
            sb.append("名称:").append(service.serviceName).append(",host:").append(service.ipAddress).append("\n");
            Log.e("result","监听的数据 service name:"+service.serviceName+",host:"+service.ipAddress+",port:"+service.port);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvMdns.setText(sb.toString());
            }
        });

    }

    @Override
    protected void onDestroy() {
        nsdHelper.stopDiscovery();
        super.onDestroy();
    }
}