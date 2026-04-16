package com.example.studysync_project;

import android.app.Application;

import com.example.studysync_project.utils.GlobalTimerWidgetLifecycle;

public class StudySyncApplication extends Application {

    private GlobalTimerWidgetLifecycle globalTimerWidgetLifecycle;

    @Override
    public void onCreate() {
        super.onCreate();
        globalTimerWidgetLifecycle = new GlobalTimerWidgetLifecycle();
        registerActivityLifecycleCallbacks(globalTimerWidgetLifecycle);
    }
}
