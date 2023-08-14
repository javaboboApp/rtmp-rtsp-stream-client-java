package com.river.apollo.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.text.format.Formatter;

public class NetworkUtils {

    public static String getLocalIpAddress(Context context) {
        if (context == null) {
            return null;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For devices running Android 6.0 or later
            if (connectivityManager.getActiveNetwork() != null) {
                return Formatter.formatIpAddress(getWifiIpAddress(context));
            }
        } else {
            // For devices running Android 5.1 or earlier
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                return Formatter.formatIpAddress(wifiInfo.getIpAddress());
            }
        }

        return null;
    }

    private static int getWifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            return wifiInfo.getIpAddress();
        }
        return 0;
    }
}
