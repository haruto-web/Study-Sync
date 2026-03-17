package com.example.studysync_project.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * Utility class for checking network connectivity
 * Used to determine if offline mode should be enabled
 */
public class NetworkUtil {

    /**
     * Check if device has active internet connection
     */
    public static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            
            if (connectivityManager != null) {
                Network network = connectivityManager.getActiveNetwork();
                
                if (network != null) {
                    NetworkCapabilities networkCapabilities = 
                        connectivityManager.getNetworkCapabilities(network);
                    
                    if (networkCapabilities != null) {
                        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Check if device is connected to WiFi
     */
    public static boolean isConnectedToWiFi(Context context) {
        try {
            ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            
            if (connectivityManager != null) {
                Network network = connectivityManager.getActiveNetwork();
                
                if (network != null) {
                    NetworkCapabilities networkCapabilities = 
                        connectivityManager.getNetworkCapabilities(network);
                    
                    if (networkCapabilities != null) {
                        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Check if device is connected to mobile data
     */
    public static boolean isConnectedToMobileData(Context context) {
        try {
            ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            
            if (connectivityManager != null) {
                Network network = connectivityManager.getActiveNetwork();
                
                if (network != null) {
                    NetworkCapabilities networkCapabilities = 
                        connectivityManager.getNetworkCapabilities(network);
                    
                    if (networkCapabilities != null) {
                        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
