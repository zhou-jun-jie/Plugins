package com.maxvision.plugin.mdns;

import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.maxvision.mdns.NsdBean;
import com.maxvision.mdns.NsdHelper;
import com.maxvision.plugin.R;

import java.util.Formattable;
import java.util.List;

/**
 * user: zjj
 * date: 2024/6/7
 * desc: mdns测试类
 */
public class MdnsActivity extends AppCompatActivity implements NsdHelper.ResolveResultListener {

    private NsdHelper nsdHelper;

    private StringBuilder sb = new StringBuilder();;
    private TextView tvMdns;

    private EditText etServiceType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mdns);
        tvMdns = findViewById(R.id.tv_mdns);
        etServiceType = findViewById(R.id.et_serviceType);

    }

    public void start(View view) {
        nsdHelper = new NsdHelper(this);
        sb.delete(0,sb.toString().length());
        tvMdns.setText("");
        nsdHelper.setResultListener(this);
        String serviceType = "_tcp.local.";
        if (!TextUtils.isEmpty(etServiceType.getText().toString())) {
            serviceType = etServiceType.getText().toString();
        }
        nsdHelper.discoverServices(serviceType);
    }

    public void stop(View view) {
        nsdHelper.stopDiscovery();
    }

    @Override
    public void onAllServicesResolved(List<NsdBean> services) {

        for (NsdBean service : services) {
            sb.append("名称:").append(service.serviceName).append(",host:").append(service.ipAddress).append(",sn:").append(service.attributes.mv_sn).append("\n");
            Log.e("result", "监听的数据 service name:" + service.serviceName + ",host:" + service.ipAddress + ",sn:" + service.attributes.mv_sn);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e("result","设置文本:"+sb.toString());
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