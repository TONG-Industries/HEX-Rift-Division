package com.trd.client.gecko.item.energy;

import com.trd.item.energy.EnergyCellItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class EnergyCellItemRenderer extends GeoItemRenderer<EnergyCellItem> {
    public EnergyCellItemRenderer() {
        super(new EnergyCellItemModel());
    }
}
