package com.trd.event;

import com.trd.item.armor.GrenadierGogglesItem;
import com.trd.main.MainRegistry;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID)
public class GrenadierGogglesHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (helmet.getItem() instanceof GrenadierGogglesItem) {
                DamageSource source = event.getSource();
                if (source.is(DamageTypeTags.IS_EXPLOSION)) {
                    float originalDamage = event.getAmount();
                    float reducedDamage = originalDamage * 0.7f;
                    event.setAmount(reducedDamage);
                }
            }
        }
    }
}