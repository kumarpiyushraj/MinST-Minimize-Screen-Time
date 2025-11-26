package com.inc.minst1;

import android.graphics.drawable.Drawable;
import java.io.Serializable; // IMPORT THIS

public class AppUsageModel implements Serializable{
    private final String name;
    private final long ms;
    //private transient Drawable icon; // 'transient' keyword prevents the icon from being serialized
    private final String pkg;
    private int percent; // Usage percentage

    public AppUsageModel(String name, long ms, String pkg) {
        this.name = name;
        this.ms = ms;
        //this.icon = icon;
        this.pkg = pkg;
        this.percent = 0; // Default
    }

    // --- Getters ---
    public String getName() {
        return name;
    }

    public long getMs() {
        return ms;
    }

//    public Drawable getIcon() {
//        return icon;
//    }

    public String getPkg() {
        return pkg;
    }

    public int getPercent() {
        return percent;
    }

    // --- Setter ---
    public void setPercent(int percent) {
        this.percent = percent;
    }
}
