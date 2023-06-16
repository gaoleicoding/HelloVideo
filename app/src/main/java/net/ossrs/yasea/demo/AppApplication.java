package net.ossrs.yasea.demo;

import android.app.Application;

public class AppApplication extends Application {
    private final String TAG = "AppApplication";
    private static AppApplication context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    public static AppApplication getContext() {
        return context;
    }
}
