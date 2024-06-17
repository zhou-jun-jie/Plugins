package com.maxvision.plugin.wifi;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;


import java.util.ArrayList;
import java.util.List;

public class ConnectWifiUtils {
 
    private static final String TAG = ConnectWifiUtils.class.getSimpleName();
 
    private final ConnectivityManager connectivityManager;//连接管理者
 
    private final WifiManager wifiManager;//Wifi管理者
 
    private WifiConnectCallback wifiConnectCallback;
 
    @SuppressLint("StaticFieldLeak")
    private static volatile ConnectWifiUtils mInstance;
 
    private final Context mContext;
 
    public ConnectWifiUtils(Context context) {
        mContext = context;
        wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
 
    public static ConnectWifiUtils initialize(Context context) {
        if (mInstance == null) {
            synchronized (ConnectWifiUtils.class) {
                if (mInstance == null) {
                    mInstance = new ConnectWifiUtils(context);
                }
            }
 
        }
        return mInstance;
    }
 
    public void setWifiConnectCallback(WifiConnectCallback wifiConnectCallback) {
        this.wifiConnectCallback = wifiConnectCallback;
    }
 
    /**
     * 连接Wifi
     *
     * @param password   密码
     */
    public void connectWifi(String ssid, String password,String type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
 
            connectBySuggestion(ssid, password);
            connectByNew(ssid, password);
            connectByOld(ssid, password,type);
        } else {
            connectByOld(ssid, password,type);
        }
    }
 
    /**
     * Android 10 以下使用
     *
     * @param password   密码
     */
    private void connectByOld(String ssid, String password,String type) {
        boolean isSuccess;
        int netId = 0;
        WifiConfiguration configured = isExist(ssid);
//        if (configured != null) {
//            //在配置表中找到了，直接连接
//            isSuccess = wifiManager.enableNetwork(configured.networkId, true);
//        } else {
        WifiConfiguration wifiConfig = createWifiConfig(ssid, password, getCipherType(type));
 
        netId = wifiManager.addNetwork(wifiConfig);
        isSuccess = wifiManager.enableNetwork(netId, true);
//        }
 
        if (!isSuccess) {// 移除
            wifiManager.removeNetwork(netId);
            wifiManager.disableNetwork(netId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiManager.removeNetworkSuggestions(suggestionList);
            }
            if (null != wifiConnectStateCallBack) {
                wifiConnectStateCallBack.failCallBack();
            }
        } else {
            if (null != wifiConnectStateCallBack) {
                wifiConnectStateCallBack.successCallBack();
            }
        }
 
 
        Log.d(TAG, "connectWifi: " + (isSuccess ? "成功" : "失败"));
 
    }
 
 
    /**
     * wifi
     * 状态回调
     */
    private WifiConnectStateCallBack wifiConnectStateCallBack;
 
    public void setWifiStateCallbackListener(WifiConnectStateCallBack wifiConnectStateCallBack) {
        this.wifiConnectStateCallBack = wifiConnectStateCallBack;
    }
 
    public interface WifiConnectStateCallBack {
        void successCallBack();
 
        void failCallBack();
    }
 
    /**
     * Android 10及以上版本使用此方式连接Wifi
     *
     * @param ssid     名称
     * @param password 密码
     */
    @SuppressLint("NewApi")
    private void connectByNew(String ssid, String password) {
 
        WifiNetworkSpecifier wifiNetworkSpecifier = new WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build();
        //网络请求
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build();
        //网络回调处理
        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                if (wifiConnectCallback != null) {
                    wifiConnectCallback.onSuccess(network);
                    Log.d("WifiUtils", "======onAvailable: ====连接成功======");
                }
            }
 
            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Log.d("WifiUtils", "======onAvailable: ====连接失败======");
                if (wifiConnectCallback != null) {
                    wifiConnectCallback.onFailure();
                }
            }
        };
 
        //请求连接网络
        connectivityManager.requestNetwork(request, networkCallback);
    }
 
    private BroadcastReceiver wifiScanReceiver;
    private List<WifiNetworkSuggestion> suggestionList;
 
    @SuppressLint("NewApi")
    public void connectBySuggestion(String ssid, String password) {
        WifiNetworkSuggestion suggestion = new WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .setIsAppInteractionRequired(true)
                .build();
 
        if (suggestionList == null) {
            suggestionList = new ArrayList<>();
        } else {
            suggestionList.clear();
        }
 
        suggestionList.add(suggestion);
        int status = wifiManager.addNetworkSuggestions(suggestionList);
 
//        if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
//            return;
//        }
//        Log.d(TAG, "======onReceive: ==网络连接状态=111111111==="+status);


//        IntentFilter intentFilter = new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);
//
//        wifiScanReceiver = new BroadcastReceiver() {
//
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                if (wifiConnectCallback != null) {
//                    wifiConnectCallback.onBroadCastSuccess();
//                    Log.d("WifiUtils", "======onAvailable: ====连接成功======");
//                }
//                Log.d(TAG, "======onReceive: ==网络连接状态====");
//                if (!intent.getAction().equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
//
//                    return;
//                }
//            }
//        };
//        mContext.registerReceiver(wifiScanReceiver, intentFilter);
    }
 
    /**
     * 接触
     * 注册
     */
    public void recycleRegister() {
        if (wifiScanReceiver != null) {
            try {
                mContext.unregisterReceiver(wifiScanReceiver);
            } catch (IllegalArgumentException e) {
 
            }
        }
    }
 
    /**
     * 创建Wifi配置
     *
     * @param ssid     名称
     * @param password 密码
     * @param type     类型
     */
    private WifiConfiguration createWifiConfig(String ssid, String password, WifiCapability type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + ssid + "\"";
        WifiConfiguration configured = isExist(ssid);
        if (configured != null) {
            wifiManager.removeNetwork(configured.networkId);
            wifiManager.saveConfiguration();
        }
 
        //不需要密码的场景
        if (type == WifiCapability.WIFI_CIPHER_NO_PASS) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            //以WEP加密的场景
        } else if (type == WifiCapability.WIFI_CIPHER_WEP) {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
            //以WPA加密的场景，自己测试时，发现热点以WPA2建立时，同样可以用这种配置连接
        } else if (type == WifiCapability.WIFI_CIPHER_WPA) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }
 
 
