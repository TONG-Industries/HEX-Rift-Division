package com.trd.client.gecko.item.armor;

import com.trd.item.armor.GrenadierGogglesItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class GrenadierGogglesRenderer extends GeoArmorRenderer<GrenadierGogglesItem> {

    public GrenadierGogglesRenderer() {
        super(new GrenadierGogglesModel());
    }

    @Override
    public RenderType getRenderType(GrenadierGogglesItem animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
