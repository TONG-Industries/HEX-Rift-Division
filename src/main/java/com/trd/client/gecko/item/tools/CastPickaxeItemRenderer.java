package com.trd.client.gecko.item.tools;

import com.trd.item.tools.cast_pickaxes.CastPickaxeItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class CastPickaxeItemRenderer extends GeoItemRenderer<CastPickaxeItem> {
    public CastPickaxeItemRenderer() {
        super(new CastPickaxeItemModel());
    }
}