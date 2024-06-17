package com.maxvision.plugin.wifi;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.maxvision.plugin.R;
import com.maxvision.plugin.mdns.MdnsActivity;
import com.maxvision.wifi.WifiHelper;
import com.maxvision.wifi.WifiReceiver;

import java.util.ArrayList;
import java.util.List;


public class WifiActivity extends AppCompatActivity {

    private WifiHelper wifiHelper;

    private boolean change = true;

    private WifiManager wifiManager;
    private List<WifiNetworkSuggestion> suggestionsList;

    private ConnectivityManager mConnectivityManager;

    private ConnectivityManager.NetworkCallback mNetworkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        wifiHelper = new WifiHelper();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

    }

    // 连接
    public void connect(View view) {
        String ssid = "";
        String pwd = "";

        if (change) {
            ssid = "ROBOT_TEST";
            pwd = "maxrobot";
        } else {
            ssid = "Maxvision-5G";
            pwd = "12345ssdlh";
        }
        Log.e("WifiConnection", ssid);
        // todo 方式一
//        wifiHelper.connectWifi(this, ssid, pwd);
        // todo 方式二
//        WifiUtils.withContext(this)
//                .connectWith(ssid,pwd)
//                .onConnectionResult(new ConnectionSuccessListener() {
//                    @Override
//                    public void success() {
//                        Log.e("WifiConnection","success");
//                    }
//
//                    @Override
//                    public void failed(@NonNull ConnectionErrorCode errorCode) {
//                        Log.e("WifiConnection","errorCode:"+errorCode);
//                    }
//                }).start();
//        // todo 方式三
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            suggestNetwork(ssid, pwd);
//        }

        // todo 方式四
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            connectWifi(ssid, pwd);
        }

        // todo 方式五
//        ConnectWifiUtils.initialize(this).connectWifi(ssid,pwd,"WPK");

        change = !change;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void suggestNetwork(String ssid, String password) {
        if (suggestionsList != null && !suggestionsList.isEmpty())
            wifiManager.removeNetworkSuggestions(suggestionsList);
        WifiNetworkSuggestion.Builder suggestionBuilder = new WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .setIsAppInteractionRequired(true); // Optional (Needs location permission)

        WifiNetworkSuggestion suggestion = suggestionBuilder.build();
        suggestionsList = new ArrayList<>();
        suggestionsList.add(suggestion);

        int status = wifiManager.addNetworkSuggestions(suggestionsList);

        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            Toast.makeText(this, "Network Suggestion Added Successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to Add Network Suggestion", Toast.LENGTH_SHORT).show();
        }
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

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void connectWifi(String ssid, String pwd) {
        isPing = false;
        // andorid 10以上
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            //step1-创建建议列表
            WifiNetworkSuggestion suggestion =
                    new WifiNetworkSuggestion.Builder()
                            .setSsid(ssid)
                            .setWpa2Passphrase(pwd)
                            .setIsAppInteractionRequired(true)
                            .build();

            if (null == suggestionsList) {
                suggestionsList = new ArrayList<>();
            } else {
                suggestionsList.clear();
            }
            suggestionsList.add(suggestion);
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int status = wifiManager.addNetworkSuggestions(suggestionsList);
            // step2-添加建议成功
            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                WifiNetworkSpecifier wifiNetworkSpecifier = new WifiNetworkSpecifier.Builder()
                        .setSsid(ssid)
                        .setWpa2Passphrase(pwd)
                        .build();

                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                        .setNetworkSpecifier(wifiNetworkSpecifier)
                        .build();

                mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                        Log.d("WifiConnection", "onAvailable");
                        // todo 这个方法绑定后,无法连接网络
                        // 问题1: 无外网的网络Maxvision-5G,连上没有显示连接wifi
                        // 问题2: bindProcessToNetwork后
//                        mConnectivityManager.bindProcessToNetwork(network);
//                        register();



                        /*DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                        int ipAddress = dhcpInfo.ipAddress;
                        if (ipAddress != 0) {
                            // 成功获取IP地址
                            String ip = String.format("%d.%d.%d.%d",
                                    (ipAddress & 0xff),
                                    (ipAddress >> 8 & 0xff),
                                    (ipAddress >> 16 & 0xff),
                                    (ipAddress >> 24 & 0xff));
                            Log.d("WifiConnection", "IP Address: " + ip);
                            isPing = true;
                            ping(ip);
                        } else {
                            // 未能获取IP地址
                            Log.d("WifiConnection", "Failed to obtain IP address");
                        }*/

//                        connectByOld(ssid, pwd);

                        startActivity(new Intent(WifiActivity.this, MdnsActivity.class));
                    }

                    @Override
                    public void onUnavailable() {
                        super.onUnavailable();
                        Log.d("WifiConnection", "onUnavailable");
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        Log.d("WifiConnection", "onLost");
//                        mConnectivityManager.bindProcessToNetwork(null);
                    }
                };
                // step3-连接wifi
                mConnectivityManager.requestNetwork(request, mNetworkCallback);
            }
            connectByOld(ssid, pwd);
        }


    }

    private void connectByOld(String ssid, String password) {
        boolean isSuccess;
        int netId;
        WifiConfiguration wifiConfig = createWifiConfig(ssid, password, getCipherType("WPA"));
        netId = wifiManager.addNetwork(wifiConfig);
        isSuccess = wifiManager.enableNetwork(netId, true);
        if (!isSuccess) {// 移除
            wifiManager.removeNetwork(netId);
            wifiManager.disableNetwork(netId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiManager.removeNetworkSuggestions(suggestionsList);
            }
        }
        Log.d("WifiConnection", "connectWifi: " + (isSuccess ? "成功" : "失败"));

    }

    public enum WifiCapability {
        WIFI_CIPHER_WEP, WIFI_CIPHER_WPA, WIFI_CIPHER_NO_PASS
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


    private boolean isPing = true;

    private void ping(String ip) {
        while (isPing) {
            String ipFromUrl = PingUtil.getIPFromUrl(ip);
            Log.d("WifiConnection", "ipFromUrl: " + ipFromUrl);
            if (!TextUtils.isEmpty(ipFromUrl)) {
                isPing = false;
                break;
            } else {
                SystemClock.sleep(1000);
            }
        }
    }

    private WifiReceiver wifiReceiver;

    private void register() {
        if (wifiReceiver == null) {
            wifiReceiver = new WifiReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(wifiReceiver, intentFilter);
    }

    private void unregister() {
        if (null != wifiReceiver) {
            unregisterReceiver(wifiReceiver);
        }
    }


}