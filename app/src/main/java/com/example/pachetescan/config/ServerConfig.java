package com.example.pachetescan.config;
import android.content.Context;
import android.content.SharedPreferences;
public class ServerConfig {
    private static final String DEFAULT_BASE_URL = "http://10.10.240.185:5021";

    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("server_config", Context.MODE_PRIVATE);
        return prefs.getString("base_url", DEFAULT_BASE_URL);
    }
}
