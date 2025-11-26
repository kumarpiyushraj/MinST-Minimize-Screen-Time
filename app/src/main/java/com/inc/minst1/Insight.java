package com.inc.minst1;

public class Insight {
    private final int iconRes;
    private final String title;
    private final String value;
    private final String subtitle; // NEW FIELD

    // Overloaded constructor for insights without a subtitle
    public Insight(int iconRes, String title, String value) {
        this(iconRes, title, value, null);
    }

    // Main constructor
    public Insight(int iconRes, String title, String value, String subtitle) {
        this.iconRes = iconRes;
        this.title = title;
        this.value = value;
        this.subtitle = subtitle;
    }

    public int getIconRes() { return iconRes; }
    public String getTitle() { return title; }
    public String getValue() { return value; }
    public String getSubtitle() { return subtitle; } // NEW GETTER
}