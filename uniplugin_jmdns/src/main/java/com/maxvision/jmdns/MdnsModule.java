package com.maxvision.jmdns;

import android.app.Activity;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.common.UniModule;

/**
 * user: zjj
 * date: 2024/6/7
 * desc: 描述
 */
public class MdnsModule extends UniModule {

    private static final String TAG = "zjj_mdns";

    private MdnsHelper mdnsHelper = null;

    public MdnsModule() {}

    // 初始化插件
    @UniJSMethod()
    public boolean initDiscovery() {
        return initDiscoverer();
    }

    // 查询发现
    @UniJSMethod(uiThread = false)
    public void startDiscovery(String serviceType) {
        if (mdnsHelper == null) {
            return;
        }
        mdnsHelper.setMdnsListener(resultListener);
        mdnsHelper.startDiscovery(serviceType);
    }

    // 初始化
    private boolean initDiscoverer() {
        if (mdnsHelper != null) {
            return true;
        } else if (mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
            // 初始化
            mdnsHelper = new MdnsHelper(mUniSDKInstance.getContext());
            Log.d(TAG, "Discoverer created");
            return true;
        } else {
            return false;
        }
    }

    // 停止搜索
    @UniJSMethod(uiThread = false)
    public void stopDiscovery() {
        if (null != mdnsHelper) {
            mdnsHelper.stopDiscovery();
        }
    }

    // 设备监听
    public final MdnsHelper.MdnsListener resultListener = services -> {
        Map<String, Object> params = new HashMap<>();
        params.put("serviceList",services);
        emitEvent("result",params);
        Log.d(TAG,"监听回调:"+ params);
    };

    private void emitEvent(String eventName, Map<String, Object> params) {
        if (mUniSDKInstance != null) {
            mUniSDKInstance.fireGlobalEventCallback(eventName, params);
            Log.d(TAG, "Emit event: " + eventName);
        }
    }

    public void onActivityDestroy() {
        Log.d(TAG, "onActivityDestroy");
        mdnsHelper.stopDiscovery();
        mdnsHelper = null;
        super.onActivityDestroy();
    }
}
