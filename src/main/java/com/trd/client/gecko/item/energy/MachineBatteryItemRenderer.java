package com.trd.client.gecko.item.energy;

import com.trd.item.energy.MachineBatteryBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MachineBatteryItemRenderer extends GeoItemRenderer<MachineBatteryBlockItem> {
    public MachineBatteryItemRenderer() {
        super(new MachineBatteryItemModel());
    }
}
