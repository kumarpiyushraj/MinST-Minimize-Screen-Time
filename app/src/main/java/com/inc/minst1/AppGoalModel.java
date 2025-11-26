package com.inc.minst1;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

public class AppGoalModel {
    public ApplicationInfo appInfo;
    public String appName;
    public Drawable appIcon;

    public AppGoalModel(ApplicationInfo appInfo, String appName, Drawable appIcon) {
        this.appInfo = appInfo;
        this.appName = appName;
        this.appIcon = appIcon;
    }
}