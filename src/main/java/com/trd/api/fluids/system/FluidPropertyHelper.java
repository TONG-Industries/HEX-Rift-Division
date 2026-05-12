package com.trd.api.fluids.system;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;

public class FluidPropertyHelper {
    private static final String TAG_CORROSIVITY = "Corrosivity";
    private static final String TAG_TEMPERATURE = "Temperature";

    public static FluidStack setProperties(FluidStack stack, int corrosivity, int temperature) {
        if (stack.isEmpty()) return stack;
        CompoundTag nbt = stack.getOrCreateTag();
        nbt.putInt(TAG_CORROSIVITY, Math.max(0, corrosivity));
        nbt.putInt(TAG_TEMPERATURE, temperature);
        return stack;
    }

    public static int getCorrosivity(FluidStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(TAG_CORROSIVITY) : 0;
    }

    public static int getTemperature(FluidStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(TAG_TEMPERATURE) : stack.getFluid().getFluidType().getTemperature();
    }

    public static FluidStack createSulfuricAcid(int amount) {
        return setProperties(new FluidStack(com.trd.api.fluids.ModFluids.SULFURIC_ACID_SOURCE.get(), amount), 80, 20);
    }

    public static FluidStack createSteam(int amount) {
        return setProperties(new FluidStack(com.trd.api.fluids.ModFluids.STEAM_SOURCE.get(), amount), 0, 100);
    }
}