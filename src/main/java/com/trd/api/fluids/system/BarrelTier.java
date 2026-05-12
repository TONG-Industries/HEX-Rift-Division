package com.trd.api.fluids.system;

public enum BarrelTier {
    // capacity, meltingPoint °C, corrosionResistance, leakRate (мБ/сек). 0 = не течёт
    CORRUPTED(16000, 0, 0, 900),
    LEAKING(16000, 0, 0, 50),
    IRON(16000, 958, 40, 0),
    STEEL(24000, 1440, 95, 0),
    LEAD(20000, 327, 250, 0);

    private final int capacity;
    private final int meltingPoint;
    private final int corrosionResistance;
    private final int leakRate;

    BarrelTier(int capacity, int meltingPoint, int corrosionResistance, int leakRate) {
        this.capacity = capacity;
        this.meltingPoint = meltingPoint;
        this.corrosionResistance = corrosionResistance;
        this.leakRate = leakRate;
    }

    public int getCapacity() { return capacity; }
    public int getMeltingPoint() { return meltingPoint; }
    public int getCorrosionResistance() { return corrosionResistance; }
    public int getLeakRate() { return leakRate; }
    public boolean isLeaking() { return leakRate > 0; }
}