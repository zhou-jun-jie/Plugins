package com.maxvision.jmdns;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.impl.ServiceEventImpl;

/**
 * user: zjj
 * date: 2024/6/18
 * desc: mdns帮助类
 */
public class MdnsHelper {

    private static final String TAG = "zjj_mdns";

    private JmDNS jmdns;
    private MdnsListener mdnsListener;
    private final HashSet<String> resolvedServiceSet;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final HttpRequestQueueManager httpRequestQueueManager;


    public interface MdnsListener {
        void onServiceResolved(MdnsBean services);
    }

    public MdnsHelper(Context context) {
        this(context, 0);
    }

    public MdnsHelper(Context context, long timeout) {
        resolvedServiceSet = new HashSet<>();
        httpRequestQueueManager = new HttpRequestQueueManager();
        if (timeout != 0) {
            handler.postDelayed(() -> {
                Log.e(TAG, "handler延迟" + timeout / 1000L + "关闭");
                stopDiscovery();
            }, timeout);
        }
    }

    public void setMdnsListener(MdnsListener mdnsListener) {
        this.mdnsListener = mdnsListener;
    }

    public void startDiscovery() {
        startDiscovery("_maxvision._tcp.local.");
    }

    public void startDiscovery(String serviceType) {
        new Thread(() -> {
            try {
                // 获取本机地址
                InetAddress addr = getLocalIpAddress();
                // 初始化JmDNS实例
                jmdns = JmDNS.create(addr);
                // 添加服务监听器
                jmdns.addServiceListener(serviceType, new SampleListener());
            } catch (IOException e) {
                Log.e(TAG, "异常:" + e.getMessage());
            }
        }).start();
    }

    private class SampleListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
            Log.d("JmDNS", "Service added: " + event.getInfo());
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            ServiceInfo info = event.getInfo();
            Log.d("JmDNS", "Service removed: " + info);
            if (null != info) {
                resolvedServiceSet.remove(((ServiceEventImpl) event).getIpAddress());
            }
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            ServiceInfo info = event.getInfo();
            Log.d("JmDNS", "Service resolved: " + event);
            parseServiceInfo(info, ((ServiceEventImpl) event).getIpAddress());
        }
    }

    private synchronized void parseServiceInfo(ServiceInfo serviceInfo, String ip) {
        if (!resolvedServiceSet.contains(ip)) {
            Log.e(TAG, "添加数据:" + (ip));
            request(serviceInfo, ip);
            resolvedServiceSet.add(ip);
        } else {
            Log.e(TAG, "重复数据:" + (ip));
        }
    }

    public void stopDiscovery() {
        Thread thread = new Thread(() -> {
            if (jmdns != null) {
                try {
                    jmdns.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        thread.start();

        resolvedServiceSet.clear();
        httpRequestQueueManager.stop();
        mdnsListener = null;
        handler.removeCallbacksAndMessages(null);

    }

    /**
     * 获取本机wifi地址
     */
    public InetAddress getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress;
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void request(ServiceInfo serviceInfo, String ip) {
        String url = "http://".concat(ip).concat(":8090/service/queryMqttServerInfo");
        httpRequestQueueManager.addRequest(url, new HttpRequestQueueManager.HttpResponseCallback() {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, "详细数据 url:" + url + ",response," + response);
                MdnsResponse mdnsResponse = JSON.parseObject(response, MdnsResponse.class);
                MdnsResponse.DataDTO data = mdnsResponse.data;
                if (data != null /*&& !TextUtils.isEmpty(data.sn)*/) {
                    if (null != mdnsListener) {
                        MdnsBean mdnsBean = new MdnsBean();
                        mdnsBean.ipAddress = ip;
                        mdnsBean.port = serviceInfo.getPort();
                        mdnsBean.serviceType = serviceInfo.getType();
                        mdnsBean.serviceName = serviceInfo.getName();
                        mdnsBean.attributes = new MdnsBean.AttributesDTO();
                        mdnsBean.attributes.mv_sn = data.sn;
                        mdnsBean.attributes.mv_type = data.type;
                        mdnsListener.onServiceResolved(mdnsBean);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (null != mdnsListener) {
                    MdnsBean mdnsBean = new MdnsBean();
                    mdnsBean.ipAddress = ip;
                    mdnsBean.port = serviceInfo.getPort();
                    mdnsBean.serviceType = serviceInfo.getType();
                    mdnsBean.serviceName = serviceInfo.getName();
                    mdnsBean.attributes = new MdnsBean.AttributesDTO();
                    mdnsBean.attributes.errorMsg = e.getMessage();
                    mdnsListener.onServiceResolved(mdnsBean);
                }
                resolvedServiceSet.remove(ip);
            }
        });
    }

}
