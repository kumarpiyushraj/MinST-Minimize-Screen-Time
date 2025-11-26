package com.inc.minst1;

public class ProcessedUsageStats {

    public String packageName; // Add this line
    public long totalTimeInForeground;
    public long lastTimeUsed;
    public int launchCount;
    public long lastLaunchTime = 0;

    public ProcessedUsageStats() {
        this.totalTimeInForeground = 0;
        this.lastTimeUsed = 0;
        this.launchCount = 0;
    }

    public ProcessedUsageStats(String pkg) {
        this.packageName = pkg;
     }
}