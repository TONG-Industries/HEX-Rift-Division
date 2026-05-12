package com.trd.api.fluids.system;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;

import java.util.function.Consumer;

public class BaseFluidType extends FluidType {
    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;
    private final ResourceLocation guiTexture;
    private final int tintColor;
    private final int displayTemperature; // °C
    private final int corrosivity;

    public BaseFluidType(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture,
                         ResourceLocation guiTexture, int tintColor, int displayTemperature, int corrosivity) {
        super(properties);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.guiTexture = guiTexture;
        this.tintColor = tintColor;
        this.displayTemperature = displayTemperature;
        this.corrosivity = corrosivity;
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return stillTexture; }
            @Override public ResourceLocation getFlowingTexture() { return flowingTexture; }
            @Override public int getTintColor() { return tintColor; }
            public ResourceLocation getRenderOverlayTexture(net.minecraft.world.entity.Entity viewer) { return null; }
        });
    }

    public int getDisplayTemperature() { return displayTemperature; }
    public int getCorrosivity() { return corrosivity; }
    public ResourceLocation getGuiTexture() { return guiTexture; }
    public int getTintColor() { return tintColor; }
    public ResourceLocation getStillTexture() { return stillTexture; }
    public ResourceLocation getFlowingTexture() { return flowingTexture; }
}