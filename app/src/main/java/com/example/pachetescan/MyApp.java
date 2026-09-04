package com.example.pachetescan;

import android.app.Application;

public class MyApp extends Application {
    private static MyApp instance;
    // variabile

    @Override
    public void onCreate(){
        super.onCreate();
        instance=this;
    }

    public static MyApp getInstance(){
        return instance;
    }
}
