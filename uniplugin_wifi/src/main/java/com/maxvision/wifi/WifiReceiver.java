package com.maxvision.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * user: zjj
 * date: 2024/6/12
 * desc: wifi广播
 */
public class WifiReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            int supplicantError = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);

            if (SupplicantState.COMPLETED.equals(state)) {
                Log.i("WifiConnection", "Supplicant state is completed");
            } else if (SupplicantState.DISCONNECTED.equals(state) || supplicantError == WifiManager.ERROR_AUTHENTICATING) {
                Log.e("WifiConnection", "Failed to connect to Wi-Fi");
            }
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo != null && networkInfo.isConnected()) {
                Log.i("WifiConnection", "Connected to Wi-Fi:"+ networkInfo.getState());
            }
        }
    }

}
