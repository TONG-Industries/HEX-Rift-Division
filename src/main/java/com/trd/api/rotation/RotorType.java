package com.trd.api.rotation;

import net.minecraft.util.StringRepresentable;

public enum RotorType implements StringRepresentable {
    COPPER("copper", 0.8f),
    GOLD("gold", 1.0f);

    private final String name;
    private final float efficiency;

    RotorType(String name, float efficiency) {
        this.name = name;
        this.efficiency = efficiency;
    }

    public float getEfficiency() {
        return efficiency;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
