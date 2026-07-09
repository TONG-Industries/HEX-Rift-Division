package com.trd.item.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProtectorItem extends Item {
    private final int corrosion;
    private final int heat;
    private final int radiation; // оставлено для совместимости конструктора, но не используется

    public ProtectorItem(int corrosion, int heat, int radiation, Properties properties) {
        super(properties);
        this.corrosion = corrosion;
        this.heat = heat;
        this.radiation = radiation;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("  +" + corrosion + "°C к точке плавления").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  +" + heat + " к коррозионной стойкости").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§7Устанавливается в бочку"));
    }
}