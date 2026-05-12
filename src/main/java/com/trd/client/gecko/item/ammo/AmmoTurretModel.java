package com.trd.client.gecko.item.ammo;


import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import com.trd.item.weapons.ammo.AmmoTurretItem;
import com.trd.main.MainRegistry;
import software.bernie.geckolib.model.GeoModel;

// Используем базовый класс AmmoTurretItem как дженерик
public class AmmoTurretModel extends GeoModel<AmmoTurretItem> {

    @Override
    public ResourceLocation getModelResource(AmmoTurretItem object) {
        // Одна модель для всех коробок
        return new ResourceLocation(MainRegistry.MOD_ID, "geo/ammo_turret.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AmmoTurretItem object) {
        // 1. Получаем ID предмета (например "trd:ammo_turret_fire")
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(object);

        if (registryName != null) {
            String path = registryName.getPath(); // "ammo_turret_fire"

            // 2. Ищем текстуру с таким же именем в папке textures/item/
            // Пример: assets/trd/textures/item/ammo_turret_fire.png
            return new ResourceLocation(MainRegistry.MOD_ID, "textures/item/ammo/" + path + ".png");
        }

        // Фолбэк на дефолт
        return new ResourceLocation(MainRegistry.MOD_ID, "textures/item/ammo/ammo_turret.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AmmoTurretItem animatable) {
        return new ResourceLocation(MainRegistry.MOD_ID, "animations/ammo_turret.animation.json");
    }
}
