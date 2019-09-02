package com.example.pluggable;

import android.app.Application;

import com.example.api.PluginManager;

public class AppApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PluginManager.init(this.getBaseContext())
            .preLoad();
    }
}
