package com.maxvision.jmdns;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import com.alibaba.fastjson.JSON;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.impl.ServiceEventImpl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * user: zjj
 * date: 2024/6/18
 * desc: mdns帮助类
 */
public class MdnsHelper {

    private static final String TAG = "zjj_mdns";

    private JmDNS jmdns;
    private MdnsListener mdnsListener;

    private final List<MdnsBean> resolvedServices;

    private final HashSet<String> resolvedServiceSet;

    public final HashSet<String> test;

    private Handler handler = new Handler(Looper.getMainLooper());

    private HttpRequestQueueManager httpRequestQueueManager;


    public interface MdnsListener {
        void onServiceResolved(MdnsBean services);
    }

    public MdnsHelper(Context context) {
        this(context, 0);
    }

    public MdnsHelper(Context context,long timeout) {
        resolvedServices = new ArrayList<>();
        resolvedServiceSet = new HashSet<>();
        httpRequestQueueManager = new HttpRequestQueueManager();
        test = new HashSet<>();
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
                e.printStackTrace();
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
                resolvedServiceSet.remove(((ServiceEventImpl)event).getIpAddress());
            }
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            ServiceInfo info = event.getInfo();
            Log.d("JmDNS", "Service resolved: " + event);
            parseServiceInfo(info,((ServiceEventImpl)event).getIpAddress());
            // todo 测试
            test.add(((ServiceEventImpl)event).getIpAddress());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.e("zjj_ip", String.valueOf(test.size()));
            }
        }
    }

    private synchronized void parseServiceInfo(ServiceInfo serviceInfo,String ip) {

//        // 定义正则表达式，匹配控制字符和键值对
//        String regex = "\\\\([0-9]+)([^\\\\]+)";
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(serviceInfo.getNiceTextString());
//        mdnsBean.attributes = new MdnsBean.AttributesDTO();
//        // 匹配并输出键值对
//        String sn = "";
//        while (matcher.find()) {
//            String keyValue = matcher.group(2);    // 对应的键值对部分
//            if (keyValue != null) {
//                // 解析键值对
//                String[] keyValueArray = keyValue.split("=", 2);
//                if (keyValueArray.length == 2) {
//                    String key = keyValueArray[0];
//                    String value = keyValueArray[1];
//                    if ("mv_sn".equals(key)) {
//                        sn = value;
//                        mdnsBean.attributes.mv_sn = value;
//                        Log.d("JmDNS", "ipAddress: " + ip+",sn:"+value);
//                    }
//                    if ("mv_type".equals(key)) {
//                        mdnsBean.attributes.mv_type = value;
//                    }
//                }
//            }
//        }
//        if (TextUtils.isEmpty(sn)) return;


        if (!resolvedServiceSet.contains(ip)) {
            Log.e(TAG, "添加数据:" + (ip));

            // todo http请求
            request(serviceInfo,ip);
            resolvedServiceSet.add(ip);

        } else {
            Log.e(TAG, "重复数据:" + (ip));
        }
    }


    public void stopDiscovery() {
        new Thread(()-> {
            if (jmdns != null) {
                try {
                    jmdns.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        resolvedServices.clear();
        resolvedServiceSet.clear();
        httpRequestQueueManager.stop();
        mdnsListener = null;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
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


    private OkHttpClient client = new OkHttpClient();

    public void request(ServiceInfo serviceInfo,String ip) {
        // URL of the server to which you want to send the POST request
        String url = "http://".concat(ip).concat(":8090/service/queryMqttServerInfo");
//
//        // Data to be sent in the POST request
//        long time = System.currentTimeMillis();
//        RequestBody formBody = new FormBody.Builder()
//                .add("requestId", String.valueOf(time))
//                .add("timestamp", String.valueOf(time))
//                .build();
//
//        // Create the request
//        Request request = new Request.Builder()
//                .url(url)
//                .post(formBody)
//                .build();

        // Send the request
        /*client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle the error
                Log.e(TAG, "HTTP request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Handle the response
                    String responseData = response.body().string();
                    Log.d(TAG, "HTTP response: " + responseData);

                    if (mdnsListener != null) {

                    }

                } else {
                    // Handle the response error
                    Log.e(TAG, "HTTP request failed: " + response.code());
                }
            }
        });*/
        httpRequestQueueManager.addRequest(url, new HttpRequestQueueManager.HttpResponseCallback() {
            @Override
            public void onResponse(String response) {
                MdnsResponse mdnsResponse = JSON.parseObject(response, MdnsResponse.class);
                MdnsResponse.DataDTO data = mdnsResponse.data;


                Log.e("zjj_ip","ip:"+ip+",请求数据为:"+mdnsResponse);


                if (data != null) {
                    MdnsBean mdnsBean = new MdnsBean();
                    mdnsBean.ipAddress = ip;
                    
                    mdnsBean.port = serviceInfo.getPort();
                    mdnsBean.serviceType = serviceInfo.getType();
                    mdnsBean.serviceName = serviceInfo.getName();
                    mdnsBean.attributes = new MdnsBean.AttributesDTO();
                    mdnsBean.attributes.mv_sn = data.sn;
                    mdnsBean.attributes.mv_type = data.type;
                    if (null != mdnsListener) {
                        mdnsListener.onServiceResolved(mdnsBean);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                MdnsBean mdnsBean = new MdnsBean();
                mdnsBean.ipAddress = ip;
                mdnsBean.port = serviceInfo.getPort();
                mdnsBean.serviceType = serviceInfo.getType();
                mdnsBean.serviceName = serviceInfo.getName();
                mdnsBean.attributes = new MdnsBean.AttributesDTO();
                mdnsBean.attributes.mv_sn = "失败数据";
                if (null != mdnsListener) {
                    mdnsListener.onServiceResolved(mdnsBean);
                }
                resolvedServiceSet.remove(ip);
            }
        });
    }

}
