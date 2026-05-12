package com.trd.client.gecko.item.ammo;


import com.trd.item.weapons.ammo.AmmoTurretItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class AmmoTurretRenderer extends GeoItemRenderer<AmmoTurretItem> {
    public AmmoTurretRenderer() {
        super(new AmmoTurretModel());
    }
}