//    /**
//     * 连接
//     * WiFi
//     */
//    private String wifiUserName = "";
//    private String wifiPassWord = "";
//    private ConnectivityManager connectivityManager;
//
//    private void connectWiFi(String name, String passWord) {
//
//        WifiConfiguration wifiCong = new WifiConfiguration();
//        Log.d("wifiCong", "======connectWiFi: =======" + name.trim());
//
//        wifiCong.SSID = "\"" + name.trim() + "\"";//\"转义字符，代表"
//        wifiCong.preSharedKey = "\"" + passWord.trim() + "\"";//WPA-PSK密码
//        wifiCong.hiddenSSID = true;
//        wifiCong.status = WifiConfiguration.Status.ENABLED;
//
//        int resstate = -1;//107将配置好的特定WIFI密码信息添加,添加完成后默认是不激活状态，成功返回ID，否则为-1
//
//        resstate = wifiMgr.addNetwork(wifiCong);
//
//        Log.d("WifiUtils", "===connectWiFi: ====" + resstate + "====" + wifiCong.preSharedKey + "====" + wifiCong.SSID);
//
//        boolean isConected = wifiMgr.enableNetwork(resstate, true);  // 连接配置好的指定ID的网络 true连接成功
//        wifiMgr.reconnect();
//
//        WifiManagerUtils.saveNetworkByConfig(wifiMgr, wifiCong, name.trim(), passWord.trim(), null);
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (isConected) {
//                    Toast.makeText(WiFiActivity.this, "wifi连接成功", Toast.LENGTH_LONG).show();
//                } else {
//                    Toast.makeText(WiFiActivity.this, "wifi连接失败", Toast.LENGTH_LONG).show();
//                }
//
//            }
//        });
//
//        //标记WiFi连接状态
//        // isConected
//        // 连接状态
//        Log.d("TAG", "===标记WiFi连接状态connectWiFi: ===" + isConected);
//        if (isConected) {
//            wifiConnectSuccessState();
//        }
//
//        Gson gson = new Gson();
//        List<WifiConfiguration> sdsw = WifiManagerUtils.getConfiguredNetworks(wifiMgr);
//        for (int y = 0; y < sdsw.size(); y++) {
//            Log.d("TAG", "===gson===" + gson.toJson(sdsw.get(y).SSID));
//        }
//
//    }
 
 
    /**
     * 网络是否连接
     */
    @SuppressLint("NewApi")
    public static boolean isNetConnected(ConnectivityManager connectivityManager) {
        return connectivityManager.getActiveNetwork() != null;
    }
 
    /**
     * 连接网络类型是否为Wifi
     */
    @SuppressLint("NewApi")
    public static boolean isWifi(ConnectivityManager connectivityManager) {
        if (connectivityManager.getActiveNetwork() == null) {
            return false;
        }
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        if (networkCapabilities != null) {
            return false;
        }
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }
 
    /**
     * 配置表是否存在对应的Wifi配置
     *
     * @param SSID
     * @return
     */
    @SuppressLint("MissingPermission")
    private WifiConfiguration isExist(String SSID) {
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }
 
    private WifiCapability getCipherType(String capabilities) {
        if (capabilities.contains("WEB")) {
            return WifiCapability.WIFI_CIPHER_WEP;
        } else if (capabilities.contains("PSK")) {
            return WifiCapability.WIFI_CIPHER_WPA;
        } else if (capabilities.contains("WPS")) {
            return WifiCapability.WIFI_CIPHER_NO_PASS;
        } else {
            return WifiCapability.WIFI_CIPHER_NO_PASS;
        }
    }
 
    /**
     * wifi连接回调接口
     */
    public interface WifiConnectCallback {
 
        void onSuccess(Network network);
 
        void onFailure();
 
        void onBroadCastSuccess();
    }
 
    public enum WifiCapability {
        WIFI_CIPHER_WEP, WIFI_CIPHER_WPA, WIFI_CIPHER_NO_PASS
    }
 
}