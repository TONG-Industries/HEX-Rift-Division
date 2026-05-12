package com.trd.api.fluids.system;

public enum PipeTier {
    BRONZE(930, 50),
    STEEL(1440, 70),
    LEAD(327, 250),
    TUNGSTEN(3400, 540);

    private final int maxTemperature;
    private final int maxCorrosivity;

    PipeTier(int maxTemperature, int maxCorrosivity) {
        this.maxTemperature = maxTemperature;
        this.maxCorrosivity = maxCorrosivity;
    }

    public int getMaxTemperature() { return maxTemperature; }
    public int getMaxCorrosivity() { return maxCorrosivity; }
}