package com.trd.client.gecko.item.turrets;



import com.trd.item.weapons.turrets.TurretLightPlacerBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TurretLightPlacerItemRenderer extends GeoItemRenderer<TurretLightPlacerBlockItem> {
    public TurretLightPlacerItemRenderer() {
        super(new TurretLightPlacerItemModel()); // Используем правильную модель
    }
}
