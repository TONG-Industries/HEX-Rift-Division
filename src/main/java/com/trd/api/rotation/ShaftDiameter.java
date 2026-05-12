package com.trd.api.rotation;

public enum ShaftDiameter {
    LIGHT(4, "light", 0.5f, 1.5f, 0.3f, 1.5f, 0.5f),
    MEDIUM(8, "medium", 1.0f, 1.0f, 1.0f, 1.0f, 1.0f),
    HEAVY(12, "heavy", 2.5f, 0.6f, 3.0f, 0.5f, 2.5f);

    public final int pixels;
    public final String name;
    public final float torqueMod;
    public final float speedMod;
    public final float inertiaMod;
    public final float speedMultiplier;
    public final float torqueMultiplier;

    ShaftDiameter(int pixels, String name, float torqueMod, float speedMod, float inertiaMod,
                  float speedMultiplier, float torqueMultiplier) {
        this.pixels = pixels;
        this.name = name;
        this.torqueMod = torqueMod;
        this.speedMod = speedMod;
        this.inertiaMod = inertiaMod;
        this.speedMultiplier = speedMultiplier;
        this.torqueMultiplier = torqueMultiplier;
    }

    public float getSpeedMultiplier() { return speedMultiplier; }
    public float getTorqueMultiplier() { return torqueMultiplier; }
}