package com.maxvision.wifi;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * user: zjj
 * date: 2024/6/12
 * desc: wifi帮助类
 */
public class WifiHelper {

    private static final String TAG = "WifiHelper";

    private Context context;
    private WifiReceiver wifiReceiver;

    public void connectWifi(Context context,String ssid,String passkey) {
        this.context = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.e(TAG,"Android版本9以上连接:"+Build.VERSION.SDK_INT);
            connectToWifiAndroidQAndAbove(context,ssid,passkey);
        } else {
            Log.e(TAG,"Android版本9及以下连接:"+Build.VERSION.SDK_INT);
            connectToWifi(context,ssid,passkey);
        }
    }

    private void connectToWifi(Context context,String ssid, String passkey) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", passkey);

        // 设置Wi-Fi网络优先级
        wifiConfig.priority = 9999;
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();

        if (wifiReceiver == null) {
            wifiReceiver = new WifiReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        context.registerReceiver(wifiReceiver, intentFilter);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void connectToWifiAndroidQAndAbove(Context context,String ssid, String password) {
        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build();

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build();
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                connectivityManager.bindProcessToNetwork(network);
                Log.i(TAG, "Connected to " + ssid);
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Log.e(TAG, "Failed to connect to " + ssid);
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                connectivityManager.bindProcessToNetwork(null);
                Log.e(TAG, "Lost connection to " + ssid);
            }
        };
        connectivityManager.requestNetwork(request, networkCallback);
    }


    public void unregisterReceiver() {
        try {
            context.unregisterReceiver(wifiReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was probably already unregistered
        }
    }

}
