package com.trd.api.rotation;

public enum ShaftDiameter {
    LIGHT(4, "light", 0.3f, 1.5f, 0.5f, 5),
    MEDIUM(8, "medium", 1.0f, 1.0f, 1.0f, 10),
    HEAVY(12, "heavy", 2.0f, 0.5f, 3.0f, 20);

    public final int pixels;
    public final String name;
    public final float inertiaMod;
    public final float speedMultiplier;
    public final float torqueMultiplier;
    public final int maxSupportDistance;

    ShaftDiameter(int pixels, String name, float inertiaMod,
            float speedMultiplier, float torqueMultiplier, int maxSupportDistance) {
        this.pixels = pixels;
        this.name = name;
        this.inertiaMod = inertiaMod;
        this.speedMultiplier = speedMultiplier;
        this.torqueMultiplier = torqueMultiplier;
        this.maxSupportDistance = maxSupportDistance;
    }

    public float getSpeedMultiplier() {
        return speedMultiplier;
    }

    public float getTorqueMultiplier() {
        return torqueMultiplier;
    }
}