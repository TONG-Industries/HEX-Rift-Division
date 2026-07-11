package com.trd.api.fuel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "trd", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FuelTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        int burnTime = 0;

        // Проверяем предметы мода
        for (var entry : ModFuels.ITEM_FUELS.entrySet()) {
            if (item == entry.getKey().get()) {
                burnTime = entry.getValue();
                break;
            }
        }

        // Если не нашли в предметах — проверяем блоки (через BlockItem)
        if (burnTime == 0 && item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            for (var entry : ModFuels.BLOCK_FUELS.entrySet()) {
                if (block == entry.getKey().get()) {
                    burnTime = entry.getValue();
                    break;
                }
            }
        }

        // Добавляем тултип ТОЛЬКО если это зарегистрированное топливо
        if (burnTime > 0) {
            String efficiency = ModFuels.getEfficiencyComparedToCoal(burnTime);
            event.getToolTip().add(
                    Component.literal("⛽ " + efficiency)
                            .withStyle(ChatFormatting.GOLD)
            );
        }
    }
}