package com.maxvision.plugin.udp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.maxvision.jmdns.MdnsBean;
import com.maxvision.jmdns.MdnsHelper;
import com.maxvision.mdns.NsdHelper;
import com.maxvision.plugin.R;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.ServiceInfoImpl;

/**
 * user: zjj
 * date: 2024/6/7
 * desc: mdns测试类
 */
public class UdpActivity extends AppCompatActivity implements MdnsHelper.MdnsListener {
    private static final String TAG = "zjj_mdns";
    private TextView textView;
    private EditText et;
    private MdnsHelper mdnsHelper;

    private StringBuilder sb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udp);
        textView = findViewById(R.id.tv_mdns);
        et = findViewById(R.id.et);
        mdnsHelper = new MdnsHelper(this);
    }

    public void start(View view) {
        textView.setText("");
        sb = new StringBuilder();
        mdnsHelper.setMdnsListener(this);
        mdnsHelper.startDiscovery();
    }

    public void http(View view) {
        String url = "http://".concat(et.getText().toString()).concat(":8090/service/queryMqttServerInfo");
        mdnsHelper.request(null,url);
    }

    public void stop(View view) {
        mdnsHelper.stopDiscovery();
    }

    @Override
    public void onServiceResolved(MdnsBean service) {
        Log.e(TAG,"onServiceResolved:"+Thread.currentThread().getName());
        if (TextUtils.isEmpty(service.attributes.mv_sn)) {
            sb.append("名称:").append(service.serviceName).append(",host:").append(service.ipAddress).append("\n");
        } else {
            sb.append("名称:").append(service.serviceName).append(",host:").append(service.ipAddress).append(",sn:").append(service.attributes.mv_sn).append("\n");
        }
        runOnUiThread(() -> {
            textView.setText(sb.toString());
        });
    }


}