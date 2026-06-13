package com.trd.item.energy;

import net.minecraft.world.item.Item;

public class StatorCoilItem extends Item {
    private final String materialName;
    private final long energyConversionRate;
    private final long energyBuffer;
    private final long baseTorqueLoad;
    private final float asymmetryMultiplier;

    public StatorCoilItem(Properties properties, String materialName, long energyConversionRate, long energyBuffer, long baseTorqueLoad, float asymmetryMultiplier) {
        super(properties);
        this.materialName = materialName;
        this.energyConversionRate = energyConversionRate;
        this.energyBuffer = energyBuffer;
        this.baseTorqueLoad = baseTorqueLoad;
        this.asymmetryMultiplier = asymmetryMultiplier;
    }

    public String getMaterialName() {
        return materialName;
    }

    public long getEnergyConversionRate() {
        return energyConversionRate;
    }

    public long getEnergyBuffer() {
        return energyBuffer;
    }

    public long getBaseTorqueLoad() {
        return baseTorqueLoad;
    }

    public float getAsymmetryMultiplier() {
        return asymmetryMultiplier;
    }
}
